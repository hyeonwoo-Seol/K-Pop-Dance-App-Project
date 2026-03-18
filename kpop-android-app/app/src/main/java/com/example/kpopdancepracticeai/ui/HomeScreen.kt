package com.example.kpopdancepracticeai.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.kpopdancepracticeai.KpopApplication
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import com.example.kpopdancepracticeai.viewmodel.RecentChoreoUiModel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private const val HOME_PROMO_VIDEO_ASSET = "home_intro.mp4"
private const val HOME_PROMO_COLLAPSE_DURATION_MS = 560 // 카드가 위로 올라오는 속도는 이 값으로 조절

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
    var isPromoAnimatingOut by remember { mutableStateOf(false) }
    var promoContainerHeightPx by remember { mutableIntStateOf(0) }
    val promoVideoAlpha = remember { Animatable(1f) }
    val promoContentTranslationY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = promoContentTranslationY.value
                        },
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (showPromoVideo) {
                        HomePromoVideoSection(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .onSizeChanged { size ->
                                    promoContainerHeightPx = size.height
                                }
                                .graphicsLayer {
                                    alpha = promoVideoAlpha.value
                                },
                            assetFileName = HOME_PROMO_VIDEO_ASSET,
                            onPlaybackFinished = {
                                if (isPromoAnimatingOut) return@HomePromoVideoSection

                                isPromoAnimatingOut = true
                                scope.launch {
                                    val targetOffset = -promoContainerHeightPx.toFloat()
                                    joinAll(
                                        launch {
                                            promoVideoAlpha.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = promoCollapseDurationMillis)
                                            )
                                        },
                                        launch {
                                            promoContentTranslationY.animateTo(
                                                targetValue = targetOffset,
                                                animationSpec = tween(
                                                    durationMillis = promoCollapseDurationMillis,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                    )
                                    showPromoVideo = false
                                    promoVideoAlpha.snapTo(1f)
                                    promoContentTranslationY.snapTo(0f)
                                    isPromoAnimatingOut = false
                                }
                            }
                        )
                    }

                    RecentChoreoSection(
                        recentChoreo = recentChoreo,
                        onSongClick = onSongClick
                    )

                    RegisteredChoreoSection(
                        dbSongs = dbSongs,
                        onSongClick = onSongClick
                    )

                    TrendingChallengeSection(
                        dbSongs = dbSongs,
                        onSongClick = onSongClick
                    )
                }
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
