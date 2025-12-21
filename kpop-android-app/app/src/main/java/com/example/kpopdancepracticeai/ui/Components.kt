package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.*

// --- 1. 섹션 타이틀 ---
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        color = TextDark
    )
}

// --- 2. 배지 칩 (통합 버전) ---
@Composable
fun BadgeChip(
    text: String,
    color: Color, // 배경색
    textColor: Color = Color.Black.copy(alpha = 0.8f), // 기본 텍스트 색상
    borderColor: Color? = null // 테두리 (옵션)
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = color,
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// --- 3. 다음 단계/액션 버튼 ---
@Composable
fun NextStepButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color = if (containerColor == Color.White) Color.Black else Color.White
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (containerColor == Color.White) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)) else null
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

// --- 4. 카드 컨테이너 ---
@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(2.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}

// --- 5. 배경이 있는 통계 아이템 ---
@Composable
fun StatItemWithBackground(
    title: String,
    value: String,
    bgColor: Color,
    valueColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// --- 6. 아이콘과 제목이 있는 섹션 항목 ---
@Composable
fun SectionItem(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = TextGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextGray
            )
        }
        content()
    }
}