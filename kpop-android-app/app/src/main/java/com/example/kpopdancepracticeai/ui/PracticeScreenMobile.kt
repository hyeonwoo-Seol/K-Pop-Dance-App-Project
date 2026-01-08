package com.example.kpopdancepracticeai.ui

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
import androidx.compose.material.icons.filled.VolumeUp // ⚠️ [경고] 그대로 유지
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp // ⭐️ [오류 수정] Dp 임포트 추가
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

// --- 1. 색상 및 상수 정의 ---

private val ColorVideoOverlay = Color(0x4D000000) // 비디오 오버레이 (검정 30%)
private val ColorControlInactive = Color(0x1AFFFFFF) // 비활성화된 컨트롤 배경 (흰색 10%)
private val ColorControlBorder = Color(0x4DFFFFFF) // 비활성화된 컨트롤 테두리 (흰색 30%)
private val ColorProgressBarTrack = Color(0xFFECECF0) // 슬라이더 트랙
private val ColorProgressBarFill = Color(0xFF030213) // 슬라이더 채움
private val ColorTimeText = Color(0xFF99A1AF) // 시간 텍스트
private val ColorSpeedTextInactive = Color(0xFFD1D5DC) // 비활성화된 속도 텍스트

// --- 2. 메인 Composable: 연습 화면 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreenMobile(
    songTitle: String, //  [추가] 곡 제목
    artistPart: String, //  [추가] 아티스트 및 파트 정보
    difficulty: String, //  [추가] 난이도
    length: String, //  [추가] 곡/파트 길이
    onBackClick: () -> Unit = {},
    //  [추가] 녹화 화면 이동을 위한 콜백 파라미터 추가
    onRecordClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    // 상태 관리 (경고 사항은 수정하지 않음)
    var currentPosition by remember { mutableStateOf(0.1f) } // ⚠️ [경고] 그대로 유지
    var isPlaying by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableStateOf(1.0f) } // ⚠️ [경고] 그대로 유지

    // 컨트롤 패널 가시성 상태 관리
    var areControlsVisible by remember { mutableStateOf(true) }
    // 비디오 영역 전체를 차지하는 Box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // 비디오가 없는 기본 배경색
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 터치 시 물결 효과 제거
            ) {
                areControlsVisible = !areControlsVisible
            }
    ) {

        // 중앙 비디오 영역 (임시: 보라색 그라데이션)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF673AB7).copy(alpha = 0.8f), Color(0xFF3F51B5).copy(alpha = 0.9f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // 중앙의 재생 아이콘 및 안내 텍스트 (UI 스니펫의 '댄스 튜토리얼 영상' 영역)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 중앙 재생 버튼 (흰색 원형)
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
                            contentDescription = "재생/일시정지",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "댄스 튜토리얼 영상",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "세로 화면",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // --- 상단 컨트롤 바 ---
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
                // 뒤로가기 버튼
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color.White)
                }

                // 우측 아이콘 그룹 (미러링, 볼륨, 설정)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 미러링 버튼 (아이콘 임시 대체)
                    RoundIconButton(icon = Icons.Default.CameraAlt, onClick = { /* TODO */ })
                    // 볼륨 버튼 (경고 사항은 수정하지 않음)
                    RoundIconButton(icon = Icons.Default.VolumeUp, onClick = { /* TODO */ })
                    // 설정 버튼
                    RoundIconButton(icon = Icons.Default.Settings, onClick = onSettingsClick)
                }
            }
        }

        // --- 하단 제어판 영역 ---
        AnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(ColorVideoOverlay) // 하단 제어판 영역의 투명한 검정 오버레이
                    .navigationBarsPadding() // 하단 네비게이션 바 영역 확보
                    .padding(horizontal = 16.dp, vertical = 24.dp), // 내부 패딩
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 곡 정보
                SongInfoBar(
                    title = songTitle, // ⭐️ [수정] 인자 사용
                    artistPart = artistPart, // ⭐️ [수정] 인자 사용
                    difficulty = difficulty // ⭐️ [수정] 인자 사용
                )

                // 2. 재생 슬라이더 및 시간
                PlaybackSlider(
                    currentPosition = currentPosition,
                    totalTime = length, // ⭐️ [수정] 인자 사용
                    onPositionChange = { newPosition -> currentPosition = newPosition }
                )

                // 3. 속도 조절 버튼 (0.5x, 0.75x, 1x, 1.25x, 1.5x)
                SpeedControlRow(
                    selectedSpeed = selectedSpeed,
                    onSpeedSelected = { selectedSpeed = it }
                )

                // 4. 액션 버튼 (처음부터, 재생/일시정지, 따라하기)
                ActionButtons(
                    isPlaying = isPlaying,
                    onRefreshClick = { currentPosition = 0.0f },
                    onPlayPauseClick = { isPlaying = !isPlaying },
                    onFollowClick = onRecordClick
                )
            }
        }
    }
}

// --- 3. 재사용 가능한 Composable 요소들 ---

/**
 * 상단 우측의 원형 아이콘 버튼
 */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: Dp = 36.dp, // ⭐️ Dp가 임포트되어 오류 해결됨
    iconSize: Dp = 16.dp, // ⭐️ Dp가 임포트되어 오류 해결됨
    backgroundColor: Color = Color.White.copy(alpha = 0.2f), // Figma의 0x33ffffff
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}

/**
 * 곡 정보 표시줄
 */
@Composable
fun SongInfoBar(title: String, artistPart: String, difficulty: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "· $artistPart",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f) // Figma의 0xffd1d5dc와 유사
        )
        // 난이도 배지
        DifficultyBadge(difficulty = difficulty)
    }
}

/**
 * 난이도 배지
 */
@Composable
private fun DifficultyBadge(difficulty: String) {
    val (bgColor, textColor, borderColor) = when (difficulty) {
        "보통" -> Triple(Color(0x33F0B100), Color(0xFFFFDF20), Color(0x80F0B100))
        // 다른 난이도도 여기에 추가 가능
        else -> Triple(Color.Gray.copy(alpha = 0.3f), Color.White, Color.White.copy(alpha = 0.5f))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.817.dp, borderColor, RoundedCornerShape(8.dp)) // Figma의 border 값
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = difficulty,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)
        )
    }
}

/**
 * 재생 슬라이더 및 시간 표시
 */
@Composable
fun PlaybackSlider(
    currentPosition: Float,
    totalTime: String,
    onPositionChange: (Float) -> Unit
) {
    Column {
        // 슬라이더
        Slider(
            value = currentPosition,
            onValueChange = onPositionChange,
            modifier = Modifier.fillMaxWidth().height(16.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = ColorProgressBarFill,
                inactiveTrackColor = ColorProgressBarTrack,
            )
        )
        // 시간 표시
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0:00", // 현재 시간은 currentPosition에 따라 계산해야 하지만, Figma에 따라 "0:00" 고정
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = ColorTimeText
            )
            Text(
                text = totalTime,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = ColorTimeText
            )
        }
    }
}

/**
 * 속도 조절 버튼 행
 */
@Composable
fun SpeedControlRow(
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "속도:" 텍스트
        Text(
            text = "속도:",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = ColorTimeText,
            modifier = Modifier.padding(end = 4.dp)
        )

        // 속도 버튼들
        speeds.forEach { speed ->
            val isSelected = selectedSpeed == speed
            SpeedButton(
                speed = speed,
                isSelected = isSelected,
                onClick = { onSpeedSelected(speed) },
                modifier = Modifier.weight(1f) // ⭐️ [오류 수정] RowScope에서 weight를 적용
            )
        }
    }
}

/**
 * 개별 속도 조절 버튼
 */
@Composable
fun SpeedButton(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // ⭐️ [오류 수정] weight를 받기 위한 Modifier 파라미터 추가
) {
    val text = "${speed}x"

    // Figma의 Box 스타일을 Button의 Modifier로 적용
    Box(
        modifier = modifier // ⭐️ [오류 수정] 전달받은 modifier를 사용
            .height(32.dp) // Figma의 31.98dp에 가까움
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF9810FA) else ColorControlInactive)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = if (isSelected) Color.White else ColorSpeedTextInactive,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 하단 액션 버튼 행
 */
@Composable
fun ActionButtons(
    isPlaying: Boolean,
    onRefreshClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 처음부터 버튼
        ActionOutlinedButton(
            text = "처음부터",
            icon = Icons.Default.Refresh,
            onClick = onRefreshClick,
            modifier = Modifier.weight(1f)
        )

        // 2. 재생/일시정지 버튼
        ActionButton(
            text = if (isPlaying) "일시정지" else "재생",
            icon = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
            onClick = onPlayPauseClick,
            modifier = Modifier.weight(1f),
            containerColor = Color.White,
            contentColor = Color.Black
        )

        // 3. 따라하기 버튼
        ActionButton(
            text = "따라하기",
            icon = Icons.Default.CameraAlt,
            onClick = onFollowClick,
            modifier = Modifier.weight(1f),
            containerColor = Color.White,
            contentColor = Color.Black
        )
    }
}

/**
 * 하단 액션 버튼 (채워진 형태)
 */
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp), // Figma의 35.99dp에 가까움
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 8.dp) // 내부 패딩 줄이기
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    }
}

/**
 * 하단 액션 버튼 (아웃라인 형태)
 */
@Composable
fun ActionOutlinedButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    borderColor: Color = ColorControlBorder,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp), // Figma의 35.99dp에 가까움
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ColorControlInactive,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    }
}


// --- 4. 미리보기 ---

@Preview(showBackground = true, device = "spec:width=394dp,height=852dp,dpi=440")
@Composable
fun PracticeScreenMobilePreview() {
    KpopDancePracticeAITheme {
        // 프리뷰에서는 더미 데이터를 사용
        PracticeScreenMobile(
            songTitle = "Dynamite",
            artistPart = "BTS · Part 2: 메인 파트",
            difficulty = "보통",
            length = "2:15",
        )
    }
}