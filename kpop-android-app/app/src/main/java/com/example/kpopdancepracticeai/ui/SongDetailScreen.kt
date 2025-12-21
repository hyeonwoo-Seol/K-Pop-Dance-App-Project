package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import kotlinx.coroutines.delay

// --- 1. UI 상태 및 데이터 클래스 정의 ---

// AWS에서 가져올 곡 정보 (캐시 대상)
data class SongInfo(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val level: String,
    val time: String,
    val parts: String,
    val practiceCount: String,
    val likes: String
)

// AWS에서 가져올 사용자 분석 (매번 로드)
data class UserAnalysis(
    val averageAccuracy: Float,
    val totalPracticeCount: Int,
    // (타임스탬프 문자열, 오차 값)
    val timeSeriesError: List<Pair<String, Int>>,
    val majorMistakeSection: String,
    // (관절 이름, 오차 값)
    val jointErrors: List<Pair<String, Float>>,
    // (제안 제목, 제안 내용)
    val suggestions: List<Pair<String, String>>
)

// 화면의 UI 상태
sealed class SongDetailUiState {
    object Loading : SongDetailUiState()
    data class Success(
        val songInfo: SongInfo,
        val userAnalysis: UserAnalysis? // 분석 결과는 없을 수도 있음
    ) : SongDetailUiState()
    data class Error(val message: String) : SongDetailUiState()
}

// --- 2. 임시 더미 데이터 (image_ec1ee3.png 기반) ---

val dummySongInfo = SongInfo(
    id = "getup_newjeans_01",
    title = "Get Up",
    artist = "NewJeans",
    albumArtUrl = "https://placehold.co/300x300/6060FF/FFFFFF?text=NewJeans",
    level = "Medium",
    time = "2:48",
    parts = "4개 파트",
    practiceCount = "125.0K 연습",
    likes = "8.9K 좋아요"
)

val dummyUserAnalysis = UserAnalysis(
    averageAccuracy = 82.7f,
    totalPracticeCount = 5,
    timeSeriesError = listOf(
        "0:00" to 18, "0:18" to 15, "0:45" to 27, "1:12" to 36, "1:39" to 22,
        "1:59" to 18, "2:06" to 30, "2:33" to 24, "3:00" to 12
    ),
    majorMistakeSection = "1:03 - 1:15 (코러스 시작 부분)에서 가장 큰 오차 발생",
    jointErrors = listOf(
        "왼쪽 어깨" to 23.5f,
        "오른쪽 팔꿈치" to 18.2f,
        "왼쪽 무릎" to 15.8f,
        "오른쪽 어깨" to 12.3f,
        "오른쪽 무릎" to 8.7f
    ),
    suggestions = listOf(
        "어깨 동작 연습" to "왼쪽 어깨의 오차가 큽니다. 거울을 보며 어깨 높이를 맞춰 연습해보세요.",
        "코러스 구간 집중 연습" to "1:03 - 1:15 구간을 반복 연습하여 정확도를 높여보세요.",
        "속도 조절 연습" to "느린 속도부터 시작하여 점진적으로 원래 속도까지 올려보세요."
    )
)


// --- 3. 메인 Composable ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    songId: String,
    navController: NavHostController,
    onBackClick: () -> Unit
) {
    // --- ViewModel 로직 (임시) ---
    var uiState by remember { mutableStateOf<SongDetailUiState>(SongDetailUiState.Loading) }

    LaunchedEffect(songId) {
        uiState = SongDetailUiState.Loading
        delay(1000) // 1초간의 가짜 AWS 로딩

        // 시뮬레이션: 더미 데이터로 UI 상태 업데이트
        uiState = SongDetailUiState.Success(dummySongInfo, dummyUserAnalysis)
    }
    // --- ViewModel 로직 끝 ---

    Scaffold(
        containerColor = Color.Transparent, // AppNavigation의 그라데이션 배경이 보이도록
        topBar = {
            TopAppBar(
                title = { /* 제목 없음 */ },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White // 배경이 어두우므로 흰색 아이콘
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // 그라데이션 배경이 보이도록
                )
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // TopAppBar 영역만큼 패딩
        ) {
            when (val state = uiState) {
                is SongDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SongDetailUiState.Success -> {
                    // 로드 성공
                    SongDetailContent(
                        songInfo = state.songInfo,
                        userAnalysis = state.userAnalysis,
                        navController = navController
                    )
                }
                is SongDetailUiState.Error -> {
                    // 오류 발생
                    Text(
                        text = "오류 발생: ${state.message}",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3-1. 데이터 로드 성공 시 표시될 본문
 */
@Composable
fun SongDetailContent(
    songInfo: SongInfo,
    userAnalysis: UserAnalysis?,
    navController: NavHostController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // contentPadding을 설정하지 않아 최상단까지 콘텐츠가 올라감
    ) {
        // --- 1. 곡 정보 헤더 ---
        item {
            SongDetailHeader(songInfo = songInfo)
        }

        // --- 2. 연습 시작 버튼 ---
        item {
            Button(
                onClick = {
                    // ⭐️ [수정] 클릭 시 곡 파트 선택 화면으로 이동
                    navController.navigate("songPartSelect/${songInfo.id}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "연습 시작하기",
                    style = TextStyle(
                        fontWeight = FontWeight(700),
                        fontSize = 18.sp
                    )
                )
            }
        }

        // --- 3. "분석" 배너 ---
        item {
            AnalysisBanner()
        }

        // --- 4. 분석 결과 (흰색 배경 카드) ---
        item {
            AnalysisContent(userAnalysis = userAnalysis)
        }
    }
}

/**
 * 1-1. 곡 정보 헤더
 */
@Composable
fun SongDetailHeader(songInfo: SongInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 앨범 아트 및 기본 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(songInfo.albumArtUrl),
                contentDescription = songInfo.title,
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = songInfo.title,
                    style = TextStyle(
                        fontWeight = FontWeight(700),
                        fontSize = 24.sp,
                        lineHeight = 32.sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = songInfo.artist,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    ),
                    color = Color(0xff717182)
                )

                // 태그 (Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = songInfo.level,
                        modifier = Modifier
                            .background(Color(0xfff0b100), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        color = Color.White,
                        style = TextStyle(
                            fontWeight = FontWeight(400),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    )
                }
            }
        }

        // 상세 스탯 (시간, 파트, 연습, 좋아요)
        // 2x2 Grid 사용
        Row(Modifier.fillMaxWidth()) {
            SongStatItem(icon = Icons.Default.AccessTime, text = songInfo.time, modifier = Modifier.weight(1f))
            SongStatItem(icon = Icons.Default.People, text = songInfo.parts, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            SongStatItem(icon = Icons.Default.PlayArrow, text = songInfo.practiceCount, modifier = Modifier.weight(1f))
            SongStatItem(icon = Icons.Default.FavoriteBorder, text = songInfo.likes, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 1-2. 곡 스탯 아이템 (재사용)
 */
@Composable
fun SongStatItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp),
            tint = Color(0xff717182)
        )
        Text(
            text = text,
            style = TextStyle(
                fontWeight = FontWeight(400),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            color = Color(0xff717182)
        )
    }
}

/**
 * 3-1. "분석" 배너
 */
@Composable
fun AnalysisBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6366F1)) // 스니펫의 색상과 유사
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                text = "분석",
                style = TextStyle(
                    fontWeight = FontWeight(700),
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                ),
                color = Color.White
            )
            Text(
                text = "춤 실력을 분석하고 개선점을 확인하세요",
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
                color = Color(0xffdbeafe) // 연한 파란색
            )
        }
    }
}

/**
 * 4-1. 분석 결과 콘텐츠 (흰색 배경)
 */
@Composable
fun AnalysisContent(userAnalysis: UserAnalysis?) {
    // 상단이 둥근 흰색 배경
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        // 분석 데이터가 없는 경우 (연습 기록 없음)
        if (userAnalysis == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 연습 기록이 없습니다.\n첫 연습을 시작하고 분석 리포트를 받아보세요!",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Surface
        }

        // 분석 데이터가 있는 경우
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 평균 정확도 / 총 연습 횟수
            StatCardsRow(
                accuracy = userAnalysis.averageAccuracy,
                count = userAnalysis.totalPracticeCount
            )

            // 시간대별 오차 분석
            TimeSeriesCard(
                timeSeriesData = userAnalysis.timeSeriesError,
                mistakeInfo = userAnalysis.majorMistakeSection
            )

            // 관절별 오차 분석
            JointErrorCard(jointErrors = userAnalysis.jointErrors)

            // 개선 제안
            SuggestionCard(suggestions = userAnalysis.suggestions)

            Spacer(modifier = Modifier.height(16.dp)) // 하단 여백
        }
    }
}

/**
 * 4-2. 평균 정확도 / 총 연습 횟수 카드
 */
@Composable
fun StatCardsRow(accuracy: Float, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 평균 정확도 카드
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xffd6deff))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "평균 정확도",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xff717182)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${accuracy}%",
                    style = TextStyle(
                        fontWeight = FontWeight(700),
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    ),
                    color = Color(0xff155dfc) // 파란색
                )
            }
        }
        // 총 연습 횟수 카드
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xffd6deff))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "총 연습 횟수",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xff717182)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${count}회",
                    style = TextStyle(
                        fontWeight = FontWeight(700),
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    ),
                    color = Color(0xffd08700) // 노란색
                )
            }
        }
    }
}

/**
 * 4-3. 시간대별 오차 분석 카드
 */
@Composable
fun TimeSeriesCard(timeSeriesData: List<Pair<String, Int>>, mistakeInfo: String) {
    AnalysisCardBase(title = "시간대별 오차 분석") {
        // TODO: 실제 차트 라이브러리(MPAndroidChart, Vico 등)로 교체 필요
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "시간대별 오차 그래프 (Placeholder)", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 주요 실수 구간
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xfffef2f2), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xffffc9c9), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "주요 실수 구간",
                tint = Color(0xff9f0712)
            )
            Column {
                Text(
                    text = "주요 실수 구간",
                    style = TextStyle(
                        fontWeight = FontWeight(700),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color(0xff9f0712)
                )
                Text(
                    text = mistakeInfo,
                    style = TextStyle(
                        fontWeight = FontWeight(400),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color(0xffc10007)
                )
            }
        }
    }
}

/**
 * 4-4. 관절별 오차 분석 카드
 */
@Composable
fun JointErrorCard(jointErrors: List<Pair<String, Float>>) {
    // 스니펫을 보면 가장 큰 오차(23.5)를 기준으로 progress bar의 최대값을 정하는 것으로 보입니다.
    val maxError = jointErrors.maxOfOrNull { it.second } ?: 1f

    AnalysisCardBase(title = "관절별 오차 분석") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            jointErrors.forEach { (joint, error) ->
                JointErrorBar(
                    label = joint,
                    value = error,
                    maxValue = maxError
                )
            }
        }
    }
}

/**
 * 4-4-1. 관절별 오차 프로그레스 바 (재사용)
 */
@Composable
fun JointErrorBar(label: String, value: Float, maxValue: Float) {
    val fraction = value / maxValue

    // 스니펫의 색상 스키마 (빨강 > 주황 > 초록)
    val barColor = when {
        fraction > 0.75 -> Color(0xfffb2c36) // 빨강
        fraction > 0.5 -> Color(0xfff0b100) // 주황
        else -> Color(0xff00c950) // 초록
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                ),
                color = Color(0xff0a0a0a)
            )
            Text(
                text = "${value}°",
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                ),
                color = Color(0xff717182)
            )
        }
        // 프로그레스 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xffe5e7eb)) // 트랙 색상
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor) // 오차 값 색상
            )
        }
    }
}

/**
 * 4-5. 개선 제안 카드
 */
@Composable
fun SuggestionCard(suggestions: List<Pair<String, String>>) {
    AnalysisCardBase(title = "개선 제안") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 스니펫의 배경색을 순서대로 적용
            val colors = listOf(
                Color(0xffeff6ff) to Color(0xffbedbff), // 파랑
                Color(0xfff0fdf4) to Color(0xffb9f8cf), // 초록
                Color(0xfffaf5ff) to Color(0xffe9d4ff)  // 보라
            )

            suggestions.forEachIndexed { index, (title, description) ->
                val (bgColor, borderColor) = colors[index % colors.size]
                SuggestionItem(
                    title = title,
                    description = description,
                    backgroundColor = bgColor,
                    borderColor = borderColor
                )
            }
        }
    }
}

/**
 * 4-5-1. 개선 제안 항목 (재사용)
 */
@Composable
fun SuggestionItem(
    title: String,
    description: String,
    backgroundColor: Color,
    borderColor: Color
) {
    val textColor = when (backgroundColor) {
        Color(0xffeff6ff) -> Color(0xff193cb8)
        Color(0xfff0fdf4) -> Color(0xff016630)
        Color(0xfffaf5ff) -> Color(0xff6e11b0)
        else -> Color.Black
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontWeight = FontWeight(700),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            ),
            color = textColor
        )
        Text(
            text = description,
            style = TextStyle(
                fontWeight = FontWeight(400),
                fontSize = 11.sp,
                lineHeight = 14.sp,
            ),
            color = textColor
        )
    }
}

/**
 * 4-x. 분석 카드 기본 템플릿 (재사용)
 */
@Composable
fun AnalysisCardBase(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xffdbdffe)) // 연한 보라색 테두리
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                ),
                color = Color(0xff0a0a0a)
            )
            content()
        }
    }
}


// --- 5. 미리보기 ---

@Preview(showBackground = true)
@Composable
fun SongDetailScreenPreview() {
    KpopDancePracticeAITheme {
        // 프리뷰에서는 그라데이션 배경을 직접 적용
        Box(modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFFDDE3FF), Color(0xFFF0E8FF))
            )
        )) {
            SongDetailScreen(
                songId = "preview_song_id",
                navController = rememberNavController(),
                onBackClick = {}
            )
        }
    }
}