package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onSignUpSubmit: (String, String) -> Unit
) {
    // 배경 그라데이션 (기존 앱 스타일 유지)
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    // 입력 상태 관리
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient),
        contentAlignment = Alignment.Center
    ) {
        // 메인 카드 컨테이너
        Surface(
            modifier = Modifier
                .width(354.dp)
                // 내용물에 따라 높이 유동적 조절 또는 고정
                .background(Color.Transparent),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 1. 헤더 영역
                SignUpHeader()

                // 2. 입력 폼 영역
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 이메일 입력
                    LabeledInputHooks(
                        label = "이메일",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "example@email.com",
                        icon = Icons.Outlined.Email
                    )

                    // 비밀번호 입력
                    LabeledInputHooks(
                        label = "비밀번호",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "8자 이상 입력해주세요",
                        icon = Icons.Outlined.Lock,
                        isPassword = true
                    )

                    // 다음 버튼
                    Button(
                        onClick = { onSignUpSubmit(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F39F6) // 디자인 시안의 보라색
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "다음",
                                fontSize = 16.sp,
                                fontWeight = FontWeight(400),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // 아이콘이 필요하다면 추가
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // 3. 하단 로그인 링크
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이미 계정이 있으신가요? ",
                        style = TextStyle(
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            color = Color(0xFF4A5565)
                        )
                    )
                    Text(
                        text = "로그인",
                        style = TextStyle(
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            color = Color(0xFF4F39F6)
                        ),
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}

@Composable
fun SignUpHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "회원가입",
            style = TextStyle(
                fontWeight = FontWeight(700),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                color = Color(0xFF101828)
            )
        )
        Text(
            text = "계정을 만들기 위해 정보를 입력해주세요",
            style = TextStyle(
                fontWeight = FontWeight(400),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Color(0xFF4A5565)
            )
        )
    }
}

@Composable
fun LabeledInputHooks(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontWeight = FontWeight(400),
                fontSize = 14.sp,
                color = Color(0xFF364153)
            )
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0x800A0A0A),
                    fontSize = 16.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Gray
                )
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = if (isPassword) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions(keyboardType = KeyboardType.Email)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp), // 디자인 시안과 유사한 높이
            shape = RectangleShape, // 디자인 시안의 각진 모서리 반영 (필요 시 RoundedCornerShape(8.dp)로 변경 가능)
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color(0xFF4F39F6),
                unfocusedBorderColor = Color(0xFFD1D5DC)
            ),
            singleLine = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    KpopDancePracticeAITheme {
        SignUpScreen(
            onNavigateToLogin = {},
            onSignUpSubmit = { _, _ -> }
        )
    }
}