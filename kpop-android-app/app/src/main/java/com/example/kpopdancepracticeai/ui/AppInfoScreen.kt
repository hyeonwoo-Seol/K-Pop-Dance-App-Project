package com.example.kpopdancepracticeai.ui

import com.example.kpopdancepracticeai.ui.motion.rememberIosLikeFlingBehavior

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kpopdancepracticeai.KpopApplication
import com.example.kpopdancepracticeai.R
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.util.sendSupportEmail
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    onBackClick: () -> Unit,
    onNavigateToFaq: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToOpenSource: () -> Unit // ⭐️ 이 부분이 추가되었습니다.
) {
    val context = LocalContext.current
    val app = context.applicationContext as KpopApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()

    // 기기에서 실제 앱 버전을 불러옵니다.
    val versionInfo = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0.0"

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }

            "$versionName (Build $versionCode) - 최신 버전입니다"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0 - 버전 정보를 불러올 수 없습니다"
        }
    }

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
        ) { innerPadding ->
            LazyColumn(
            flingBehavior = rememberIosLikeFlingBehavior(),
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 0. 상단바 ---
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
                        windowInsets = WindowInsets(0.dp)
                    )
                }

                // --- 1. 앱 아이콘 및 이름 ---
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        AppIcon() // 앱 아이콘
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

                // --- 2. 지원 및 피드백 카드 (문의하기, FAQ) ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsCard(title = "지원 및 피드백") {
                            SettingsClickableItem(
                                title = "문의하기",
                                description = "",
                                icon = Icons.Outlined.ChatBubbleOutline,
                                onClick = { context.sendSupportEmail() }
                            )
                            SettingsDivider()
                            SettingsClickableItem(
                                title = "FAQ",
                                description = "",
                                icon = Icons.Outlined.HelpOutline,
                                onClick = onNavigateToFaq
                            )
                        }
                    }
                }

                // --- 3. 법적 고지 카드 (이용 약관, 처리 방침) ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsCard(title = "법적 고지") {
                                SettingsClickableItem(
                                    title = "서비스 이용 약관",
                                    description = "",
                                    icon = Icons.Outlined.Description,
                                    onClick = onNavigateToTerms
                                )
                                SettingsDivider()
                                SettingsClickableItem(
                                    title = "개인정보 처리 방침",
                                    description = "",
                                    icon = Icons.Outlined.Shield,
                                    onClick = onNavigateToPrivacyPolicy
                                )
                            }
                        }
                    }
                }

                // --- 4. 앱 세부 정보 카드 (버전 정보, 라이선스) ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsCard(title = "앱 세부 정보") {
                                SettingsClickableItem(
                                    title = "버전 정보",
                                    description = versionInfo,
                                    icon = Icons.Outlined.Info,
                                    onClick = {
                                        Toast.makeText(context, "현재 최신 버전을 사용 중입니다.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                SettingsDivider()
                                SettingsClickableItem(
                                    title = "오픈소스 라이선스",
                                    description = "",
                                    icon = Icons.Outlined.Code,
                                    onClick = onNavigateToOpenSource // ⭐️ 여기 연결이 누락되어 있었습니다! 이제 정상 작동합니다.
                                )
                                SettingsDivider()
                                SettingsClickableItem(
                                    title = "테스트 데이터 생성(개발자)",
                                    description = "partId 5401, 5404, 5422, 5424, 5511, 4522, 5681에 대해 5~8일 / 1~3회 랜덤 생성",
                                    icon = Icons.Outlined.Code,
                                    onClick = {
                                        scope.launch {
                                            val currentUser = AuthRepository(context).getCurrentUser()
                                            if (currentUser == null) {
                                                Toast.makeText(context, "로그인 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }

                                            val inserted = repository.generateDeveloperPracticeHistory(currentUser.uid)
                                            Toast.makeText(
                                                context,
                                                if (inserted > 0) "테스트 데이터 ${inserted}건 생성 완료" else "대상 파트가 없어 생성된 데이터가 없습니다.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
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

@Composable
fun AppIcon() {
    Surface(
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "앱 아이콘",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppInfoScreenPreview() {
    KpopDancePracticeAITheme {
        AppInfoScreen(
            onBackClick = {},
            onNavigateToFaq = {},
            onNavigateToTerms = {},
            onNavigateToPrivacyPolicy = {},
            onNavigateToOpenSource = {}
        )
    }
}
