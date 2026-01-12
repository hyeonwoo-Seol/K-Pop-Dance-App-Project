package com.example.kpopdancepracticeai
// MainActivity.kt

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
// import androidx.room.Room // 추후 주석 해제 필요
// import com.example.kpopdancepracticeai.data.database.AppDatabase // 추후 주석 해제 필요
// import com.example.kpopdancepracticeai.data.repository.AppRepository // 추후 주석 해제 필요
import com.example.kpopdancepracticeai.ui.LoginScreen
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme // 본인의 테마 이름
import com.example.kpopdancepracticeai.ui.HomeScreen
import com.example.kpopdancepracticeai.ui.KpopDancePracticeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 전략 문서에 따른 DB 및 Repository 초기화 (신규 파일 생성 후 활성화)
        // val database = Room.databaseBuilder(
        //     applicationContext,
        //     AppDatabase::class.java,
        //     "kpop-dance-db"
        // ).build()
        // val repository = AppRepository(database.userDao(), database.historyDao(), database.achievementDao())

        setContent {
            // 프로젝트 생성 시 만들어진 테마(Theme)를 적용합니다.
            KpopDancePracticeAITheme {
                val context = LocalContext.current
                var permissionsGranted by remember { mutableStateOf(false) }
                var showPermissionDeniedDialog by remember { mutableStateOf(false) }

                // 요청할 권한 목록 정의 (Android 13 이상 대응)
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

                // 앱 시작 시 권한 체크 및 요청
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
                    if (permissionsGranted) {
                        KpopDancePracticeApp()
                    }

                    if (showPermissionDeniedDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                // 다이얼로그 밖을 눌러도 닫히지 않게 하거나, 닫히면 앱 종료 처리
                                finish()
                            },
                            title = { Text(text = "권한 필요") },
                            text = {
                                Text(text = "앱을 사용하기 위해서는 카메라, 마이크, 알림 및 저장소 권한이 필요합니다. 권한을 허용해야 앱을 사용할 수 있습니다.")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        finish() // 권한 거부 시 앱 종료
                                    }
                                ) {
                                    Text("종료")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}