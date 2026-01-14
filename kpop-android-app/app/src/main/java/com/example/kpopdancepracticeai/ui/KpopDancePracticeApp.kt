package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

@Composable
fun KpopDancePracticeApp(
    viewModel: MainViewModel // [수정] ViewModel을 매개변수로 받음
) {
    KpopDancePracticeAITheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            AppNavigation(
                navController = navController,
                viewModel = viewModel // [수정] 전달받은 ViewModel을 넘겨줌
            )
        }
    }
}