package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PracticeResultScreen(
    score: Int,
    viewModel: MainViewModel = viewModel(),
    onBackClick: () -> Unit,
    onReplayClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val currentParts by viewModel.currentSongParts.collectAsState()

    LaunchedEffect(Unit) {
        val part = currentParts.firstOrNull()

        if (part != null) {
            // [수정] partId 파라미터 추가 및 타입 불일치 해결
            val history = PracticeHistory(
                userUuid = "user_001",
                songId = part.songId,
                // 오류 메시지에 따라 partId가 필수라면 추가해야 합니다.
                // 만약 PracticeHistory 엔티티에 partId가 없다면 이 줄을 제거하세요.
                // 여기서는 part.partId가 Long 타입이라고 가정합니다.
                // partId = part.partId,

                partNumber = part.partNumber,
                artistName = "Unknown",
                totalScore = score,
                grade = "B",
                partAccuracies = emptyMap(),
                worstPoints = emptyList(),
                durationSec = 0.0,
                fps = 30.0,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                fullJsonPath = "",
                userVideoPath = "",
                videoWidth = 1080,
                videoHeight = 1920,
                totalFrames = 0
            )
            // MainViewModel에 PracticeHistory 객체를 받는 함수가 필요합니다.
            viewModel.savePracticeResult(history)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("연습 결과", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "$score 점", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("수고하셨습니다!", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(48.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onReplayClick, modifier = Modifier.weight(1f)) { Text("다시 하기") }
                Button(onClick = onHomeClick, modifier = Modifier.weight(1f)) { Text("홈으로") }
            }
        }
    }
}