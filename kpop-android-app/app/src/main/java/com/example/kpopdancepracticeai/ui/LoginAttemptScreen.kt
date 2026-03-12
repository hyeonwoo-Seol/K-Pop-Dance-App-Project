package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.viewmodel.LoginState
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

@Composable
fun LoginAttemptScreen(
    viewModel: MainViewModel,
    email: String,
    password: String,
    onLoginSuccess: () -> Unit,
    onNeedProfile: (String, String) -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }

    val loginState by viewModel.loginState.collectAsState()
    var hasStarted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!hasStarted) {
            hasStarted = true
            val result = authRepository.signInWithEmail(email, password)
            if (result.isSuccess) {
                val uid = result.getOrNull()?.uid
                if (uid != null) {
                    viewModel.checkUserExists(uid)
                } else {
                    errorMessage = "UID를 가져올 수 없습니다."
                }
            } else {
                errorMessage = "로그인 실패. 아이디/비번을 확인하세요."
            }
        }
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                viewModel.resetLoginState()
                onLoginSuccess()
            }
            is LoginState.NeedProfile -> {
                viewModel.resetLoginState()
                onNeedProfile(email, password)
            }
            is LoginState.Error -> {
                errorMessage = (loginState as LoginState.Error).message
                viewModel.resetLoginState()
            }
            else -> Unit
        }
    }

    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFAEC6FF),
            Color(0xFFD0BCFF)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = "로그인 시도 중",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = "사용자 정보를 확인하고 있어요.\n잠시만 기다려주세요.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Button(
                onClick = onBackToLogin,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("로그인 화면으로 돌아가기")
            }
        }
    }
}
