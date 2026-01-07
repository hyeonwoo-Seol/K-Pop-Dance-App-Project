package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

/**
 * 앱 정보 화면 (전체 화면)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    onBackClick: () -> Unit
) {
    // 앱 전체의 그라데이션 배경
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF), // 상단 연한 파랑
            Color(0xFFF0E8FF)  // 하단 연한 보라
        )
    )

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
                horizontalAlignment = Alignment.CenterHorizontally // 아이콘, 텍스트 중앙 정렬
            ) {
                // --- 0. 상단바 (스크롤 가능하도록 이곳으로 이동) ---
                item {
                    TopAppBar(
                        title = { Text("앱 정보", fontWeight = FontWeight.Bold) },
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

                // --- 1. 앱 아이콘 및 이름 ---
                item {
                    // 중앙 정렬 및 여백 유지를 위한 Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        AppIcon() // 앱 아이콘 Composable
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Dance Practice App",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xff1e2939)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // --- 2. 지원 및 피드백 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "지원 및 피드백") {
                            SettingsClickableItem(
                                title = "문의하기",
                                description = "",
                                icon = Icons.Outlined.ChatBubbleOutline,
                                onClick = { /* TODO: 문의하기 화면 이동 */ }
                            )
                            SettingsDivider()
                            SettingsClickableItem(
                                title = "FAQ",
                                description = "",
                                icon = Icons.Outlined.HelpOutline,
                                onClick = { /* TODO: FAQ 화면 이동 */ }
                            )
                        }
                    }
                }

                // --- 3. 법적 고지 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsCard(title = "법적 고지") {
                                SettingsClickableItem(
                                    title = "서비스 이용 약관",
                                    description = "",
                                    icon = Icons.Outlined.Description,
                                    onClick = { /* TODO: 서비스 이용 약관 화면 이동 */ }
                                )
                                SettingsDivider()
                                SettingsClickableItem(
                                    title = "개인정보 처리 방침",
                                    description = "",
                                    icon = Icons.Outlined.Shield,
                                    onClick = { /* TODO: 개인정보 처리 방침 화면 이동 */ }
                                )
                            }
                        }
                    }
                }

                // --- 4. 앱 세부 정보 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsCard(title = "앱 세부 정보") {
                                SettingsClickableItem(
                                    title = "버전 정보",
                                    description = "1.0.0 (Build 100)",
                                    icon = Icons.Outlined.Info,
                                    onClick = { } // 버전 정보는 보통 클릭 안 됨
                                )
                                SettingsDivider()
                                SettingsClickableItem(
                                    title = "오픈소스 라이선스",
                                    description = "",
                                    icon = Icons.Outlined.Code,
                                    onClick = { /* TODO: 오픈소스 라이선스 화면 이동 */ }
                                )
                            }
                        }
                    }
                }

                // --- 5. Copyright ---
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = "© 2025 Dance Practice App",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "All rights reserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

/**
 * 앱 정보 화면의 아이콘
 */
@Composable
fun AppIcon() {
    val iconGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF8A2BE2), // Violet
            Color(0xFFFF69B4)  // HotPink
        )
    )
    Surface(
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(20.dp), // 부드러운 사각형
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(iconGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💃",
                fontSize = 40.sp
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AppInfoScreenPreview() {
    KpopDancePracticeAITheme {
        AppInfoScreen(onBackClick = {})
    }
}