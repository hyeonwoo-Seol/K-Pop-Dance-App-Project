package com.example.kpopdancepracticeai.ui

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kpopdancepracticeai.data.dto.FrameData
import com.example.kpopdancepracticeai.util.DataConverter
import com.example.kpopdancepracticeai.util.JsonResultLoader
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.abs

/**
 * [Step 3] 연습 결과 화면 (오버레이 통합)
 * - 내부 저장소의 JSON을 읽어 비디오 위에 Skeleton Overlay를 표시합니다.
 * - JSON 파싱 및 오버레이 구현을 우선시하며, DB 저장은 별도로 처리합니다.
 */
@OptIn(UnstableApi::class)
@Composable
fun PracticeResultScreen(
    jsonFileName: String, // 내부 저장소 파일명 (예: "user_song_part_timestamp.json")
    videoPath: String,    // 사용자 녹화 영상 경로 (String Uri)
    score: Int = 0,       // (선택) 점수, JSON에서 파싱 가능하므로 0 기본값
    viewModel: MainViewModel = viewModel(),
    onBackClick: () -> Unit,
    onReplayClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val context = LocalContext.current

    // 1. 전체 분석 데이터 상태
    var allFrames by remember { mutableStateOf<List<FrameData>>(emptyList()) }
    var totalScore by remember { mutableIntStateOf(score) } // 점수 상태
    var accuracyGrade by remember { mutableStateOf("-") } // 등급 상태

    // 2. 현재 보여줄 오버레이 데이터 상태
    var currentKeyPoints by remember { mutableStateOf<List<KeyPoint>>(emptyList()) }
    var currentErrors by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 3. 비디오 플레이어 상태
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    var isPlaying by remember { mutableStateOf(false) }

    // 플레이어 리스너 등록
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 4. JSON 파일 로드 (비동기) & 비디오 설정
    LaunchedEffect(jsonFileName) {
        if (jsonFileName.isNotBlank()) {
            val result = JsonResultLoader.loadAnalysisResult(context, jsonFileName)
            if (result != null) {
                // 타임스탬프 순으로 정렬하여 저장
                allFrames = result.frames.sortedBy { it.timestamp }

                // 요약 정보 업데이트
                totalScore = result.summary.totalScore
                accuracyGrade = result.summary.accuracyGrade
            }
        }
    }

    // 비디오 로드
    LaunchedEffect(videoPath) {
        if (videoPath.isNotBlank()) {
            val uri = Uri.parse(videoPath)
            // 파일 경로인 경우 처리
            val mediaItem = if (videoPath.startsWith("/")) {
                MediaItem.fromUri(Uri.fromFile(File(videoPath)))
            } else {
                MediaItem.fromUri(uri)
            }
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true // 자동 재생
        }
    }

    // 5. 플레이어 시간 감지 및 오버레이 업데이트 루프 (싱크 맞춤)
    LaunchedEffect(exoPlayer, isPlaying, allFrames) {
        if (allFrames.isNotEmpty()) {
            while (isActive) {
                if (isPlaying) {
                    val currentSec = exoPlayer.currentPosition / 1000.0 // 밀리초 -> 초 변환

                    // 현재 시간과 가장 가까운 프레임 찾기 (오차 범위 0.1초 내외)
                    val targetFrame = allFrames.minByOrNull { abs(it.timestamp - currentSec) }

                    if (targetFrame != null && abs(targetFrame.timestamp - currentSec) < 0.1) {
                        // 데이터 변환 및 상태 업데이트 -> SkeletonOverlay 리컴포지션
                        currentKeyPoints = DataConverter.convertToKeyPoints(targetFrame)
                        currentErrors = targetFrame.errors
                    } else {
                        // 매칭되는 프레임이 없으면 오버레이 숨김
                        currentKeyPoints = emptyList()
                    }
                }
                delay(33) // 약 30fps 주기로 갱신
            }
        }
    }

    Scaffold(
        containerColor = Color.Black // 영상 몰입을 위해 배경 검정
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // [상단] 영상 및 오버레이 영역 (1:1 비율 또는 weight)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                // A. 비디오 플레이어
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true // 기본 컨트롤러 사용
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // B. 스켈레톤 오버레이 (비디오 위에 겹쳐서 표시)
                if (currentKeyPoints.isNotEmpty()) {
                    SkeletonOverlay(
                        keyPoints = currentKeyPoints,
                        errors = currentErrors,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // [하단] 결과 정보 및 버튼 영역
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("연습 결과", style = MaterialTheme.typography.titleMedium)

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$totalScore",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " 점",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = "등급: $accuracyGrade",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onReplayClick, // 보통은 영상 다시보기 (seekTo(0))
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("다시 보기")
                        }
                        Button(
                            onClick = onHomeClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("홈으로")
                        }
                    }
                }
            }
        }
    }
}