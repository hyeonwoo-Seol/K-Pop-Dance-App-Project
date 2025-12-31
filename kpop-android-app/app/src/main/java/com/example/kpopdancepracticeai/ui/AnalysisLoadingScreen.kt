package com.example.kpopdancepracticeai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.*
import kotlinx.coroutines.delay

// 분석 단계 정의
enum class AnalysisStage(val message: String, val progress: Float) {
    UPLOADING("영상을 클라우드로 전송 중...", 0.2f),
    SKELETON_EXTRACT("AI가 관절 포인트 추출 중...", 0.5f),
    COMPARING("전문가 댄서와 동작 비교 중...", 0.8f),
    SCORING("최종 점수 산출 중...", 0.95f),
    COMPLETED("분석 완료!", 1.0f)
}

@Composable
fun AnalysisWaitingScreen(
    onAnalysisComplete: () -> Unit = {}
) {
    // 배경 그라데이션
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF), // 상단 연한 파랑
            Color(0xFFF0E8FF)  // 하단 연한 보라
        )
    )

    // 상태 관리
    var currentStage by remember { mutableStateOf(AnalysisStage.UPLOADING) }

    // 단계별 진행 시뮬레이션 (실제 앱에서는 API 응답에 따라 상태 변경)
    LaunchedEffect(Unit) {
        delay(1500) // 업로드 시뮬레이션
        currentStage = AnalysisStage.SKELETON_EXTRACT
        delay(2000) // 스켈레톤 추출 시뮬레이션
        currentStage = AnalysisStage.COMPARING
        delay(2000) // 비교 시뮬레이션
        currentStage = AnalysisStage.SCORING
        delay(1000) // 점수 산출 시뮬레이션
        currentStage = AnalysisStage.COMPLETED
        delay(500)  // 완료 후 잠시 대기
        onAnalysisComplete() // 결과 화면으로 이동
    }

    // 펄스 애니메이션 (중앙 로고용)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // 1. 중앙 AI 분석 비주얼
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // 뒤쪽 퍼지는 원 (장식)
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                // 중앙 원
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { currentStage.progress },
                            modifier = Modifier.size(120.dp),
                            color = Color(0xFF9378F3), // Theme.kt의 포인트 컬러
                            trackColor = Color(0xFFE0E0E0),
                            strokeWidth = 9.dp,
                            strokeCap = StrokeCap.Round,
                        )
                        Text(
                            text = "${(currentStage.progress * 100).toInt()}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PointPurple
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. 상태 메시지 (애니메이션 효과를 주면 더 좋음)
            Text(
                text = currentStage.message,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B), // Dark Text
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 팁 영역 (사용자가 지루하지 않게)
            TipCard()
        }
    }
}

@Composable
fun TipCard() {
    val tips = listOf(
        "💡 Tip: 팔을 쭉 뻗을수록 AI가 더 정확하게 인식해요!",
        "💡 Tip: 조명이 밝은 곳에서 촬영하면 점수가 더 정확하게 나옵니다.",
        "💡 Tip: 헐렁한 옷보다는 몸의 라인이 보이는 옷이 좋아요."
    )
    // 팁을 주기적으로 변경
    var currentTipIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentTipIndex = (currentTipIndex + 1) % tips.size
        }
    }

    Surface(
        color = Color.White.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = tips[currentTipIndex],
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF45556C), // TextGray
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AnalysisWaitingScreenPreview() {
    KpopDancePracticeAITheme {
        AnalysisWaitingScreen()
    }
}