package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.viewmodel.SearchFilters
import com.example.kpopdancepracticeai.viewmodel.SearchUiState
import com.example.kpopdancepracticeai.viewmodel.SearchViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    query: String,
    difficulty: String?,
    artistGender: String?,
    tempo: String?,
    navController: NavHostController,
    paddingValues: PaddingValues,
    viewModel: SearchViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val text by viewModel.query.collectAsState()

    LaunchedEffect(query, difficulty, artistGender, tempo) {
        viewModel.updateQuery(query)
        viewModel.updateFilters(
            SearchFilters(
                difficulty = difficulty,
                artistGender = artistGender,
                tempo = tempo
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "검색 결과",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            OutlinedTextField(
                value = text,
                onValueChange = { viewModel.updateQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("노래, 아티스트 검색") }
            )
        }

        item {
            Crossfade(
                targetState = uiState,
                animationSpec = tween(durationMillis = 300),
                label = "search_result_state"
            ) { state ->
                when (state) {
                    SearchUiState.Idle -> Spacer(modifier = Modifier.height(8.dp))
                    SearchUiState.Empty -> EmptySearchResult()
                    is SearchUiState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.songs.forEachIndexed { index, song ->
                                var isVisible by remember(song.songId) { mutableStateOf(false) }

                                LaunchedEffect(song.songId, text) {
                                    isVisible = false
                                    delay(index * 45L)
                                    isVisible = true
                                }

                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn(tween(280)) + slideInVertically(
                                        initialOffsetY = { it / 2 },
                                        animationSpec = tween(280)
                                    ),
                                    exit = fadeOut(tween(140)) + slideOutVertically(
                                        targetOffsetY = { -it / 8 },
                                        animationSpec = tween(140)
                                    )
                                ) {
                                    SearchSongItem(song = song) {
                                        navController.navigate("songPartSelect/${song.songId}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SearchSongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = song.titleKr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = song.artistKr, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "난이도: ${song.difficulty}", style = MaterialTheme.typography.labelMedium)
                Text(text = "템포: ${song.tempo}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EmptySearchResult() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "검색 결과 없음",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("검색 결과가 없습니다", style = MaterialTheme.typography.titleMedium)
            Text("다른 검색어로 다시 시도해보세요", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
