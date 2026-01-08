package com.example.kpopdancepracticeai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Image // 사진 및 동영상 아이콘 대체
import androidx.compose.material.icons.outlined.Mic // 마이크 아이콘
import androidx.compose.material.icons.outlined.Notifications // 알림 아이콘
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

/**
 * 개인정보 보호 및 권한 화면 (전체 화면)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 앱 전체의 그라데이션 배경
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF), // 상단 연한 파랑
            Color(0xFFF0E8FF)  // 하단 연한 보라
        )
    )

    // 설정 값 상태 관리 (임시)
    var isServerUploadEnabled by remember { mutableStateOf(false) }
    var isAnalyticsEnabled by remember { mutableStateOf(false) }

    // 권한 상태 갱신을 위한 키 (앱 설정에서 돌아왔을 때 갱신)
    var refreshKey by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 생명주기 감지: ON_RESUME 시 refreshKey 업데이트
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 권한 상태 체크 헬퍼 함수
    fun checkPermission(permission: String): Boolean {
        return if (permission.isEmpty()) {
            true // 권한이 필요 없는 경우 (예: 구버전 안드로이드 알림 등)
        } else {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 권한 상태에 따른 텍스트와 색상 반환
    fun getPermissionStatus(isGranted: Boolean): Pair<String, Color> {
        return if (isGranted) {
            "허용됨" to Color(0xFF00A63E) // Green
        } else {
            "거부됨" to Color.Red
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            // topBar 제거: 스크롤 영역 내부로 이동
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize(),
                // .padding(horizontal = 16.dp), // TopAppBar의 전체 너비를 위해 패딩 제거 후 내부 아이템에 적용
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 0. 상단바 (스크롤 가능하도록 이곳으로 이동) ---
                item {
                    TopAppBar(
                        title = { Text("개인정보 보호 및 권한", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "뒤로가기"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        // Scaffold의 innerPadding이 상단 여백을 처리하므로, TopAppBar 자체의 인셋 제거
                        windowInsets = WindowInsets(0.dp)
                    )
                }

                // --- 1. 데이터 처리 및 수집 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "데이터 처리 및 수집") {
                            SettingsToggleItem(
                                title = "서버 영상 전송 동의",
                                description = "댄스 영상을 GPU로 처리하기 위해 서버로 전송하는 것에 동의합니다. 전송된 영상은 분석 완료 후 자동으로 삭제됩니다.",
                                icon = Icons.Outlined.CloudUpload,
                                checked = isServerUploadEnabled,
                                onCheckedChange = { isServerUploadEnabled = it }
                            )
                            SettingsDivider()
                            SettingsToggleItem(
                                title = "댄스 성향 수집 동의",
                                description = "맞춤형 추천 영상 제공을 위해 사용자의 댄스 성향 및 연습 패턴을 수집하는 것에 동의합니다.",
                                icon = Icons.Outlined.TrendingUp,
                                checked = isAnalyticsEnabled,
                                onCheckedChange = { isAnalyticsEnabled = it }
                            )
                        }
                    }
                }

                // --- 2. 기기 권한 카드 ---
                item {
                    // refreshKey가 변경될 때마다 이 블록이 리컴포지션되어 권한 상태를 다시 읽어옵니다.
                    val trigger = refreshKey

                    // 1. 마이크 권한
                    val micPermission = Manifest.permission.RECORD_AUDIO
                    val isMicGranted = checkPermission(micPermission)
                    val (micStatus, micColor) = getPermissionStatus(isMicGranted)

                    // 2. 사진 및 동영상 (저장소) 권한
                    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES // Android 13+ (이미지 기준)
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE // Android 12 이하
                    }
                    val isStorageGranted = checkPermission(storagePermission)
                    val (storageStatus, storageColor) = getPermissionStatus(isStorageGranted)

                    // 3. 알림 권한
                    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.POST_NOTIFICATIONS
                    } else {
                        "" // Android 12 이하는 별도 런타임 권한 없음 (자동 허용으로 간주)
                    }
                    val isNotifGranted = checkPermission(notifPermission)
                    val (notifStatus, notifColor) = getPermissionStatus(isNotifGranted)

                    // 4. 카메라 권한
                    val cameraPermission = Manifest.permission.CAMERA
                    val isCameraGranted = checkPermission(cameraPermission)
                    val (cameraStatus, cameraColor) = getPermissionStatus(isCameraGranted)

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "기기 권한") {
                            SettingsClickableItem(
                                title = "권한 설정",
                                description = "마이크, 사진 및 동영상, 알림, 카메라",
                                icon = Icons.Outlined.Shield,
                                onClick = {
                                    // 앱 설정 화면으로 이동하는 Intent
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            )
                            SettingsDivider()

                            // 마이크 항목
                            PermissionStatusItem(
                                label = "마이크",
                                icon = Icons.Outlined.Mic,
                                status = micStatus,
                                statusColor = micColor
                            )

                            // 사진 및 동영상 항목
                            PermissionStatusItem(
                                label = "사진 및 동영상",
                                icon = Icons.Outlined.Image, // PhotoLibrary 대용
                                status = storageStatus,
                                statusColor = storageColor
                            )

                            // 알림 항목
                            PermissionStatusItem(
                                label = "알림",
                                icon = Icons.Outlined.Notifications,
                                status = notifStatus,
                                statusColor = notifColor
                            )

                            // 카메라 항목
                            PermissionStatusItem(
                                label = "카메라",
                                icon = Icons.Outlined.CameraAlt,
                                status = cameraStatus,
                                statusColor = cameraColor
                            )
                        }
                    }
                }

                // --- 3. 알림 사항 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InfoCard()
                    }
                }

                // 하단 여백
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 기기 권한 상태 표시 항목 (이 파일 전용)
 */
@Composable
fun PermissionStatusItem(
    label: String,
    icon: ImageVector,
    status: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp), // SettingsToggleItem과 유사한 패딩
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 하단 알림 카드 (이 파일 전용)
 */
@Composable
fun InfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF0FFF6), // 연한 초록색 배경
        border = BorderStroke(1.dp, Color(0xFFB3E6C9))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "수집된 데이터는 암호화되어 안전하게 보관됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF016630) // 진한 초록색
            )
            Text(
                text = "자세한 내용은 개인정보 처리방침을 확인해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF016630) // 진한 초록색
            )
        }
    }
}


// -----------------------------------------------------------------
// [PracticeSettingsScreen.kt]에서 복사해 온 재사용 가능한 Composable
// (파일을 독립적으로 만들기 위해 포함)
//
// ⭐️ [오류 수정]
// 아래에 중복 정의된 SettingsCard, SettingsToggleItem,
// SettingsClickableItem, SettingsDivider 함수들을 모두 제거했습니다.
// PracticeSettingsScreen.kt 파일에 있는 함수를 공통으로 사용합니다.
// -----------------------------------------------------------------


@Preview(showBackground = true)
@Composable
fun PrivacySettingsScreenPreview() {
    KpopDancePracticeAITheme {
        PrivacySettingsScreen(onBackClick = {})
    }
}