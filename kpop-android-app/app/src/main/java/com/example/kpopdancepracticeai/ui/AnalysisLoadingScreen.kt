package com.example.kpopdancepracticeai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.border

@Composable
fun AnalysisLoadingScreen(onAnalysisComplete: () -> Unit
) {
    // 배경 그라데이션
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    // 점(.) 애니메이션 상태 관리
    var dotCount by remember { mutableIntStateOf(0) }

    // 0.5초마다 점 개수 변경
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // 0.5초 대기
            dotCount = (dotCount + 1) % 4
        }
    }

    // 3초 뒤 화면 이동 (테스트용)
    LaunchedEffect(Unit) {
        delay(3000)
        onAnalysisComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoadingSpinner()

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "AI가 춤을 분석하고 있어요",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6C5CE7)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val dots = ".".repeat(dotCount) // dotCount만큼 점 찍기
            Text(
                text = "잠시만 기다려 주세요$dots",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
@Composable
fun LoadingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "angle"
    )

    // 간단한 원형 로딩바 (커스텀)
    Box(
        modifier = Modifier
            .size(80.dp)
            .border(
                width = 8.dp,
                color = Color(0xFFE0E0E0), // 회색 트랙
                shape = CircleShape
            )
            .border(
                width = 8.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, Color(0xFF6C5CE7)) // 돌아가는 보라색
                ),
                shape = CircleShape
            )
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AnalysisLoadingScreenPreview() {
    com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme {
        AnalysisLoadingScreen(
            onAnalysisComplete = {}
        )
    }
}