package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
// viewModel 관련 import는 AppNavigation에서 처리하므로 여기서는 제거하거나 유지해도 무방

// [주의] data entity의 Song과 충돌을 피하기 위해 파일 내부에서만 사용하거나 패키지 구분을 명확히 해야 함
// 여기서는 UI용 데이터 클래스로 그대로 유지
data class SongUiModel(
    val id: String,
    val artist: String,
    val title: String,
    val views: String,
    val thumbnailUrl: String = ""
)

val popularSongs = listOf(
    SongUiModel("1", "aespa", "Whiplash", "2.5만회 조회"),
    SongUiModel("2", "NMIXX", "Blue Valentine", "1.2만회 조회"),
    SongUiModel("3", "프로미스나인", "LIKE YOU BETTER", "3.4만회 조회")
)
val challengeSongs = listOf(
    SongUiModel("4", "aespa", "Whiplash", "2.5만회 조회"),
    SongUiModel("5", "NMIXX", "Blue Valentine", "1.2만회 조회"),
    SongUiModel("6", "프로미스나인", "LIKE YOU BETTER", "3.4만회 조회")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    onSongClick: (String) -> Unit,
    paddingValues: PaddingValues,
    // ViewModel은 필요 시 추가. 현재 UI 코드에는 사용되지 않음.
    // viewModel: MainViewModel? = null
) {
    var searchText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "KPOP 댄스 연습 앱",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("연습할 곡을 검색하세요") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(searchText)
                    }
                )
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // SectionTitle이 없다면 일반 Text로 대체하거나 별도 컴포넌트 필요
                // 여기서는 일반 Text로 구현 (Components.kt 의존성 제거를 위해)
                Text("인기 급상승 안무", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("인기 급상승 챌린지", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("최근 내가 조회한 안무", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
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
        HomeScreen(onSearch = {}, onSongClick = {}, paddingValues = PaddingValues())
    }
}