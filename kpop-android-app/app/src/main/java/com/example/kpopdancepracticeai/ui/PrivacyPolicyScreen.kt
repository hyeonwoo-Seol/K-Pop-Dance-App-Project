package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    // 앱 전체 공통 그라데이션 배경 적용
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("개인정보 처리 방침", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp), // 양옆 여백
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 하얀색 둥근 카드 생성
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 제목 텍스트 중앙 정렬
                            Text(
                                text = "KPOP Dance Practice App\n개인정보 처리 방침",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center, // 제목은 중앙 정렬
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )

                            //  약관 텍스트 내용 (내용은 왼쪽 정렬)
                            Text(
                                text = """
제1조 (목적)
본 방침은 KPOP Dance Practice App(이하 "서비스")이 이용자의 개인정보를 어떻게 수집, 이용, 보관, 파기하는지에 대한 정보를 제공함을 목적으로 합니다.

제2조 (수집하는 개인정보 항목)
회사는 회원가입, 고객상담, 서비스 제공을 위해 아래와 같은 개인정보를 수집하고 있습니다.
- 필수항목 : 이메일 주소, 비밀번호, 닉네임
- 선택항목 : 프로필 이미지, 댄스 실력 정보
- 자동수집항목 : 서비스 이용 기록, 접속 로그, 기기 정보

제3조 (개인정보의 수집 및 이용목적)
회사는 수집한 개인정보를 다음의 목적을 위해 활용합니다.
1. 서비스 제공에 관한 계약 이행 및 서비스 제공에 따른 콘텐츠 제공

2. 회원 관리 : 회원제 서비스 이용에 따른 본인확인, 개인 식별, 불량회원의 부정 이용 방지와 비인가 사용 방지, 가입 의사 확인

제4조 (개인정보의 보유 및 이용기간)
원칙적으로, 개인정보 수집 및 이용목적이 달성된 후에는 해당 정보를 지체 없이 파기합니다. 단, 관계법령의 규정에 의하여 보존할 필요가 있는 경우 회사는 관계법령에서 정한 일정한 기간 동안 회원정보를 보관합니다.

[부칙]
본 방침은 2026년 1월 1일부터 시행됩니다.
                                """.trimIndent(),
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}