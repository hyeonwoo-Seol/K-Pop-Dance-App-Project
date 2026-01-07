package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WarningAmber
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
 * 회원 탈퇴 화면 (전체 화면)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onBackClick: () -> Unit,
    onWithdrawConfirm: () -> Unit
) {
    // 앱 전체의 그라데이션 배경
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF), // 상단 연한 파랑
            Color(0xFFF0E8FF)  // 하단 연한 보라
        )
    )

    // 약관 동의 상태
    var isAgreed by remember { mutableStateOf(false) }

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
                        title = { Text("회원 탈퇴", fontWeight = FontWeight.Bold) },
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

                // --- 1. 상단 경고 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WithdrawalWarningCard()
                    }
                }

                // --- 2. 삭제될 데이터 카드 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DeletedDataCard()
                    }
                }

                // --- 3. 약관 동의 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AgreementCard(
                            isAgreed = isAgreed,
                            onAgreedChange = { isAgreed = it }
                        )
                    }
                }

                // --- 4. 하단 버튼 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ActionButtons(
                            isAgreed = isAgreed,
                            onCancelClick = onBackClick,
                            onWithdrawClick = onWithdrawConfirm
                        )
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
 * 1. 상단 경고 카드
 */
@Composable
fun WithdrawalWarningCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xfffef2f2), // 연한 빨강 배경
        border = BorderStroke(1.dp, Color(0xfffecaca)) // 연한 빨강 테두리
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = "경고",
                tint = Color(0xff9f0712), // 진한 빨강
                modifier = Modifier.padding(top = 2.dp)
            )
            Column {
                Text(
                    text = "정말 탈퇴하시겠습니까?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xff9f0712)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "회원 탈퇴 시 모든 데이터가 영구적으로 삭제되며, 복구할 수 없습니다. 신중하게 결정해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xffc10007),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * 2. 삭제될 데이터 목록 카드
 */
@Composable
fun DeletedDataCard() {
    SettingsCard(title = "삭제될 데이터") {
        DeletedDataItem(
            icon = Icons.Outlined.Videocam,
            title = "연습 영상",
            description = "업로드한 모든 댄스 영상"
        )
        SettingsDivider()
        DeletedDataItem(
            icon = Icons.Outlined.TrendingUp,
            title = "분석 데이터",
            description = "연습 기록 및 성과 데이터"
        )
        SettingsDivider()
        DeletedDataItem(
            icon = Icons.Outlined.EmojiEvents,
            title = "업적 및 배지",
            description = "획득한 모든 업적과 배지"
        )
        SettingsDivider()
        DeletedDataItem(
            icon = Icons.Outlined.BookmarkBorder,
            title = "저장한 콘텐츠",
            description = "북마크 및 즐겨찾기"
        )
    }
}

/**
 * 2-1. 삭제될 데이터 항목 (재사용)
 */
@Composable
fun DeletedDataItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xfff9fafb), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xff1e2939)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xff6a7282)
            )
        }
    }
}

/**
 * 3. 약관 동의 카드
 */
@Composable
fun AgreementCard(
    isAgreed: Boolean,
    onAgreedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAgreedChange(!isAgreed) }, // 영역 전체 클릭 가능
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xffffc9c9)) // 연한 빨강 테두리
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isAgreed,
                onCheckedChange = onAgreedChange,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "위 내용을 모두 확인했으며, 회원 탈퇴 시 모든 데이터가 영구적으로 삭제되는 것에 동의합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xff364153),
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 4. 하단 액션 버튼 (취소, 탈퇴)
 */
@Composable
fun ActionButtons(
    isAgreed: Boolean,
    onCancelClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Black
            )
        ) {
            Text("취소", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onWithdrawClick,
            enabled = isAgreed, // 동의 여부에 따라 활성화
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error, // 활성화 시 빨간색
                contentColor = Color.White,
                disabledContainerColor = Color(0xffd1d5dc), // 비활성화 시 회색
                disabledContentColor = Color.White
            )
        ) {
            Text("탈퇴하기", fontWeight = FontWeight.Bold)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun WithdrawalScreenPreview() {
    KpopDancePracticeAITheme {
        WithdrawalScreen(onBackClick = {}, onWithdrawConfirm = {})
    }
}