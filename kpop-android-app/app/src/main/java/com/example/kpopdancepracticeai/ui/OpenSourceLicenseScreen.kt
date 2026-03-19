package com.example.kpopdancepracticeai.ui

import com.example.kpopdancepracticeai.ui.motion.rememberIosLikeFlingBehavior

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 라이선스 정보를 담을 데이터 클래스
data class OpenSourceItem(
    val name: String,
    val copyright: String,
    val license: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicenseScreen(
    onBackClick: () -> Unit
) {
    // 앱 전체 공통 그라데이션 배경 적용
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    // 사용 중인 오픈소스 목록
    val openSourceList = listOf(
        OpenSourceItem("AndroidX & Jetpack", "Copyright (c) The Android Open Source Project", "Apache License 2.0"),
        OpenSourceItem("AWS Android SDK", "Copyright (c) Amazon Web Services, Inc.", "Apache License 2.0"),
        OpenSourceItem("Retrofit2 & OkHttp3", "Copyright (c) Square, Inc.", "Apache License 2.0"),
        OpenSourceItem("AndroidX Media3 (ExoPlayer)", "Copyright (c) The Android Open Source Project", "Apache License 2.0"),
        OpenSourceItem("Google Gson", "Copyright (c) Google Inc.", "Apache License 2.0")
    )

    // 아파치 라이선스 공통 내용
    val apacheLicenseText = """
        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

           http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("오픈소스 라이선스", fontWeight = FontWeight.Bold) },
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
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 라이선스 목록 카드 반복 렌더링
                items(openSourceList) { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = item.copyright,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "License: ${item.license}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // 라이선스 본문 영역 (회색 박스)
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = apacheLicenseText,
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
