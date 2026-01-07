package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kpopdancepracticeai.ui.theme.KpopDancePracticeAITheme

/**
 * 프로필 설정 전체 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit
) {
    // 앱 전체의 그라데이션 배경
    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF), // 상단 연한 파랑
            Color(0xFFF0E8FF)  // 하단 연한 보라
        )
    )

    // 상태 관리 (임시)
    var name by remember { mutableStateOf("김다연") }
    var email by remember { mutableStateOf("dayeon.kim@example.com") }
    var phone by remember { mutableStateOf("010-1234-5678") }
    var birthdate by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("자신을 소개해주세요") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            // topBar 제거: 스크롤 영역 내부로 이동
            bottomBar = {
                // 하단 '취소' / '저장' 버튼
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
                            onClick = { /* TODO: 저장 로직 */ onBackClick() },
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
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize(),
                // .padding(horizontal = 16.dp), // TopAppBar의 전체 너비를 위해 여기 패딩 제거
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 0. 상단바 (스크롤 가능하도록 이곳으로 이동) ---
                item {
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
                        // Scaffold의 innerPadding이 이미 상단 여백을 제공하므로, TopAppBar 자체의 시스템 창 인셋은 제거
                        windowInsets = WindowInsets(0.dp)
                    )
                }

                // --- 1. 프로필 사진 변경 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ProfileImageCard(onClick = { /* TODO: 이미지 선택 로직 */ })
                    }
                }
                // --- 2. 기본 정보 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BasicInfoCard(
                            name = name, onNameChange = { name = it },
                            email = email, onEmailChange = { email = it },
                            phone = phone, onPhoneChange = { phone = it },
                            birthdate = birthdate, onBirthdateChange = { birthdate = it }
                        )
                    }
                }
                // --- 3. 댄스 정보 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DanceInfoCard(
                            bio = bio, onBioChange = { bio = it }
                        )
                    }
                }
                // --- 4. 활동 통계 ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ActivityStatsCard()
                    }
                }
                // --- 하단 버튼 영역 확보용 Spacer ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 1. 프로필 사진 변경 카드
 */
@Composable
fun ProfileImageCard(onClick: () -> Unit) {
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
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    tint = Color.Gray
                )
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
 * 2. 기본 정보 입력 카드
 */
@Composable
fun BasicInfoCard(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
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
                label = "이름",
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
                label = "전화번호",
                value = phone,
                onValueChange = onPhoneChange,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
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
    bio: String, onBioChange: (String) -> Unit
) {
    var danceLevel by remember { mutableStateOf("중급 - 기본기를 다지는 단계") }
    val levels = listOf("초급", "중급 - 기본기를 다지는 단계", "고급")
    var expanded by remember { mutableStateOf(false) }

    val genres = listOf("K-POP", "힙합", "재즈", "발레", "현대무용", "비보잉", "하우스", "왁킹")
    val selectedGenres by remember { mutableStateOf(setOf("K-POP", "힙합")) } // 임시

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

            // 댄스 레벨 (Dropdown)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                SettingsTextField(
                    label = "댄스 레벨",
                    value = danceLevel,
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
                                danceLevel = level
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 관심 장르
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
                        val isSelected = genre in selectedGenres
                        FilterChip(
                            selected = isSelected,
                            onClick = { /* TODO: 장르 선택 로직 */ },
                            label = { Text(genre) }
                        )
                    }
                }
            }

            // 자기소개
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
fun ActivityStatsCard() {
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
                    value = "42",
                    color = Color(0xfffaf5ff), // 보라
                    valueColor = Color(0xff9810fa)
                )
                SmallStatCard(
                    label = "연습 시간",
                    value = "156",
                    color = Color(0xfffdf2f8), // 핑크
                    valueColor = Color(0xffe60076)
                )
                SmallStatCard(
                    label = "획득 배지",
                    value = "28",
                    color = Color(0xfffff7ed), // 주황
                    valueColor = Color(0xfff54900)
                )
            }
        }
    }
}

/**
 * 재사용 가능한 텍스트 입력 필드
 */
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

/**
 * 활동 통계에 사용되는 작은 스탯 카드
 */
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