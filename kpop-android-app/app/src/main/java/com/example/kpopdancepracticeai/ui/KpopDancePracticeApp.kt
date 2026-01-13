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
            AppNavigation(
                navController = navController
            )
        }
    }
}