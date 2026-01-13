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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.kpopdancepracticeai.KpopApplication
import com.example.kpopdancepracticeai.data.repository.AppRepository
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.test.IntegrationTestScreen
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- 1. 내비게이션 경로(Route) 정의 ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Login : Screen("login", "로그인", Icons.Default.Home)
    object SignUp : Screen("signUp", "회원가입", Icons.Default.Person)
    object SignUpSecond : Screen("signUpSecond", "회원가입2", Icons.Default.Person)

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

    object SearchResults : Screen("searchResults/{query}", "검색 결과", Icons.Default.Search)
    object SongDetail : Screen("songDetail/{songId}", "곡 상세", Icons.Default.MusicNote)

    object Test : Screen("test", "시스템 테스트", Icons.Default.Build)
    object SongPartSelect : Screen("songPartSelect/{songId}", "곡 파트 선택", Icons.Default.MusicNote)

    object DancePractice : Screen(
        "dancePractice/{songTitle}/{artistPart}/{difficulty}/{length}",
        "댄스 연습",
        Icons.Default.MusicNote
    )
    object Record : Screen(
        "record/{songTitle}/{artistPart}/{difficulty}",
        "녹화",
        Icons.Default.CameraAlt
    )

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

    val context = LocalContext.current
    val app = context.applicationContext as KpopApplication
    val repository = app.repository

    // MainViewModelFactory 클래스 대신 MainViewModel.provideFactory 사용
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(repository)
    )

    val authRepository = remember { AuthRepository(context) }
    val currentUser = authRepository.getCurrentUser()
    val userId = currentUser?.uid

    val startDestination = remember {
        if (currentUser != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    }

    val screensToHideBars = listOf(
        Screen.Login.route,
        Screen.SignUp.route,
        Screen.SignUpSecond.route,
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
        Screen.AnalysisLoading.route,
        Screen.Record.route,
        Screen.Analysis.route,
        Screen.Test.route
    )

    val showMainBars = if (currentRoute != null) {
        screensToHideBars.none { route ->
            currentRoute == route || currentRoute.startsWith("$route/")
        }
    } else {
        false
    }

    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            modifier = Modifier.systemBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = showMainBars &&
                            currentRoute != Screen.Home.route &&
                            currentRoute != Screen.Profile.route &&
                            currentRoute != Screen.Search.route &&
                            (currentRoute?.startsWith(Screen.SearchResults.route.substringBefore("/{")) == false),
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
            AppNavHost(
                navController = navController,
                innerPadding = innerPadding,
                viewModel = mainViewModel,
                startDestination = startDestination,
                userId = userId,
                repository = repository
            )
        }
    }
}

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
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    Surface(
        color = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(50.dp)),
        tonalElevation = 4.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
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

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    viewModel: MainViewModel,
    startDestination: String,
    userId: String?,
    repository: AppRepository
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onGoogleLoginSuccess = {
                    val dummyArg = Screen.encodeArg("GOOGLE_LOGIN")
                    navController.navigate("${Screen.SignUpSecond.route}/$dummyArg/$dummyArg")
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSubmit = { email, password ->
                    val encodedEmail = Screen.encodeArg(email)
                    val encodedPassword = Screen.encodeArg(password)
                    navController.navigate("${Screen.SignUpSecond.route}/$encodedEmail/$encodedPassword")
                }
            )
        }

        composable(
            route = "${Screen.SignUpSecond.route}/{email}/{password}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")?.let { Screen.decodeArg(it) } ?: ""
            val password = backStackEntry.arguments?.getString("password")?.let { Screen.decodeArg(it) } ?: ""

            SignUpSecondScreen(
                email = email,
                password = password,
                onSignUpComplete = { _, _ ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                // [수정] 정의되지 않은 파라미터(viewModel 등) 제거
                // onSearch, onSongClick, paddingValues만 전달
                onSearch = { query -> navController.navigate("searchResults/${Screen.encodeArg(query)}") },
                onSongClick = { songId -> navController.navigate("songDetail/$songId") },
                paddingValues = innerPadding
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                paddingValues = innerPadding,
                navController = navController
            )
        }

        composable(Screen.Analysis.route) {
            AnalysisScreen(
                paddingValues = innerPadding,
                onBackClick = { navController.popBackStack() }
            )
        }

        // [수정] ProfileScreen 호출부 - 원래 코드로 복구하여 userId 파라미터 제거
        // MainViewModel을 공유해서 사용
        composable(Screen.Profile.route) {
            // 별도의 ProfileViewModel 생성 없이 공유 ViewModel 사용
            ProfileScreen(
                paddingValues = innerPadding,
                // userId 파라미터는 제거됨
                viewModel = viewModel,
                onNavigateToProfileEdit = { navController.navigate(Screen.ProfileEdit.route) },
                onNavigateToPracticeSettings = { navController.navigate(Screen.PracticeSettings.route) },
                onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings.route) },
                onNavigateToPrivacySettings = { navController.navigate(Screen.PrivacySettings.route) },
                onNavigateToAppInfo = { navController.navigate(Screen.AppInfo.route) },
                onNavigateToWithdrawal = { navController.navigate(Screen.Withdrawal.route) },
                onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                onNavigateToTest = { navController.navigate(Screen.Test.route) }
            )
        }

        composable(Screen.Test.route) { IntegrationTestScreen() }
        composable(Screen.ProfileEdit.route) { ProfileEditScreen(onBackClick = { navController.popBackStack() }) }
        composable(Screen.PracticeSettings.route) { PracticeSettingsScreen(onBackClick = { navController.popBackStack() }) }
        composable(Screen.NotificationSettings.route) { NotificationSettingsScreen(onBackClick = { navController.popBackStack() }) }
        composable(Screen.PrivacySettings.route) { PrivacySettingsScreen(onBackClick = { navController.popBackStack() }) }
        composable(Screen.AppInfo.route) { AppInfoScreen(onBackClick = { navController.popBackStack() }) }

        composable(Screen.Withdrawal.route) {
            WithdrawalScreen(
                onBackClick = { navController.popBackStack() },
                onWithdrawConfirm = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )
        }

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

        composable(
            route = Screen.SongDetail.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            SongDetailScreen(
                songId = songId,
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SongPartSelect.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            SongPartSelectScreen(
                songId = songId,
                onBackClick = { navController.popBackStack() },
                onNavigateToPractice = { songTitle, artistPart, difficulty, length ->
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

        composable(
            route = Screen.DancePractice.route,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("artistPart") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("length") { type = NavType.StringType }
            )
        ) { backStackEntry ->
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
                    navController.navigate(Screen.AnalysisLoading.route) {
                        popUpTo(Screen.DancePractice.route) { inclusive = true }
                    }
                },
                onRecordClick = {
                    val encodedTitle = Screen.encodeArg(songTitle)
                    val encodedArtistPart = Screen.encodeArg(artistPart)
                    val encodedDifficulty = Screen.encodeArg(difficulty)
                    navController.navigate("record/$encodedTitle/$encodedArtistPart/$encodedDifficulty")
                },
                onSettingsClick = { navController.navigate(Screen.PracticeSettings.route) }
            )
        }

        composable(Screen.AnalysisLoading.route) {
            AnalysisWaitingScreen(
                onAnalysisComplete = {
                    navController.navigate(Screen.PracticeResult.route) {
                        popUpTo(Screen.AnalysisLoading.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PracticeResult.route) {
            PracticeResultScreen(
                onBackClick = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onCompareClick = { /* TODO */ },
                onRetryClick = { songId -> navController.navigate("songPartSelect/$songId") },
                onNextPartClick = { songId -> navController.navigate("songPartSelect/$songId") }
            )
        }

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
            val parts = artistPart.split("·").map { it.trim() }
            val artistName = parts.getOrNull(0) ?: "Unknown"
            val partName = parts.getOrNull(1) ?: artistPart

            RecordScreen(
                songTitle = songTitle,
                difficulty = difficulty,
                artist = artistName,
                part = partName,
                onBack = { navController.popBackStack() },
                onRecordingComplete = { s3Key -> navController.popBackStack() }
            )
        }
    }
}