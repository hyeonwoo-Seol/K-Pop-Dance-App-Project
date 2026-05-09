package com.example.kpopdancepracticeai.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kpopdancepracticeai.data.PresignedUrlUploader
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.entity.User
import com.example.kpopdancepracticeai.data.mapper.AnalysisMapper
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.util.FilenameParser
import com.example.kpopdancepracticeai.util.NetworkUtils
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import com.example.kpopdancepracticeai.viewmodel.SettingsViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.util.Locale

private const val TabletRecordCountdownSeconds = 3

private fun extractTabletExpertIdentifier(
    expertVideoUrl: String,
    songTitle: String,
    artist: String,
    part: String
): String {
    val fromUrl = expertVideoUrl
        .substringBefore("?")
        .let { raw ->
            val parsed = Uri.parse(raw).lastPathSegment ?: raw.substringAfterLast("/")
            runCatching { URLDecoder.decode(parsed, "UTF-8") }.getOrDefault(parsed)
        }
        .substringBeforeLast(".")
        .trim()
        .takeIf { it.isNotBlank() }

    if (fromUrl != null) return fromUrl

    val songToken = songTitle.replace(" ", "").replace("_", "")
    val artistToken = artist
        .substringAfter("(")
        .substringBefore(")")
        .ifBlank { artist }
        .replace(" ", "")
        .replace("_", "")
    val partToken = part.filter { it.isDigit() }.ifEmpty { "0" }
    return "${songToken}_${artistToken}_${partToken}"
}

@Composable
fun PracticeScreenTablet(
    songId: Long = 0L,
    songTitle: String = "Dynamite",
    artistPart: String = "BTS · Part 2: 메인 파트",
    difficulty: String = "보통",
    length: String = "2:15",
    videoUrl: String = "",
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNavigateHome: () -> Unit = onBackClick,
    onRecordingComplete: (String) -> Unit = {},
    mainViewModel: MainViewModel? = null,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current
    val uploader = remember { PresignedUrlUploader(context) }
    val userProfile by mainViewModel?.currentUserProfile?.collectAsStateWithLifecycle() ?: remember { mutableStateOf<User?>(null) }
    val authUserId = remember { AuthRepository(context).getCurrentUser()?.uid }
    val artistAndPart = remember(artistPart) { artistPart.split("·").map { it.trim() } }
    val artist = artistAndPart.getOrNull(0) ?: "Unknown"
    val part = artistAndPart.getOrNull(1) ?: artistPart

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

    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recordingState = remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isCountdownVisible by remember { mutableStateOf(false) }
    var countdownNumber by remember { mutableIntStateOf(0) }
    var recordingTime by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var showAnalysisLoading by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var analysisStatusMessage by remember { mutableStateOf("업로드 준비 중...") }
    var hasAutoStoppedRecording by remember { mutableStateOf(false) }
    var progressRampJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(settings.isFrontCamera) {
        lensFacing = if (settings.isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(videoUrl) {
        if (videoUrl.isBlank()) return@LaunchedEffect

        val finalUrl = if (!videoUrl.startsWith("asset:///")) {
            videoUrl.replace("file:///android_asset/", "asset:///")
        } else {
            videoUrl
        }

        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(finalUrl)))
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            exoPlayer.prepare()
            exoPlayer.seekTo(0)
            exoPlayer.playWhenReady = false
        } catch (e: Exception) {
            Log.e("PracticeTablet", "영상 로드 실패: $finalUrl", e)
        }
    }

    LaunchedEffect(selectedSpeed) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            totalDurationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            delay(100)
        }
    }

    DisposableEffect(exoPlayer, isRecording) {
        val playbackListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && isRecording && !hasAutoStoppedRecording) {
                    hasAutoStoppedRecording = true
                    recordingState.value?.stop()
                    recordingState.value = null
                    isRecording = false
                }
            }
        }

        exoPlayer.addListener(playbackListener)
        onDispose { exoPlayer.removeListener(playbackListener) }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }

    fun handleRecordingFinalize(recordEvent: VideoRecordEvent.Finalize) {
        exoPlayer.pause()

        if (recordEvent.hasError()) {
            recordingState.value?.close()
            isRecording = false
            Toast.makeText(context, "녹화 저장 실패: ${recordEvent.error}", Toast.LENGTH_LONG).show()
            return
        }

        val uri = recordEvent.outputResults.outputUri
        val userId = userProfile?.userUuid ?: authUserId ?: "none"
        val expertIdentifier = extractTabletExpertIdentifier(videoUrl, songTitle, artist, part)
        val partNum = expertIdentifier.substringAfterLast("_", "0").ifBlank { "0" }
        val partNumberForDb = partNum.toIntOrNull() ?: 0
        val timestamp = System.currentTimeMillis()
        val filename = "${userId}_${expertIdentifier}_${timestamp}.mp4"
        val songIdForDbLong = if (songId > 0L) songId else (expertIdentifier.substringBefore("_").toLongOrNull() ?: 0L)
        val songIdForDb = songIdForDbLong.toString()

        scope.launch {
            if (!settings.isServerUploadEnabled) {
                Toast.makeText(context, "서버 전송 동의가 꺼져 있어 로컬에 저장합니다.", Toast.LENGTH_SHORT).show()
                mainViewModel?.markPracticePartCompleted(userId, songIdForDbLong, partNumberForDb, artist)
                onNavigateHome()
                return@launch
            }

            if (!settings.isAutoUpload) {
                Toast.makeText(context, "자동 전송이 꺼져 있어 로컬에 저장합니다.", Toast.LENGTH_SHORT).show()
                mainViewModel?.markPracticePartCompleted(userId, songIdForDbLong, partNumberForDb, artist)
                onNavigateHome()
                return@launch
            }

            if (settings.isWifiOnlyUpload && !NetworkUtils.isWifiConnected(context)) {
                Toast.makeText(context, "WIFI 전용 업로드 설정으로 로컬 저장 후 홈으로 이동합니다.", Toast.LENGTH_SHORT).show()
                mainViewModel?.markPracticePartCompleted(userId, songIdForDbLong, partNumberForDb, artist)
                onNavigateHome()
                return@launch
            }

            Toast.makeText(context, "녹화 완료. 업로드 시작...", Toast.LENGTH_SHORT).show()
            showAnalysisLoading = true
            analysisProgress = 0f
            analysisStatusMessage = "영상을 클라우드로 전송 중..."
            uploader.uploadVideo(
                fileUri = uri,
                filename = filename,
                onUploadProgress = { uploadProgress ->
                    val cappedUploadProgress = (uploadProgress * 0.2f).coerceIn(0f, 0.2f)
                    if (analysisProgress < 0.2f) {
                        analysisProgress = cappedUploadProgress.coerceAtLeast(analysisProgress)
                    }
                    analysisStatusMessage = "영상을 클라우드로 전송 중... ${(uploadProgress * 100).toInt()}%"
                },
                onComplete = {
                    Toast.makeText(context, "업로드 성공!", Toast.LENGTH_SHORT).show()
                    analysisProgress = 0.2f
                    analysisStatusMessage = "서버에서 AI 분석 중..."

                    progressRampJob?.cancel()
                    progressRampJob = scope.launch {
                        repeat(12) {
                            delay(1000)
                            if (analysisProgress >= 0.92f) return@launch
                            analysisProgress = (analysisProgress + 0.06f).coerceAtMost(0.92f)
                        }
                    }

                    scope.launch {
                        uploader.pollAnalysisResult(
                            userId = userId,
                            timestamp = timestamp,
                            onProgress = { msg ->
                                analysisStatusMessage = msg
                                analysisProgress = (analysisProgress + 0.03f).coerceAtMost(0.95f)
                            },
                            onComplete = { resultS3Key ->
                                scope.launch {
                                    try {
                                        progressRampJob?.cancel()

                                        while (analysisProgress < 1f) {
                                            analysisProgress = (analysisProgress + 0.04f).coerceAtMost(1f)
                                            delay(60)
                                        }

                                        val jsonString = uploader.downloadResultJson(resultS3Key)
                                        val jsonFileName = uploader.extractResultFileName(resultS3Key)
                                        val response = Gson().fromJson(jsonString, AnalysisResultResponse::class.java)
                                        val metadata = FilenameParser.ParsedMetadata(
                                            userId = userId,
                                            songId = songIdForDb,
                                            artist = artist,
                                            partNumber = partNum
                                        )
                                        val jsonPath = File(File(context.filesDir, "analysis_results"), jsonFileName).absolutePath
                                        val historyEntity = AnalysisMapper.mapToPracticeHistory(
                                            analysisResult = response,
                                            metadata = metadata,
                                            videoPath = uri.toString(),
                                            fullJsonPath = jsonPath
                                        )

                                        mainViewModel?.savePracticeResult(historyEntity)
                                        analysisStatusMessage = "분석 완료!"
                                        showAnalysisLoading = false
                                        Toast.makeText(context, "분석 완료!", Toast.LENGTH_SHORT).show()
                                        onRecordingComplete("$jsonFileName|${uri}")
                                    } catch (e: Exception) {
                                        progressRampJob?.cancel()
                                        showAnalysisLoading = false
                                        Toast.makeText(context, "결과 처리 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                        Log.e("PracticeTablet", "결과 처리 실패", e)
                                    }
                                }
                            },
                            onError = { e ->
                                progressRampJob?.cancel()
                                showAnalysisLoading = false
                                Toast.makeText(context, "분석 실패: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                onError = { e ->
                    progressRampJob?.cancel()
                    showAnalysisLoading = false
                    Toast.makeText(context, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("PracticeTablet", "Upload failed", e)
                }
            )
        }
    }

    fun startRecordingWithCountdown() {
        val videoCapture = videoCaptureState.value
        if (videoCapture == null) {
            Toast.makeText(context, "카메라 준비 중입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isRecording || isCountdownVisible) return

        scope.launch {
            exoPlayer.seekTo(0)
            exoPlayer.pause()

            countdownNumber = TabletRecordCountdownSeconds
            isCountdownVisible = true
            while (countdownNumber > 0) {
                delay(1000)
                countdownNumber--
            }
            isCountdownVisible = false

            exoPlayer.seekTo(0)
            exoPlayer.pause()
            hasAutoStoppedRecording = false

            val name = "Kpop_Tablet_${System.currentTimeMillis()}.mp4"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/KpopDancePractice")
            }
            val mediaStoreOutputOptions = MediaStoreOutputOptions
                .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build()

            recordingState.value = videoCapture.output
                .prepareRecording(context, mediaStoreOutputOptions)
                .apply { if (hasPermissions) withAudioEnabled() }
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                            isRecording = true
                        }

                        is VideoRecordEvent.Finalize -> handleRecordingFinalize(event)
                    }
                }
        }
    }

    val formatTime = { ms: Long ->
        val totalSeconds = ms / 1000
        String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
    val currentTimeStr = formatTime(currentPositionMs)
    val totalTimeStr = if (totalDurationMs > 0) formatTime(totalDurationMs) else length
    val sliderPosition = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f

    if (showAnalysisLoading) {
        AnalysisWaitingScreen(
            progress = analysisProgress,
            statusMessage = analysisStatusMessage,
            onAnalysisComplete = { }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { areControlsVisible = !areControlsVisible }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } },
                    modifier = Modifier.fillMaxSize(),
                    update = { if (it.player != exoPlayer) it.player = exoPlayer }
                )

                if (!isRecording && !isCountdownVisible) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                                .clickable {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (exoPlayer.isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                contentDescription = "안무 영상 재생",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("댄서 안무 영상", color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                if (isPreview) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                        Text("카메라 미리보기 (Preview)", color = Color.White)
                    }
                } else if (hasPermissions) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                val qualitySelector = QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD))
                                val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
                                val videoCapture = VideoCapture.withOutput(recorder)
                                videoCaptureState.value = videoCapture
                                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
                                } catch (e: Exception) {
                                    Log.e("PracticeTablet", "Camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("카메라 및 오디오 권한이 필요합니다.", color = Color.White)
                        Button(onClick = { launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }) {
                            Text("권한 허용하기")
                        }
                    }
                }

                Text(
                    text = "내 녹화 화면",
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isCountdownVisible) {
            Text(
                text = countdownNumber.toString(),
                color = Color.White,
                fontSize = 112.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isRecording) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 24.dp),
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

        AnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(
                        icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        onClick = { isMuted = !isMuted }
                    )
                    RoundIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT }
                    )
                    RoundIconButton(icon = Icons.Default.Settings, onClick = onSettingsClick)
                }
            }
        }

        AnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SongInfoBar(title = songTitle, artistPart = artistPart, difficulty = difficulty)
                PlaybackSlider(
                    currentPosition = sliderPosition,
                    currentTime = currentTimeStr,
                    totalTime = totalTimeStr,
                    onPositionChange = { newPosition ->
                        val targetMs = (newPosition * totalDurationMs).toLong()
                        exoPlayer.seekTo(targetMs)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SpeedControlRow(selectedSpeed = selectedSpeed, onSpeedSelected = { selectedSpeed = it })
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Button(
                        onClick = {
                            if (isRecording) {
                                recordingState.value?.stop()
                                recordingState.value = null
                                isRecording = false
                            } else {
                                startRecordingWithCountdown()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.CameraAlt,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRecording) "녹화 중지" else "3초 후 녹화")
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    val buttonPadding by animateDpAsState(if (isRecording) 14.dp else 5.dp, label = "tabletRecordButtonPadding")
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(buttonPadding)
                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(Color.Red)
                            .clickable {
                                if (isRecording) {
                                    recordingState.value?.stop()
                                    recordingState.value = null
                                    isRecording = false
                                } else {
                                    startRecordingWithCountdown()
                                }
                            }
                    )
                }

                Text(
                    text = "녹화 버튼을 누르면 3초 카운트다운 후 왼쪽 안무 영상이 처음부터 재생되고 오른쪽 카메라 녹화가 시작됩니다.",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@ComposePreview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun PracticeScreenTabletPreview() {
    KpopDancePracticeAITheme {
        PracticeScreenTablet()
    }
}
