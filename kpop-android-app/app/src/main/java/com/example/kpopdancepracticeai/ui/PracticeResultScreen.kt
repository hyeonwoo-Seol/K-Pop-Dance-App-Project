package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.*

// 결과 데이터 모델
private data class PracticeResultData(
    val title: String,
    val accuracy: Int,
    val accuracyChange: Int,
    val experienceGained: Int,
    val nextLevelXpNeeded: Int,
    val newRecord: Boolean,
    val previousRecord: Int,
    val avgAngleError: Float,
    val avgTimingError: Float,
    val mistakeJoints: List<Triple<String, Int, String>>,
    val songId: String
)

// 더미 데이터
private val dummyResultData = PracticeResultData(
    title = "Dynamite - Part 2",
    accuracy = 87,
    accuracyChange = 5,
    experienceGained = 1250,
    nextLevelXpNeeded = 750,
    newRecord = true,
    previousRecord = 80,
    avgAngleError = 10.5f,
    avgTimingError = 0.3f,
    mistakeJoints = listOf(
        Triple("왼쪽 어깨", 1, "28%"),
        Triple("오른쪽 팔꿈치", 2, "22%"),
        Triple("왼쪽 무릎", 3, "18%"),
    ),
    songId = "1"
)

@Composable
fun PracticeResultScreen(
    onBackClick: () -> Unit = {},
    onCompareClick: () -> Unit = {},
    onRetryClick: (songId: String) -> Unit = { },
    onNextPartClick: (songId: String) -> Unit = { }
) {
    val result = dummyResultData
    val scrollState = rememberScrollState()

    // Root Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)) // 배경색 추가
    ) {
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 81.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // 1. Grade Card
            Box(
                modifier = Modifier
                    .height(138.dp)
                    .fillMaxWidth()
                    .background(Color(0xfffefce8), RoundedCornerShape(14.dp))
                    .border(1.25.dp, Color(0xfffff085), RoundedCornerShape(14.dp))
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, // Center contents
                ) {
                    Box(
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B", // Logic based on result.accuracy can be added here
                            style = TextStyle(
                                fontWeight = FontWeight(700),
                                fontSize = 60.sp,
                                lineHeight = 60.sp,
                            ),
                            color = Color(0xffd08700),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Box(
                        modifier = Modifier.height(20.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "평가 등급",
                            style = TextStyle(
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            ),
                            color = Color(0xff717182),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // 2. Score Card (Accuracy, Rhythm, Power, Flexibility)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.25.dp, Color(0xffe9d4ff), RectangleShape) // RectangleShape or Rounded? Figma said Rectangle in code snippet but had corners in others. Using RectangleShape as per code.
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.25.dp, Color(0xffe9d4ff), RoundedCornerShape(14.dp)) // Override with corners
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    // Total Accuracy
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${result.accuracy}%",
                            style = TextStyle(
                                fontWeight = FontWeight(700),
                                fontSize = 48.sp,
                                lineHeight = 48.sp,
                            ),
                            color = Color(0xff9810fa),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "전체 정확도",
                            style = TextStyle(
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            ),
                            color = Color(0xff717182),
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Song Info
                    Box(
                        modifier = Modifier
                            .height(45.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .fillMaxWidth()
                                .background(Color(0x33030213), RoundedCornerShape(50.dp))
                        ) {
                            // Progress Bar Background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(result.accuracy / 100f) // Dynamic progress
                                    .height(12.dp)
                                    .background(Color(0xff030213), RoundedCornerShape(50.dp))
                            )
                        }
                        // Song Title Overlay (Visual tweak to match Figma layout logic)
                        Text(
                            modifier = Modifier.padding(top=32.dp),
                            text = "${result.title} - 3:00",
                            style = TextStyle(
                                fontWeight = FontWeight(400),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            ),
                            color = Color(0xff717182),
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Detailed Scores (Rhythm, Accuracy, Power) - Mocked based on Figma
                    // In a real app, these would come from 'result'
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScoreDetailItem("리듬감", 85)
                        ScoreDetailItem("정확도", 88)
                        ScoreDetailItem("파워", 80)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ScoreDetailItem("유연성", 75)
                    }
                }
            }

            // 3. Total Statistics (Graph Placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xffffffff), RoundedCornerShape(14.dp))
                    .border(1.25.dp, Color(0x1a000000), RoundedCornerShape(14.dp))
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
                        Text(
                            text = "종합 통계",
                            style = TextStyle(
                                fontWeight = FontWeight(400),
                                fontSize = 16.sp,
                                lineHeight = 16.sp,
                            ),
                            color = Color(0xff0a0a0a),
                        )
                    }

                    // Graph Area Placeholder
                    Box(
                        modifier = Modifier
                            .height(256.dp)
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("통계 그래프 영역", color = Color.Gray)
                    }
                }
            }

            // 4. Mistake Joints Top 3
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xffffffff), RoundedCornerShape(14.dp))
                    .border(1.25.dp, Color(0x1a000000), RoundedCornerShape(14.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Red)
                    Text(
                        text = "많이 틀린 관절 TOP 3",
                        style = TextStyle(
                            fontWeight = FontWeight(400),
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                        ),
                        color = Color(0xff0a0a0a),
                    )
                }

                // List
                result.mistakeJoints.forEachIndexed { index, mistake ->
                    val color = when(index) {
                        0 -> Color(0xfffb2c36) // Red
                        1 -> Color(0xffff6900) // Orange
                        else -> Color(0xfff0b100) // Yellow
                    }
                    val bg = Color(0xfffef2f2)
                    val border = Color(0xffffc9c9)

                    Box(
                        modifier = Modifier
                            .height(66.dp)
                            .fillMaxWidth()
                            .background(bg, RoundedCornerShape(10.dp))
                            .border(1.25.dp, border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, RoundedCornerShape(50.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = TextStyle(
                                        fontWeight = FontWeight(400),
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                    ),
                                    color = Color(0xffffffff),
                                )
                            }
                            Text(
                                text = mistake.first,
                                style = TextStyle(
                                    fontWeight = FontWeight(700),
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                ),
                                color = Color(0xff0a0a0a),
                            )
                        }
                    }
                }
            }

            // 5. Achievement Progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xffffffff), RoundedCornerShape(14.dp))
                    .border(1.25.dp, Color(0x1a000000), RoundedCornerShape(14.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xfff0b100))
                    Text(
                        text = "업적 진행도",
                        style = TextStyle(
                            fontWeight = FontWeight(400),
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                        ),
                        color = Color(0xff0a0a0a),
                    )
                }

                // Achievements List
                AchievementItem("🏃‍♀️", "첫 완주", "100%", true)
                AchievementItem("🎯", "정확도 마스터", "75%", false)
                AchievementItem("💪", "연습벌레", "60%", false)
            }

            // 6. Best Record
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.25.dp, Color(0xfffff085), RoundedCornerShape(14.dp))
                    .padding(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xfff0b100), RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "이 곡 최고 기록",
                                style = TextStyle(
                                    fontWeight = FontWeight(400),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                ),
                                color = Color(0xff717182),
                            )
                            Text(
                                text = "${result.title}",
                                style = TextStyle(
                                    fontWeight = FontWeight(400),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                ),
                                color = Color(0xff717182),
                            )
                        }
                    }
                    Text(
                        text = "${if(result.newRecord) result.accuracy else result.previousRecord}%",
                        style = TextStyle(
                            fontWeight = FontWeight(700),
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                        ),
                        color = Color(0xffd08700),
                        textAlign = TextAlign.End,
                    )
                }
            }

            // 7. Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Retry Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(Color(0xffffffff), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                        .clickable { onRetryClick(result.songId) }
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "다시 연습",
                            style = TextStyle(
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            ),
                            color = Color(0xff0a0a0a),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Share Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(Color(0xffffffff), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                        .clickable { onBackClick() } // Using back click as generic action
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "결과 공유",
                            style = TextStyle(
                                fontWeight = FontWeight(400),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            ),
                            color = Color(0xff000000),
                        )
                    }
                }
            }
        }

        // Header (Overlay)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp) // Fixed height for header area
                .background(Color(0x00ffffff)) // Transparent
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Back Button (Icon)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black
                    )
                }

                // Title
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "연습 결과",
                        style = TextStyle(
                            fontWeight = FontWeight(700),
                            fontSize = 24.sp,
                            lineHeight = 24.sp,
                        ),
                        color = Color(0xff0a0a0a),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreDetailItem(label: String, score: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xff9810fa), RoundedCornerShape(50.dp))
        )
        Text(
            text = "$label: ${score}점",
            style = TextStyle(
                fontWeight = FontWeight(400),
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            color = Color(0xff717182),
        )
    }
}

@Composable
fun AchievementItem(emoji: String, title: String, percentage: String, isCompleted: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = emoji,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                    ),
                    color = Color(0xff0a0a0a),
                )
                Text(
                    text = title,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    color = Color(0xff0a0a0a),
                )
                Text(
                    text = percentage,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    color = Color(0xff717182),
                )
            }
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .background(Color(0xfff0b100), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "달성 완료!",
                        style = TextStyle(
                            fontWeight = FontWeight(400),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        color = Color(0xffffffff),
                    )
                }
            }
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .height(8.dp)
                .fillMaxWidth()
                .background(Color(0x33030213), RoundedCornerShape(50.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.replace("%","").toFloat() / 100f)
                    .height(8.dp)
                    .background(Color(0xff030213), RoundedCornerShape(50.dp))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PracticeResultScreenPreview() {
    KpopDancePracticeAITheme {
        PracticeResultScreen()
    }
}