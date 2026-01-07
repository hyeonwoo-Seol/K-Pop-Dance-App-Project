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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wifi
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
 * 연습 화면 설정 (전체 화면)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSettingsScreen(
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
    var isMirrorMode by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var countdownTime by remember { mutableStateOf("3초") }
    var isAutoUpload by remember { mutableStateOf(true) }
    var isWifiOnly by remember { mutableStateOf(true) }
    var cacheSize by remember { mutableStateOf("약 256 MB") }

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
                // .padding(horizontal = 16.dp), // TopAppBar의 전체 너비를 위해 패딩 제거 -> 내부 아이템에 적용
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 0. 상단바 (스크롤 가능하도록 이곳으로 이동) ---
                item {
                    TopAppBar(
                        title = { Text("연습 화면 설정", fontWeight = FontWeight.Bold) },
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
                        // Scaffold 내부 List에 있으므로 별도의 윈도우 인셋 처리 불필요 혹은 0으로 설정
                        windowInsets = WindowInsets(0.dp)
                    )
                }

                // --- 1. 카메라 설정 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "카메라") {
                            SettingsToggleItem(
                                title = "카메라 좌우 반전",
                                icon = Icons.Outlined.FlipCameraAndroid,
                                checked = isMirrorMode,
                                onCheckedChange = { isMirrorMode = it }
                            )
                            SettingsDivider()
                            SettingsToggleItem(
                                title = "기본 카메라",
                                description = if (isFrontCamera) "전면 카메라" else "후면 카메라",
                                icon = Icons.Outlined.Videocam,
                                checked = isFrontCamera,
                                onCheckedChange = { isFrontCamera = it }
                            )
                            SettingsDivider()
                            SettingsClickableItem(
                                title = "카운트다운 타이머",
                                description = countdownTime,
                                icon = Icons.Outlined.Timer,
                                onClick = { /* TODO: 타이머 선택 다이얼로그 표시 */ }
                            )
                        }
                    }
                }

                // --- 2. 업로드 설정 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "업로드") {
                            SettingsToggleItem(
                                title = "연습 영상 자동 전송",
                                icon = Icons.Outlined.UploadFile,
                                checked = isAutoUpload,
                                onCheckedChange = { isAutoUpload = it }
                            )
                            SettingsDivider()
                            SettingsToggleItem(
                                title = "WIFI에만 업로드",
                                icon = Icons.Outlined.Wifi,
                                checked = isWifiOnly,
                                onCheckedChange = { isWifiOnly = it }
                            )
                        }
                    }
                }

                // --- 3. 저장 공간 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "저장 공간") {
                            SettingsStorageItem(
                                title = "캐시 데이터 삭제",
                                description = cacheSize,
                                icon = Icons.Outlined.Storage,
                                onClearClick = { /* TODO: 캐시 삭제 로직 */ }
                            )
                        }
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
 * 설정 항목을 감싸는 카드 (재사용)
 */
@Composable
fun SettingsCard(
    title: String,
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
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
 */
@Composable
fun SettingsToggleItem(
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
 * 클릭 가능한 설정 항목 (재사용)
 */
@Composable
fun SettingsClickableItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.7f)
        )
    }
}

/**
 * 저장 공간 (캐시 삭제) 항목 (재사용)
 */
@Composable
fun SettingsStorageItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClearClick: () -> Unit
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
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        OutlinedButton(
            onClick = onClearClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Outlined.DeleteForever,
                contentDescription = "삭제",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("삭제")
        }
    }
}

/**
 * 설정 카드 내부 구분선
 */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = Color.Gray.copy(alpha = 0.15f),
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}


@Preview(showBackground = true)
@Composable
fun PracticeSettingsScreenPreview() {
    KpopDancePracticeAITheme {
        PracticeSettingsScreen(onBackClick = {})
    }
}