package com.example.kpopdancepracticeai.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kpopdancepracticeai.data.PresignedUrlUploader
import com.example.kpopdancepracticeai.util.NetworkUtils
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PracticeScreenTablet(
    songTitle: String = "Dynamite",
    artistPart: String = "BTS · Part 2: 메인 파트",
    difficulty: String = "보통",
    length: String = "2:15",
    videoUrl: String = "",
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRecordingComplete: (String) -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val uploader = remember { PresignedUrlUploader(context) }

    // 권한 상태 관리
    var hasPermissions by remember {
        mutableStateOf(
            if (isPreview) true else
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms -> hasPermissions = perms.values.all { it } }
    )

    LaunchedEffect(Unit) {
        if (!hasPermissions && !isPreview) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // CameraX 상태
    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recordingState = remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }

    LaunchedEffect(settings.isFrontCamera) {
        lensFacing = if (settings.isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    }

    // UI 상태
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(true) }


    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(context, "영상 재생 에러: ${error.message}\n경로를 확인하세요.", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            val finalUrl = if (!videoUrl.startsWith("asset:///")) {
                videoUrl.replace("file:///android_asset/", "asset:///")
            } else {
                videoUrl
            }

            try {
                exoPlayer.setMediaItem(MediaItem.fromUri(finalUrl.toUri()))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                isPlaying = true
            } catch (e: Exception) {
                Log.e("PracticeTablet", "Video load failed", e)
            }
        } else if (!isPreview) {
            Toast.makeText(context, "오류: 영상 주소가 비어있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(selectedSpeed) {
        exoPlayer.playbackParameters = exoPlayer.playbackParameters.withSpeed(selectedSpeed)
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            totalDurationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            kotlinx.coroutines.delay(100)
        }
    }

    val formatTime = { ms: Long ->
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    val currentTimeStr = formatTime(currentPositionMs)
    val totalTimeStr = if (totalDurationMs > 0) formatTime(totalDurationMs) else length
    val sliderPosition = if (totalDurationMs > 0) {
        currentPositionMs.toFloat() / totalDurationMs.toFloat()
    } else {
        0f
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }

    // 녹화 토글 함수 (중앙 버튼 및 하단 버튼 공용)
    val toggleRecording = {
        if (isRecording) {
            recordingState.value?.stop()
            recordingState.value = null
            isRecording = false
        } else {
            val videoCapture = videoCaptureState.value
            if (videoCapture != null) {
                isRecording = true
                //noinspection SpellCheckingInspection
                val name = "KPop_Tablet_${System.currentTimeMillis()}.mp4"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    //noinspection SpellCheckingInspection
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/KPopDancePractice")
                }
                val mediaStoreOutputOptions = MediaStoreOutputOptions
                    .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    .setContentValues(contentValues)
                    .build()

                recordingState.value = videoCapture.output
                    .prepareRecording(context, mediaStoreOutputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            if (!event.hasError()) {
                                val uri = event.outputResults.outputUri
                                
                                // [파일명 형식 생성] RecordScreenMobile.kt와 동일
                                //noinspection SpellCheckingInspection
                                val userId = "xooyong"
                                val songIdClean = songTitle.replace(" ", "").replace("_", "")
                                
                                // artistPart 파싱: "BTS · Part 2: 메인 파트"
                                val parts = artistPart.split("·")
                                val partInfo = parts.getOrNull(1)?.trim() ?: artistPart
                                val partNum = partInfo.filter { it.isDigit() }.ifEmpty { "0" }
                                val partName = partInfo.split(":").lastOrNull()?.replace(" ", "")?.replace("_", "") ?: "None"
                                val timestamp = System.currentTimeMillis()

                                val filename = "${userId}_${songIdClean}_${partNum}_${partName}_${timestamp}.mp4"

                                scope.launch {
                                    if (!settings.isServerUploadEnabled) {
                                        Toast.makeText(context, "서버 전송 동의가 꺼져 있어 로컬에 저장합니다.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    if (!settings.isAutoUpload) {
                                        Toast.makeText(context, "자동 전송이 꺼져 있어 로컬에 저장합니다.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    if (settings.isWifiOnlyUpload && !NetworkUtils.isWifiConnected(context)) {
                                        Toast.makeText(context, "WIFI 전용 업로드 설정으로 로컬 저장합니다.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    Toast.makeText(context, "업로드 시작...", Toast.LENGTH_SHORT).show()
                                    uploader.uploadVideo(
                                        fileUri = uri,
                                        filename = filename,
                                        onComplete = { s3Key ->
                                            Toast.makeText(context, "업로드 완료!", Toast.LENGTH_SHORT).show()
                                            onRecordingComplete(s3Key)
                                        },
                                        onError = { e ->
                                            Toast.makeText(context, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                            Log.e("TabletRecord", "Upload failed", e)
                                        }
                                    )
                                }
                            }
                            isRecording = false
                        }
                    }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101828))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 왼쪽: 댄서 튜토리얼 영상 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPreview || videoUrl.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF673AB7).copy(alpha = 0.8f),
                                        Color(0xFF3F51B5).copy(alpha = 0.9f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("댄서 튜토리얼 영상", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            if (view.player != exoPlayer) view.player = exoPlayer
                        }
                    )
                }

                TabletAnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                                .clickable { isPlaying = !isPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "일시정지" else "재생",
                                modifier = Modifier.size(50.dp),
                                tint = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("댄서 튜토리얼 영상", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
                    }
                }
            }

            // 오른쪽: 사용자 녹화 화면 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                if (isPreview) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("카메라 미리보기 (Preview)", color = Color.White)
                    }
                } else if (hasPermissions) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val recorder = Recorder.Builder()
                                    .setQualitySelector(QualitySelector.from(Quality.FHD))
                                    .build()
                                val videoCapture = VideoCapture.withOutput(recorder)
                                videoCaptureState.value = videoCapture

                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner, cameraSelector, preview, videoCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("PracticeTablet", "Camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("카메라 권한이 필요합니다.", color = Color.White)
                    }
                }

                // 녹화 시간 표시
                if (isRecording) {
                    Row(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Red))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", recordingTime / 60, recordingTime % 60),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 상단 컨트롤 바
        TabletAnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundIconButton(
                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = { isMuted = !isMuted }
                    )
                    RoundIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        }
                    )
                    RoundIconButton(icon = Icons.Default.Settings, onClick = onSettingsClick)
                }
            }
        }

        // 하단 제어판
        TabletAnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SongInfoBar(title = songTitle, artistPart = artistPart, difficulty = difficulty)

                PlaybackSlider(
                    currentPosition = sliderPosition,
                    currentTime = currentTimeStr,
                    totalTime = totalTimeStr,
                    onPositionChange = { newPosition ->
                        if (totalDurationMs > 0) {
                            exoPlayer.seekTo((newPosition * totalDurationMs).toLong())
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SpeedControlRow(
                            selectedSpeed = selectedSpeed,
                            onSpeedSelected = { selectedSpeed = it }
                        )
                    }

                    // 하단 녹화 버튼
                    val buttonPadding by animateDpAsState(if (isRecording) 12.dp else 4.dp, label = "")
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(buttonPadding)
                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(Color.Red)
                            .clickable { toggleRecording() }
                    )
                }
            }
        }
    }
}


@Composable
private fun TabletAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun PracticeScreenTabletPreview() {
    KpopDancePracticeAITheme {
        PracticeScreenTablet()
    }
}
