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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kpopdancepracticeai.data.S3Uploader // [추가됨] S3Uploader 임포트

@Composable
fun RecordScreen(
    songTitle: String = "Dynamite",
    difficulty: String = "보통",
    artist: String = "BTS",
    part: String = "Part 2: 메인 파트",
    onBack: () -> Unit = {},
    onRecordingComplete: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // [추가됨] S3 업로더 인스턴스
    val s3Uploader = remember { S3Uploader(context) }

    // [추가됨] 권한 상태 관리
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // [추가됨] 권한 요청 런처
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            hasPermissions = perms.values.all { it }
        }
    )

    // 화면 진입 시 권한 요청
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // CameraX 상태
    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recordingState = remember { mutableStateOf<Recording?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }

    // 타이머 로직 (기존 코드 유지)
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

    if (!hasPermissions) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("카메라 및 오디오 권한이 필요합니다.", color = Color.White)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101828))) {
        // 1. 카메라 미리보기 (CameraX PreviewView)
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
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // [수정됨] Recorder 설정 (HD 화질, S3 업로드 효율 고려)
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
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
                        Log.e("CameraX", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // 2. 상단 헤더 UI (기존 코드 유지)
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(songTitle, color = Color.White, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0x33F0B100), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x80F0B100), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(difficulty, color = Color(0xFFFFDF20), fontSize = 12.sp)
                        }
                    }
                    Text("$artist · $part", color = Color(0xFFD1D5DC), fontSize = 14.sp)
                }
            }
        }

        // 3. 녹화 시간 표시 (기존 코드 유지)
        if (isRecording) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Red))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%02d:%02d", recordingTime / 60, recordingTime % 60),
                    color = Color.White
                )
            }
        }

        // 4. 하단 컨트롤러 (녹화 버튼 및 카메라 전환)
        Box(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            // 녹화 버튼
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else Color(0xFFFB2C36))
                    .clickable {
                        if (isRecording) {
                            // [수정됨] 녹화 중지 로직
                            recordingState.value?.stop()
                            recordingState.value = null
                            isRecording = false
                        } else {
                            // [수정됨] 실제 녹화 시작 로직 추가
                            val videoCapture = videoCaptureState.value
                            if (videoCapture != null) {
                                isRecording = true

                                // MediaStore 저장 설정
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
                                    .apply {
                                        if (hasPermissions) {
                                            withAudioEnabled()
                                        }
                                    }
                                    .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                        if (recordEvent is VideoRecordEvent.Finalize) {
                                            if (!recordEvent.hasError()) {
                                                val uri = recordEvent.outputResults.outputUri
                                                Toast.makeText(context, "녹화 완료. 업로드 시작...", Toast.LENGTH_SHORT).show()

                                                // [추가됨] S3 업로드 트리거 (Task T3-1 핵심)
                                                // TODO: 실제 userId를 로그인 정보에서 가져와야 함
                                                val userId = "user_test_01"
                                                val s3Key = "$userId/${System.currentTimeMillis()}/raw.mp4"

                                                s3Uploader.uploadVideo(
                                                    fileUri = uri,
                                                    s3Key = s3Key,
                                                    onComplete = {
                                                        // 메인 스레드 UI 처리 필요 시 Handler 사용 권장
                                                        Log.d("RecordScreen", "Upload Complete: $s3Key")
                                                        onRecordingComplete(s3Key)
                                                    },
                                                    onError = { e ->
                                                        Log.e("RecordScreen", "Upload Failed", e)
                                                    }
                                                )
                                            } else {
                                                recordingState.value?.close()
                                                isRecording = false
                                                Log.e("RecordScreen", "녹화 오류: ${recordEvent.error}")
                                            }
                                        }
                                    }
                            }
                        }
                    }
            )

            // 카메라 전환 버튼 (기존 코드 유지)
            IconButton(
                onClick = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
                    .size(56.dp)
                    .background(Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Switch Camera", tint = Color.White)
            }
        }
    }
}