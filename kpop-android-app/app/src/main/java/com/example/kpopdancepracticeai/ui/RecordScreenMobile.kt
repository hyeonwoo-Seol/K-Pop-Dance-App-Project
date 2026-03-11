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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kpopdancepracticeai.data.PresignedUrlUploader
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.mapper.AnalysisMapper
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.util.FilenameParser
import com.example.kpopdancepracticeai.util.NetworkUtils
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import com.example.kpopdancepracticeai.viewmodel.SettingsViewModel
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder

private fun extractExpertIdentifier(
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
fun RecordScreen(
    songTitle: String = "ELEVEN",
    difficulty: String = "보통",
    artist: String = "IVE",
    part: String = "1절 코러스",
    expertVideoUrl: String = "asset:///540_원영_1.mp4",
    onBack: () -> Unit = {},
    onNavigateHome: () -> Unit = onBack,
    onRecordingComplete: (String) -> Unit = {},
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val userProfile by mainViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val authUserId = remember { AuthRepository(context).getCurrentUser()?.uid }
    val isTablet = remember(configuration) { configuration.screenWidthDp >= 600 }

    val uploader = remember { PresignedUrlUploader(context) }

    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms -> hasPermissions = perms.values.all { it } }
    )

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recordingState = remember { mutableStateOf<Recording?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var countdownNumber by remember { mutableIntStateOf(0) }
    var isCountdownVisible by remember { mutableStateOf(false) }
    var showAnalysisLoading by remember { mutableStateOf(false) }
    var hasAutoStoppedRecording by remember { mutableStateOf(false) }

    LaunchedEffect(settings.isFrontCamera) {
        lensFacing = if (settings.isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(expertVideoUrl) {
        if (expertVideoUrl.isNotBlank()) {
            try {
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(expertVideoUrl)))
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                Log.e("RecordScreen", "영상 로드 실패: $expertVideoUrl", e)
            }
        }
    }

    DisposableEffect(exoPlayer, isRecording) {
        val playbackListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (
                    playbackState == Player.STATE_ENDED &&
                    isRecording &&
                    !hasAutoStoppedRecording
                ) {
                    hasAutoStoppedRecording = true
                    recordingState.value?.stop()
                    recordingState.value = null
                    isRecording = false
                }
            }
        }

        exoPlayer.addListener(playbackListener)
        onDispose {
            exoPlayer.removeListener(playbackListener)
        }
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
        if (recordEvent.hasError()) {
            recordingState.value?.close()
            isRecording = false
            return
        }

        val uri = recordEvent.outputResults.outputUri
        val userId = userProfile?.userUuid ?: authUserId ?: "none"
        val expertIdentifier = extractExpertIdentifier(expertVideoUrl, songTitle, artist, part)
        val partNum = expertIdentifier.substringAfterLast("_", "0").ifBlank { "0" }
        val timestamp = System.currentTimeMillis()
        val filename = "${userId}_${expertIdentifier}_${timestamp}.mp4"
        val songIdForDbLong = expertIdentifier
            .substringBefore("_")
            .toLongOrNull() ?: 0L
        val songIdForDb = songIdForDbLong.toString()

        scope.launch {
            if (!settings.isServerUploadEnabled) {
                Toast.makeText(context, "서버 전송 동의가 꺼져 있어 로컬에 저장합니다.", Toast.LENGTH_SHORT).show()
                mainViewModel.markPracticePartCompleted(userId, songIdForDbLong, artist)
                onNavigateHome()
                return@launch
            }

            if (!settings.isAutoUpload) {
                Toast.makeText(context, "자동 전송이 꺼져 있어 로컬에 저장합니다.", Toast.LENGTH_SHORT).show()
                mainViewModel.markPracticePartCompleted(userId, songIdForDbLong, artist)
                onNavigateHome()
                return@launch
            }

            if (settings.isWifiOnlyUpload && !NetworkUtils.isWifiConnected(context)) {
                Toast.makeText(context, "WIFI 전용 업로드 설정으로 로컬 저장 후 홈으로 이동합니다.", Toast.LENGTH_SHORT).show()
                mainViewModel.markPracticePartCompleted(userId, songIdForDbLong, artist)
                onNavigateHome()
                return@launch
            }

            Toast.makeText(context, "녹화 완료. 업로드 시작...", Toast.LENGTH_SHORT).show()
            showAnalysisLoading = true
            uploader.uploadVideo(
                fileUri = uri,
                filename = filename,
                onComplete = {
                    Toast.makeText(context, "업로드 성공!", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        uploader.pollAnalysisResult(
                            userId = userId,
                            timestamp = timestamp,
                            onProgress = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                            onComplete = { resultS3Key ->
                                scope.launch {
                                    try {
                                        val jsonString = uploader.downloadResultJson(resultS3Key)
                                        val jsonFileName = uploader.extractResultFileName(resultS3Key)
                                        val response = Gson().fromJson(
                                            jsonString,
                                            AnalysisResultResponse::class.java
                                        )

                                        val metadata = FilenameParser.ParsedMetadata(
                                            userId = userId,
                                            songId = songIdForDb,
                                            artist = artist,
                                            partNumber = partNum
                                        )

                                        val jsonPath = File(
                                            File(context.filesDir, "analysis_results"),
                                            jsonFileName
                                        ).absolutePath

                                        val historyEntity = AnalysisMapper.mapToPracticeHistory(
                                            analysisResult = response,
                                            metadata = metadata,
                                            videoPath = uri.toString(),
                                            fullJsonPath = jsonPath
                                        )
                                        mainViewModel.savePracticeResult(historyEntity)

                                        showAnalysisLoading = false
                                        Toast.makeText(context, "분석 완료!", Toast.LENGTH_SHORT).show()
                                        onRecordingComplete("$jsonFileName|${uri}")
                                    } catch (e: Exception) {
                                        showAnalysisLoading = false
                                        Toast.makeText(context, "결과 처리 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                        Log.e("RecordScreen", "결과 처리 실패", e)
                                    }
                                }
                            },
                            onError = { e ->
                                showAnalysisLoading = false
                                Toast.makeText(context, "분석 실패: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                onError = { e ->
                    showAnalysisLoading = false
                    Toast.makeText(context, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun startRecordingWithCountdown() {
        val videoCapture = videoCaptureState.value ?: return
        if (isRecording || isCountdownVisible) return

        scope.launch {
            val startCount = settings.countdownSeconds
            if (startCount > 0) {
                countdownNumber = startCount
                isCountdownVisible = true
                while (countdownNumber > 0) {
                    delay(1000)
                    countdownNumber--
                }
                isCountdownVisible = false
            }

            exoPlayer.seekTo(0)
            exoPlayer.play()
            hasAutoStoppedRecording = false

            isRecording = true
            val name = "Kpop_${System.currentTimeMillis()}.mp4"
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
                    if (event is VideoRecordEvent.Finalize) {
                        handleRecordingFinalize(event)
                    }
                }
        }
    }

    if (showAnalysisLoading) {
        AnalysisWaitingScreen(
            onAnalysisComplete = { }
        )
        return
    }

    if (!hasPermissions) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("카메라 및 오디오 권한이 필요합니다.", color = Color.White)
                Button(onClick = { launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }) { Text("권한 허용하기") }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { PlayerView(it).apply { player = exoPlayer; useController = false } },
            modifier = Modifier.fillMaxSize(),
            update = { if (it.player != exoPlayer) it.player = exoPlayer }
        )

        val cameraModifier = if (isRecording) {
            Modifier.align(Alignment.BottomEnd).padding(bottom = 140.dp, end = 20.dp).size(120.dp, 180.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, Color.White, RoundedCornerShape(12.dp))
        } else {
            Modifier.align(Alignment.BottomEnd).size(1.dp).alpha(0f)
        }

        Box(modifier = cameraModifier) {
            AndroidView(
                factory = { PreviewView(it).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val targetQuality = if (isTablet) Quality.FHD else Quality.HD
                        val qualitySelector = QualitySelector.from(targetQuality, FallbackStrategy.lowerQualityOrHigherThan(targetQuality))
                        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
                        val videoCapture = VideoCapture.withOutput(recorder)
                        videoCaptureState.value = videoCapture
                        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
                        } catch (e: Exception) {
                            Log.e("CameraX", "Binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding().align(Alignment.TopStart)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Spacer(modifier = Modifier.size(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(songTitle, color = Color.White, fontSize = 20.sp)
                        Spacer(modifier = Modifier.size(12.dp))
                        Box(modifier = Modifier.background(Color(0x33F0B100), RoundedCornerShape(8.dp)).border(1.dp, Color(0x80F0B100), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(difficulty, color = Color(0xFFFFDF20), fontSize = 12.sp)
                        }
                    }
                    Text("$artist · $part", color = Color(0xFFD1D5DC), fontSize = 14.sp)
                }
            }
        }

        if (isCountdownVisible) {
            Text(
                text = countdownNumber.toString(),
                color = Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isRecording) {
            Row(modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Red))
                Spacer(modifier = Modifier.size(8.dp))
                Text(String.format("%02d:%02d", recordingTime / 60, recordingTime % 60), color = Color.White)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
            if (isRecording) {
                val buttonPadding by animateDpAsState(24.dp, label = "")
                Box(
                    modifier = Modifier.size(80.dp).align(Alignment.Center).border(4.dp, Color.White, CircleShape).padding(buttonPadding).clip(RoundedCornerShape(12.dp)).background(Color.Red).clickable {
                        recordingState.value?.stop()
                        recordingState.value = null
                        isRecording = false
                    }
                )

                IconButton(
                    onClick = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp).size(56.dp).background(Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Switch Camera", tint = Color.White)
                }
            } else {
                Button(
                    onClick = { startRecordingWithCountdown() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB2C36)),
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("따라하기", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
