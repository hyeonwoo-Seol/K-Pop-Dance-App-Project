package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Navigation Routes 정의
object Routes {
    const val HOME = "home"
    const val SONG_DETAIL = "songDetail/{songId}"
    const val SONG_PART_SELECT = "songPartSelect/{songId}"
    // 연습 화면 (튜토리얼 시청)
    const val PRACTICE = "practice/{songTitle}/{partTitle}/{difficulty}/{length}"
    // 녹화 화면
    const val RECORD = "record/{songTitle}/{partTitle}/{difficulty}/{length}"
    const val PRACTICE_RESULT = "practiceResult/{score}"

    fun encode(arg: String): String = try {
        URLEncoder.encode(arg, StandardCharsets.UTF_8.toString())
    } catch (e: Exception) { arg }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel // [수정] 기본값(= viewModel()) 제거하여 외부 주입 강제
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        // 1. 홈 화면
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onSearch = { query -> navController.navigate("searchResults/${Routes.encode(query)}") },
                onSongClick = { songId -> navController.navigate(Routes.SONG_DETAIL.replace("{songId}", songId)) }
            )
        }

        // 2. 노래 상세
        composable(
            route = Routes.SONG_DETAIL,
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

        // 3. 파트 선택
        composable(
            route = Routes.SONG_PART_SELECT,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            SongPartSelectScreen(
                songId = songId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToPractice = { songTitle, partTitle, difficulty, length ->
                    val encodedSong = Routes.encode(songTitle)
                    val encodedPart = Routes.encode(partTitle)
                    val encodedDiff = Routes.encode(difficulty)
                    val encodedLen = Routes.encode(length)
                    // 연습 화면(튜토리얼)으로 이동
                    navController.navigate("practice/$encodedSong/$encodedPart/$encodedDiff/$encodedLen")
                }
            )
        }

        // 4. 연습 화면 (튜토리얼 시청 화면)
        composable(
            route = Routes.PRACTICE,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("partTitle") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("length") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val songTitle = backStackEntry.arguments?.getString("songTitle") ?: ""
            val partTitle = backStackEntry.arguments?.getString("partTitle") ?: ""
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: ""
            val length = backStackEntry.arguments?.getString("length") ?: ""

            // [오류 해결] PracticeScreenMobile 파라미터 매핑 수정
            PracticeScreenMobile(
                songTitle = songTitle,
                artistPart = partTitle, // partTitle을 artistPart로 전달
                difficulty = difficulty,
                length = length,
                onBackClick = { navController.popBackStack() },
                onRecordClick = {
                    // 녹화 화면으로 이동
                    val encodedSong = Routes.encode(songTitle)
                    val encodedPart = Routes.encode(partTitle)
                    val encodedDiff = Routes.encode(difficulty)
                    val encodedLen = Routes.encode(length)
                    navController.navigate("record/$encodedSong/$encodedPart/$encodedDiff/$encodedLen")
                },
                onSettingsClick = { /* 설정 다이얼로그 등 표시 */ }
            )
        }

        // 4-1. 녹화 화면 (추가됨)
        composable(
            route = Routes.RECORD,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("partTitle") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val songTitle = backStackEntry.arguments?.getString("songTitle") ?: ""
            val partTitle = backStackEntry.arguments?.getString("partTitle") ?: ""
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: ""

            RecordScreen(
                songTitle = songTitle,
                part = partTitle,
                difficulty = difficulty,
                onBack = { navController.popBackStack() },
                onRecordingComplete = { s3Key ->
                    // 녹화 및 업로드 완료 후 결과 화면(또는 분석 대기 화면)으로 이동
                    // 임시로 점수 0점으로 결과 화면 이동
                    navController.navigate(Routes.PRACTICE_RESULT.replace("{score}", "0"))
                }
            )
        }

        // 5. 결과 화면
        composable(
            route = Routes.PRACTICE_RESULT,
            arguments = listOf(navArgument("score") { type = NavType.IntType })
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            PracticeResultScreen(
                score = score,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onReplayClick = { navController.popBackStack() },
                onHomeClick = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }

        // 검색 결과 (임시)
        composable("searchResults/{query}") {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("검색 결과 화면") }
        }
    }
}