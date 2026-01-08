package com.example.kpopdancepracticeai.ui

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignUpSecondScreen(
    onSignUpComplete: (String, String) -> Unit = { _, _ -> } // 닉네임, 생년월일 전달 콜백
) {
    // 입력 상태 관리
    var nickname by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }

    // 배경 (이미지의 그라데이션 느낌을 위한 연한 배경색 혹은 테마 배경색)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEBEBF0)), // 기본 배경색 (필요 시 그라데이션으로 변경 가능)
        contentAlignment = Alignment.Center
    ) {
        // 메인 카드 (White Box)
        Column(
            modifier = Modifier
                .width(355.dp) // 피그마 기준 너비 유지 혹은 fillMaxWidth(0.9f)
                .wrapContentHeight()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x1A000000), RoundedCornerShape(14.dp)) // 테두리 미세 조정
                .padding(horizontal = 24.dp, vertical = 40.dp), // 내부 여백
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
                    placeholder = "YYYY.MM.DD" // 플레이스홀더 예시 추가
                )
            }

            // 가입 완료 버튼
            Button(
                onClick = { onSignUpComplete(nickname, birthdate) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp), // 터치 영역 확보를 위해 높이 약간 조정
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF030213)
                )
            ) {
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

/**
 * 재사용 가능한 커스텀 입력 필드
 * 피그마 디자인(회색 박스 형태)을 유지하며 TextField 기능을 구현
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
                        .height(44.dp) // 입력하기 편한 높이로 조정 (피그마 36dp -> 44dp 권장)
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

@Preview(showBackground = true)
@Composable
fun SignUpSecondScreenPreview() {
    SignUpSecondScreen()
}