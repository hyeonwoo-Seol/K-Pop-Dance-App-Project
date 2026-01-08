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
import com.example.kpopdancepracticeai.data.PresignedUrlUploader
import com.example.kpopdancepracticeai.data.S3Uploader // [추가됨] S3Uploader 임포트
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope() // Coroutine Scope 생성

    // [추가됨] S3 업로더 인스턴스
    val uploader = remember { PresignedUrlUploader(context) }

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
    // 권한이 없을 때의 화면 처리
    if (!hasPermissions) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. 뒤로가기 버튼 추가 (툴바가 없으므로 필수)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding() // 상태바와 겹치지 않게 패딩
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // 2. 권한 요청 안내 및 버튼
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("카메라 및 오디오 권한이 필요합니다.", color = Color.White)
                Button(
                    onClick = {
                        launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                    }
                ) {
                    Text("권한 허용하기")
                }
            }
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
// ✅ [수정됨] 파일명 생성 로직: 조각을 정확히 5개로 맞춤
                                                val userId = "xooyong"

                                                // 1. songTitle에서 공백과 언더바를 제거하여 한 단어로 만듦
                                                val songId = songTitle.replace(" ", "").replace("_", "")

                                                // 2. part 문자열에서 숫자만 추출하여 partNum 생성 (예: "Part 2" -> "2")
                                                val partNum = part.filter { it.isDigit() }.ifEmpty { "0" }

                                                // 3. ":" 뒤의 이름에서 공백과 언더바 제거하여 partName 생성 (예: "메인 파트" -> "메인파트")
                                                val partName = part.split(":").lastOrNull()?.replace(" ", "")?.replace("_", "") ?: "None"

                                                // 4. Polling과 공유할 정확한 타임스탬프
                                                val timestamp = System.currentTimeMillis()

                                                // ✅ 최종 파일명: xooyong_Dynamite_2_메인파트_1767882222994.mp4 (조각 5개 확인!)
                                                val filename = "${userId}_${songId}_${partNum}_${partName}_${timestamp}.mp4"

                                                scope.launch {
                                                    uploader.uploadVideo(
                                                        fileUri = uri,
                                                        filename = filename,
                                                        onComplete = { s3Key ->
                                                            Toast.makeText(context, "업로드 성공!", Toast.LENGTH_SHORT).show()
                                                            Log.d("RecordScreen", "Upload Complete Key: $s3Key")
                                                            Log.d("RecordScreen", "폴링 시작 요청값 확인 -> ID: $userId, TIME: $timestamp")
                                                            scope.launch {
                                                                uploader.pollAnalysisResult(
                                                                    userId = userId,
                                                                    timestamp = timestamp,
                                                                    onProgress = { message ->
                                                                        //  분석 중 상태 표시
                                                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    onComplete = { resultS3Key ->
                                                                        //  분석 완료
                                                                        Toast.makeText(context, "분석 완료!", Toast.LENGTH_SHORT).show()
                                                                        Log.d("RecordScreen", "Result Ready: $resultS3Key")

                                                                        // JSON 다운로드
                                                                        scope.launch {
                                                                            try {
                                                                                val jsonResult = uploader.downloadResultJson(resultS3Key)
                                                                                Log.d("RecordScreen", "Result JSON: $jsonResult")
                                                                                onRecordingComplete(resultS3Key)
                                                                            } catch (e: Exception) {
                                                                                Log.e("RecordScreen", "JSON 다운로드 실패", e)
                                                                            }
                                                                        }
                                                                    },
                                                                    onError = { e ->
                                                                        Toast.makeText(context, "분석 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                                                    }
                                                                )
                                                            }
                                                        },
                                                        onError = { e ->
                                                            Toast.makeText(context, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                }
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