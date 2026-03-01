package com.example.kpopdancepracticeai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

// FAQ 데이터 모델
data class FaqItem(val question: String, val answer: String)
data class FaqCategory(val title: String, val items: List<FaqItem>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onBackClick: () -> Unit) {
    // 제안된 FAQ 리스트 데이터
    val faqData = remember {
        listOf(
            FaqCategory(
                title = "🤖 AI 댄스 인식 및 촬영 관련",
                items = listOf(
                    FaqItem(
                        question = "Q. AI 분석 정확도가 낮게 나와요. 어떻게 해야 하나요?",
                        answer = "전신이 카메라 화면 안에 모두 들어오도록 거리를 조절해 주세요. 역광이거나 배경이 너무 복잡하면 인식이 어려울 수 있으니 밝고 깔끔한 배경에서 촬영하는 것을 권장합니다."
                    ),
                    FaqItem(
                        question = "Q. 어두운 방이나 야외에서도 인식이 잘 되나요?",
                        answer = "카메라는 빛의 영향을 많이 받습니다. 가급적 밝은 조명이 있는 실내에서 연습해 주시면 가장 높은 분석 정확도를 얻을 수 있습니다."
                    ),
                    FaqItem(
                        question = "Q. 영상 분석은 얼마나 걸리나요?",
                        answer = "네트워크 상태 및 영상 길이에 따라 다르지만, 보통 1~2분 내외로 AI 분석이 완료됩니다."
                    )
                )
            ),
            FaqCategory(
                title = "🎵 곡 및 안무 콘텐츠 관련",
                items = listOf(
                    FaqItem(
                        question = "Q. 새로운 K-Pop 안무는 언제 업데이트되나요?",
                        answer = "매주 금요일, 최신 유행하는 K-Pop 신곡 안무가 업데이트됩니다."
                    ),
                    FaqItem(
                        question = "Q. 제가 원하는 곡의 안무를 신청할 수 있나요?",
                        answer = "네! '앱 정보 > 문의하기'를 통해 원하시는 곡명과 아티스트를 남겨주시면 업데이트 일정에 적극 반영하겠습니다."
                    )
                )
            ),
            FaqCategory(
                title = "📊 계정 및 데이터(기록) 관련",
                items = listOf(
                    FaqItem(
                        question = "Q. 연습한 영상은 어디에 저장되나요? 다른 사람도 볼 수 있나요?",
                        answer = "연습 영상은 회원님의 기기 내부에만 안전하게 임시 저장되며, AI 분석 완료 후 서버에는 데이터(정확도, 스코어 등)만 전송됩니다. 영상 자체는 타인에게 절대 공유되지 않습니다."
                    ),
                    FaqItem(
                        question = "Q. 기기를 변경하면 이전 연습 기록과 뱃지가 사라지나요?",
                        answer = "아니요, 로그인하신 계정(이메일 등)에 모든 연습 기록과 획득한 업적, 뱃지가 안전하게 연동되어 있으므로 기기를 변경하셔도 그대로 이어서 사용하실 수 있습니다."
                    )
                )
            ),
            FaqCategory(
                title = "⚙️ 앱 설정 및 오류",
                items = listOf(
                    FaqItem(
                        question = "Q. 카메라/마이크 권한은 어떻게 다시 허용하나요?",
                        answer = "스마트폰의 [설정] > [애플리케이션] > [K-Pop Dance AI] > [권한] 메뉴에서 카메라와 마이크 접근 권한을 '허용'으로 변경해 주세요."
                    )
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("자주 묻는 질문 (FAQ)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF7F8FA) // 전체 배경색
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            faqData.forEach { category ->
                item {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }

                items(category.items) { faqItem ->
                    FaqExpandableCard(faqItem = faqItem)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun FaqExpandableCard(faqItem: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBEBEB)),
        shadowElevation = if (expanded) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faqItem.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (expanded) FontWeight.Bold else FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    lineHeight = 22.sp
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faqItem.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555),
                        lineHeight = 20.sp,
                        modifier = Modifier.background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp)).padding(12.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FaqScreenPreview() {
    KpopDancePracticeAITheme {
        FaqScreen(onBackClick = {})
    }
}