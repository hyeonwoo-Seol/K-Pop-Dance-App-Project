package com.example.kpopdancepracticeai.ui

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
// import com.example.kpopdancepracticeai.viewmodel.MainViewModel // 추후 주석 해제

import com.example.kpopdancepracticeai.ui.theme.*

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
    // viewModel: MainViewModel = viewModel() // 추후 활성화: 뷰모델 주입
) {
    // 전략 문서 반영: ViewModel에서 UserStats 데이터를 관찰(Observe)해야 함
    // val userStats by viewModel.userStats.collectAsState(initial = null)
    // val achievements by viewModel.achievements.collectAsState(initial = emptyList())

    var selectedTab by rememberSaveable { mutableStateOf("통계") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ProfileHeaderCard(onDetailClick = onNavigateToAnalysis) }
        item { ProfileTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it }) }

        when (selectedTab) {
            "통계" -> {
                item { StatisticsRow() } // 추후 userStats 데이터 전달 필요
                item { AchievementsSummaryCard() }
                item { AcquiredBadgesCard() }
            }
            "업적" -> {
                item {
                    Text(text = "업적 및 성과", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
                // DB 연동 시 achievements 리스트 사용
                val achievementsList = listOf(
                    Triple("완벽주의자", "95% 이상의 정확도 5회 달성", 0.8f),
                    Triple("연습 벌레", "총 연습 시간 100시간 달성", 0.41f),
                    Triple("BTS 마스터", "BTS 챌린지 10개 완료", 0.5f),
                    Triple("챌린지 헌터", "모든 챌린지 1회 이상 완료", 0.1f),
                    Triple("신입 댄서", "첫 연습 영상 업로드", 1.0f)
                )
                items(achievementsList) { (title, description, progress) ->
                    AchievementCard(title = title, description = description, progress = progress, progressText = "${(progress * 100).toInt()}%")
                }
            }
            "설정" -> {
                item {
                    SettingsContent(onNavigateToProfileEdit, onNavigateToPracticeSettings, onNavigateToNotificationSettings, onNavigateToPrivacySettings, onNavigateToAppInfo, onNavigateToWithdrawal)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ProfileHeaderCard(onDetailClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 4.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, "프로필 이미지", modifier = Modifier.size(64.dp).clip(CircleShape), tint = Color.LightGray)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column { Text("내 프로필", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Text("김원준", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    CustomShadowButton("상세 통계 보기", onDetailClick, 92.dp, 35.dp, 11.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // DB 데이터 연동 시 실제 값 표시
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { StatColumn("경험치", "1250 XP"); StatColumn("Level", "Lv. 5") }
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("평균 정확도", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium); Text("87/100", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                    Spacer(modifier = Modifier.height(4.dp)); LinearProgressIndicator(progress = { 0.87f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column { Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
}

@Composable
fun ProfileTabRow(selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("통계", "업적", "설정").forEach { tabName ->
            val isSelected = selectedTab == tabName
            Box(modifier = Modifier.weight(1f).height(40.dp).shadow(2.dp, RoundedCornerShape(50.dp)).background(if (isSelected) Color(0xFF4C5E8A) else Color.White, RoundedCornerShape(50.dp)).then(if (!isSelected) Modifier.border(1.dp, BorderLight, RoundedCornerShape(50.dp)) else Modifier).clip(RoundedCornerShape(50.dp)).clickable { onTabSelected(tabName) }, contentAlignment = Alignment.Center) {
                Text(tabName, style = TextStyle(fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else TextGray))
            }
        }
    }
}

@Composable
fun CustomShadowButton(text: String, onClick: () -> Unit, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp, containerColor: Color = Color.White, contentColor: Color = TextGray, borderColor: Color = BorderLight) {
    Box(modifier = Modifier.width(width).height(height).shadow(2.dp, RoundedCornerShape(15.dp)).background(containerColor, RoundedCornerShape(15.dp)).border(1.dp, borderColor, RoundedCornerShape(15.dp)).clip(RoundedCornerShape(15.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = contentColor, textAlign = TextAlign.Center))
    }
}

@Composable
fun StatisticsRow() {
    // DB 데이터 연동 시 실제 값 사용
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(Modifier.weight(1f), "41H", "총 연습시간"); StatCard(Modifier.weight(1f), "5개", "완료한 곡 개수"); StatCard(Modifier.weight(1f), "89%", "평균 정확도")
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Surface(modifier = modifier.height(100.dp), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(value, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp), color = Color.Black); Spacer(modifier = Modifier.height(8.dp)); Text(label, style = TextStyle(fontSize = 12.sp, color = Color.Gray), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AchievementsSummaryCard() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 이모티콘 제거
            Text("진행중인 업적 요약", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf("완벽주의자" to 0.8f, "연습 벌레" to 0.3f, "BTS 마스터" to 0.5f, "챌린지 헌터" to 0.1f).forEach { (l, p) -> AchievementProgressItem(l, p, "${(p * 100).toInt()}%") }
        }
    }
}

@Composable
fun AchievementProgressItem(label: String, progress: Float, progressText: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)))
            Text(progressText, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcquiredBadgesCard() {
    val badges = mapOf("BTS 마스터" to Color(0xFFEBEBFF), "NewJeans 팬" to Color(0xFFD6F5FF), "BLACKPINK 전문가" to Color(0xFFFFD6EB), "초급자 졸업" to Color(0xFFD9FFE5), "중급자" to Color(0xFFFFFAD6))
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 이모티콘 제거
            Text("획득한 뱃지", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.forEach { (text, color) -> BadgeChip(text = text, color = color) }
            }
        }
    }
}

// BadgeChip 컴포넌트는 Components.kt 또는 다른 파일에 정의되어 있다고 가정

@Composable
fun SettingsContent(onNavigateToProfileEdit: () -> Unit, onNavigateToPracticeSettings: () -> Unit, onNavigateToNotificationSettings: () -> Unit, onNavigateToPrivacySettings: () -> Unit, onNavigateToAppInfo: () -> Unit, onNavigateToWithdrawal: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                SettingsMenuItem("프로필 설정", Icons.Outlined.Person, Color(0xFFEBF0FF), onClick = onNavigateToProfileEdit); SettingsMenuDivider()
                SettingsMenuItem("연습 화면 설정", Icons.Outlined.Tv, Color(0xFFF0EFFF), onClick = onNavigateToPracticeSettings); SettingsMenuDivider()
                SettingsMenuItem("알림 설정", Icons.Outlined.Notifications, Color(0xFFFFF9E6), onClick = onNavigateToNotificationSettings); SettingsMenuDivider()
                SettingsMenuItem("개인정보 보호 및 권한", Icons.Outlined.Shield, Color(0xFFE6F7EB), onClick = onNavigateToPrivacySettings); SettingsMenuDivider()
                SettingsMenuItem("앱 정보", Icons.Outlined.Info, Color(0xFFF3F4F6), onClick = onNavigateToAppInfo); SettingsMenuDivider()
                SettingsMenuItem("회원 탈퇴", Icons.Outlined.ExitToApp, Color(0xFFFFF0F0), textColor = Color.Red, onClick = onNavigateToWithdrawal)
            }
        }
    }
}

@Composable
fun SettingsMenuItem(text: String, icon: ImageVector, iconBgColor: Color, textColor: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBgColor), contentAlignment = Alignment.Center) { Icon(icon, text, modifier = Modifier.size(24.dp), tint = Color.Black.copy(alpha = 0.8f)) }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray.copy(alpha = 0.7f))
    }
}

@Composable
fun SettingsMenuDivider() { HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.padding(start = 76.dp, end = 20.dp)) }

@Composable
fun AchievementCard(title: String, description: String, progress: Float, progressText: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDAE0FF))) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("진행률", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.Black)
                Text(progressText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), trackColor = Color(0x33030213), color = Color(0xff030213))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    KpopDancePracticeAITheme { ProfileScreen(PaddingValues(), {}, {}, {}, {}, {}, {}, {}) }
}