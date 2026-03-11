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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(), // DB 데이터를 가져오기 위한 ViewModel
    onSearch: (String) -> Unit,
    onSongClick: (String) -> Unit,
    paddingValues: PaddingValues
) {
    // DB에서 불러온 노래 목록을 상태로 관리
    val dbSongs by viewModel.songs.collectAsState()
    val recentChoreo by viewModel.recentChoreo.collectAsState()
    var searchText by remember { mutableStateOf("") }

    // 화면 진입 시 최신 데이터 로드 (필요한 경우)
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    val layoutDirection = LocalLayoutDirection.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection),
            bottom = paddingValues.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. 타이틀 및 검색창 섹션
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KPOP 댄스 연습 AI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("연습할 곡을 검색하세요") }, // label 대신 placeholder 사용
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), // 둥근 모서리
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch(searchText) }
                    )
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    title = "최근 연습한 안무",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (recentChoreo.isEmpty()) {
                    Text(
                        text = "최근에 연습한 안무가 없습니다",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recentChoreo.take(4)) { item ->
                            SongCard(
                                artist = item.artist,
                                title = "${item.title} (파트 ${item.partNumber})",
                                views = "마지막 연습 ${item.lastPracticedAt}",
                                imageUrl = item.coverUrl,
                                onClick = { onSongClick(item.songId.toString()) }
                            )
                        }
                    }
                }
            }
        }

        // 2. 등록된 안무 목록 (DB 연동)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    title = "등록된 안무 목록",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (dbSongs.isEmpty()) {
                    // 데이터가 없을 때 표시할 UI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(horizontal = 16.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "등록된 곡이 없습니다.\n데이터를 동기화해주세요.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    // 데이터가 있을 때 가로 스크롤 리스트 표시
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dbSongs) { song ->
                            SongCard(
                                artist = song.artistKr ?: "Unknown Artist",
                                title = song.titleKr ?: "Unknown Title",
                                views = "난이도 ${song.difficulty}", // 조회수 대신 난이도 표시 (DB 필드 활용)
                                imageUrl = song.coverUrl,
                                onClick = { onSongClick(song.songId.toString()) }
                            )
                        }
                    }
                }
            }
        }

        // 3. 인기 급상승 챌린지 (현재는 DB 데이터 재사용 또는 더미)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionTitle(
                    title = "인기 급상승 챌린지",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 임시로 DB 데이터를 역순으로 보여줌 (실제 로직 구현 시 변경)
                if (dbSongs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dbSongs.reversed()) { song ->
                            SongCard(
                                artist = song.artistKr ?: "",
                                title = song.titleKr ?: "",
                                views = "인기",
                                imageUrl = song.coverUrl,
                                onClick = { onSongClick(song.songId.toString()) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "챌린지 목록을 불러올 수 없습니다.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// 섹션 제목 컴포넌트
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = modifier
    )
}

// 곡 정보 카드 컴포넌트
@Composable
fun SongCard(
    artist: String,
    title: String,
    views: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp) // 카드 너비 조정
            .clickable(onClick = onClick)
    ) {
        // 앨범 커버 이미지
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant), // 로딩 중 배경색
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "$title cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 이미지가 없을 때 아이콘 표시
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 곡 제목
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 아티스트
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // 부가 정보 (조회수/난이도)
        Text(
            text = views,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KpopDancePracticeAITheme {
        // Preview를 위한 더미 데이터 구성은 실제 런타임에는 영향을 주지 않습니다.
        HomeScreen(onSearch = {}, onSongClick = {}, paddingValues = PaddingValues())
    }
}
