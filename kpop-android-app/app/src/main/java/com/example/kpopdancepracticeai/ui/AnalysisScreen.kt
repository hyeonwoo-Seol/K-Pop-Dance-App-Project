package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kpopdancepracticeai.KpopApplication
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.theme.*
import com.example.kpopdancepracticeai.viewmodel.AnalysisViewModel

// 히트맵 색상
private val HeatmapLevel0 = Color(0xfff1f5f9)
private val HeatmapLevel1 = Color(0xffa4f4cf)
private val HeatmapLevel2 = Color(0xff00d492)
private val HeatmapLevel3 = Color(0xff009966)
private val HeatmapLevel4 = Color(0xff006045)

@Composable
fun AnalysisScreen(
    paddingValues: PaddingValues,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as KpopApplication
    val repository = app.repository

    // [추가] ViewModel 주입
    val viewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModel.provideFactory(repository)
    )

    // [추가] 데이터 로드 (현재 로그인 유저 기준)
    LaunchedEffect(Unit) {
        val authRepo = AuthRepository(context)
        val currentUser = authRepo.getCurrentUser()
        if (currentUser != null) {
            viewModel.loadStatistics(currentUser.uid)
        }
    }

    // [추가] 상태 구독
    val uiState by viewModel.uiState.collectAsState()

    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
            .statusBarsPadding()
            .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextDark
                        )
                    }

                    Text(
                        text = "상세 통계",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            lineHeight = 36.sp
                        ),
                        color = TextDark,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // [수정] 실제 데이터 전달
            item { StatisticsOverviewSection(uiState) }
            item {
                GrowthGraphSection(
                    uiState = uiState,
                    onSelectChoreo = viewModel::selectChoreoFilter,
                    onRangeChange = viewModel::updateTrendRange
                )
            }
        }
    }
}

@Composable
fun StatisticsOverviewSection(uiState: AnalysisViewModel.StatisticsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "종합 통계")

        // [수정] ViewModel 데이터 바인딩
        StatInfoCard(
            icon = Icons.Default.AccessTime,
            label = "총 연습 시간",
            value = uiState.totalPlayTimeStr,
            iconBgColor = BgBlueLight,
            valueColor = PointBlue
        )
        StatInfoCard(
            icon = Icons.Default.MusicNote,
            label = "완료한 곡 / 파트",
            value = uiState.completedCountsStr,
            iconBgColor = BgPurpleLight,
            valueColor = PointPurple
        )
        StatInfoCard(
            icon = Icons.Default.CheckCircle,
            label = "전체 평균 정확도",
            value = uiState.avgAccuracyStr,
            iconBgColor = BgGreenLight,
            valueColor = PointGreen
        )
    }
}

@Composable
fun StatInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconBgColor: Color,
    valueColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(102.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    color = TextGray
                )
                Text(
                    text = value,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 24.sp
                    ),
                    color = valueColor
                )
            }
        }
    }
}

@Composable
fun GrowthGraphSection(
    uiState: AnalysisViewModel.StatisticsUiState,
    onSelectChoreo: (String) -> Unit,
    onRangeChange: (AnalysisViewModel.TrendRange) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "성장 그래프")
        HeatmapCard(uiState.heatmapData)
        AccuracyTrendCard(uiState.graphData, uiState.graphLabels)
        ChoreoInsightCard(uiState, onSelectChoreo, onRangeChange)
        // SongMasteryCard는 데이터가 있으면 표시
        if (uiState.graphData.isNotEmpty()) {
            SongMasteryCard(uiState.graphData, uiState.graphLabels)
        }
    }
}

@Composable
fun HeatmapCard(heatmapData: List<Int>) {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("연습 시간 캘린더", style = TextStyle(fontWeight = FontWeight(400), fontSize = 16.sp), color = TextDark)
            PracticeHeatmapGrid(heatmapData)
            HeatmapLegend()
        }
    }
}

@Composable
fun PracticeHeatmapGrid(data: List<Int>) {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    val weeks = 12

    // 데이터 개수 안전 처리
    val safeData = if (data.size >= 84) data else List(84) { 0 }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // 요일 라벨
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.height(136.dp)) {
            days.forEach { day -> Text(text = day, style = TextStyle(fontSize = 12.sp), color = TextLightGray) }
        }

        // 그리드
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(weeks) { weekIndex ->
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.height(136.dp)) {
                    repeat(7) { dayIndex ->
                        // 인덱스 계산: 데이터를 순서대로 배치 (단순화)
                        val dataIndex = weekIndex * 7 + dayIndex
                        val level = safeData.getOrElse(dataIndex) { 0 }

                        val color = when (level) {
                            0 -> HeatmapLevel0
                            1 -> HeatmapLevel1
                            2 -> HeatmapLevel2
                            3 -> HeatmapLevel3
                            else -> HeatmapLevel4
                        }
                        Box(modifier = Modifier.size(16.dp).background(color, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun HeatmapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("적음", fontSize = 12.sp, color = TextGray)
        listOf(HeatmapLevel0, HeatmapLevel1, HeatmapLevel2, HeatmapLevel3, HeatmapLevel4).forEach { color -> Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp))) }
        Text("많음", fontSize = 12.sp, color = TextGray)
    }
}

@Composable
fun AccuracyTrendCard(dataPoints: List<Float>, labels: List<String>) {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("평균 정확도 추이", style = TextStyle(fontWeight = FontWeight(400), fontSize = 16.sp), color = TextDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ToggleButton("최근", true); ToggleButton("전체", false) }
            }
            if (dataPoints.isNotEmpty()) {
                SimpleLineChart(dataPoints, labels, PointBlue, 0.0f)
            } else {
                Text("아직 데이터가 충분하지 않습니다.", color = TextGray, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreoInsightCard(
    uiState: AnalysisViewModel.StatisticsUiState,
    onSelectChoreo: (String) -> Unit,
    onRangeChange: (AnalysisViewModel.TrendRange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = uiState.choreoOptions.firstOrNull { it.key == uiState.selectedChoreoKey }

    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("안무별 상세 통계", style = TextStyle(fontWeight = FontWeight(500), fontSize = 16.sp), color = TextDark)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedOption?.label ?: "안무를 선택하세요",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "안무 선택")
                    }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.choreoOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onSelectChoreo(option.key)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterToggleButton(
                    text = "최근 7일",
                    isSelected = uiState.selectedRange == AnalysisViewModel.TrendRange.RECENT_7_DAYS,
                    onClick = { onRangeChange(AnalysisViewModel.TrendRange.RECENT_7_DAYS) }
                )
                FilterToggleButton(
                    text = "전체(1달)",
                    isSelected = uiState.selectedRange == AnalysisViewModel.TrendRange.FULL_1_MONTH,
                    onClick = { onRangeChange(AnalysisViewModel.TrendRange.FULL_1_MONTH) }
                )
            }

            if (uiState.filteredTrendData.isNotEmpty()) {
                SimpleLineChart(uiState.filteredTrendData, uiState.filteredTrendLabels, PointBlue, 0.0f)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightStat("최고 등급", uiState.bestGrade)
                InsightStat("연습 횟수", "${uiState.filteredPracticeCount}회")
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("누적 worst points TOP 3", color = TextGray, fontSize = 13.sp)
                if (uiState.topWorstPoints.isEmpty()) {
                    Text("데이터가 없습니다.", color = TextLightGray, fontSize = 12.sp)
                } else {
                    uiState.topWorstPoints.forEachIndexed { index, pair ->
                        Text("${index + 1}. ${pair.first} (${pair.second}회)", color = TextDark, fontSize = 13.sp)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("가장 많이 틀린 관절 구간", color = TextGray, fontSize = 13.sp)
                Text(uiState.mostErrorJointSummary, color = TextDark, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun FilterToggleButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PointBlue else Color(0xffe2e8f0),
            contentColor = if (isSelected) Color.White else Color(0xff314158)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 13.sp)
    }
}

@Composable
fun InsightStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = TextGray, fontSize = 12.sp)
        Text(value, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ToggleButton(text: String, isSelected: Boolean) {
    Box(modifier = Modifier.background(if (isSelected) PointBlue else Color(0xffe2e8f0), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = if (isSelected) Color.White else Color(0xff314158), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SongMasteryCard(dataPoints: List<Float>, labels: List<String>) {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("최근 연습 점수", style = TextStyle(fontWeight = FontWeight(400), fontSize = 16.sp), color = TextDark)
            }
            if (dataPoints.isNotEmpty()) {
                SimpleLineChart(dataPoints, labels, Color(0xff8b5cf6), 0.0f)
            } else {
                Text("아직 데이터가 충분하지 않습니다.", color = TextGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SimpleLineChart(dataPoints: List<Float>, labels: List<String>, lineColor: Color, minY: Float = 0.0f, maxY: Float = 1.0f) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val xStep = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width
                val yRange = maxY - minY

                // 가로선 그리기
                for (i in 0..5) {
                    val y = height - ((i.toFloat() / 5) * height)
                    drawLine(
                        color = Color(0xffe5e7eb),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                val path = Path()
                dataPoints.forEachIndexed { index, value ->
                    // 값이 범위 내에 있도록 클램핑
                    val clampedValue = value.coerceIn(minY, maxY)
                    val x = index * xStep
                    val y = height - (((clampedValue - minY) / yRange) * height)

                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

                dataPoints.forEachIndexed { index, value ->
                    val clampedValue = value.coerceIn(minY, maxY)
                    val x = index * xStep
                    val y = height - (((clampedValue - minY) / yRange) * height)
                    drawCircle(Color.White, 5.dp.toPx(), Offset(x, y))
                    drawCircle(lineColor, 5.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
                }
            }
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                listOf("100%", "80%", "60%", "40%", "20%", "0%").forEach { Text(it, fontSize = 10.sp, color = TextLightGray) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, fontSize = 10.sp, color = TextLightGray) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalysisScreenPreview(){
    KpopDancePracticeAITheme {
        AnalysisScreen(
            PaddingValues(),
            onBackClick = {}
        )
    }
}
