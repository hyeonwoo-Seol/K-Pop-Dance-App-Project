package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

// UI용 데이터 클래스
data class SongInfoUi(
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val level: String,
    val time: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    songId: String,
    viewModel: MainViewModel = viewModel(),
    navController: NavHostController,
    onBackClick: () -> Unit
) {
    val songs by viewModel.songs.collectAsState()
    val selectedSong = songs.find { it.songId.toString() == songId }

    var uiState by remember { mutableStateOf<SongInfoUi?>(null) }

    LaunchedEffect(selectedSong) {
        if (selectedSong != null) {
            // [오류 해결] Song 엔티티 필드명 수정
            uiState = SongInfoUi(
                title = selectedSong.titleKr,
                artist = selectedSong.artistKr,
                albumArtUrl = selectedSong.coverUrl ?: "",
                level = selectedSong.difficulty,
                // duration 필드 대신 releaseDate 또는 기본값 사용
                time = selectedSong.releaseDate ?: "-"
            )
        }
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = { navController.navigate("songPartSelect/$songId") },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("파트 선택하고 연습하기")
            }
        }
    ) { innerPadding ->
        if (uiState != null) {
            val info = uiState!!
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(info.albumArtUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                    IconButton(onClick = onBackClick, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Text(info.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(info.artist, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(0.8f))
                    }
                }
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("상세 정보", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("난이도: ${info.level}")
                    Text("발매일: ${info.time}")
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}