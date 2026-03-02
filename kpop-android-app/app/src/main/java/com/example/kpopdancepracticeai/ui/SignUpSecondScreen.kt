package com.example.kpopdancepracticeai.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.data.entity.User
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.viewmodel.LoginState
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SignUpSecondScreen(
    viewModel: MainViewModel, // [추가] ViewModel 주입
    email: String, // 이전 화면에서 전달받은 이메일
    password: String, // 이전 화면에서 전달받은 비밀번호 (또는 "GOOGLE_LOGIN" 식별자)
    onSignUpComplete: (String, String) -> Unit = { _, _ -> } // 완료 후 메인 이동 콜백
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // AuthRepository 초기화
    val authRepository = remember { AuthRepository(context) }

    // 입력 상태 관리
    var nickname by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }
    var isSigningUp by remember { mutableStateOf(false) } // 로딩 상태 관리

    // [추가] ViewModel 상태 관찰
    val loginState by viewModel.loginState.collectAsState()

    // 회원가입/DB저장 완료 처리
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            Toast.makeText(context, "회원가입 및 로그인 완료", Toast.LENGTH_SHORT).show()
            viewModel.resetLoginState()
            onSignUpComplete(nickname, birthdate)
        } else if (loginState is LoginState.Error) {
            Toast.makeText(context, (loginState as LoginState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetLoginState()
            isSigningUp = false
        }
    }

    // 배경 (이미지의 그라데이션 느낌을 위한 연한 배경색 혹은 테마 배경색)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEBEBF0)), // 기본 배경색
        contentAlignment = Alignment.Center
    ) {
        // 메인 카드 (White Box)
        Column(
            modifier = Modifier
                .width(355.dp)
                .wrapContentHeight()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x1A000000), RoundedCornerShape(14.dp))
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            // 헤더 텍스트
            Text(
                text = "닉네임과 생년월일을 입력해주세요",
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFF717182),
                    textAlign = TextAlign.Center
                )
            )

            // 입력 폼 영역
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. 닉네임 입력
                InputField(
                    label = "닉네임",
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = "닉네임을 입력하세요"
                )

                // 2. 생년월일 입력
                InputField(
                    label = "생년월일",
                    value = birthdate,
                    onValueChange = { birthdate = it },
                    placeholder = "YYYY.MM.DD"
                )
            }

            // 가입 완료 버튼
            Button(
                onClick = {
                    // 입력값 유효성 확인
                    if (nickname.isNotBlank() && birthdate.isNotBlank()) {
                        if (!isSigningUp) {
                            isSigningUp = true
                            scope.launch {
                                var uid: String? = null
                                var finalEmail = email

                                // 1. 인증 처리 (구글 로그인 된 상태 vs 이메일 가입 필요)
                                if (password == "GOOGLE_LOGIN") {
                                    // 이미 Firebase 인증됨. 현재 유저 정보 가져오기
                                    val currentUser = authRepository.getCurrentUser()
                                    uid = currentUser?.uid
                                    finalEmail = currentUser?.email ?: email
                                } else {
                                    // 이메일 신규 가입 시도
                                    val result = authRepository.signUpWithEmail(email, password)
                                    if (result.isSuccess) {
                                        uid = result.getOrNull()?.uid
                                    } else {
                                        Toast.makeText(context, "회원가입 실패: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        isSigningUp = false
                                        return@launch
                                    }
                                }

                                // 2. RoomDB에 User 정보 저장 요청
                                if (uid != null) {
                                    val newUser = User(
                                        userUuid = uid,
                                        loginId = finalEmail, // loginId를 이메일로 대체
                                        email = finalEmail,
                                        passwordHash = if (password == "GOOGLE_LOGIN") null else password, // 해시 처리 필요시 로직 추가
                                        name = nickname,
                                        birthDate = birthdate,
                                        gender = "Unknown" // 성별 입력이 없으므로 기본값
                                    )
                                    // ViewModel에 저장 요청 -> 완료 시 LaunchedEffect(Success) 호출됨
                                    viewModel.registerUser(newUser)
                                } else {
                                    Toast.makeText(context, "UID 생성 오류", Toast.LENGTH_SHORT).show()
                                    isSigningUp = false
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "닉네임과 생년월일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF030213)
                ),
                enabled = !isSigningUp
            ) {
                if (isSigningUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "가입 완료",
                        style = TextStyle(
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * 재사용 가능한 커스텀 입력 필드
 */
@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 라벨
        Text(
            text = label,
            style = TextStyle(
                fontWeight = FontWeight(400),
                fontSize = 14.sp,
                color = Color(0xFF0A0A0A)
            )
        )

        // 입력 박스
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = Color(0xFF0A0A0A)
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color.Black),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFFF3F3F5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontWeight = FontWeight(400),
                                fontSize = 16.sp,
                                color = Color(0xFF717182) // 플레이스홀더 색상
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}