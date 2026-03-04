package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

@Composable
fun SongPartSelectScreen(
    songId: String,
    viewModel: MainViewModel = viewModel(),
    onBackClick: () -> Unit,
    onNavigateToPractice: (String, String, String, String) -> Unit
) {
    LaunchedEffect(songId) { viewModel.selectSong(songId.toLongOrNull() ?: 0L) }

    val dbParts by viewModel.currentSongParts.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val currentSong = songs.find { it.songId.toString() == songId }

    SongPartSelectContent(
        currentSong = currentSong,
        dbParts = dbParts,
        onBackClick = onBackClick,
        onNavigateToPractice = onNavigateToPractice
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPartSelectContent(
    currentSong: Song?,
    dbParts: List<SongPart>,
    onBackClick: () -> Unit,
    onNavigateToPractice: (String, String, String, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("파트 선택") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (currentSong != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // [오류 해결] Song 엔티티 필드명 수정
                        Text(currentSong.titleKr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(currentSong.artistKr, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dbParts.isEmpty()) {
                    item { Text("등록된 파트가 없습니다.", modifier = Modifier.padding(16.dp)) }
                } else {
                    items(dbParts) { part ->
                        PartCard(
                            // [오류 해결] SongPart 엔티티 필드명 수정 (partName)
                            title = part.partName,
                            time = "${part.startTimeMs/1000}초 - ${part.endTimeMs/1000}초",
                            onPracticeClick = {
                                onNavigateToPractice(
                                    currentSong?.titleKr ?: "",
                                    "${currentSong?.artistKr} · ${part.partName}",
                                    "Normal",
                                    "${(part.endTimeMs - part.startTimeMs)/1000}s"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PartCard(title: String, time: String, onPracticeClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(time, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onPracticeClick) {
                Text("연습")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongPartSelectScreenPreview() {
    val sampleSong = Song(
        songId = 1L,
        titleKr = "Super Shy",
        titleEn = "Super Shy",
        artistKr = "뉴진스",
        artistEn = "NewJeans",
        artistGender = "Female",
        genre = "Dance",
        tempo = "Fast",
        difficulty = "Normal",
        coverUrl = null,
        releaseDate = "2023-07-07"
    )
    val sampleParts = listOf(
        SongPart(1L, 1L, 1, "Intro", 15, null, null, 0L, 15000L),
        SongPart(2L, 1L, 2, "Chorus 1", 20, null, null, 15000L, 35000L)
    )

    KpopDancePracticeAITheme {
        SongPartSelectContent(
            currentSong = sampleSong,
            dbParts = sampleParts,
            onBackClick = {},
            onNavigateToPractice = { _, _, _, _ -> }
        )
    }
}
