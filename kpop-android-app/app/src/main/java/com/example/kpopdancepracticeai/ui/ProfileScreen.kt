package com.example.kpopdancepracticeai.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kpopdancepracticeai.viewmodel.AchievementUiModel
import com.example.kpopdancepracticeai.viewmodel.BadgeUiModel
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import com.example.kpopdancepracticeai.ui.theme.*

val BorderLight = Color(0xFFE0E0E0)
val TextGray = Color(0xFF757575)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToPracticeSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToPrivacySettings: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
    onNavigateToWithdrawal: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    viewModel: MainViewModel
) {
    val userStats by viewModel.userStats.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    val levelInfo by viewModel.userLevelInfo.collectAsState()
    val achievements by viewModel.achievementProgress.collectAsState()
    val badges by viewModel.userBadges.collectAsState()
    val topPracticedChoreos by viewModel.topPracticedChoreos.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.updateUsageTime()
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf("통계") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeaderCard(
                userProfile = userProfile,
                userStats = userStats,
                levelInfo = levelInfo,
                onDetailClick = onNavigateToAnalysis
            )
        }
        item { ProfileTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it }) }

        when (selectedTab) {
            "통계" -> {
                item { StatisticsRow(userStats = userStats) }
                item { AchievementsSummaryCard(achievements = achievements, topPracticedChoreos = topPracticedChoreos) }
                item { AcquiredBadgesCard(badges = badges) }
            }
            "업적" -> {
                item {
                    Text(
                        text = "업적 및 성과",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                if (achievements.isEmpty()) {
                    item {
                        Text(
                            "아직 진행중인 업적이 없습니다.",
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    items(achievements) { item ->
                        AchievementCard(
                            title = item.title,
                            description = item.description,
                            progressDetail = item.progressText,
                            progress = item.progress,
                            progressText = "${(item.progress * 100).toInt()}%"
                        )
                    }
                }
            }
            "설정" -> {
                item {
                    SettingsContent(
                        onNavigateToProfileEdit,
                        onNavigateToPracticeSettings,
                        onNavigateToNotificationSettings,
                        onNavigateToPrivacySettings,
                        onNavigateToAppInfo,
                        onNavigateToWithdrawal,
                        onSyncClick = { viewModel.refreshData() },
                        isSyncing = isSyncing
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ProfileHeaderCard(
    userProfile: com.example.kpopdancepracticeai.data.entity.User?,
    userStats: com.example.kpopdancepracticeai.data.entity.UserStats?,
    levelInfo: Pair<Int, Pair<Long, Long>>,
    onDetailClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [수정됨] DB에 프로필 이미지 경로가 있으면 노출
            if (!userProfile?.profileImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = userProfile?.profileImageUrl,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    "프로필 이미지",
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    tint = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("내 프로필", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            text = userProfile?.name ?: "사용자",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    CustomShadowButton("상세 통계 보기", onDetailClick, 92.dp, 35.dp, 11.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))

                val appLevel = levelInfo.first
                val currentExp = levelInfo.second.first
                val maxExp = levelInfo.second.second
                val avgAccuracy = userStats?.avgAccuracy?.toFloat() ?: 0f

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatColumn("평균 정확도", "${avgAccuracy.toInt()}%")
                    StatColumn("Level", "Lv. $appLevel")
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("경험치", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("$currentExp / $maxExp XP", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (maxExp > 0) (currentExp.toFloat() / maxExp).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileTabRow(selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("통계", "업적", "설정").forEach { tabName ->
            val isSelected = selectedTab == tabName
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .shadow(2.dp, RoundedCornerShape(50.dp))
                    .background(
                        if (isSelected) Color(0xFF4C5E8A) else Color.White,
                        RoundedCornerShape(50.dp)
                    )
                    .then(if (!isSelected) Modifier.border(1.dp, BorderLight, RoundedCornerShape(50.dp)) else Modifier)
                    .clip(RoundedCornerShape(50.dp))
                    .clickable { onTabSelected(tabName) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tabName,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextGray
                    )
                )
            }
        }
    }
}

@Composable
fun CustomShadowButton(
    text: String,
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    containerColor: Color = Color.White,
    contentColor: Color = TextGray,
    borderColor: Color = BorderLight
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .shadow(2.dp, RoundedCornerShape(15.dp))
            .background(containerColor, RoundedCornerShape(15.dp))
            .border(1.dp, borderColor, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun StatisticsRow(userStats: com.example.kpopdancepracticeai.data.entity.UserStats?) {
    val totalSeconds = userStats?.totalPlayTime ?: 0L
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60

    val totalTimeText = if (hours > 0) {
        "${hours}H ${mins}M"
    } else {
        "${mins}M" // 1시간 미만이면 분 단위로 표시
    }

    val completedSongs = "${userStats?.completedParts ?: 0}개"
    val avgAccuracy = "${userStats?.avgAccuracy?.toInt() ?: 0}%"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(Modifier.weight(1f), totalTimeText, "총 연습시간")
        StatCard(Modifier.weight(1f), completedSongs, "완료한 곡 개수")
        StatCard(Modifier.weight(1f), avgAccuracy, "평균 정확도")
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp), color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = TextStyle(fontSize = 12.sp, color = Color.Gray), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AchievementsSummaryCard(
    achievements: List<AchievementUiModel>,
    topPracticedChoreos: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopPracticedChoreosSection(topPracticedChoreos = topPracticedChoreos)
            Text("진행중인 업적 요약", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (achievements.isEmpty()) {
                Text("진행 중인 업적이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                achievements.take(4).forEach { item ->
                    AchievementProgressItem(item.title, item.progress, item.progressText)
                }
            }
        }
    }
}

@Composable
private fun TopPracticedChoreosSection(topPracticedChoreos: List<String>) {
    Text("가장 많이 연습한 안무 TOP 3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (topPracticedChoreos.isEmpty()) {
        Text("아직 연습 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        return
    }

    topPracticedChoreos.forEachIndexed { index, title ->
        Text(
            text = "${index + 1}. $title",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}

@Composable
fun AchievementProgressItem(label: String, progress: Float, progressText: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Text(
                progressText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcquiredBadgesCard(badges: List<BadgeUiModel>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("획득한 뱃지", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (badges.isEmpty()) {
                Text("획득한 뱃지가 없습니다.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    badges.forEach { badge ->
                        BadgeChip(text = badge.name, color = badge.color)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsContent(
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToPracticeSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToPrivacySettings: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
    onNavigateToWithdrawal: () -> Unit,
    onSyncClick: () -> Unit,
    isSyncing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "설정",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                SettingsMenuItem("프로필 설정", Icons.Outlined.Person, Color(0xFFEBF0FF), onClick = onNavigateToProfileEdit); SettingsMenuDivider()
                SettingsMenuItem("연습 화면 설정", Icons.Outlined.Tv, Color(0xFFF0EFFF), onClick = onNavigateToPracticeSettings); SettingsMenuDivider()
                SettingsMenuItem("알림 설정", Icons.Outlined.Notifications, Color(0xFFFFF9E6), onClick = onNavigateToNotificationSettings); SettingsMenuDivider()
                SettingsMenuItem("개인정보 보호 및 권한", Icons.Outlined.Shield, Color(0xFFE6F7EB), onClick = onNavigateToPrivacySettings); SettingsMenuDivider()
                SettingsMenuItem("앱 정보", Icons.Outlined.Info, Color(0xFFF3F4F6), onClick = onNavigateToAppInfo); SettingsMenuDivider()

                SettingsMenuItem(
                    text = if (isSyncing) "데이터 받아오는 중..." else "최신 데이터 동기화",
                    icon = if (isSyncing) Icons.Default.HourglassEmpty else Icons.Default.Sync,
                    iconBgColor = Color(0xFFE3F2FD),
                    textColor = if (isSyncing) Color.Gray else Color.Black,
                    onClick = onSyncClick
                ); SettingsMenuDivider()
                SettingsMenuItem(
                    "회원 탈퇴",
                    Icons.Outlined.ExitToApp,
                    Color(0xFFFFF0F0),
                    textColor = Color.Red,
                    onClick = onNavigateToWithdrawal
                )
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    text: String,
    icon: ImageVector,
    iconBgColor: Color,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, text, modifier = Modifier.size(24.dp), tint = Color.Black.copy(alpha = 0.8f))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray.copy(alpha = 0.7f))
    }
}

@Composable
fun SettingsMenuDivider() {
    HorizontalDivider(
        color = Color.Gray.copy(alpha = 0.15f),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 76.dp, end = 20.dp)
    )
}

@Composable
fun AchievementCard(
    title: String,
    description: String,
    progressDetail: String,
    progress: Float,
    progressText: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDAE0FF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
            Text(progressDetail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("진행률", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.Black)
                Text(progressText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                trackColor = Color(0x33030213),
                color = Color(0xff030213)
            )
        }
    }
}

@Composable
fun BadgeChip(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
}
