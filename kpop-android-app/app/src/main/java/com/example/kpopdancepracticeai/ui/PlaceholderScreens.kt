package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding

// 임시 화면
@Composable
fun PlaceholderScreen(screenName: String, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues), // ⭐️ Scaffold의 패딩을 여기에 적용합니다.
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$screenName 화면",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}