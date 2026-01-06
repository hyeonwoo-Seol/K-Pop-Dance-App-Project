package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter // ⭐️ [오류 수정] 이 줄이 추가되었습니다.
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

// 임시 데이터 클래스
data class SongData(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String,
    val views: String,
    val level: String,
    val time: String,
    val parts: String,
    val practiceCount: String,
    val likes: String,
    val tags: List<String>
)

// 임시 더미 데이터 (image_ea5c67.png 기반)
val dummySearchResult = SongData(
    id = "whiplash_aespa_01",
    title = "Whiplash",
    artist = "aespa",
    imageUrl = "https://placehold.co/300x450/000000/FFFFFF?text=aespa", // Placeholder
    views = "2.5만회 조회",
    level = "Medium",
    time = "2:48",
    parts = "4개 파트",
    practiceCount = "125.0K 연습",
    likes = "8.9K 좋아요",
    tags = listOf("Medium", "Popular")
)

val dummyRecommendedSongs = listOf(
    SongData("getup_newjeans_01", "Get Up", "NewJeans", "https://placehold.co/300x450/6060FF/FFFFFF?text=NewJeans", "5.5만회 조회", "", "", "", "", "", listOf()),
    SongData("blue_nmixx_01", "Blue Valentine", "NMIXX", "https://placehold.co/300x450/333333/FFFFFF?text=NMIXX", "1.2만회 조회", "", "", "", "", "", listOf()),
    SongData("likeyou_fromis_01", "LIKE YOU BETTER", "프로미스나인", "https://placehold.co/300x450/FF6060/FFFFFF?text=fromis_9", "3.4만회 조회", "", "", "", "", "", listOf())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    query: String,
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    // 검색창의 텍스트 상태는 제거되었습니다. (요청사항 반영)

    // TODO: 이 query를 ViewModel에 전달하여 실제 검색 결과 (songResult, recommendedSongs)를 가져옵니다.
    // 여기서는 더미 데이터를 즉시 사용합니다.
    val songResult = dummySearchResult
    val recommendedSongs = dummyRecommendedSongs

    // 스니펫에서 추출한 필터 목록
    val filters = listOf("보통", "걸그룹", "인기곡")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- 1. 화면 타이틀 ---
        item {
            Text(
                text = "검색 결과",
                style = TextStyle( // 스니펫 스타일 적용
                    fontWeight = FontWeight(600),
                    fontSize = 32.sp,
                ),
                color = Color(0xff000000),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // --- 2. 검색창 제거됨 (요청사항 반영) ---
        // 원래 위치하던 OutlinedTextField 블록이 삭제되었습니다.

        // --- 3. 필터 칩 (스니펫 스타일 적용) ---
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filterName ->
                    StyledFilterChip(
                        text = filterName,
                        onClick = { /* TODO: 필터 로직 */ }
                    )
                }
            }
        }

        // --- 4. 사용자가 검색한 곡 정보 ---
        item {
            Text(
                text = "사용자가 검색한 곡 정보",
                style = TextStyle( // 스니펫 스타일
                    fontWeight = FontWeight(600),
                    fontSize = 12.sp,
                ),
                color = Color(0xff000000),
            )
            Spacer(modifier = Modifier.height(8.dp)) // 섹션 타이틀과 카드 간격
            SearchResultCard(
                song = songResult,
                onClick = {
                    // "곡 상세" 화면으로 songId를 전달하며 이동
                    navController.navigate("songDetail/${songResult.id}")
                }
            )
        }

        // --- 5. 다른 추천 곡 ---
        item {
            Text(
                text = "다른 추천 곡",
                style = TextStyle( // 스니펫 스타일
                    fontWeight = FontWeight(600),
                    fontSize = 12.sp,
                ),
                color = Color(0xff000000),
            )
            Spacer(modifier = Modifier.height(8.dp)) // 섹션 타이틀과 카드 간격
        }
        items(recommendedSongs) { song ->
            RecommendedSongItem(
                song = song,
                onClick = {
                    // "곡 상세" 화면으로 songId를 전달하며 이동
                    navController.navigate("songDetail/${song.id}")
                }
            )
        }

        // 하단 여백
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 3-1. 스니펫 스타일이 적용된 커스텀 필터 칩
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledFilterChip(
    text: String,
    onClick: () -> Unit
) {
    // 스니펫의 스타일을 기반으로 칩의 색상을 결정합니다.
    val (backgroundColor, borderColor, textColor) = when (text) {
        "보통" -> Triple(Color(0xfffefce8), Color(0xffffdf20), Color(0xffd08700))
        "걸그룹" -> Triple(Color(0xfffdf2f8), Color(0xfffccee8), Color(0xffe60076))
        "인기곡" -> Triple(Color(0xfffff1f2), Color(0xffffccd3), Color(0xffec003f))
        else -> Triple(Color.White, Color.Gray, Color.Black)
    }

    // FilterChip을 사용하되, 스니펫의 Box 스타일을 모방합니다.
    FilterChip(
        selected = true, // 여기서는 항상 선택된 스타일로 표시
        onClick = onClick,
        label = {
            Text(
                text = text,
                color = textColor,
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            )
        },
        modifier = Modifier
            .height(30.dp),
        shape = RoundedCornerShape(16.dp), // 스니펫의 15~16dp
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = backgroundColor,
        ),
        border = FilterChipDefaults.filterChipBorder(
            selected = true,
            enabled = true,
            selectedBorderColor = borderColor,
            // 스니펫의 1.63...dp
            selectedBorderWidth = 1.63.dp
        )
    )
}


/**
 * 4-1. 검색 결과 카드 (image_ea5c67.png 기반)
 */
@Composable
fun SearchResultCard(
    song: SongData,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, // 카드 배경색
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 앨범 아트
            Image(
                painter = rememberAsyncImagePainter(song.imageUrl), // ⭐️ 오류가 발생했던 부분
                contentDescription = song.title,
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // 곡 정보
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 아티스트 (스니펫 스타일 적용)
                Text(
                    text = song.artist,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 12.sp,
                    ),
                    color = Color(0x80000000), // 50% Black
                )
                // 타이틀 (스니펫 스타일 적용)
                Text(
                    text = song.title,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 14.sp,
                    ),
                    color = Color(0xff000000),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // 조회수 (스니펫 스타일 적용)
                Text(
                    text = song.views,
                    style = TextStyle(
                        fontWeight = FontWeight(500),
                        fontSize = 16.sp,
                    ),
                    color = Color(0xff000000),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 태그 (Medium, Popular)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    song.tags.forEach { tag ->
                        val (bgColor, textColor) = getTagColors(tag)
                        Text(
                            text = tag,
                            modifier = Modifier
                                .background(bgColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f)) // 하단 아이콘을 밀어내기 위한 Spacer

                // 아이콘 정보 (image_ea5c67.png 기반)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoIcon(icon = Icons.Default.AccessTime, text = song.time)
                    InfoIcon(icon = Icons.Default.People, text = song.parts)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoIcon(icon = Icons.Default.PlayArrow, text = song.practiceCount)
                    InfoIcon(icon = Icons.Default.Favorite, text = song.likes)
                }
            }
        }
    }
}

// 태그 이름에 따라 색상을 반환하는 헬퍼 함수
@Composable
private fun getTagColors(tag: String): Pair<Color, Color> {
    return when (tag) {
        "Medium" -> Color(0xfffefce8) to Color(0xffd08700) // 노란색
        "Popular" -> Color(0xfffff1f2) to Color(0xffec003f) // 붉은색
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
}

@Composable
private fun InfoIcon(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * 5-1. 추천 곡 아이템 (스니펫 스타일 적용)
 */
@Composable
fun RecommendedSongItem(
    song: SongData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(song.imageUrl), // ⭐️ 오류가 발생했던 부분
            contentDescription = song.title,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            // 아티스트 (스니펫 스타일)
            Text(
                text = song.artist,
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 12.sp,
                ),
                color = Color(0x80000000),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 타이틀 (스니펫 스타일)
            Text(
                text = song.title,
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp,
                ),
                color = Color(0xff000000),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 조회수 (스니펫 스타일)
            Text(
                text = song.views,
                style = TextStyle(
                    fontWeight = FontWeight(500),
                    fontSize = 16.sp,
                ),
                color = Color(0xff000000),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SearchResultsScreenPreview() {
    KpopDancePracticeAITheme {
        SearchResultsScreen(
            query = "whiplash",
            navController = rememberNavController(),
            paddingValues = PaddingValues()
        )
    }
}