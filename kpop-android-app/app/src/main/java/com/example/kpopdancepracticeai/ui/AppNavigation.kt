package com.example.kpopdancepracticeai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.net.URLDecoder

// --- 1. 내비게이션 경로(Route) 정의 ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Login : Screen("login", "로그인", Icons.Default.Home)
    object Home : Screen("home", "홈", Icons.Default.Home)
    object Search : Screen("search", "검색", Icons.Default.Search)
    object Analysis : Screen("analysis", "분석", Icons.Default.Analytics)
    object Profile : Screen("profile", "프로필", Icons.Default.Person)
    object ProfileEdit : Screen("profileEdit", "프로필 설정", Icons.Default.Edit)
    object PracticeSettings : Screen("practiceSettings", "연습 화면 설정", Icons.Outlined.Settings)
    object NotificationSettings : Screen("notificationSettings", "알림 설정", Icons.Outlined.Notifications)
    object PrivacySettings : Screen("privacySettings", "개인정보 보호 및 권한", Icons.Outlined.Shield)
    object AppInfo : Screen("appInfo", "앱 정보", Icons.Outlined.Info)
    object Withdrawal : Screen("withdrawal", "회원 탈퇴", Icons.Outlined.ExitToApp)

    // 검색 시스템을 위한 경로
    object SearchResults : Screen("searchResults/{query}", "검색 결과", Icons.Default.Search)
    object SongDetail : Screen("songDetail/{songId}", "곡 상세", Icons.Default.MusicNote)

    // 곡 파트 선택 화면 경로
    object SongPartSelect : Screen("songPartSelect/{songId}", "곡 파트 선택", Icons.Default.MusicNote)

    // 댄스 연습 화면 경로
    object DancePractice : Screen(
        "dancePractice/{songTitle}/{artistPart}/{difficulty}/{length}",
        "댄스 연습",
        Icons.Default.MusicNote
    )
    //  [추가] 녹화 화면 경로 정의 (인자 전달 포함)
    object Record : Screen(
        "record/{songTitle}/{artistPart}/{difficulty}",
        "녹화",
        Icons.Default.CameraAlt
    )

    // 연습 결과 화면 경로
    object PracticeResult : Screen("practiceResult", "연습 결과", Icons.Default.Analytics)

    object AnalysisLoading : Screen("analysisLoading", "분석 중", Icons.Default.Analytics)
    companion object {
        fun encodeArg(arg: String): String {
            return URLEncoder.encode(arg, StandardCharsets.UTF_8.toString())
        }
        fun decodeArg(arg: String): String {
            return URLDecoder.decode(arg, StandardCharsets.UTF_8.toString())
        }
    }
}

// 탭 목록
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Profile,
)

// --- 2. 앱의 메인 Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpopDancePracticeApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 상/하단 바 숨길 화면 목록
    val screensToHideBars = listOf(
        Screen.Login.route,
        Screen.ProfileEdit.route,
        Screen.PracticeSettings.route,
        Screen.NotificationSettings.route,
        Screen.PrivacySettings.route,
        Screen.AppInfo.route,
        Screen.Withdrawal.route,
        Screen.SongDetail.route,
        Screen.SongPartSelect.route,
        Screen.DancePractice.route,
        Screen.PracticeResult.route,
        Screen.AnalysisLoading.route, // 로딩 화면에서도 바 숨김
        Screen.Record.route // RecordScreenMobile 화면에서 상단 제목과 하단 툴바 숨김
    )
    val showMainBars = currentRoute !in screensToHideBars

    // 피그마 디자인의 그라데이션 배경
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF), // 상단 연한 파랑
            Color(0xFFF0E8FF)  // 하단 연한 보라
        )
    )

    // 그라데이션을 Scaffold 배경으로 적용
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent, // Scaffold 배경을 투명하게
            topBar = {
                AnimatedVisibility(
                    // [수정됨] 홈, 프로필, 검색, 그리고 검색 결과 화면일 때는 상단 바를 숨깁니다
                    visible = showMainBars &&
                            currentRoute != Screen.Home.route &&
                            currentRoute != Screen.Profile.route &&
                            currentRoute != Screen.Search.route &&
                            currentRoute != Screen.SearchResults.route, // 검색 결과 화면에서도 상단 바 숨김
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    AppTopBar()
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showMainBars,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    AppBottomNavigationBar(navController = navController)
                }
            }
        ) { innerPadding ->
            // --- 3. 내비게이션 호스트 ---
            AppNavHost(
                navController = navController,
                innerPadding = innerPadding
            )
        }
    }
}

// 상단 앱 바
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "KPOP 댄스 연습 앱",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent // 그라데이션 배경이 보이도록 투명화
        )
    )
}

// --- 4. 하단 네비게이션 바 (완전 커스텀) ---
@Composable
fun AppBottomNavigationBar(navController: NavController) {
    // Surface로 배경을 만들고 Row로 직접 배치합니다.
    // 이렇게 하면 NavigationBarItem의 강제 높이 제한(80dp)을 피할 수 있습니다.
    Surface(
        color = Color.White.copy(alpha = 0.8f), // 반투명 흰색
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp) // 플로팅 여백
            .height(64.dp) //  [높이 고정] 64dp로 얇게 설정
            .clip(RoundedCornerShape(50.dp)), // 둥근 모서리
        tonalElevation = 4.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround, // 아이템 균등 배치
            verticalAlignment = Alignment.CenterVertically // 세로 중앙 정렬
        ) {
            bottomNavItems.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                // 커스텀 탭 아이템
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // 클릭 시 물결 효과 제거 (원하시면 추가 가능)
                        ) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

// --- 5. 내비게이션 경로 설정 ---
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        // 로그인 화면 (패딩 적용 X)
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // 홈 화면 (Scaffold 패딩 적용)
        composable(Screen.Home.route) {
            HomeScreen(
                // [수정] 검색어 입력 시 결과 화면으로 이동
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        navController.navigate("searchResults/$query")
                    }
                },
                // onSongClick 구현: SongDetail로 이동
                onSongClick = { songId ->
                    navController.navigate("songDetail/$songId")
                },
                paddingValues = innerPadding
            )
        }

        // 검색 화면 (Scaffold 패딩 적용)
        composable(Screen.Search.route) {
            SearchScreen(
                paddingValues = innerPadding,
                navController = navController
            )
        }

        // 분석 화면
        composable(Screen.Analysis.route) {
            AnalysisScreen(paddingValues = innerPadding
                    ,onBackClick = {})
        }

        // 프로필 화면 (Scaffold 패딩 적용)
        composable(Screen.Profile.route) {
            ProfileScreen(
                paddingValues = innerPadding,
                onNavigateToProfileEdit = {
                    navController.navigate(Screen.ProfileEdit.route)
                },
                onNavigateToPracticeSettings = {
                    navController.navigate(Screen.PracticeSettings.route)
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToPrivacySettings = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onNavigateToAppInfo = {
                    navController.navigate(Screen.AppInfo.route)
                },
                onNavigateToWithdrawal = {
                    navController.navigate(Screen.Withdrawal.route)
                },
                onNavigateToAnalysis = {
                    navController.navigate(Screen.Analysis.route)
                }
            )
        }

        // 프로필 설정 화면 (전체 화면, innerPadding 적용 X)
        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 연습 화면 설정 (전체 화면, innerPadding 적용 X)
        composable(Screen.PracticeSettings.route) {
            PracticeSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 알림 설정 화면 (전체 화면, innerPadding 적용 X)
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 개인정보 보호 및 권한 화면 (전체 화면, innerPadding 적용 X)
        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 앱 정보 화면 (전체 화면, innerPadding 적용 X)
        composable(Screen.AppInfo.route) {
            AppInfoScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 회원 탈퇴 화면 (전체 화면, innerPadding 적용 X)
        composable(Screen.Withdrawal.route) {
            WithdrawalScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onWithdrawConfirm = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // 검색 결과 화면
        composable(
            route = Screen.SearchResults.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchResultsScreen(
                query = query,
                navController = navController,
                paddingValues = innerPadding
            )
        }

        // 곡 상세 화면 (SongDetailScreen)
        composable(
            route = Screen.SongDetail.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            SongDetailScreen(
                songId = songId,
                navController = navController,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // 곡 파트 선택 화면 (SongPartSelectScreen)
        composable(
            route = Screen.SongPartSelect.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            SongPartSelectScreen(
                songId = songId,
                onBackClick = {
                    navController.popBackStack()
                },
                // onNavigateToPractice 람다 구현: 인코딩하여 PracticeScreenMobile로 이동
                onNavigateToPractice = { songTitle, artistPart, difficulty, length ->
                    // 특수 문자가 포함될 수 있는 문자열 인코딩
                    val encodedTitle = Screen.encodeArg(songTitle)
                    val encodedArtistPart = Screen.encodeArg(artistPart)
                    val encodedDifficulty = Screen.encodeArg(difficulty)
                    val encodedLength = Screen.encodeArg(length)

                    navController.navigate(
                        "dancePractice/$encodedTitle/$encodedArtistPart/$encodedDifficulty/$encodedLength"
                    )
                }
            )
        }

        // 댄스 연습 화면 (PracticeScreenMobile)
        composable(
            route = Screen.DancePractice.route,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("artistPart") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("length") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // 전달받은 파라미터를 디코딩하여 사용
            val songTitle = backStackEntry.arguments?.getString("songTitle")?.let { Screen.decodeArg(it) } ?: "곡 정보 없음"
            val artistPart = backStackEntry.arguments?.getString("artistPart")?.let { Screen.decodeArg(it) } ?: "아티스트 정보 없음"
            val difficulty = backStackEntry.arguments?.getString("difficulty")?.let { Screen.decodeArg(it) } ?: "난이도 정보 없음"
            val length = backStackEntry.arguments?.getString("length")?.let { Screen.decodeArg(it) } ?: "시간 정보 없음"

            PracticeScreenMobile(
                songTitle = songTitle,
                artistPart = artistPart,
                difficulty = difficulty,
                length = length,
                onBackClick = {
                    // [수정] 연습이 끝나면(뒤로가기/완료 시) 분석 대기 화면으로 이동
                    navController.navigate(Screen.AnalysisLoading.route) {
                        popUpTo(Screen.DancePractice.route) { inclusive = true }
                    }
                },
                //  [추가] '따라하기' 버튼 클릭 시 녹화 화면으로 이동하는 로직 연결
                onRecordClick = {
                    val encodedTitle = Screen.encodeArg(songTitle)
                    val encodedArtistPart = Screen.encodeArg(artistPart)
                    val encodedDifficulty = Screen.encodeArg(difficulty)

                    navController.navigate("record/$encodedTitle/$encodedArtistPart/$encodedDifficulty")
                },
                onSettingsClick = {
                    navController.navigate(Screen.PracticeSettings.route)
                }
            )
        }

        // 로딩 화면 (분석 대기)
        composable(Screen.AnalysisLoading.route) {
            // [수정] AnalysisWaitingScreen으로 교체 및 연결
            AnalysisWaitingScreen(
                onAnalysisComplete = {
                    // 분석 완료 시 결과 화면으로 이동
                    navController.navigate(Screen.PracticeResult.route) {
                        popUpTo(Screen.AnalysisLoading.route) { inclusive = true }
                    }
                }
            )
        }
        // 연습 결과 화면
        composable(Screen.PracticeResult.route) {
            PracticeResultScreen(
                onBackClick = { navController.popBackStack(Screen.Home.route, inclusive = false) }, // 홈으로 돌아가기
                onCompareClick = { /* TODO: 시각적 비교 화면으로 이동 */ },
                onRetryClick = { songId -> navController.navigate("songPartSelect/$songId") }, // 파트 선택 화면으로 돌아가기
                onNextPartClick = { songId -> navController.navigate("songPartSelect/$songId") } // 다음 파트 선택 화면으로 돌아가기
            )
        }

        //  [추가] 녹화 화면 (RecordScreen) 연결
        composable(
            route = Screen.Record.route,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("artistPart") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val songTitle = backStackEntry.arguments?.getString("songTitle")?.let { Screen.decodeArg(it) } ?: "제목 없음"
            val artistPart = backStackEntry.arguments?.getString("artistPart")?.let { Screen.decodeArg(it) } ?: "정보 없음"
            val difficulty = backStackEntry.arguments?.getString("difficulty")?.let { Screen.decodeArg(it) } ?: "보통"

            // artistPart 문자열(예: "BTS · Part 2")을 분리해서 전달 (임시 로직)
            val parts = artistPart.split("·").map { it.trim() }
            val artistName = parts.getOrNull(0) ?: "Unknown"
            val partName = parts.getOrNull(1) ?: artistPart

            RecordScreen(
                songTitle = songTitle,
                difficulty = difficulty,
                artist = artistName,
                part = partName,
                onBack = { navController.popBackStack() },
                onRecordingComplete = { s3Key ->
                    // 녹화 완료 후 처리 (예: 결과 화면으로 이동하거나 토스트 메시지)
                    // 현재는 간단히 뒤로 가기
                    navController.popBackStack()
                }
            )
        }
    }
}