package com.example.kpopdancepracticeai.ui

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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

/**
 * 개인정보 보호 및 권한 화면 (전체 화면)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBackClick: () -> Unit
) {
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
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "기기 권한") {
                            SettingsClickableItem(
                                title = "권한 설정",
                                description = "카메라, 저장장치, 위치",
                                icon = Icons.Outlined.Shield,
                                onClick = { /* TODO: 기기 설정 화면으로 이동 (Intent) */ }
                            )
                            SettingsDivider()
                            PermissionStatusItem(
                                label = "카메라",
                                icon = Icons.Outlined.CameraAlt,
                                status = "허용됨",
                                statusColor = Color(0xFF00A63E) // Green
                            )
                            PermissionStatusItem(
                                label = "저장장치",
                                icon = Icons.Outlined.Storage,
                                status = "허용됨",
                                statusColor = Color(0xFF00A63E) // Green
                            )
                            PermissionStatusItem(
                                label = "위치",
                                icon = Icons.Outlined.LocationOn,
                                status = "거부됨",
                                statusColor = Color.Red
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