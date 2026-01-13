package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    onSearch: (String) -> Unit,
    onSongClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dbSongs by viewModel.songs.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("K-Pop Dance AI", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier.padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SearchBarSection(searchText, { searchText = it }, { onSearch(searchText) })
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("등록된 안무 목록", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (dbSongs.isEmpty()) {
                        item { Text("등록된 곡이 없습니다.", modifier = Modifier.padding(10.dp)) }
                    } else {
                        items(dbSongs) { song ->
                            // [오류 해결] Song 엔티티 필드명 수정 (title -> titleKr, artistName -> artistKr 등)
                            SongCard(
                                title = song.titleKr,
                                artist = song.artistKr,
                                views = "난이도 ${song.difficulty}",
                                imageUrl = song.coverUrl,
                                onClick = { onSongClick(song.songId.toString()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarSection(text: String, onValueChange: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text("검색") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
    )
}

@Composable
fun SongCard(title: String, artist: String, views: String, imageUrl: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(140.dp).clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(views, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}