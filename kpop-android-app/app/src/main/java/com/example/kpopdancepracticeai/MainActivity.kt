package com.example.kpopdancepracticeai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.KpopDancePracticeApp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Repository 및 Auth 초기화
        val app = application as KpopApplication
        val repository = app.repository
        val authRepository = AuthRepository(this)

        setContent {
            KpopDancePracticeAITheme {
                val context = LocalContext.current

                // [수정] 앱 생명주기 감지 로직 추가 (ON_START, ON_STOP)
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                // 앱이 화면에 나타날 때 (포그라운드 진입) -> 타이머 시작
                                repository.onAppForeground()
                            }
                            Lifecycle.Event.ON_STOP -> {
                                // 앱이 화면에서 사라질 때 (백그라운드/종료) -> 시간 계산 및 저장
                                val currentUser = authRepository.getCurrentUser()
                                if (currentUser != null) {
                                    // Composable 외부이므로 lifecycleScope를 사용하여 코루틴 실행
                                    lifecycleScope.launch {
                                        repository.onAppBackground(currentUser.uid)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // --- 기존 권한 요청 로직 유지 ---
                var permissionsGranted by remember { mutableStateOf(false) }
                var showPermissionDeniedDialog by remember { mutableStateOf(false) }

                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                } else {
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.values.all { it }
                    if (allGranted) {
                        permissionsGranted = true
                    } else {
                        showPermissionDeniedDialog = true
                    }
                }

                LaunchedEffect(Unit) {
                    val allPermissionsGranted = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }

                    if (allPermissionsGranted) {
                        permissionsGranted = true
                    } else {
                        launcher.launch(permissionsToRequest)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 💡 핵심: 권한 허용 여부와 상관없이 앱 전체 네비게이션 구조는 항상 먼저 렌더링합니다.
                    // 이렇게 하면 NavHost가 생명주기 시작점을 놓치지 않습니다.
                    KpopDancePracticeApp()

                    // 권한이 없어서 다이얼로그를 띄워야 할 경우만 앱 뷰 위에 오버레이로 띄웁니다.
                    if (showPermissionDeniedDialog) {
                        AlertDialog(
                            onDismissRequest = { finish() },
                            title = { Text(text = "권한 필요") },
                            text = { Text(text = "앱을 사용하기 위해서는 권한 허용이 필요합니다.") },
                            confirmButton = {
                                TextButton(onClick = { finish() }) { Text("종료") }
                            }
                        )
                    }
                }
            }
        }
    }
}