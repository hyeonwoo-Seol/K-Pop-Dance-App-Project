package com.example.kpopdancepracticeai.ui

import com.example.kpopdancepracticeai.ui.motion.rememberIosLikeFlingBehavior

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
fun TermsOfServiceScreen(
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
                    title = { Text("서비스 이용 약관", fontWeight = FontWeight.Bold) },
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
            flingBehavior = rememberIosLikeFlingBehavior(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp), // 양옆 여백
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 기존 SettingsCard와 유사한 하얀색 둥근 카드 생성
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "KPOP Dance Practice App \n 서비스 이용약관",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                textAlign = TextAlign.Center, // 텍스트 자체를 중앙으로 정렬
                                modifier = Modifier
                                    .fillMaxWidth() // 가로 영역을 꽉 채워서 완벽한 중앙에 위치하도록 함
                                    .padding(bottom = 16.dp)
                            )

                            // 약관 텍스트 내용 (추후 실제 약관 내용으로 교체하시면 됩니다)
                            Text(
                                text = """
제1조 (목적)
본 약관은 KPOP Dance Practice App(이하 "서비스")이 제공하는 제반 서비스의 이용과 관련하여 개발팀과 회원과의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.

제2조 (정의)
1. "서비스"라 함은 구현되는 단말기와 상관없이 "회원"이 이용할 수 있는 관련 제반 서비스를 의미합니다.

2. "회원"이라 함은 개발팀의 "서비스"에 접속하여 본 약관에 따라 "개발팀"과 이용계약을 체결하고 "개발팀"이 제공하는 "서비스"를 이용하는 고객을 말합니다.

제3조 (약관의 게시와 개정)
1. "개발팀"은 이 약관의 내용을 "회원"이 쉽게 알 수 있도록 서비스 초기 화면에 게시합니다.

2. "개발팀"은 "약관의 규제에 관한 법률", "정보통신망 이용촉진 및 정보보호 등에 관한 법률" 등 관련 법을 위배하지 않는 범위에서 이 약관을 개정할 수 있습니다.

제4조 (이용계약 체결)
1. 이용계약은 "회원"이 되고자 하는 자(이하 "가입신청자")가 약관의 내용에 대하여 동의를 한 다음 회원가입신청을 하고 "개발팀"이 이러한 신청에 대하여 승낙함으로써 체결됩니다.

제5조 (회원의 의무)
"회원"은 다음 행위를 하여서는 안 됩니다.
1. 신청 또는 변경 시 허위내용의 등록

2. 타인의 정보도용

3. "개발팀"이 정한 정보 이외의 정보 등의 송신 또는 게시

4. "개발팀" 및 기타 제3자의 저작권 등 지적재산권에 대한 침해

제6조 (서비스의 제공 등)
1. "개발팀"은 회원에게 아래와 같은 서비스를 제공합니다.
   - AI 기반 댄스 모션 분석 서비스
   - 연습 기록 저장 및 통계 제공
   - 기타 "개발팀"이 추가 개발하거나 다른 개발팀과의 제휴계약 등을 통해 "회원"에게 제공하는 일체의 서비스

[부칙]
본 약관은 2026년 1월 1일부터 적용됩니다.
                                """.trimIndent(),
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Start,
                                lineHeight = 22.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
