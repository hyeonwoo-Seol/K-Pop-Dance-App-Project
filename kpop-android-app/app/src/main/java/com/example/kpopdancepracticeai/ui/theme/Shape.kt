package com.example.kpopdancepracticeai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp), // 예: 일반 버튼
    large = RoundedCornerShape(14.dp), // 예: 검색 필터 카드
    extraLarge = RoundedCornerShape(30.dp) // 예: 로그인 카드
)