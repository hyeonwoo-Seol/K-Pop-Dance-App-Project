package com.example.kpopdancepracticeai.ui.test

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.dto.FrameData
import com.example.kpopdancepracticeai.data.mapper.AnalysisMapper
import com.example.kpopdancepracticeai.ui.SkeletonOverlay
import com.example.kpopdancepracticeai.util.FilenameParser
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

/**
 * [영상 오버레이 통합 테스트 화면 - 정밀 보정 기능 추가]
 * 기능:
 * 1. 파일명 파싱 및 데이터 로드
 * 2. 영상 재생 및 싱크 맞춤
 * 3. [정밀 보정] Scale/Offset 슬라이더를 통해 오버레이 위치 미세 조정 가능
 */
@OptIn(UnstableApi::class)
@Composable
fun IntegrationTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- 상태 변수 ---
    var selectedPartNumber by remember { mutableStateOf(1) }

    // 로그 및 진행 상태
    var logText by remember { mutableStateOf("상단의 파트 번호를 선택하고 [Step 1]을 눌러주세요.") }
    var isDataReady by remember { mutableStateOf(false) }

    // 영상 플레이어 상태
    var isVideoReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentVideoTime by remember { mutableStateOf(0L) }

    // 데이터
    var allFrames by remember { mutableStateOf<List<FrameData>>(emptyList()) }
    var currentKeyPoints by remember { mutableStateOf<List<com.example.kpopdancepracticeai.ui.KeyPoint>>(emptyList()) }
    var currentErrors by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 영상 비율 정보
    var videoWidth by remember { mutableStateOf(1080) }
    var videoHeight by remember { mutableStateOf(1920) }

    // ⭐️ [보정 변수] 슬라이더로 조절할 값들
    var scaleX by remember { mutableStateOf(1f) }
    var scaleY by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showCalibration by remember { mutableStateOf(true) } // 보정 패널 표시 여부

    // ExoPlayer 초기화
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingState: Boolean) {
                    isPlaying = isPlayingState
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isVideoReady = true
                    }
                }
            })
        }
    }

    // --- Step 1: 데이터 로드 ---
    fun loadAndParseData() {
        scope.launch {
            try {
                isDataReady = false
                isVideoReady = false
                isPlaying = false
                exoPlayer.stop()
                exoPlayer.clearMediaItems()

                logText = "▶ [Step 1] Part $selectedPartNumber 데이터 로드 시작...\n"

                val assetManager = context.assets
                val allFiles = assetManager.list("") ?: emptyArray()

                // 파일 찾기
                val targetJson = allFiles.find { it.endsWith(".json") && it.contains("_$selectedPartNumber") }
                val targetVideo = allFiles.find { it.endsWith(".mp4") && it.contains("_$selectedPartNumber") }
                    ?: allFiles.find { it.endsWith(".mp4") && it.contains("$selectedPartNumber") }

                if (targetJson == null || targetVideo == null) {
                    logText += "❌ 파일 매칭 실패. (JSON: $targetJson, Video: $targetVideo)\n"
                    return@launch
                }

                // JSON 파싱
                val jsonString = assetManager.open(targetJson).use {
                    InputStreamReader(it).use { reader -> BufferedReader(reader).readText() }
                }
                val response = Gson().fromJson(jsonString, AnalysisResultResponse::class.java)

                // 해상도 정보 업데이트 및 초기 스케일 계산
                if (response.metadata.videoWidth > 0 && response.metadata.videoHeight > 0) {
                    videoWidth = response.metadata.videoWidth
                    videoHeight = response.metadata.videoHeight

                    // ⭐️ 자동 보정: 세로 영상(9:16)일 경우 X축 스케일을 1.77배(1920/1080)로 자동 설정
                    if (videoWidth < videoHeight) {
                        scaleX = videoHeight.toFloat() / videoWidth.toFloat()
                        logText += "ℹ️ 세로 영상 감지: X축 스케일 자동 보정 (${String.format("%.2f", scaleX)})\n"
                    } else {
                        scaleX = 1f
                    }
                    scaleY = 1f
                    offsetX = 0f
                    offsetY = 0f
                }

                allFrames = response.frames.sortedBy { it.timestamp }
                logText += "✅ 데이터 로드 완료 (${allFrames.size} 프레임)\n"

                // 영상 로드
                val videoUri = Uri.parse("file:///android_asset/$targetVideo")
                val mediaItem = MediaItem.fromUri(videoUri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()

                isDataReady = true
                logText += "\n🎉 준비 완료! 재생 버튼을 누르고 아래 슬라이더로 위치를 미세 조정하세요."

            } catch (e: Exception) {
                e.printStackTrace()
                logText += "❌ 에러 발생: ${e.message}\n"
            }
        }
    }

    // --- 실시간 오버레이 업데이트 ---
    LaunchedEffect(isPlaying, isVideoReady, scaleX, scaleY, offsetX, offsetY) {
        if (isVideoReady && isPlaying) {
            while (isActive) {
                val currentMs = exoPlayer.currentPosition
                currentVideoTime = currentMs
                val currentSec = currentMs / 1000.0

                val targetFrame = allFrames.minByOrNull { abs(it.timestamp - currentSec) }

                if (targetFrame != null && abs(targetFrame.timestamp - currentSec) < 0.1) {
                    if (targetFrame.keypoints.isNotEmpty()) {
                        val rawPoints = DataConverter.convertToKeyPoints(targetFrame)

                        // ⭐️ [실시간 보정 적용 - 중심 기준 스케일링]
                        // (좌표 - 0.5) * Scale + 0.5 + Offset
                        // 이렇게 하면 0.5(중앙)를 기준으로 커졌다 작아졌다 합니다.
                        currentKeyPoints = rawPoints.map {
                            it.copy(
                                x = ((it.x - 0.5f) * scaleX) + 0.5f + offsetX,
                                y = ((it.y - 0.5f) * scaleY) + 0.5f + offsetY
                            )
                        }

                        currentErrors = targetFrame.errors
                    } else {
                        currentKeyPoints = emptyList()
                    }
                }
                delay(16) // 60fps
            }
        }
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("시스템 통합 테스트 (정밀 보정)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // 파트 선택
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { id ->
                Button(
                    onClick = { selectedPartNumber = id; isDataReady = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPartNumber == id) Color(0xFF6200EE) else Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text("$id") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { loadAndParseData() }, modifier = Modifier.fillMaxWidth()) {
            Text("Step 1: 데이터 로드")
        }

        // 로그창
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = logText, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 영상 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoWidth.toFloat() / videoHeight.toFloat())
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            if (isDataReady) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (currentKeyPoints.isNotEmpty()) {
                    SkeletonOverlay(
                        keyPoints = currentKeyPoints,
                        errors = currentErrors,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 컨트롤러
                IconButton(
                    onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier.align(Alignment.Center).size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                Text("데이터 로드 대기 중...", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            }
        }

        // ⭐️ [정밀 보정 컨트롤러]
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showCalibration = !showCalibration },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = Color(0xFF6200EE))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("오버레이 위치 미세 조정", fontWeight = FontWeight.Bold)
                    }
                    Switch(checked = showCalibration, onCheckedChange = { showCalibration = it })
                }

                if (showCalibration) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // X Scale
                    Text("가로 비율 (X Scale): ${String.format("%.2f", scaleX)}", fontSize = 12.sp)
                    Slider(
                        value = scaleX,
                        onValueChange = { scaleX = it },
                        valueRange = 0.5f..2.5f
                    )

                    // X Offset
                    Text("가로 위치 (X Offset): ${String.format("%.2f", offsetX)}", fontSize = 12.sp)
                    Slider(
                        value = offsetX,
                        onValueChange = { offsetX = it },
                        valueRange = -0.5f..0.5f
                    )

                    // Y Offset (높이 조절이 필요할 경우)
                    Text("세로 위치 (Y Offset): ${String.format("%.2f", offsetY)}", fontSize = 12.sp)
                    Slider(
                        value = offsetY,
                        onValueChange = { offsetY = it },
                        valueRange = -0.2f..0.2f
                    )

                    // 초기화 버튼
                    Button(
                        onClick = {
                            // 초기화 로직
                            if (videoWidth < videoHeight) scaleX = videoHeight.toFloat() / videoWidth.toFloat() else scaleX = 1f
                            scaleY = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text("값 초기화")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
}