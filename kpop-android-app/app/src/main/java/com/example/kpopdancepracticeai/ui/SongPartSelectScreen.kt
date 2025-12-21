package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import kotlinx.coroutines.delay

// --- 1. UI 상태 및 임시 데이터 클래스 ---

// 상단 곡 정보
private data class PartSelectSongInfo(
    val title: String,
    val artist: String,
    val length: String,
    val imageUrl: String
)

// 파트별 정보
private data class SongPart(
    val id: String,
    val title: String,
    val time: String,
    val difficulty: String,
    val progress: Float, // 0.0f ~ 1.0f
    val bestScore: Int,
    val isLocked: Boolean
)

// --- 2. 임시 더미 데이터 (이미지 기반) ---
private val dummySong = PartSelectSongInfo(
    title = "Dynamite",
    artist = "BTS",
    length = "2:15",
    // 임시 플레이스홀더 이미지
    imageUrl = "https://placehold.co/100x100/D6BCFF/000000?text=Dynamite"
)

private val dummyParts = listOf(
    SongPart("part1", "Part 1: 인트로", "0:00 - 0:30", "쉬움", 1.0f, 95, false),
    SongPart("part2", "Part 2: 메인 파트", "0:30 - 1:15", "보통", 0.65f, 82, false),
    SongPart("part3", "Part 3: 브릿지", "1:15 - 1:45", "어려움", 0.05f, 0, false),
    SongPart("part4", "Part 4: 아웃트로", "1:45 - 2:15", "보통", 0.0f, 0, true)
)

// --- 3. 메인 Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPartSelectScreen(
    songId: String,
    onBackClick: () -> Unit,
    // ⭐️ [추가] 연습 화면으로 네비게이션하는 람다 추가
    onNavigateToPractice: (songTitle: String, artistPart: String, difficulty: String, length: String) -> Unit
) {
    // 앱 전체의 그라데이션 배경
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF), // 상단 연한 파랑
            Color(0xFFF0E8FF)  // 하단 연한 보라
        )
    )

    // TODO: songId를 기반으로 ViewModel에서 실제 곡 정보와 파트 정보를 가져옵니다.
    // 여기서는 더미 데이터를 사용합니다.
    val songInfo = dummySong
    val parts = dummyParts

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("곡 파트 선택 화면", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        // 하단 플로팅 버튼 (이미지 참고)
        floatingActionButton = {
            Button(
                onClick = {
                    // ⭐️ [수정] 전체 곡 시작하기 클릭 시 네비게이션: 전체 곡 정보 전달
                    val fullTitle = songInfo.title
                    val fullArtistPart = "${songInfo.artist} · 전체 곡" // 전체 곡을 위한 임의의 파트 제목
                    // 전체 곡의 난이도는 곡의 대표 난이도를 사용하거나, '보통' 등으로 임의 설정
                    val fullDifficulty = "보통"
                    val fullLength = songInfo.length
                    onNavigateToPractice(fullTitle, fullArtistPart, fullDifficulty, fullLength)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "전체 곡 시작하기",
                    style = TextStyle(
                        fontWeight = FontWeight(700),
                        fontSize = 18.sp
                    )
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 곡 프로필 카드
            item {
                SongProfileCard(song = songInfo)
            }

            // 2. 파트 선택 타이틀
            item {
                Text(
                    text = "연습할 파트를 선택하세요",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xff364153)
                )
            }

            // 3. 파트 목록
            items(parts) { part ->
                PartCard(
                    part = part,
                    onPracticeClick = {
                        // ⭐️ [수정] 파트 연습 시작 클릭 시 네비게이션: 파트 정보 전달
                        val partSongTitle = songInfo.title
                        val partArtistPart = "${songInfo.artist} · ${part.title}"
                        val partDifficulty = part.difficulty
                        // 파트의 시간 정보 (예: "0:30 - 1:15")는 PracticeScreen의 총 시간 정보로 부적절하므로,
                        // 임시로 파트의 시간 정보만 전달 (0:45) 하거나, 전체 곡 길이를 전달해야 하지만,
                        // Figma 스니펫에는 전체 곡 길이("2:15")가 있었으므로, 여기서는 파트의 시간 정보(part.time)를 전달하겠습니다.
                        val partLength = part.time
                        onNavigateToPractice(partSongTitle, partArtistPart, partDifficulty, partLength)
                    }
                )
            }

            // 4. 하단 플로팅 버튼 공간 확보
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * 상단 곡 프로필 카드
 */
@Composable
private fun SongProfileCard(song: PartSelectSongInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0x1a000000))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(song.imageUrl),
                contentDescription = song.title,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xff1e2939)
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xff4a5565)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 시간 정보
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "곡 길이",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xff6a7282)
                    )
                    Text(
                        text = song.length,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xff6a7282)
                    )
                }
            }
        }
    }
}

/**
 * 개별 파트 카드
 */
@Composable
private fun PartCard(
    part: SongPart,
    onPracticeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0x1a000000))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 상단: 파트 정보 및 연습 버튼 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 파트 정보 (좌측)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 파트 제목 (아이콘 포함)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = part.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xff1e2939)
                        )
                        // 상태 아이콘
                        if (part.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "잠김",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (part.progress >= 1.0f) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "완료",
                                tint = Color(0xFF00A63E), // 쉬움 태그의 녹색
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 시간 및 난이도 (아이콘 미포함 버전)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 시간
                        InfoRow(icon = Icons.Default.AccessTime, text = part.time)
                        // 난이도 배지
                        DifficultyBadge(difficulty = part.difficulty)
                    }
                }

                // 연습 버튼 (우측)
                if (!part.isLocked) {
                    Button(
                        onClick = onPracticeClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF3F4F6), // 연한 회색 배경
                            contentColor = Color.Black
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)) // 연한 회색 테두리
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "연습",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("연습")
                    }
                }
            }

            // --- 하단: 진행도 또는 잠금 메시지 ---
            if (part.isLocked) {
                LockMessage()
            } else {
                ProgressSection(progress = part.progress, score = part.bestScore)
            }
        }
    }
}

/**
 * 시간, 아이콘 표시용
 */
@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xff6a7282)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xff6a7282)
        )
    }
}

/**
 * 난이도 배지
 */
@Composable
private fun DifficultyBadge(difficulty: String) {
    // SearchScreen.kt의 getChipColors 로직 재활용
    val (bgColor, textColor) = when (difficulty) {
        "쉬움" -> Color(0xfff0fdf4) to Color(0xff00a63e)
        "보통" -> Color(0xfffefce8) to Color(0xffa65f00) // 이미지의 노란색에 맞게 수정
        "어려움" -> Color(0xffffe2e2) to Color(0xffc10007)
        else -> Color(0xFFF3F4F6) to Color.Black
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = difficulty,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * 진행도 표시 섹션
 */
@Composable
private fun ProgressSection(progress: Float, score: Int) {
    val progressPercent = (progress * 100).toInt()
    val scoreColor = Color(0xff9810fa) // 이미지의 보라색

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // 진행도 텍스트
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "진행도",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xff4a5565)
            )
            Text(
                text = "${progressPercent}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }

        // 진행도 바
        val trackColor = if (progressPercent > 5) Color(0x33030213) else Color.Transparent
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xff030213), // 검은색
            trackColor = trackColor
        )

        // 최고 점수
        if (score > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "최고 점수:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xff6a7282)
                )
                Text(
                    text = "${score}점",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
        }
    }
}

/**
 * 잠금 메시지
 */
@Composable
private fun LockMessage() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xff6a7282)
        )
        Text(
            text = "이전 파트를 먼저 완료해주세요",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xff6a7282)
        )
    }
}


// --- 4. 미리보기 ---
@Preview(showBackground = true)
@Composable
fun SongPartSelectScreenPreview() {
    KpopDancePracticeAITheme {
        SongPartSelectScreen(
            songId = "preview_id",
            onBackClick = {},
            onNavigateToPractice = { _, _, _, _ -> }
        )
    }
}