package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.*

// 히트맵 색상 (이 파일 전용)
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
            //  시스템 상태 표시줄과 겹치지 않게 패딩 추가
            .statusBarsPadding()
            // 하단 네비게이션 바 높이만큼 패딩 처리
            .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            //  뒤로가기 버튼과 타이틀을 Box로 묶어서 배치 (타이틀 중앙 정렬, 버튼 좌측 정렬)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp) // 상단 여백
                ) {
                    // 뒤로가기 버튼 (좌측 정렬)
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(24.dp) // 버튼 크기 조절 (필요 시)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextDark // 아이콘 색상
                        )
                    }

                    // 타이틀 텍스트 (중앙 정렬)
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

            item { StatisticsOverviewSection() }
            item { GrowthGraphSection() }
        }
    }
}

@Composable
fun StatisticsOverviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "종합 통계") // Components.kt의 SectionTitle 사용

        StatInfoCard(
            icon = Icons.Default.AccessTime,
            label = "총 연습 시간",
            value = "247시간 32분",
            iconBgColor = BgBlueLight,
            valueColor = PointBlue
        )
        StatInfoCard(
            icon = Icons.Default.MusicNote,
            label = "완료한 곡 / 파트",
            value = "42곡 / 156파트",
            iconBgColor = BgPurpleLight,
            valueColor = PointPurple
        )
        StatInfoCard(
            icon = Icons.Default.CheckCircle,
            label = "전체 평균 정확도",
            value = "94.7%",
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
fun GrowthGraphSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "성장 그래프") // Components.kt의 SectionTitle 사용
        HeatmapCard()
        AccuracyTrendCard()
        SongMasteryCard()
    }
}

@Composable
fun HeatmapCard() {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("연습 시간 캘린더", style = TextStyle(fontWeight = FontWeight(400), fontSize = 16.sp), color = TextDark)
            PracticeHeatmapGrid()
            HeatmapLegend()
        }
    }
}

@Composable
fun PracticeHeatmapGrid() {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    val weeks = 12
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.height(136.dp)) {
            days.forEach { day -> Text(text = day, style = TextStyle(fontSize = 12.sp), color = TextLightGray) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(weeks) {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.height(136.dp)) {
                    repeat(7) {
                        val color = when ((0..10).random()) { 0, 1, 2 -> HeatmapLevel0; 3, 4 -> HeatmapLevel1; 5, 6 -> HeatmapLevel2; 7, 8 -> HeatmapLevel3; else -> HeatmapLevel4 }
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
fun AccuracyTrendCard() {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("평균 정확도 추이", style = TextStyle(fontWeight = FontWeight(400), fontSize = 16.sp), color = TextDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ToggleButton("주간", true); ToggleButton("월간", false) }
            }
            SimpleLineChart(listOf(0.92f, 0.93f, 0.91f, 0.94f, 0.95f, 0.94f, 0.96f), listOf("11/23", "11/24", "11/25", "11/26", "11/27", "11/28", "11/29"), PointBlue, 0.8f)
        }
    }
}

@Composable
fun ToggleButton(text: String, isSelected: Boolean) {
    Box(modifier = Modifier.background(if (isSelected) PointBlue else Color(0xffe2e8f0), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = if (isSelected) Color.White else Color(0xff314158), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SongMasteryCard() {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("곡별 마스터리", style = TextStyle(fontWeight = FontWeight(400), fontSize = 16.sp), color = TextDark)
                Surface(shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xffcad5e2)), modifier = Modifier.width(140.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Spring Day", fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("BTS", fontSize = 10.sp, color = TextLightGray) }
                        Icon(Icons.Default.ArrowDropDown, null, tint = TextLightGray)
                    }
                }
            }
            SimpleLineChart(listOf(0.78f, 0.81f, 0.83f, 0.84f, 0.86f, 0.88f, 0.89f), listOf("10/15", "10/20", "10/25", "10/30", "11/04", "11/09", "11/14"), Color(0xff8b5cf6), 0.6f)
        }
    }
}

@Composable
fun SimpleLineChart(dataPoints: List<Float>, labels: List<String>, lineColor: Color, minY: Float = 0.0f, maxY: Float = 1.0f) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width; val height = size.height; val xStep = width / (dataPoints.size - 1); val yRange = maxY - minY
                for (i in 0..5) {
                    val y = height - ((i.toFloat() / 5) * height)
                    // ⭐️ [오류 수정] drawLine의 매개변수 이름을 명시하여 전달합니다.
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
                    val x = index * xStep; val y = height - (((value - minY) / yRange) * height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                dataPoints.forEachIndexed { index, value ->
                    val x = index * xStep; val y = height - (((value - minY) / yRange) * height)
                    drawCircle(Color.White, 5.dp.toPx(), Offset(x, y)); drawCircle(lineColor, 5.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
                }
            }
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                listOf("100%", "90%", "80%", "70%", "60%", "50%").forEach { Text(it, fontSize = 10.sp, color = TextLightGray) }
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