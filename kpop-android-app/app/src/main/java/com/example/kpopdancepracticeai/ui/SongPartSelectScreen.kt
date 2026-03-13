package com.example.kpopdancepracticeai.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.util.Locale

private data class PartSelectAnimationSpec(
    val itemFadeDurationMs: Int = 200,
    val itemSlideDurationMs: Int = 260,
    val itemStaggerDelayMs: Int = 55,
    val maxItemDelayMs: Int = 260,
    val itemOffsetDivisor: Int = 10
)

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
    var analysisProgress by remember { mutableStateOf(0f) }
    var analysisStatusMessage by remember { mutableStateOf("업로드 준비 중...") }
    var progressRampJob by remember { mutableStateOf<Job?>(null) }

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
                analysisProgress = 0f
                analysisStatusMessage = "영상을 클라우드로 전송 중..."
                uploader.uploadVideo(
                    fileUri = uri,
                    filename = filename,
                    onUploadProgress = { uploadProgress ->
                        val cappedUploadProgress = (uploadProgress * 0.2f).coerceIn(0f, 0.2f)
                        if (analysisProgress < 0.2f) {
                            analysisProgress = cappedUploadProgress.coerceAtLeast(analysisProgress)
                        }
                        analysisStatusMessage = "영상을 클라우드로 전송 중... ${(uploadProgress * 100).toInt()}%"
                    },
                    onComplete = { s3Key ->
                        Log.d("SongPartSelect", "Upload Success Key: $s3Key")
                        Toast.makeText(context, "업로드 완료! 분석을 기다립니다...", Toast.LENGTH_SHORT).show()
                        analysisProgress = 0.2f
                        analysisStatusMessage = "서버에서 AI 분석 중..."

                        progressRampJob?.cancel()
                        progressRampJob = scope.launch {
                            repeat(24) {
                                delay(1000)
                                if (analysisProgress >= 0.92f) return@launch
                                analysisProgress = (analysisProgress + 0.03f).coerceAtMost(0.92f)
                            }
                        }

                        scope.launch {
                            uploader.pollAnalysisResult(
                                userId = userId,
                                timestamp = timestamp,
                                onProgress = { msg ->
                                    analysisStatusMessage = msg
                                },
                                onComplete = { resultS3Key ->
                                    scope.launch {
                                        try {
                                            progressRampJob?.cancel()

                                            while (analysisProgress < 1f) {
                                                analysisProgress = (analysisProgress + 0.04f).coerceAtMost(1f)
                                                delay(60)
                                            }

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

                                            analysisStatusMessage = "분석 완료!"
                                            showAnalysisLoading = false
                                            Toast.makeText(context, "분석 완료!", Toast.LENGTH_SHORT).show()
                                            onNavigateToResult(jsonFileName, uri.toString())
                                        } catch (e: Exception) {
                                            progressRampJob?.cancel()
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
                                    progressRampJob?.cancel()
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
                        progressRampJob?.cancel()
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
            progress = analysisProgress,
            statusMessage = analysisStatusMessage,
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
    // 이미지와 유사한 보라빛~푸른빛 배경 그라데이션
    val pageGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFB9C4FA), // 연한 파스텔 블루
            Color(0xFFE4D9FC)  // 연한 파스텔 퍼플
        )
    )

    val songTitle = currentSong?.titleKr?.ifBlank { currentSong.titleEn }.orEmpty()
    val songArtist = currentSong?.artistKr?.ifBlank { currentSong.artistEn }.orEmpty()
    val totalTimeLabel = if (dbParts.isNotEmpty()) {
        (dbParts.last().endTimeMs).toTimeLabel()
    } else {
        "0:00"
    }
    val animationSpec = remember { PartSelectAnimationSpec() }
    val partAnimationKey = remember(dbParts) { dbParts.joinToString(separator = ",") { it.partId.toString() } }
    var showPartItems by remember(currentSong?.songId) { mutableStateOf(false) }

    LaunchedEffect(currentSong?.songId, partAnimationKey) {
        if (dbParts.isEmpty()) {
            showPartItems = false
            return@LaunchedEffect
        }

        showPartItems = false
        withFrameNanos { }
        showPartItems = true
    }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (currentSong != null) {
                    SongInfoCard(
                        currentSong = currentSong,
                        songTitle = songTitle,
                        songArtist = songArtist,
                        totalTimeLabel = totalTimeLabel,
                        songCardBg = songCardBg,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    )
                }

                Text(
                    text = "연습할 파트를 선택하세요",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF4A4E67),
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (dbParts.isEmpty()) {
                        item { Text("등록된 파트가 없습니다.", modifier = Modifier.padding(16.dp)) }
                    } else {
                        items(dbParts.size, key = { dbParts[it].partId }) { index ->
                            val part = dbParts[index]
                            AnimatedVisibility(
                                visible = showPartItems,
                                enter = partEnterTransition(index = index, animationSpec = animationSpec),
                                exit = ExitTransition.None,
                                label = "partItemAnimation"
                            ) {
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
    }
}

@Composable
private fun SongInfoCard(
    currentSong: Song,
    songTitle: String,
    songArtist: String,
    totalTimeLabel: String,
    songCardBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = songCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val coverUrl = currentSong.coverUrl.orEmpty()
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "$songTitle cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = songArtist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Total Time",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = totalTimeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

private fun partEnterTransition(index: Int, animationSpec: PartSelectAnimationSpec): EnterTransition {
    val delay = (index * animationSpec.itemStaggerDelayMs).coerceAtMost(animationSpec.maxItemDelayMs)
    return fadeIn(
        animationSpec = tween(
            durationMillis = animationSpec.itemFadeDurationMs,
            delayMillis = delay,
            easing = LinearOutSlowInEasing
        )
    ) +
        slideInVertically(
            animationSpec = tween(
                durationMillis = animationSpec.itemSlideDurationMs,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { it / animationSpec.itemOffsetDivisor }
        )
}

@Composable
fun PartCard(
    title: String,
    time: String,
    onPracticeClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 왼쪽: 제목 및 시간
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = time,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // 오른쪽: 버튼들 (세로 배치로 깔끔하게)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPracticeClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Practice",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("연습", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onUploadClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF64748B) // 약간 더 연한 텍스트
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text("동영상 업로드", fontSize = 12.sp)
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
        titleKr = "Dynamite",
        titleEn = "Dynamite",
        artistKr = "BTS",
        artistEn = "BTS",
        artistGender = "Male",
        genre = "Dance",
        tempo = "Fast",
        difficulty = "Normal",
        coverUrl = null,
        releaseDate = "2020-08-21"
    )
    val sampleParts = listOf(
        SongPart(1L, 1L, 1, "인트로", 30, null, null, 0L, 30000L),
        SongPart(2L, 1L, 2, "메인 파트", 45, null, null, 30000L, 75000L),
        SongPart(3L, 1L, 3, "브릿지", 30, null, null, 75000L, 105000L)
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
