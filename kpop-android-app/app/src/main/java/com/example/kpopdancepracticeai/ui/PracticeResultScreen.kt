package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.*

private data class PracticeResultData(
    val title: String,
    val accuracy: Int,
    val accuracyChange: Int,
    val experienceGained: Int,
    val nextLevelXpNeeded: Int,
    val newRecord: Boolean,
    val previousRecord: Int,
    val avgAngleError: Float,
    val avgTimingError: Float,
    val mistakeJoints: List<Triple<String, Int, String>>,
    val songId: String
)

private val dummyResultData = PracticeResultData(
    title = "Dynamite - Part 2: 메인 파트",
    accuracy = 85,
    accuracyChange = 5,
    experienceGained = 1250,
    nextLevelXpNeeded = 750,
    newRecord = true,
    previousRecord = 80,
    avgAngleError = 10.5f,
    avgTimingError = 0.3f,
    mistakeJoints = listOf(
        Triple("왼쪽 팔", 1, "28%"),
        Triple("오른쪽 다리", 2, "22%"),
        Triple("허리 회전", 3, "18%"),
    ),
    songId = "1"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeResultScreen(
    onBackClick: () -> Unit = {},
    onCompareClick: () -> Unit = {},
    onRetryClick: (songId: String) -> Unit = { },
    onNextPartClick: (songId: String) -> Unit = { }
) {
    val result = dummyResultData
    val appGradient = Brush.verticalGradient(colors = listOf(Color(0xFFDDE3FF), Color(0xFFF0E8FF)))

    Box(modifier = Modifier.fillMaxSize().background(appGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("춤 연습 결과 화면", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBackClick) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(contentPadding = innerPadding, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                item { Spacer(modifier = Modifier.height(16.dp)); ResultHeader(result.title) }
                item { CoreFeedbackCard(result) }
                item { CompareButtonCard(onCompareClick) }
                item { DetailedAnalysisCard(result) }
                item { NextStepCard({ onRetryClick(result.songId) }, { onNextPartClick(result.songId) }) }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun ResultHeader(songTitle: String) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text("연습 완료!", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 36.sp), color = PointPurple)
        Text("$songTitle 에 대한 결과입니다. 수고하셨습니다.", style = TextStyle(fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp), color = TextGray)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CoreFeedbackCard(result: PracticeResultData) {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionTitle("핵심 피드백")
            SectionItem(Icons.Default.BarChart, "종합 정확도") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${result.accuracy}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PointPurple)
                        if (result.accuracyChange > 0) BadgeChip("+${result.accuracyChange}%", BgGreenLight, PointGreen)
                    }
                    LinearProgressIndicator(progress = { result.accuracy / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color.Black, trackColor = Color(0x33030213))
                }
            }
            SectionItem(Icons.Default.Star, "획득 경험치") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+${result.experienceGained} XP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PointPurple)
                    val progress = (result.experienceGained / (result.experienceGained + result.nextLevelXpNeeded).toFloat()).coerceIn(0f, 1f)
                    Text("다음 레벨까지 ${result.nextLevelXpNeeded} XP", style = MaterialTheme.typography.bodyMedium, color = TextLightGray)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color.Black, trackColor = Color(0x33030213))
                }
            }
            SectionItem(Icons.Default.MusicNote, "파트 최고 기록") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (result.newRecord) BadgeChip("🎉 NEW RECORD!", Color(0xFFFFD180), Color.Black)
                    Text("이전 기록: ${result.previousRecord}%", style = MaterialTheme.typography.bodyMedium, color = TextLightGray)
                    Text("신기록: ${result.accuracy}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PointPurple)
                }
            }
        }
    }
}

@Composable
fun CompareButtonCard(onClick: () -> Unit) {
    CardContainer(modifier = Modifier.clickable(onClick = onClick)) {
        NextStepButton("내 동작과 원본 비교하기", onClick, Icons.Outlined.Videocam, Color.White)
    }
}

@Composable
private fun DetailedAnalysisCard(result: PracticeResultData) {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionTitle("상세 분석")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("구간별 오차 그래프", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, BorderLight, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("구간별 오차 그래프 (Placeholder)", color = Color.Gray) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StatItemWithBackground("평균 오차 각도", "${result.avgAngleError}°", BgPurpleLight, PointPurple)
                StatItemWithBackground("평균 타이밍 오차", "±${result.avgTimingError}초", BgBlueLight, PointBlue)
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("주요 실수 부위 Top3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                result.mistakeJoints.forEach { (joint, rank, errorRate) -> MistakeJointItem(rank, joint, errorRate) }
            }
        }
    }
}

@Composable
fun MistakeJointItem(rank: Int, joint: String, errorRate: String) {
    val rankColor = when (rank) { 1 -> PointYellow; 2 -> Color(0xffd1d5dc); 3 -> Color(0xffe17100); else -> Color.Gray }
    Row(modifier = Modifier.fillMaxWidth().background(Color(0xfff9fafb), RoundedCornerShape(10.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(color = rankColor, shape = _root_ide_package_.androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { Text(rank.toString(), color = if (rank == 2) Color(0xff364153) else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Text(joint, style = MaterialTheme.typography.titleMedium, color = Color(0xff364153))
        }
        BadgeChip("오차율 $errorRate", BgRedLight, Color(0xffe7000b), Color(0xffffa2a2))
    }
}

@Composable
fun NextStepCard(onRetryClick: () -> Unit, onNextPartClick: () -> Unit) {
    CardContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle("다음 단계")
            // 흰색 버튼 (다시 연습하기) - 테두리는 내부에서 처리됨 (컨테이너가 White일 때)
            NextStepButton("다시 연습하기", onRetryClick, Icons.Default.Refresh, Color.White)
            // 검은색 버튼 (다음 파트)
            NextStepButton("다음 파트 도전", onNextPartClick, Icons.Default.PlayArrow, Color.Black, Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PracticeResultScreenPreview() {
    KpopDancePracticeAITheme { PracticeResultScreen() }
}