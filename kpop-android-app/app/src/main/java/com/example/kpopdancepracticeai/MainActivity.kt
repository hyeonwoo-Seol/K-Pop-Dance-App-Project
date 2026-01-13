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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
// [중요] KpopDancePracticeApp은 같은 패키지 또는 ui 패키지에 있어야 합니다.
import com.example.kpopdancepracticeai.ui.KpopDancePracticeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KpopDancePracticeAITheme {
                val context = LocalContext.current
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
                    if (permissionsGranted) {
                        // [수정] 이제 KpopDancePracticeApp이 정의되어 있으므로 호출 가능합니다.
                        KpopDancePracticeApp()
                    }

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