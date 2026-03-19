package com.example.kpopdancepracticeai.ui

import com.example.kpopdancepracticeai.ui.motion.rememberIosLikeFlingBehavior

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kpopdancepracticeai.data.RealDataSource
import com.example.kpopdancepracticeai.ui.theme.AppBackgroundBottom
import com.example.kpopdancepracticeai.ui.theme.AppBackgroundTop
import com.example.kpopdancepracticeai.viewmodel.MainViewModel

private val DetailCardBackground = Color(0xFFFFFFFF)
private val DetailCardBorder = Color(0xFFDBDFFE)
private val DetailPrimaryText = Color(0xFF0A0A0A)
private val DetailSecondaryText = Color(0xFF717182)
private val DetailBadgeColor = Color(0xFFF0B100)
private val DetailMetaBackground = Color(0xFFF8FAFF)
private val DetailButtonBackground = Color(0xFFFFFFFF)
private val DetailButtonText = Color(0xFF000000)

data class SongInfoUi(
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val level: String,
    val releaseDate: String,
    val mainArtist: String?,
    val composers: List<String>,
    val lyricists: List<String>,
    val producers: List<String>,
    val source: String?,
    val partCount: Int
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
    val screenBackground = remember {
        Brush.verticalGradient(
            colors = listOf(AppBackgroundTop, AppBackgroundBottom)
        )
    }

    var uiState by remember { mutableStateOf<SongInfoUi?>(null) }

    LaunchedEffect(selectedSong) {
        uiState = selectedSong?.let { song ->
            val detailMetadata = RealDataSource.songDetailMetadataByTitleKr[song.titleKr]
            SongInfoUi(
                title = song.titleKr,
                artist = song.artistKr,
                albumArtUrl = song.coverUrl.orEmpty(),
                level = song.difficulty,
                releaseDate = song.releaseDate ?: "-",
                mainArtist = detailMetadata?.mainArtist,
                composers = detailMetadata?.composers.orEmpty(),
                lyricists = detailMetadata?.lyricists.orEmpty(),
                producers = detailMetadata?.producers.orEmpty(),
                source = detailMetadata?.source,
                partCount = RealDataSource.getRealSongParts.count { it.songId == song.songId }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { navController.navigate("songPartSelect/$songId") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DetailButtonBackground,
                            contentColor = DetailButtonText
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "연습 시작하기",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) { innerPadding ->
            val info = uiState
            if (info == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn(
            flingBehavior = rememberIosLikeFlingBehavior(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = Color.White
                            )
                        }
                    }

                    item {
                        SongHeaderCard(info = info)
                    }

                    item {
                        DetailInfoCard(
                            title = "곡 정보",
                            items = listOfNotNull(
                                info.mainArtist?.let { "메인 아티스트" to it },
                                "작곡가" to info.composers.joinToString().ifBlank { "정보 없음" },
                                "작사가" to info.lyricists.joinToString().ifBlank { "정보 없음" },
                                "프로듀서" to info.producers.joinToString().ifBlank { "정보 없음" },
                                info.source?.let { "출처" to it }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongHeaderCard(info: SongInfoUi) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DetailCardBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, DetailCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(info.albumArtUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${info.title} 앨범 커버",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14000000))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DetailPrimaryText
                        )
                        Text(
                            text = info.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = DetailSecondaryText
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DifficultyChip(level = info.level)
                            MetaIconText(
                                icon = Icons.Default.CalendarToday,
                                text = info.releaseDate,
                                textStyle = MaterialTheme.typography.labelSmall
                            )
                        }
                        MetaIconText(
                            icon = Icons.Default.QueueMusic,
                            text = "${info.partCount}개 파트"
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaStatRow(
                    icon = Icons.Default.Groups,
                    label = "대표 아티스트",
                    value = info.mainArtist ?: info.artist
                )
                MetaStatRow(
                    icon = Icons.Default.GraphicEq,
                    label = "난이도",
                    value = info.level
                )
            }
        }
    }
}

@Composable
private fun DifficultyChip(level: String) {
    AssistChip(
        onClick = { },
        enabled = false,
        label = {
            Text(
                text = level,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = DetailBadgeColor,
            disabledLabelColor = Color.White
        ),
        border = null
    )
}

@Composable
private fun MetaIconText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DetailSecondaryText,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = DetailSecondaryText,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetaStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DetailMetaBackground, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DetailSecondaryText,
            modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DetailSecondaryText
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = DetailPrimaryText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DetailInfoCard(
    title: String,
    items: List<Pair<String, String>>
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DetailCardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, DetailCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DetailPrimaryText
            )

            items.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = DetailSecondaryText
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DetailPrimaryText
                    )
                }
            }
        }
    }
}
