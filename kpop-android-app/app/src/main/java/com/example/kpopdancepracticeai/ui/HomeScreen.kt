package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import androidx.navigation.compose.rememberNavController

data class Song(
    val id: String,
    val artist: String,
    val title: String,
    val views: String,
    val thumbnailUrl: String = ""
)

val popularSongs = listOf(
    Song("1", "aespa", "Whiplash", "2.5만회 조회"),
    Song("2", "NMIXX", "Blue Valentine", "1.2만회 조회"),
    Song("3", "프로미스나인", "LIKE YOU BETTER", "3.4만회 조회")
)
val challengeSongs = listOf(
    Song("4", "aespa", "Whiplash", "2.5만회 조회"),
    Song("5", "NMIXX", "Blue Valentine", "1.2만회 조회"),
    Song("6", "프로미스나인", "LIKE YOU BETTER", "3.4만회 조회")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onSongClick: (String) -> Unit,
    paddingValues: PaddingValues
) {
    var searchText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("연습할 곡을 검색하세요") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onSearchClick() },
                readOnly = true,
            )
        }

        item {
            SectionTitle(title = "인기 급상승 안무") // ⭐️ Components.kt의 SectionTitle 사용
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(popularSongs) { song ->
                    SongCard(
                        artist = song.artist,
                        title = song.title,
                        views = song.views,
                        onClick = { onSongClick(song.id) }
                    )
                }
            }
        }

        item {
            SectionTitle(title = "인기 급상승 챌린지") // ⭐️ Components.kt의 SectionTitle 사용
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(challengeSongs) { song ->
                    SongCard(
                        artist = song.artist,
                        title = song.title,
                        views = song.views,
                        onClick = { onSongClick(song.id) }
                    )
                }
            }
        }

        item {
            SectionTitle(title = "최근 내가 조회한 안무") // ⭐️ Components.kt의 SectionTitle 사용
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(popularSongs.reversed()) { song ->
                    SongCard(
                        artist = song.artist,
                        title = song.title,
                        views = song.views,
                        onClick = { onSongClick(song.id) }
                    )
                }
            }
        }
    }
}

// ⭐️ 여기에 있던 SectionTitle 함수 삭제됨!

@Composable
fun SongCard(
    artist: String,
    title: String,
    views: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = title, tint = Color.Gray)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist,
            fontSize = 12.sp,
            fontWeight = FontWeight(400),
            color = Color(0x80000000),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight(400),
            color = Color(0xff000000),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = views,
            fontSize = 16.sp,
            fontWeight = FontWeight(500),
            color = Color(0xff000000),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KpopDancePracticeAITheme {
        HomeScreen(onSearchClick = {}, onSongClick = {}, paddingValues = PaddingValues())
    }
}