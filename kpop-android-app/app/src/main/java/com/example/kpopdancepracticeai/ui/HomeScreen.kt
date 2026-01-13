package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSongSelect: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val userStats by viewModel.userStats.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSongSelect,
                containerColor = Color(0xFF6200EE),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "연습하기")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("안녕하세요!", fontSize = 16.sp, color = Color.Gray)
                        Text("오늘도 춤 연습 시작해볼까요?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        modifier = Modifier.size(48.dp).clickable { onNavigateToProfile() },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("MY", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(50.dp).clickable { onNavigateToSongSelect() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0F0F0)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("연습하고 싶은 곡을 검색하세요", color = Color.Gray)
                    }
                }
            }

            item {
                Text("내 활동 요약", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 수정됨: userStats 필드명 변경 반영
                        val totalTime = userStats?.totalPlayTime ?: 0
                        val hours = totalTime / 3600
                        val mins = (totalTime % 3600) / 60

                        StatSummaryItem("총 시간", "${hours}시간 ${mins}분")
                        StatSummaryItem("완곡", "${userStats?.completedParts ?: 0}곡")
                        StatSummaryItem("평균 점수", "${userStats?.avgAccuracy?.toInt() ?: 0}점")
                    }
                }
            }
        }
    }
}

@Composable
fun StatSummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}