package com.example.kpopdancepracticeai.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.kpopdancepracticeai.KpopApplication
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.viewmodel.RecentChoreoUiModel
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures

private const val HOME_PROMO_VIDEO_ASSET = "home_intro.mp4"
private const val HOME_PROMO_COLLAPSE_DURATION_MS = 560 // 카드가 위로 올라오는 속도는 이 값으로 조절
// iPhone 앱에서 느껴지는 것처럼 관성이 조금 더 길게 살아 있고, 복귀는 급하지 않게 보이도록 맞춘 기본 프리셋입니다.
private const val SONG_CARD_RETURN_DURATION_MS = 360

// SongCard를 손가락으로 스와이프했을 때의 관성(Fling) 감쇠 강도입니다.
// 값을 키우면 더 빨리 멈추고, 낮추면 더 멀리 미끄러집니다.
private const val SONG_CARD_FLING_FRICTION = 1.72f

// 이 속도(px/s)를 넘으면 손을 뗐을 때 fling을 적용합니다.
// 값을 키우면 더 세게 스와이프해야 관성이 붙고, 낮추면 가볍게 밀어도 멀리 이동합니다.
private const val SONG_CARD_FLING_VELOCITY_THRESHOLD = 680f

// 카드가 화면 밖으로 너무 멀리 나가지 않도록 제한하는 최대 이동 비율입니다.
private const val SONG_CARD_MAX_SWIPE_RATIO = 0.52f

// 드래그 거리 대비 카드 회전량입니다. 수치를 키우면 스와이프 체감이 더 극적으로 보입니다.
private const val SONG_CARD_ROTATION_PER_PIXEL = 0.055f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(), // DB 데이터를 가져오기 위한 ViewModel
    onSearch: (String) -> Unit,
    onSongClick: (songId: String, originX: Float, originY: Float) -> Unit,
    paddingValues: PaddingValues,
    onAiTipOverlayVisibilityChanged: (Boolean) -> Unit = {},
    promoCollapseDurationMillis: Int = HOME_PROMO_COLLAPSE_DURATION_MS
) {
    // DB에서 불러온 노래 목록을 상태로 관리
    val dbSongs by viewModel.songs.collectAsState()
    val recentChoreo by viewModel.recentChoreo.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val app = LocalContext.current.applicationContext as KpopApplication
    var showPromoVideo by remember { mutableStateOf(app.consumeHomePromoVideoVisibility()) }

    // 화면 진입 시 최신 데이터 로드 (필요한 경우)
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    val layoutDirection = LocalLayoutDirection.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showAiTipOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(showAiTipOverlay) {
        onAiTipOverlayVisibilityChanged(showAiTipOverlay)
    }

    BackHandler(enabled = showAiTipOverlay) {
        showAiTipOverlay = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KPOP 댄스 연습 AI",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("연습할 곡을 검색하세요") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                onSearch(searchText)
                            }
                        )
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = showPromoVideo,
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = promoCollapseDurationMillis),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(animationSpec = tween(durationMillis = promoCollapseDurationMillis))
                ) {
                    HomePromoVideoSection(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        assetFileName = HOME_PROMO_VIDEO_ASSET,
                        onPlaybackFinished = { showPromoVideo = false }
                    )
                }
            }

            item {
                RecentChoreoSection(
                    recentChoreo = recentChoreo,
                    onSongClick = onSongClick
                )
            }

            item {
                RegisteredChoreoSection(
                    dbSongs = dbSongs,
                    onSongClick = onSongClick
                )
            }

            item {
                TrendingChallengeSection(
                    dbSongs = dbSongs,
                    onSongClick = onSongClick
                )
            }
        }

        if (!showAiTipOverlay) {
            FloatingActionButton(
                onClick = { showAiTipOverlay = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = paddingValues.calculateBottomPadding() + 20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI 연습 팁")
            }
        }

        AnimatedVisibility(
            visible = showAiTipOverlay,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { showAiTipOverlay = false }
            )
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = showAiTipOverlay,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 280)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 180))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AiPracticeTipScreen(
                        paddingValues = PaddingValues(0.dp),
                        onBackClick = { showAiTipOverlay = false }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(UnstableApi::class)
private fun HomePromoVideoSection(
    assetFileName: String,
    modifier: Modifier = Modifier,
    onPlaybackFinished: () -> Unit
) {
    val context = LocalContext.current
    var finishHandled by remember { mutableStateOf(false) }
    val exoPlayer = remember(assetFileName) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse("asset:///$assetFileName"))
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !finishHandled) {
                    finishHandled = true
                    onPlaybackFinished()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!finishHandled) {
                    finishHandled = true
                    onPlaybackFinished()
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = exoPlayer
                }
            },
            update = { it.player = exoPlayer }
        )
    }
}

@Composable
private fun RecentChoreoSection(
    recentChoreo: List<RecentChoreoUiModel>,
    onSongClick: (songId: String, originX: Float, originY: Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(
            title = "최근 연습한 안무",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (recentChoreo.isEmpty()) {
            Text(
                text = "최근에 연습한 안무가 없습니다",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentChoreo.take(4)) { item ->
                    SongCard(
                        artist = item.artist,
                        title = "${item.title} (파트 ${item.partNumber})",
                        views = "마지막 연습 ${item.lastPracticedAt}",
                        imageUrl = item.coverUrl,
                        onClick = { originX, originY ->
                            onSongClick(item.songId.toString(), originX, originY)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisteredChoreoSection(
    dbSongs: List<Song>,
    onSongClick: (songId: String, originX: Float, originY: Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(
            title = "등록된 안무 목록",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (dbSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 16.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "등록된 곡이 없습니다.\n데이터를 동기화해주세요.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(dbSongs) { song ->
                    SongCard(
                        artist = song.artistKr ?: "Unknown Artist",
                        title = song.titleKr ?: "Unknown Title",
                        views = "난이도 ${song.difficulty}",
                        imageUrl = song.coverUrl,
                        onClick = { originX, originY ->
                            onSongClick(song.songId.toString(), originX, originY)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingChallengeSection(
    dbSongs: List<Song>,
    onSongClick: (songId: String, originX: Float, originY: Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(
            title = "인기 급상승 챌린지",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (dbSongs.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(dbSongs.reversed()) { song ->
                    SongCard(
                        artist = song.artistKr ?: "",
                        title = song.titleKr ?: "",
                        views = "인기",
                        imageUrl = song.coverUrl,
                        onClick = { originX, originY ->
                            onSongClick(song.songId.toString(), originX, originY)
                        }
                    )
                }
            }
        } else {
            Text(
                text = "챌린지 목록을 불러올 수 없습니다.",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

// 섹션 제목 컴포넌트
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = modifier
    )
}

// 곡 정보 카드 컴포넌트
@Composable
fun SongCard(
    artist: String,
    title: String,
    views: String,
    imageUrl: String?,
    onClick: (originX: Float, originY: Float) -> Unit
) {
    val thumbnailRotation = remember { Animatable(0f) }
    val thumbnailOffsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isAnimating by remember { mutableStateOf(false) }
    var thumbnailWidthPx by remember { mutableStateOf(0f) }
    val coordinateHolder = remember { object { var coordinates: LayoutCoordinates? = null } }
    val velocityTracker = remember { VelocityTracker() }
    val swipeDecay = remember {
        exponentialDecay<Float>(frictionMultiplier = SONG_CARD_FLING_FRICTION)
    }

    fun resolveClickOrigin(tapOffset: Offset): Pair<Float, Float> {
        val coordinates = coordinateHolder.coordinates ?: return 0.5f to 0.5f
        val rootCoordinates = coordinates.findRootCoordinates()
        val rootSize = rootCoordinates.size

        if (rootSize.width <= 0 || rootSize.height <= 0) return 0.5f to 0.5f

        val tapInRoot = coordinates.localToRoot(tapOffset)
        return (tapInRoot.x / rootSize.width.toFloat()).coerceIn(0f, 1f) to
            (tapInRoot.y / rootSize.height.toFloat()).coerceIn(0f, 1f)
    }

    fun calculateBoundedOffset(targetOffset: Float): Float {
        val maxOffset = (thumbnailWidthPx * SONG_CARD_MAX_SWIPE_RATIO).coerceAtLeast(1f)
        return targetOffset.coerceIn(-maxOffset, maxOffset)
    }

    Column(
        modifier = Modifier
            .width(140.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    translationX = thumbnailOffsetX.value
                    rotationZ = thumbnailRotation.value
                }
                .onGloballyPositioned { coordinates ->
                    coordinateHolder.coordinates = coordinates
                    thumbnailWidthPx = coordinates.size.width.toFloat()
                }
                .pointerInput(isAnimating) {
                    detectTapGestures { tapOffset ->
                        if (isAnimating) return@detectTapGestures

                        val (originX, originY) = resolveClickOrigin(tapOffset)

                        scope.launch {
                            isAnimating = true
                            thumbnailRotation.animateTo(
                                targetValue = 15f,
                                animationSpec = tween(durationMillis = 120)
                            )
                            thumbnailRotation.animateTo(
                                targetValue = -5f,
                                animationSpec = tween(durationMillis = 180)
                            )
                            onClick(originX, originY)
                            thumbnailOffsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = SONG_CARD_RETURN_DURATION_MS)
                            )
                            thumbnailRotation.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 100)
                            )
                            isAnimating = false
                        }
                    }
                }
                .pointerInput(isAnimating, thumbnailWidthPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (isAnimating) return@detectDragGestures
                            velocityTracker.resetTracking()
                            velocityTracker.addPosition(0L, offset)
                            scope.launch {
                                thumbnailOffsetX.stop()
                                thumbnailRotation.stop()
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (isAnimating) return@detectDragGestures
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            val nextOffset = calculateBoundedOffset(thumbnailOffsetX.value + dragAmount.x)
                            val nextRotation = nextOffset * SONG_CARD_ROTATION_PER_PIXEL

                            scope.launch {
                                thumbnailOffsetX.snapTo(nextOffset)
                                thumbnailRotation.snapTo(nextRotation)
                            }
                        },
                        onDragEnd = {
                            if (isAnimating) return@detectDragGestures
                            val velocityX = velocityTracker.calculateVelocity().x
                            val maxOffset = (thumbnailWidthPx * SONG_CARD_MAX_SWIPE_RATIO).coerceAtLeast(1f)

                            scope.launch {
                                if (kotlin.math.abs(velocityX) >= SONG_CARD_FLING_VELOCITY_THRESHOLD) {
                                    thumbnailOffsetX.updateBounds(-maxOffset, maxOffset)
                                    thumbnailOffsetX.animateDecay(
                                        initialVelocity = velocityX,
                                        animationSpec = swipeDecay
                                    )
                                    thumbnailOffsetX.updateBounds(
                                        lowerBound = Float.NEGATIVE_INFINITY,
                                        upperBound = Float.POSITIVE_INFINITY
                                    )
                                }

                                thumbnailOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = SONG_CARD_RETURN_DURATION_MS,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                                thumbnailRotation.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = SONG_CARD_RETURN_DURATION_MS,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                thumbnailOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = SONG_CARD_RETURN_DURATION_MS,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                                thumbnailRotation.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = SONG_CARD_RETURN_DURATION_MS,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "$title cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = views,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KpopDancePracticeAITheme {
        HomeScreen(
            onSearch = {},
            onSongClick = { _, _, _ -> },
            paddingValues = PaddingValues()
        )
    }
}
