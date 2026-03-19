package com.example.kpopdancepracticeai.ui

import com.example.kpopdancepracticeai.ui.motion.rememberIosLikeFlingBehavior

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme
import com.example.kpopdancepracticeai.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * 갤러리에서 선택한 이미지를 앱 내부 저장소로 복사하여 저장하는 헬퍼 함수
 */
fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 프로필 설정 전체 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var danceSkill by remember { mutableStateOf("초급") }
    var favoriteGenres by remember { mutableStateOf(setOf<String>()) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) } // [추가됨] 이미지 경로 관리

    // 데이터가 로드되면 상태 업데이트
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            name = user.name
            email = user.email
            password = user.passwordHash?.takeIf { it.isNotEmpty() } ?: "********"
            birthdate = user.birthDate
            bio = user.bio ?: ""
            danceSkill = user.danceSkill
            favoriteGenres = if (user.favoriteGenres.isNotBlank()) {
                user.favoriteGenres.split(",").toSet()
            } else {
                emptySet()
            }
            profileImageUrl = user.profileImageUrl // 기존 이미지 불러오기
        }
    }

    // [추가됨] 갤러리에서 이미지 가져오는 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 내부 저장소에 안전하게 복사
            val savedUri = saveImageToInternalStorage(context, it)
            if (savedUri != null) {
                profileImageUrl = savedUri.toString() // 새 이미지 경로 할당
            }
        }
    }

    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.8f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("취소")
                        }
                        Button(
                            onClick = {
                                // [수정됨] 이미지 경로 포함하여 저장 로직 호출
                                viewModel.updateUserProfile(
                                    name = name,
                                    email = email,
                                    passwordHash = password,
                                    birthDate = birthdate,
                                    bio = bio,
                                    danceSkill = danceSkill,
                                    favoriteGenres = favoriteGenres.toList(),
                                    profileImageUrl = profileImageUrl
                                )
                                onBackClick()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("변경사항 저장")
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState(), flingBehavior = rememberIosLikeFlingBehavior()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TopAppBar(
                    title = { Text("프로필 설정", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0.dp)
                )

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileImageCard(
                        profileImageUrl = profileImageUrl,
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                }

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BasicInfoCard(
                        name = name, onNameChange = { name = it },
                        email = email, onEmailChange = { email = it },
                        password = password, onPasswordChange = { password = it },
                        birthdate = birthdate, onBirthdateChange = { birthdate = it }
                    )
                }

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DanceInfoCard(
                        bio = bio,
                        onBioChange = { bio = it },
                        currentLevel = danceSkill,
                        onLevelChange = { danceSkill = it },
                        currentGenres = favoriteGenres,
                        onGenresChange = { favoriteGenres = it }
                    )
                }

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ActivityStatsCard(
                        completedSongs = userStats?.completedParts?.toString() ?: "0",
                        playTime = "${(userStats?.totalPlayTime ?: 0) / 60}",
                        badgeCount = userStats?.badgeCount?.toString() ?: "0"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 1. 프로필 사진 변경 카드 (이미지가 있으면 표시)
 */
@Composable
fun ProfileImageCard(profileImageUrl: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xffd6deff))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box {
                // 이미지가 등록되어있으면 표시, 없으면 기본 아이콘
                if (!profileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.LightGray, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        tint = Color.Gray
                    )
                }
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "사진 변경",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp)
                        .align(Alignment.BottomEnd),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = "프로필 사진을 변경하려면 클릭하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * 2. 기본 정보 입력 카드 (UI 유지)
 */
@Composable
fun BasicInfoCard(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    birthdate: String, onBirthdateChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xffd6deff))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("기본 정보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SettingsTextField(
                label = "닉네임",
                value = name,
                onValueChange = onNameChange
            )
            SettingsTextField(
                label = "이메일",
                value = email,
                onValueChange = onEmailChange,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
            )
            SettingsTextField(
                label = "비밀번호",
                value = password,
                onValueChange = onPasswordChange,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            )
            SettingsTextField(
                label = "생년월일",
                value = birthdate,
                onValueChange = onBirthdateChange,
                placeholder = "YYYY-MM-DD",
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
            )
        }
    }
}

/**
 * 3. 댄스 정보 입력 카드
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DanceInfoCard(
    bio: String, onBioChange: (String) -> Unit,
    currentLevel: String, onLevelChange: (String) -> Unit,
    currentGenres: Set<String>, onGenresChange: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val levels = remember { listOf("초급", "중급 - 기본기를 다지는 단계", "고급") }
    val genres = remember { listOf("K-POP", "힙합", "재즈", "발레", "현대무용", "비보잉", "하우스", "왁킹") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xffd6deff))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("댄스 정보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                SettingsTextField(
                    label = "댄스 레벨",
                    value = currentLevel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    levels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                onLevelChange(level)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("관심 장르", style = MaterialTheme.typography.labelLarge)
                Text(
                    "선호하는 댄스 장르를 선택하세요 (복수 선택 가능)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        val isSelected = genre in currentGenres
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newGenres = if (isSelected) {
                                    currentGenres - genre
                                } else {
                                    currentGenres + genre
                                }
                                onGenresChange(newGenres)
                            },
                            label = { Text(genre) }
                        )
                    }
                }
            }

            SettingsTextField(
                label = "자기소개",
                value = bio,
                onValueChange = onBioChange,
                modifier = Modifier.height(120.dp),
                singleLine = false,
                trailingIcon = {
                    Text(
                        "${bio.length}/200",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            )
        }
    }
}

/**
 * 4. 활동 통계 카드
 */
@Composable
fun ActivityStatsCard(
    completedSongs: String = "0",
    playTime: String = "0",
    badgeCount: String = "0"
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xffd6deff))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("활동 통계", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SmallStatCard(
                    label = "완료한 곡",
                    value = completedSongs,
                    color = Color(0xfffaf5ff), // 보라
                    valueColor = Color(0xff9810fa)
                )
                SmallStatCard(
                    label = "연습 시간(분)",
                    value = playTime,
                    color = Color(0xfffdf2f8), // 핑크
                    valueColor = Color(0xffe60076)
                )
                SmallStatCard(
                    label = "획득 배지",
                    value = badgeCount,
                    color = Color(0xfffff7ed), // 주황
                    valueColor = Color(0xfff54900)
                )
            }
        }
    }
}

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { placeholder?.let { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            readOnly = readOnly,
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun SmallStatCard(
    label: String,
    value: String,
    color: Color,
    valueColor: Color
) {
    Surface(
        modifier = Modifier
            .width(88.dp)
            .height(112.dp),
        shape = RoundedCornerShape(10.dp),
        color = color
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileEditScreenPreview() {
    KpopDancePracticeAITheme {
        ProfileEditScreen(onBackClick = {})
    }
}
