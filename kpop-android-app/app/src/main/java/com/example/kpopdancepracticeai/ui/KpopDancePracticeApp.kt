package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

@Composable
fun KpopDancePracticeApp() {
    KpopDancePracticeAITheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            // [수정] AppNavigation 호출 시 viewModel 파라미터를 제거했습니다.
            // (AppNavigation 내부에서 올바른 Factory를 사용해 ViewModel을 직접 생성합니다)
            AppNavigation(
                navController = navController
            )
        }
    }
}