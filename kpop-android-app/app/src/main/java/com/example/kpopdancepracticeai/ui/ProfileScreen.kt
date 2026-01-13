package com.example.kpopdancepracticeai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.kpopdancepracticeai.KpopApplication
import com.example.kpopdancepracticeai.data.entity.UserStats
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// 프로필 화면을 위한 뷰모델
// DB에서 사용자 통계를 가져오는 역할을 합니다.
class ProfileViewModel(private val repository: AppRepository) : ViewModel() {

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    // 사용자 ID를 기반으로 통계 로드
    fun loadUserStats(userId: String) {
        viewModelScope.launch {
            // UserDao의 Flow를 수집하여 상태 업데이트
            repository.userDao.getUserStats(userId).collect { stats ->
                _userStats.value = stats
            }
        }
    }
}

// 뷰모델 팩토리
class ProfileViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String? // 로그인된 사용자 ID
) {
    // Repository 가져오기
    val context = LocalContext.current
    val application = context.applicationContext as KpopApplication
    val repository = application.repository

    // ViewModel 초기화
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(repository)
    )

    // 화면 진입 시 데이터 로드
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadUserStats(userId)
        }
    }

    // UI 상태 관찰
    val userStats by viewModel.userStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "마이 페이지",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 프로필 요약 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 사용자 아이콘 또는 이미지 (임시 텍스트)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("User", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = userId ?: "게스트",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 레벨 표시 (UserStats에 level 필드가 있다고 가정)
                // 만약 UserStats에 level 필드가 없다면 이 부분은 주석 처리하세요.
                Text(
                    text = "Lv. ${userStats?.level ?: 1}",
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 통계 정보 섹션
        Text(
            text = "내 활동 분석",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 총 연습 시간
            StatCard(
                title = "총 연습 시간",
                value = formatTime(userStats?.totalPlayTime ?: 0L),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 평균 정확도
            StatCard(
                title = "평균 정확도",
                // UserStats에 averageAccuracy 필드가 있다고 가정 (없다면 totalScore 등으로 대체)
                value = "${String.format("%.1f", userStats?.averageAccuracy ?: 0f)}%",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 추가 메뉴 버튼들
        Button(
            onClick = { /* 내 영상 보관함 이동 로직 */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text("내 연습 영상 보기")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                // 로그아웃 또는 뒤로가기 로직
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("뒤로 가기")
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// 밀리세컨드를 시간 문자열로 변환하는 유틸리티 함수
fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
}