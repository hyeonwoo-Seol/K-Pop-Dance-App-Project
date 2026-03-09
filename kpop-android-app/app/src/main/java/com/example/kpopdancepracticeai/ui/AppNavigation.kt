package com.example.kpopdancepracticeai.ui

import android.content.Context
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
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.filled.CloudDownload
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
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.test.IntegrationTestScreen
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Login : Screen("login", "로그인", Icons.Default.Home)
    object SignUp : Screen("signUp", "회원가입", Icons.Default.Person)
    object SignUpSecond : Screen("signUpSecond", "회원가입2", Icons.Default.Person)

    object Home : Screen("home", "홈", Icons.Default.Home)

    object VideoDownload : Screen("videoDownload", "초기 설정", Icons.Default.CloudDownload)
    object Search : Screen("search", "검색", Icons.Default.Search)
    object Analysis : Screen("analysis", "분석", Icons.Default.Analytics)
    object Profile : Screen("profile", "프로필", Icons.Default.Person)
    object ProfileEdit : Screen("profileEdit", "프로필 설정", Icons.Default.Edit)
    object PracticeSettings : Screen("practiceSettings", "연습 화면 설정", Icons.Outlined.Settings)
    object NotificationSettings : Screen("notificationSettings", "알림 설정", Icons.Outlined.Notifications)
    object PrivacySettings : Screen("privacySettings", "개인정보 보호 및 권한", Icons.Outlined.Shield)
    object AppInfo : Screen("appInfo", "앱 정보", Icons.Outlined.Info)
    object Withdrawal : Screen("withdrawal", "회원 탈퇴", Icons.Outlined.ExitToApp)

    object TermsOfService : Screen("termsOfService", "이용 약관", Icons.Outlined.Description)
    object PrivacyPolicy : Screen("privacyPolicy", "개인정보 처리 방침", Icons.Outlined.Shield)
    object OpenSourceLicense : Screen("openSourceLicense", "오픈소스 라이선스", Icons.Outlined.Code)

    object SearchResults : Screen("searchResults/{query}", "검색 결과", Icons.Default.Search)
    object SongDetail : Screen("songDetail/{songId}", "곡 상세", Icons.Default.MusicNote)

    object Test : Screen("test", "시스템 테스트", Icons.Default.Build)
    object SongPartSelect : Screen("songPartSelect/{songId}", "곡 파트 선택", Icons.Default.MusicNote)

    // 💡 [수정] URL을 전달받기 위해 경로 끝에 /{videoUrl} 추가
    object DancePractice : Screen(
        "dancePractice/{songTitle}/{artistPart}/{difficulty}/{length}/{videoUrl}",
        "댄스 연습",
        Icons.Default.MusicNote
    )
    object Record : Screen(
        "record/{songTitle}/{artistPart}/{difficulty}/{videoUrl}",
        "녹화",
        Icons.Default.CameraAlt
    )

    object PracticeResult : Screen(
        "practiceResult/{jsonFileName}/{videoPath}",
        "연습 결과",
        Icons.Default.Analytics
    )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val app = context.applicationContext as KpopApplication
    val repository = app.repository
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(repository)
    )
    val authRepository = remember { AuthRepository(context) }

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val isVideoDownloaded = remember { prefs.getBoolean("is_expert_video_downloaded", false) }

    val startDestination = remember {
        if (authRepository.getCurrentUser() != null) {
            if (isVideoDownloaded) Screen.Home.route else Screen.VideoDownload.route
        } else {
            Screen.Login.route
        }
    }

    val currentUser = authRepository.getCurrentUser()
    if (currentUser != null) {
        androidx.compose.runtime.LaunchedEffect(currentUser.uid) {
            viewModel.loadInitialData(currentUser.uid)
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
        "faq",
        Screen.TermsOfService.route,
        Screen.PrivacyPolicy.route,
        Screen.OpenSourceLicense.route,
        Screen.Withdrawal.route,
        Screen.SongDetail.route,
        Screen.SongPartSelect.route,
        Screen.DancePractice.route,
        Screen.AnalysisLoading.route,
        Screen.Record.route,
        Screen.Analysis.route,
        Screen.Test.route
    )

    val showMainBars = if (currentRoute != null) {
        val isResultScreen = currentRoute.startsWith("practiceResult/")
        if (isResultScreen) false
        else screensToHideBars.none { route ->
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
                viewModel = viewModel,
                startDestination = startDestination,
                authRepository = authRepository
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
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.VideoDownload.route) {
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
        composable(Screen.VideoDownload.route) {
            VideoDownloadScreen(
                onDownloadComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.VideoDownload.route) { inclusive = true }
                    }
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
                viewModel = viewModel,
                email = email,
                password = password,
                onSignUpComplete = { _, _ ->
                    navController.navigate(Screen.VideoDownload.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        navController.navigate("searchResults/$query")
                    }
                },
                onSongClick = { songId ->
                    navController.navigate("songDetail/$songId")
                },
                onTestClick = {
                    navController.navigate("songPartSelect/1")
                },
                modifier = Modifier.padding(innerPadding)
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

        composable(Screen.Profile.route) {
            ProfileScreen(
                paddingValues = innerPadding,
                onNavigateToProfileEdit = { navController.navigate(Screen.ProfileEdit.route) },
                onNavigateToPracticeSettings = { navController.navigate(Screen.PracticeSettings.route) },
                onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings.route) },
                onNavigateToPrivacySettings = { navController.navigate(Screen.PrivacySettings.route) },
                onNavigateToAppInfo = { navController.navigate(Screen.AppInfo.route) },
                onNavigateToWithdrawal = { navController.navigate(Screen.Withdrawal.route) },
                onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                onNavigateToTest = { navController.navigate(Screen.Test.route) },
                viewModel = viewModel
            )
        }

        composable(Screen.Test.route) {
            IntegrationTestScreen(navController)
        }

        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(Screen.PracticeSettings.route) {
            PracticeSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.AppInfo.route) {
            AppInfoScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToFaq = { navController.navigate("faq") },
                onNavigateToTerms = { navController.navigate(Screen.TermsOfService.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToOpenSource = { navController.navigate(Screen.OpenSourceLicense.route) }
            )
        }

        composable("faq") {
            FaqScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.OpenSourceLicense.route) {
            OpenSourceLicenseScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

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
                viewModel = viewModel,
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
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                // 💡 [수정] 5번째 인자인 URL(videoUrl)도 함께 인코딩해서 라우터로 넘겨줍니다.
                onNavigateToPractice = { songTitle, artistPart, difficulty, length, videoUrl ->
                    val encodedTitle = Screen.encodeArg(songTitle)
                    val encodedArtistPart = Screen.encodeArg(artistPart)
                    val encodedDifficulty = Screen.encodeArg(difficulty)
                    val encodedLength = Screen.encodeArg(length)
                    val encodedUrl = Screen.encodeArg(videoUrl)
                    navController.navigate("dancePractice/$encodedTitle/$encodedArtistPart/$encodedDifficulty/$encodedLength/$encodedUrl")
                }
            )
        }
        composable(
            route = Screen.DancePractice.route,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("artistPart") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("length") { type = NavType.StringType },
                // 💡 URL을 위한 argument 추가
                navArgument("videoUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val songTitle = backStackEntry.arguments?.getString("songTitle")?.let { Screen.decodeArg(it) } ?: ""
            val artistPart = backStackEntry.arguments?.getString("artistPart")?.let { Screen.decodeArg(it) } ?: ""
            val difficulty = backStackEntry.arguments?.getString("difficulty")?.let { Screen.decodeArg(it) } ?: ""
            val length = backStackEntry.arguments?.getString("length")?.let { Screen.decodeArg(it) } ?: ""
            val videoUrl = backStackEntry.arguments?.getString("videoUrl")?.let { Screen.decodeArg(it) } ?: ""

            PracticeScreenMobile(
                songTitle = songTitle,
                artistPart = artistPart,
                difficulty = difficulty,
                length = length,
                videoUrl = videoUrl,
                onBackClick = { navController.popBackStack() },
                onRecordClick = {
                    val encodedTitle = Screen.encodeArg(songTitle)
                    val encodedArtistPart = Screen.encodeArg(artistPart)
                    val encodedDifficulty = Screen.encodeArg(difficulty)
                    val encodedUrl = Screen.encodeArg(videoUrl) // 💡 전달받은 URL을 RecordScreen으로 다시 패스!
                    navController.navigate("record/$encodedTitle/$encodedArtistPart/$encodedDifficulty/$encodedUrl")
                },
                onSettingsClick = { navController.navigate(Screen.PracticeSettings.route) }
            )
        }
        composable(Screen.AnalysisLoading.route) {
            AnalysisWaitingScreen(
                onAnalysisComplete = { }
            )
        }
        composable(
            route = Screen.PracticeResult.route,
            arguments = listOf(
                navArgument("jsonFileName") { type = NavType.StringType },
                navArgument("videoPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jsonFileName = backStackEntry.arguments?.getString("jsonFileName")?.let { Screen.decodeArg(it) } ?: ""
            val videoPath = backStackEntry.arguments?.getString("videoPath")?.let { Screen.decodeArg(it) } ?: ""

            PracticeResultScreen(
                jsonFileName = jsonFileName,
                videoPath = videoPath,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack(Screen.Home.route, false) },
                onReplayClick = {
                    navController.navigate(
                        "practiceResult/${Screen.encodeArg(jsonFileName)}/${Screen.encodeArg(videoPath)}"
                    ) {
                        popUpTo(Screen.PracticeResult.route) { inclusive = true }
                    }
                },
                onHomeClick = { navController.popBackStack(Screen.Home.route, false) }
            )
        }
        composable(
            route = Screen.Record.route,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("artistPart") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                // 💡 최종 목적지! URL argument 추가
                navArgument("videoUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val songTitle = backStackEntry.arguments?.getString("songTitle")?.let { Screen.decodeArg(it) } ?: ""
            val artistPart = backStackEntry.arguments?.getString("artistPart")?.let { Screen.decodeArg(it) } ?: ""
            val difficulty = backStackEntry.arguments?.getString("difficulty")?.let { Screen.decodeArg(it) } ?: ""
            val videoUrl = backStackEntry.arguments?.getString("videoUrl")?.let { Screen.decodeArg(it) } ?: "" // 꺼내기 완료!

            val parts = artistPart.split("·").map { it.trim() }
            val artistName = parts.getOrNull(0) ?: "Unknown"
            val partName = parts.getOrNull(1) ?: artistPart

            RecordScreen(
                songTitle = songTitle,
                difficulty = difficulty,
                artist = artistName,
                part = partName,
                expertVideoUrl = videoUrl,
                onBack = { navController.popBackStack() },
                onRecordingComplete = { resultString ->
                    val dataParts = resultString.split("|")
                    val rawPath = dataParts[0]
                    val videoUriString = if (dataParts.size > 1) dataParts[1] else ""
                    val jsonFileName = rawPath.split("/").last()
                    val encodedJson = Screen.encodeArg(jsonFileName)
                    val encodedVideo = Screen.encodeArg(videoUriString)
                    navController.navigate("practiceResult/$encodedJson/$encodedVideo") {
                        popUpTo(Screen.Record.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
