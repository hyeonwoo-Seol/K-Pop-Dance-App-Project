package com.example.kpopdancepracticeai.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val songTitle = currentSong?.titleKr?.ifBlank { currentSong.titleEn }.orEmpty()
    val songArtist = currentSong?.artistKr?.ifBlank { currentSong.artistEn }.orEmpty()

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
                        Text(songTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(songArtist, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    "${(part.endTimeMs - part.startTimeMs).toTimeLabel()}",
                                    part.videoUrl ?: "" // 💡 [오류 해결] 값이 비어있을 경우 안전하게 넘기도록 처리
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(time, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPracticeClick) {
                    Text("연습")
                }
                OutlinedButton(onClick = onUploadClick) {
                    Text("동영상 업로드")
                }
            }
        }
    }
}

private fun Long.toTimeLabel(): String {
    val totalSeconds = this / 1000
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
