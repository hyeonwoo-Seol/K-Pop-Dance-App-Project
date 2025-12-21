package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
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
 * 알림 설정 화면 (전체 화면)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
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
    var allNotificationsOn by remember { mutableStateOf(true) }
    var analysisNotifOn by remember { mutableStateOf(true) }
    var eventNotifOn by remember { mutableStateOf(true) }
    var collectionNotifOn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("알림 설정", fontWeight = FontWeight.Bold) },
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
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. 마스터 푸쉬 설정 카드 ---
                item {
                    // ⭐️ [오류 수정] 이름 변경
                    NotificationSettingsCard(title = null) { // 타이틀 없는 카드
                        // ⭐️ [오류 수정] 이름 변경
                        NotificationSettingsToggleItem(
                            title = "푸쉬 알림",
                            description = "모든 알림 켜기",
                            icon = Icons.Outlined.Notifications,
                            checked = allNotificationsOn,
                            onCheckedChange = { allNotificationsOn = it }
                        )
                    }
                }

                // --- 2. 알림 항목 카드 ---
                item {
                    // ⭐️ [오류 수정] 이름 변경
                    NotificationSettingsCard(title = "알림 항목") {
                        // ⭐️ [오류 수정] 이름 변경
                        NotificationSettingsToggleItem(
                            title = "분석 완료 알림",
                            description = "연습 영상 분석이 완료되었을 때",
                            icon = Icons.Outlined.CheckCircle,
                            checked = analysisNotifOn,
                            onCheckedChange = { analysisNotifOn = it }
                        )
                        // ⭐️ [오류 수정] 이름 변경
                        NotificationSettingsDivider()
                        // ⭐️ [오류 수정] 이름 변경
                        NotificationSettingsToggleItem(
                            title = "이벤트 알림",
                            description = "새로운 이벤트 및 프로모션 소식",
                            icon = Icons.Outlined.AutoFixHigh,
                            checked = eventNotifOn,
                            onCheckedChange = { eventNotifOn = it }
                        )
                        // ⭐️ [오류 수정] 이름 변경
                        NotificationSettingsDivider()
                        // ⭐️ [오류 수정] 이름 변경
                        NotificationSettingsToggleItem(
                            title = "수집 요소 알림",
                            description = "새로운 배지 및 업적 획득 시",
                            icon = Icons.Outlined.CardGiftcard,
                            checked = collectionNotifOn,
                            onCheckedChange = { collectionNotifOn = it }
                        )
                    }
                }

                // --- 3. 알림 권한 안내 ---
                item {
                    NotificationInfoBox()
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
 * 설정 항목을 감싸는 카드 (재사용)
 * ⭐️ [오류 수정] 이 파일에서만 사용하도록 이름 변경
 */
@Composable
fun NotificationSettingsCard(
    title: String?, // Nullable로 변경
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xffd6deff))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // title이 null이 아닐 때만 Text 표시
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 토글 스위치가 있는 설정 항목 (재사용)
 * ⭐️ [오류 수정] 이 파일에서만 사용하도록 이름 변경
 */
@Composable
fun NotificationSettingsToggleItem(
    title: String,
    description: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 설정 카드 내부 구분선
 * ⭐️ [오류 수정] 이 파일에서만 사용하도록 이름 변경 + HorizontalDivider로 수정
 */
@Composable
fun NotificationSettingsDivider() {
    HorizontalDivider( // ⭐️ [오류 수정] Divider -> HorizontalDivider
        color = Color.Gray.copy(alpha = 0.15f),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 4.dp) // 아이콘 영역만큼 패딩
    )
}

/**
 * 💡 하단 정보 박스
 */
@Composable
fun NotificationInfoBox() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.8f) // 반투명 흰색
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = "정보",
                tint = Color(0xff193cb8) // 피그마의 텍스트 색상
            )
            Text(
                text = "알림을 받으려면 기기 설정에서 앱 알림 권한을 허용해주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xff193cb8),
                lineHeight = 18.sp
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun NotificationSettingsScreenPreview() {
    KpopDancePracticeAITheme {
        NotificationSettingsScreen(onBackClick = {})
    }
}