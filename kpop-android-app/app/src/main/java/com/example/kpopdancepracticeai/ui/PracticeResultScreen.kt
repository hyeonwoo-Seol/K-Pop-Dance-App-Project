package com.example.kpopdancepracticeai.ui

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.kpopdancepracticeai.data.dto.FrameData
import com.example.kpopdancepracticeai.util.DataConverter
import com.example.kpopdancepracticeai.util.JsonResultLoader
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
fun PracticeResultScreen(
    jsonFileName: String = "",
    videoPath: String = "",
    score: Int = 0,
    viewModel: MainViewModel? = null,
    onBackClick: () -> Unit = {},
    onReplayClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // --- 1. 상태 선언 ---
    var showOverlay by remember { mutableStateOf(false) }
    var allFrames by remember { mutableStateOf<List<FrameData>>(emptyList()) }
    var videoWidth by remember { mutableIntStateOf(1) }
    var videoHeight by remember { mutableIntStateOf(1) }

    // UI 데이터 상태
    var totalScore by remember { mutableIntStateOf(if (isPreview) 87 else score) }
    var accuracyGrade by remember { mutableStateOf(if (isPreview) "B" else "-") }
    var worstJoints by remember { mutableStateOf<List<String>>(if (isPreview) listOf("왼쪽 어깨", "오른쪽 팔꿈치", "왼쪽 무릎") else emptyList()) }

    // 신체 부위별 점수 (순서: 0.몸통, 1.오른팔, 2.오른다리, 3.왼다리, 4.왼팔)
    var partScores by remember { mutableStateOf(if (isPreview) listOf(90f, 85f, 75f, 88f, 82f) else listOf(0f, 0f, 0f, 0f, 0f)) }

    var currentKeyPoints by remember { mutableStateOf<List<KeyPoint>>(emptyList()) }
    var currentErrors by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isPlaying by remember { mutableStateOf(false) }

    // --- 2. 플레이어 및 데이터 로드 ---
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            try {
                exoPlayer = ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF }
                exoPlayer?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingState: Boolean) { isPlaying = isPlayingState }
                })
            } catch (e: Exception) {
                Log.e("PracticeResult", "ExoPlayer 초기화 실패", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer?.release() }
    }

    // JSON 파싱 및 신체 부위 점수(part_accuracies) 연동
    LaunchedEffect(jsonFileName) {
        if (jsonFileName.isNotBlank()) {
            try {
                val result = JsonResultLoader.loadAnalysisResult(context, jsonFileName)
                if (result != null) {
                    allFrames = result.frames.sortedBy { it.timestamp }
                    videoWidth = result.metadata.videoWidth.coerceAtLeast(1)
                    videoHeight = result.metadata.videoHeight.coerceAtLeast(1)
                    totalScore = result.summary.totalScore
                    accuracyGrade = result.summary.accuracyGrade

                    // JSON의 part_accuracies 데이터를 안전하게 가져옵니다.
                    val pAcc = result.summary.partAccuracies
                    val base = totalScore.toFloat() // 데이터 누락 시 기본값 방어

                    val torso = pAcc?.get("Torso")?.toFloat() ?: base
                    val rightArm = pAcc?.get("Right Arm")?.toFloat() ?: base
                    val rightLeg = pAcc?.get("Right Leg")?.toFloat() ?: base
                    val leftLeg = pAcc?.get("Left Leg")?.toFloat() ?: base
                    val leftArm = pAcc?.get("Left Arm")?.toFloat() ?: base

                    // 오각형을 그릴 순서: 위(몸통) -> 우상(오른팔) -> 우하(오른다리) -> 좌하(왼다리) -> 좌상(왼팔)
                    partScores = listOf(torso, rightArm, rightLeg, leftLeg, leftArm)
                        .map { it.coerceIn(0f, 100f) }

                    val joints = analyzeTop3WorstJoints(allFrames)
                    if (joints.isNotEmpty()) worstJoints = joints
                }
            } catch (e: Exception) {
                Log.e("PracticeResult", "JSON 파싱 에러", e)
            }
        }
    }

    // 비디오 로드
    LaunchedEffect(videoPath, exoPlayer) {
        if (videoPath.isNotBlank() && exoPlayer != null) {
            try {
                val uri = Uri.parse(videoPath)
                val mediaItem = if (videoPath.startsWith("/")) MediaItem.fromUri(Uri.fromFile(File(videoPath))) else MediaItem.fromUri(uri)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.playWhenReady = false
            } catch (e: Exception) {
                Log.e("PracticeResult", "비디오 로드 에러", e)
            }
        }
    }

    // 오버레이 싱크
    LaunchedEffect(exoPlayer, isPlaying, allFrames) {
        if (allFrames.isNotEmpty() && exoPlayer != null) {
            while (isActive) {
                if (isPlaying) {
                    val currentSec = (exoPlayer?.currentPosition ?: 0L) / 1000.0
                    val targetFrame = allFrames.minByOrNull { abs(it.timestamp - currentSec) }
                    if (targetFrame != null && abs(targetFrame.timestamp - currentSec) < 0.1) {
                        currentKeyPoints = DataConverter.convertToKeyPoints(targetFrame)
                        currentErrors = targetFrame.errors
                    } else {
                        currentKeyPoints = emptyList()
                    }
                }
                delay(33)
            }
        }
    }

    // --- 3. UI 렌더링 ---
    if (showOverlay && exoPlayer != null) {
        // [오버레이 영상 렌더링 화면]
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } },
                modifier = Modifier.fillMaxSize()
            )
            if (currentKeyPoints.isNotEmpty()) {
                SkeletonOverlay(
                    keyPoints = currentKeyPoints,
                    errors = currentErrors,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = { showOverlay = false; exoPlayer?.pause() },
                modifier = Modifier.padding(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }
        }
    } else {
        // [결과 통계 UI 화면]
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!isPreview) {
                            showOverlay = true
                            exoPlayer?.seekTo(0)
                            exoPlayer?.play()
                        }
                    },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "재생") },
                    text = { Text("오버레이 보기", fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xff9810fa),
                    contentColor = Color.White
                )
            },
            // ⭐️ 다른 UI와 통일성을 맞추기 위해 MaterialTheme의 background 속성을 사용합니다.
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "연습 결과",
                        modifier = Modifier.weight(1f),
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // 등급 및 점수
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResultCard(modifier = Modifier.weight(1f), bgColor = Color(0xfffefce8), borderColor = Color(0xfffff085)) {
                        Text(text = if (totalScore == 0 && !isPreview) "-" else accuracyGrade, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 56.sp), color = Color(0xffd08700))
                        Text("평가 등급", style = TextStyle(fontSize = 14.sp, color = Color(0xff717182)))
                    }
                    ResultCard(modifier = Modifier.weight(1f), borderColor = Color(0xffe9d4ff)) {
                        val displayScore = if (totalScore == 0 && !isPreview) 0 else totalScore
                        Text(text = "${displayScore}%", style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 42.sp), color = Color(0xff9810fa))
                        Text("전체 정확도", style = TextStyle(fontSize = 14.sp, color = Color(0xff717182)))
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0x33030213), CircleShape).clip(CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(displayScore / 100f).fillMaxHeight().background(Color(0xff030213)))
                        }
                    }
                }

                // 종합 통계
                ResultCard(borderColor = Color.LightGray) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("종합 통계", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black))
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PentagonRadarChart(scores = partScores, modifier = Modifier.size(200.dp))
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("왼팔", "${partScores[4].toInt()}점", Color(0xff9810fa))
                            StatItem("오른팔", "${partScores[1].toInt()}점", Color(0xff9810fa))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("왼다리", "${partScores[3].toInt()}점", Color(0xff9810fa))
                            StatItem("오른다리", "${partScores[2].toInt()}점", Color(0xff9810fa))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            StatItem("몸통", "${partScores[0].toInt()}점", Color(0xff9810fa))
                        }
                    }
                }

                // 많이 틀린 관절
                ResultCard(borderColor = Color.LightGray) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("많이 틀린 관절 TOP 3", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black))
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    val colors = listOf(Color(0xfffb2c36), Color(0xffff6900), Color(0xfff0b100))
                    val dJoints = if (worstJoints.isEmpty()) listOf("데이터 없음", "데이터 없음", "데이터 없음") else worstJoints

                    for (i in 0..2) {
                        TopErrorJointItem(i + 1, dJoints.getOrNull(i) ?: "데이터 없음", colors[i])
                        if (i < 2) Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 업적 진행도
                ResultCard(borderColor = Color.LightGray) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("업적 진행도", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black))
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    AchievementItem("🏃‍♀️", "첫 완주", "100%", true, 1.0f)
                    Spacer(modifier = Modifier.height(24.dp))
                    AchievementItem("🎯", "정확도 마스터", "75%", false, 0.75f)
                    Spacer(modifier = Modifier.height(24.dp))
                    AchievementItem("💪", "연습벌레", "60%", false, 0.60f)
                }

                // 최고 기록
                ResultCard(borderColor = Color(0xfffff085), bgColor = Color.White) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).background(Color(0xfff0b100), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("이 곡 최고 기록", style = TextStyle(fontSize = 14.sp, color = Color(0xff717182)))
                                Text("Get Up - NewJeans", style = TextStyle(fontSize = 12.sp, color = Color(0xff717182)))
                            }
                        }
                        Text("92.3%", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xffd08700)))
                    }
                }

                // 하단 버튼
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = onReplayClick,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) {
                        Text("다시 연습", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { /* 결과 공유 로직 */ },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                    ) {
                        Text("결과 공유", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}


@Composable
fun ResultCard(
    modifier: Modifier = Modifier,
    bgColor: Color = Color.White,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun StatItem(title: String, score: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text("$title: $score", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xff717182)))
    }
}

@Composable
fun TopErrorJointItem(rank: Int, jointName: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xfffef2f2), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xffffc9c9), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(jointName, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black))
    }
}

@Composable
fun AchievementItem(icon: String, title: String, percentStr: String, isCompleted: Boolean, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black))
            }
            Text(percentStr, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray))
        }
        if (isCompleted) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.background(Color(0xfff0b100), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("달성 완료!", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(10.dp).background(Color(0x33030213), CircleShape).clip(CircleShape)) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color(0xff030213)))
        }
    }
}

@Composable
fun PentagonRadarChart(scores: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val numPoints = 5
        val maxScore = 100f

        // 기준선 (거미줄 모양)
        for (step in 1..5) {
            val stepRadius = radius * (step / 5f)
            val path = Path()
            for (i in 0 until numPoints) {
                val angle = -PI / 2.0 + (2.0 * PI * i / numPoints)
                val x = center.x + (stepRadius * cos(angle)).toFloat()
                val y = center.y + (stepRadius * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = Color.LightGray.copy(alpha = 0.5f), style = Stroke(width = 2f))
        }

        // 중심 축
        for (i in 0 until numPoints) {
            val angle = -PI / 2.0 + (2.0 * PI * i / numPoints)
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = center, end = Offset(x, y), strokeWidth = 2f)
        }

        // 실제 점수 면적 칠하기
        val dataPath = Path()
        for (i in 0 until numPoints) {
            val score = scores.getOrElse(i) { 0f }.coerceIn(0f, maxScore)
            val scoreRadius = radius * (score / maxScore)
            val angle = -PI / 2.0 + (2.0 * PI * i / numPoints)
            val x = center.x + (scoreRadius * cos(angle)).toFloat()
            val y = center.y + (scoreRadius * sin(angle)).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(path = dataPath, color = Color(0xff9810fa).copy(alpha = 0.3f))
        drawPath(path = dataPath, color = Color(0xff9810fa), style = Stroke(width = 5f))
    }
}

fun analyzeTop3WorstJoints(frames: List<FrameData>): List<String> {
    if (frames.isEmpty()) return emptyList()
    val errorCounts = mutableMapOf<Int, Int>()
    frames.forEach { frame ->
        frame.errors.forEachIndexed { index, isError ->
            if (isError == 1) errorCounts[index] = (errorCounts[index] ?: 0) + 1
        }
    }
    val majorJoints = (5..16).toList()
    return errorCounts.filterKeys { it in majorJoints }
        .entries.sortedByDescending { it.value }
        .take(3)
        .map { getJointNameKorean(it.key) }
}

fun getJointNameKorean(index: Int): String {
    return when(index) {
        5 -> "왼쪽 어깨"; 6 -> "오른쪽 어깨"
        7 -> "왼쪽 팔꿈치"; 8 -> "오른쪽 팔꿈치"
        9 -> "왼쪽 손목"; 10 -> "오른쪽 손목"
        11 -> "왼쪽 골반"; 12 -> "오른쪽 골반"
        13 -> "왼쪽 무릎"; 14 -> "오른쪽 무릎"
        15 -> "왼쪽 발목"; 16 -> "오른쪽 발목"
        else -> "관절"
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PracticeResultScreenPreview() {
    PracticeResultScreen(
        jsonFileName = "",
        videoPath = "",
        score = 87
    )
}
