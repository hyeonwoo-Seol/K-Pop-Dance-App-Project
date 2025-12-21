package com.example.kpopdancepracticeai
// MainActivity.kt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.kpopdancepracticeai.ui.LoginScreen
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme // 본인의 테마 이름
import com.example.kpopdancepracticeai.ui.HomeScreen
import com.example.kpopdancepracticeai.ui.KpopDancePracticeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 프로젝트 생성 시 만들어진 테마(Theme)를 적용합니다.
            KpopDancePracticeAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KpopDancePracticeApp()
                }
            }
        }
    }
}