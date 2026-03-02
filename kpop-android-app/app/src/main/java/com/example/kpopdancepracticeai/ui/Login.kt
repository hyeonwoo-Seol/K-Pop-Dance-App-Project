package com.example.kpopdancepracticeai.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.R
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.LoginState
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: MainViewModel, // [추가] ViewModel 주입
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onGoogleLoginSuccess: () -> Unit // 추가 정보(프로필) 입력 화면으로 이동
) {
    // 1. 상태 관리
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // AuthRepository는 ViewModel 내부에서도 쓰이지만, Firebase 직접 호출을 위해 여기서도 사용
    val authRepository = remember { AuthRepository(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // [추가] ViewModel의 로그인 상태 관찰
    val loginState by viewModel.loginState.collectAsState()

    // 상태 변경에 따른 처리
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
                viewModel.resetLoginState() // 상태 초기화
                onLoginSuccess()
            }
            is LoginState.NeedProfile -> {
                Toast.makeText(context, "추가 정보 입력이 필요합니다.", Toast.LENGTH_SHORT).show()
                viewModel.resetLoginState()
                onGoogleLoginSuccess() // 프로필 설정 화면으로 이동
            }
            is LoginState.Error -> {
                errorMessage = (loginState as LoginState.Error).message
                viewModel.resetLoginState()
            }
            else -> {}
        }
    }

    // --- 구글 로그인 런처 설정 ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    // 구글 토큰을 받아서 파이어베이스 로그인 시도
                    scope.launch {
                        val authResult = authRepository.firebaseAuthWithGoogle(idToken)
                        if (authResult.isSuccess) {
                            // [수정] 로그인 성공 시 바로 이동하지 않고, DB 확인 요청
                            val uid = authResult.getOrNull()?.uid
                            if (uid != null) {
                                viewModel.checkUserExists(uid)
                            } else {
                                errorMessage = "UID를 가져올 수 없습니다."
                            }
                        } else {
                            errorMessage = "구글 로그인 실패: ${authResult.exceptionOrNull()?.message}"
                        }
                    }
                }
            } catch (e: ApiException) {
                errorMessage = "구글 로그인 오류: ${e.message}"
            }
        }
    }

    // 2. 피그마 디자인의 그라데이션 배경 적용
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFAEC6FF), // 피그마의 연한 파란색
                        Color(0xFFD0BCFF)  // 어울리는 연한 보라색
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // 화면 전체 사용
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround // 콘텐츠를 위아래로 분산
        ) {
            // 3. 화면 상단 타이틀
            Text(
                text = "KPOP 댄스 연습 앱",
                style = MaterialTheme.typography.headlineLarge, // 32.sp
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // 4. 피그마의 흰색 둥근 카드
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(30.dp), // 둥근 모서리
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                // 5. 카드 내부 로그인 폼
                Column(
                    modifier = Modifier.padding(all = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // "로그인" 텍스트
                    Text(
                        text = "로그인",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 아이디 입력 필드 (라벨이 위로 간 형태)
                    LoginTextField(
                        label = "아이디",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "email@domain.com"
                    )

                    // 비밀번호 입력 필드
                    LoginTextField(
                        label = "비밀번호",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "비밀번호",
                        isPassword = true,
                        isPasswordVisible = isPasswordVisible,
                        onVisibilityChange = { isPasswordVisible = !isPasswordVisible }
                    )

                    // 로그인 버튼 (검은색)
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                scope.launch {
                                    val result = authRepository.signInWithEmail(email, password)
                                    if (result.isSuccess) {
                                        // [수정] 로그인 성공 시 DB 확인 요청
                                        val uid = result.getOrNull()?.uid
                                        if (uid != null) {
                                            viewModel.checkUserExists(uid)
                                        }
                                    } else {
                                        errorMessage = "로그인 실패. 아이디/비번을 확인하세요."
                                    }
                                }
                            } else {
                                errorMessage = "이메일과 비밀번호를 입력해주세요."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (loginState is LoginState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 에러 메시지 표시
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // "또는" 구분선
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text("또는", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Divider(modifier = Modifier.weight(1f))
                    }

                    // Google 로그인 버튼 (흰색)
                    OutlinedButton(
                        onClick = {
                            // 구글 로그인 클라이언트 실행
                            val gso = authRepository.getGoogleSignInOptions()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        // TODO: 구글 아이콘 추가
                        Text("Google 계정으로 로그인", fontSize = 16.sp)
                    }

                    // 회원가입 버튼
                    Button(
                        onClick = { onNavigateToSignUp() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F39F6), // 디자인 시안의 보라색 포인트 컬러 사용
                            contentColor = Color.White
                        )
                    ) {
                        Text("회원 가입하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    // 약관 안내
                    Text(
                        text = "계속을 클릭하면 당사의 서비스 이용 약관 및 개인정보 처리방침에 동의하는 것으로 간주됩니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// 6. 재사용 가능한 커스텀 텍스트 필드
@Composable
private fun LoginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onVisibilityChange: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // 라벨 (아이디, 비밀번호)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        // 입력창
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color.Gray) },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                if (isPassword) {
                    val image = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = onVisibilityChange) {
                        Icon(imageVector = image, contentDescription = "비밀번호 보이기/숨기기")
                    }
                }
            }
        )
    }
}