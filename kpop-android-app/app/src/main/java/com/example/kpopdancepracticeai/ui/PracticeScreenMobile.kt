package com.example.kpopdancepracticeai.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

private val ColorVideoOverlay = Color(0x4D000000)
private val ColorControlInactive = Color(0x1AFFFFFF)
private val ColorControlBorder = Color(0x4DFFFFFF)
private val ColorProgressBarTrack = Color(0xFFECECF0)
private val ColorProgressBarFill = Color(0xFF030213)
private val ColorTimeText = Color(0xFF99A1AF)
private val ColorSpeedTextInactive = Color(0xFFD1D5DC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreenMobile(
    songId: Long = 0L,
    songTitle: String,
    artistPart: String,
    difficulty: String,
    length: String,
    videoUrl: String = "",
    onBackClick: () -> Unit = {},
    onRecordClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableStateOf(1.0f) }

    LaunchedEffect(settings) {
        selectedSpeed = 1.0f
    }
    var areControlsVisible by remember { mutableStateOf(true) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }

    // 💡 [수정] ExoPlayer 인스턴스 생성 및 에러 리스너 추가
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(context, "영상 재생 에러: ${error.message}\n경로를 확인하세요.", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // URL이 변경될 때마다 플레이어 재설정
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            // "asset:///" 경로 보장
            val finalUrl = if (!videoUrl.startsWith("asset:///")) {
                videoUrl.replace("file:///android_asset/", "asset:///")
            } else videoUrl

            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(finalUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                isPlaying = true
            } catch (e: Exception) {
                Log.e("PracticeScreen", "로드 실패", e)
            }
        } else {
            Toast.makeText(context, "오류: 영상 주소가 비어있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(selectedSpeed) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // 진행 바 업데이트 루프
    LaunchedEffect(Unit) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            totalDurationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            delay(100)
        }
    }

    val formatTime = { ms: Long ->
        val totalSeconds = ms / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        String.format("%d:%02d", m, s)
    }

    val currentTimeStr = formatTime(currentPositionMs)
    val totalTimeStr = if (totalDurationMs > 0) formatTime(totalDurationMs) else length
    val sliderPosition = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.player != exoPlayer) view.player = exoPlayer
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                            .clickable(onClick = { isPlaying = !isPlaying }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                            contentDescription = "재생",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("댄스 튜토리얼 영상", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                }
            }
        }

        // 상단바
        AnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundIconButton(icon = Icons.Default.CameraAlt, onClick = { /* TODO */ })
                    RoundIconButton(
                        icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        onClick = { isMuted = !isMuted }
                    )
                    RoundIconButton(icon = Icons.Default.Settings, onClick = onSettingsClick)
                }
            }
        }

        // 하단바
        AnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorVideoOverlay)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SongInfoBar(title = songTitle, artistPart = artistPart, difficulty = difficulty)

                PlaybackSlider(
                    currentPosition = sliderPosition,
                    currentTime = currentTimeStr,
                    totalTime = totalTimeStr,
                    onPositionChange = { newPos ->
                        val targetMs = (newPos * totalDurationMs).toLong()
                        exoPlayer.seekTo(targetMs)
                    }
                )

                SpeedControlRow(selectedSpeed = selectedSpeed, onSpeedSelected = { selectedSpeed = it })

                ActionButtons(
                    isPlaying = isPlaying,
                    onRefreshClick = { exoPlayer.seekTo(0); isPlaying = true },
                    onPlayPauseClick = { isPlaying = !isPlaying },
                    onFollowClick = { isPlaying = false; onRecordClick() }
                )
            }
        }
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit, size: Dp = 36.dp, iconSize: Dp = 16.dp, backgroundColor: Color = Color.White.copy(alpha = 0.2f), tint: Color = Color.White) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(backgroundColor).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = tint)
    }
}

@Composable
fun SongInfoBar(title: String, artistPart: String, difficulty: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp), color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(text = "· $artistPart", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
        DifficultyBadge(difficulty = difficulty)
    }
}

@Composable
private fun DifficultyBadge(difficulty: String) {
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33F0B100)).border(1.dp, Color(0x80F0B100), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
        Text(text = difficulty, color = Color(0xFFFFDF20), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp))
    }
}

@Composable
fun PlaybackSlider(currentPosition: Float, currentTime: String, totalTime: String, onPositionChange: (Float) -> Unit) {
    Column {
        Slider(value = currentPosition, onValueChange = onPositionChange, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.Gray))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = currentTime, color = Color.White, fontSize = 12.sp)
            Text(text = totalTime, color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun SpeedControlRow(selectedSpeed: Float, onSpeedSelected: (Float) -> Unit) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "속도:", color = Color.White, fontSize = 12.sp)
        speeds.forEach { speed ->
            val isSelected = selectedSpeed == speed
            Box(modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(4.dp)).background(if (isSelected) Color(0xFF9810FA) else Color.White.copy(alpha = 0.1f)).clickable { onSpeedSelected(speed) }, contentAlignment = Alignment.Center) {
                Text(text = "${speed}x", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ActionButtons(isPlaying: Boolean, onRefreshClick: () -> Unit, onPlayPauseClick: () -> Unit, onFollowClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick = onRefreshClick, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, Color.White), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
            Icon(Icons.Default.Refresh, contentDescription = null); Text("처음부터")
        }
        Button(onClick = onPlayPauseClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
            Icon(if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow, contentDescription = null); Text(if (isPlaying) "일시정지" else "재생")
        }
        Button(onClick = onFollowClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null); Text("따라하기")
        }
    }
}
