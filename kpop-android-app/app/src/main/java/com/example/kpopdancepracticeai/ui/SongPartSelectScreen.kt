package com.example.kpopdancepracticeai.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kpopdancepracticeai.data.PresignedUrlUploader
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart
import com.example.kpopdancepracticeai.data.mapper.AnalysisMapper
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.util.FilenameParser
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.util.Locale

@Composable
fun SongPartSelectScreen(
    songId: String,
    viewModel: MainViewModel = viewModel(),
    onBackClick: () -> Unit,
    onNavigateToPractice: (Long, String, String, String, String, String) -> Unit,
    onNavigateToResult: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uploader = remember { PresignedUrlUploader(context) }
    var showAnalysisLoading by remember { mutableStateOf(false) }

    var selectedPartForUpload by remember { mutableStateOf<SongPart?>(null) }

    LaunchedEffect(songId) { viewModel.selectSong(songId.toLongOrNull() ?: 0L) }

    val dbParts by viewModel.currentSongParts.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val authUserId = remember { AuthRepository(context).getCurrentUser()?.uid }
    val currentSong = songs.find { it.songId.toString() == songId }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val part = selectedPartForUpload
        if (uri != null && part != null && currentSong != null) {
            val userId = userProfile?.userUuid ?: authUserId ?: "none"
            val expertIdentifier = buildExpertIdentifierForUpload(currentSong, part)
            val timestamp = System.currentTimeMillis()

            val filename = "${userId}_${expertIdentifier}_${timestamp}.mp4"

            scope.launch {
                Toast.makeText(context, "동영상 업로드 시작...", Toast.LENGTH_SHORT).show()
                showAnalysisLoading = true
                uploader.uploadVideo(
                    fileUri = uri,
                    filename = filename,
                    onComplete = { s3Key ->
                        Log.d("SongPartSelect", "Upload Success Key: $s3Key")
                        Toast.makeText(context, "업로드 완료! 분석을 기다립니다...", Toast.LENGTH_SHORT).show()

                        scope.launch {
                            uploader.pollAnalysisResult(
                                userId = userId,
                                timestamp = timestamp,
                                onProgress = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onComplete = { resultS3Key ->
                                    scope.launch {
                                        try {
                                            val jsonString = uploader.downloadResultJson(resultS3Key)
                                            val jsonFileName = uploader.extractResultFileName(resultS3Key)

                                            val response = Gson().fromJson(
                                                jsonString,
                                                AnalysisResultResponse::class.java
                                            )

                                            val metadata = FilenameParser.ParsedMetadata(
                                                userId = userId,
                                                songId = currentSong.songId.toString(),
                                                artist = currentSong.artistKr,
                                                partNumber = part.partNumber.toString()
                                            )

                                            val jsonPath = File(
                                                File(context.filesDir, "analysis_results"),
                                                jsonFileName
                                            ).absolutePath

                                            val historyEntity = AnalysisMapper.mapToPracticeHistory(
                                                analysisResult = response,
                                                metadata = metadata,
                                                videoPath = uri.toString(),
                                                fullJsonPath = jsonPath
                                            )
                                            viewModel.savePracticeResult(historyEntity)

                                            showAnalysisLoading = false
                                            Toast.makeText(context, "분석 완료!", Toast.LENGTH_SHORT).show()
                                            onNavigateToResult(jsonFileName, uri.toString())
                                        } catch (e: Exception) {
                                            showAnalysisLoading = false
                                            Log.e("SongPartSelect", "분석 결과 처리 실패", e)
                                            Toast.makeText(
                                                context,
                                                "결과 처리 실패: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onError = { e ->
                                    showAnalysisLoading = false
                                    Toast.makeText(
                                        context,
                                        "분석 실패: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    onError = { e ->
                        showAnalysisLoading = false
                        Toast.makeText(context, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
        selectedPartForUpload = null
    }

    if (showAnalysisLoading) {
        AnalysisWaitingScreen(
            onAnalysisComplete = { }
        )
    } else {
        SongPartSelectContent(
            currentSong = currentSong,
            dbParts = dbParts,
            onBackClick = onBackClick,
            onNavigateToPractice = onNavigateToPractice,
            onUploadClick = { part ->
                selectedPartForUpload = part
                videoPickerLauncher.launch("video/*")
            }
        )
    }
}

private fun buildExpertIdentifierForUpload(song: Song, part: SongPart): String {
    val fromVideoUrl = part.videoUrl
        ?.substringBefore("?")
        ?.let { raw ->
            val parsed = Uri.parse(raw).lastPathSegment ?: raw.substringAfterLast("/")
            runCatching { URLDecoder.decode(parsed, "UTF-8") }.getOrDefault(parsed)
        }
        ?.substringBeforeLast(".")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    if (fromVideoUrl != null) return fromVideoUrl

    val artistToken = song.artistKr
        .substringAfter("(")
        .substringBefore(")")
        .ifBlank { song.artistKr }
        .replace(" ", "")
        .replace("_", "")

    return "${song.songId}_${artistToken}_${part.partNumber}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPartSelectContent(
    currentSong: Song?,
    dbParts: List<SongPart>,
    onBackClick: () -> Unit,
    onNavigateToPractice: (Long, String, String, String, String, String) -> Unit,
    onUploadClick: (SongPart) -> Unit
) {
    val screenBg = Color.Transparent
    val songCardBg = Color.White
    val pageGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFC7D2FE),
            Color(0xFFE0E7FF)
        )
    )

    val songTitle = currentSong?.titleKr?.ifBlank { currentSong.titleEn }.orEmpty()
    val songArtist = currentSong?.artistKr?.ifBlank { currentSong.artistEn }.orEmpty()
    val totalSongLengthMs = dbParts.sumOf { (it.endTimeMs - it.startTimeMs).coerceAtLeast(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradient)
    ) {
        Scaffold(
            containerColor = screenBg,
            topBar = {
                TopAppBar(
                    title = { Text("파트 선택") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg),
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentSong != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = songCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x12000000))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val coverUrl = currentSong.coverUrl.orEmpty()
                                if (coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(coverUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "${songTitle} cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(Color(0xFFD0D2DB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🎵", style = MaterialTheme.typography.headlineSmall)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = songTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = songArtist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "⏱ ${totalSongLengthMs.toTimeLabel()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "연습할 파트를 선택하세요",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF4A4E67)
                        )
                    }
                }

                if (dbParts.isEmpty()) {
                    item { Text("등록된 파트가 없습니다.", modifier = Modifier.padding(16.dp)) }
                } else {
                    items(dbParts) { part ->
                        PartCard(
                            title = "Part ${part.partNumber}: ${part.partName}",
                            time = "${part.startTimeMs.toTimeLabel()} - ${part.endTimeMs.toTimeLabel()}",
                            onPracticeClick = {
                                onNavigateToPractice(
                                    currentSong?.songId ?: 0L,
                                    songTitle,
                                    "$songArtist · ${part.partName}",
                                    currentSong?.difficulty.orEmpty(),
                                    (part.endTimeMs - part.startTimeMs).toTimeLabel(),
                                    part.videoUrl.orEmpty()
                                )
                            },
                            onUploadClick = { onUploadClick(part) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PartCard(
    title: String,
    time: String,
    onPracticeClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(time, style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPracticeClick,
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1A000000))
                    ) {
                        Text(
                            text = "연습",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onUploadClick,
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1A000000))
                    ) {
                        Text(
                            text = "동영상 업로드",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun Long.toTimeLabel(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
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
            onNavigateToPractice = { _, _, _, _, _, _ -> },
            onUploadClick = {}
        )
    }
}
