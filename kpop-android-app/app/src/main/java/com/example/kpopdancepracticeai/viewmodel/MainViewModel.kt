package com.example.kpopdancepracticeai.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.*
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// UI용 업적 데이터 클래스
data class AchievementUiModel(
    val title: String,
    val description: String,
    val progress: Float, // 0.0 ~ 1.0
    val progressText: String
)

// UI용 배지 데이터 클래스
data class BadgeUiModel(
    val name: String,
    val color: Color
)

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // --- 기본 데이터 ---
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSongParts = MutableStateFlow<List<SongPart>>(emptyList())
    val currentSongParts: StateFlow<List<SongPart>> = _currentSongParts.asStateFlow()

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    private val _recentHistory = MutableStateFlow<List<PracticeHistory>>(emptyList())
    val recentHistory: StateFlow<List<PracticeHistory>> = _recentHistory.asStateFlow()

    // --- 계산된 레벨 및 경험치 정보 ---
    val userLevelInfo: StateFlow<Pair<Int, Pair<Long, Long>>> = _userStats.map { stats ->
        if (stats == null) Pair(1, Pair(0L, 1000L))
        else {
            // 레벨 계산 로직: (총점수 / 1000) + 1
            val score = stats.achievementScore.toLong()
            val level = (score / 1000).toInt() + 1
            val currentLevelExp = score % 1000
            val nextLevelExp = 1000L
            Pair(level, Pair(currentLevelExp, nextLevelExp))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(1, Pair(0L, 1000L)))

    // --- 업적 및 배지 ---
    private val _achievementProgress = MutableStateFlow<List<AchievementUiModel>>(emptyList())
    val achievementProgress: StateFlow<List<AchievementUiModel>> = _achievementProgress.asStateFlow()

    private val _userBadges = MutableStateFlow<List<BadgeUiModel>>(emptyList())
    val userBadges: StateFlow<List<BadgeUiModel>> = _userBadges.asStateFlow()

    // 업적 코드와 UI 텍스트 매핑
    private val achievementMeta = mapOf(
        "PERFECTIONIST" to ("완벽주의자" to "95% 이상의 정확도 5회 달성"),
        "PRACTICE_BUG" to ("연습 벌레" to "총 연습 시간 100시간 달성"),
        "BTS_MASTER" to ("BTS 마스터" to "BTS 챌린지 10개 완료"),
        "CHALLENGE_HUNTER" to ("챌린지 헌터" to "모든 챌린지 1회 이상 완료"),
        "NEW_DANCER" to ("신입 댄서" to "첫 연습 영상 업로드")
    )

    private var currentUserId: String? = null

    fun loadInitialData(userId: String) {
        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.fetchInitialData(userId)

                // 구독 설정
                launch { repository.getUserStats(userId).collect { _userStats.value = it } }
                launch { repository.getUserProfile(userId).collect { _currentUserProfile.value = it } }
                launch { repository.allSongs.collect { _songs.value = it } }
                launch { repository.getRecentHistory(userId).collect { _recentHistory.value = it } }

                // 업적 구독 및 변환
                launch {
                    repository.getUserAchievements(userId).collect { list ->
                        val uiModels = list.mapNotNull { item ->
                            val meta = achievementMeta[item.achievementCode] ?: return@mapNotNull null
                            val progress = if (item.goalStep > 0) item.currentStep.toFloat() / item.goalStep else 0f
                            val progressText = "${(progress * 100).toInt()}%"
                            AchievementUiModel(
                                title = meta.first,
                                description = meta.second,
                                progress = progress.coerceIn(0f, 1f),
                                progressText = progressText
                            )
                        }
                        _achievementProgress.value = uiModels
                    }
                }

                // 배지 구독 및 변환
                launch {
                    repository.getUserBadges(userId).collect { list ->
                        _userBadges.value = list.map { badge ->
                            val color = when {
                                badge.name.contains("마스터") -> Color(0xFFEBEBFF)
                                badge.name.contains("팬") -> Color(0xFFD6F5FF)
                                badge.name.contains("전문가") -> Color(0xFFFFD6EB)
                                else -> Color(0xFFD9FFE5)
                            }
                            BadgeUiModel(badge.name, color)
                        }
                    }
                }

                _syncMessage.value = "데이터 동기화 완료"
            } catch (e: Exception) {
                e.printStackTrace()
                _syncMessage.value = "동기화 실패: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // [수정됨] profileImageUrl 파라미터 추가
    fun updateUserProfile(name: String, email: String, passwordHash: String, birthDate: String, bio: String, danceSkill: String, favoriteGenres: List<String>, profileImageUrl: String?) {
        val currentUser = _currentUserProfile.value ?: return
        val updatedUser = currentUser.copy(
            name = name, email = email, passwordHash = passwordHash,
            birthDate = birthDate, bio = bio, danceSkill = danceSkill,
            favoriteGenres = favoriteGenres.joinToString(","),
            profileImageUrl = profileImageUrl
        )
        viewModelScope.launch {
            try {
                repository.updateUserProfile(updatedUser)
                _syncMessage.value = "프로필이 저장되었습니다."
            } catch (e: Exception) {
                _syncMessage.value = "저장 실패: ${e.message}"
            }
        }
    }

    fun updateUsageTime() {
        val userId = _userStats.value?.userUuid ?: currentUserId
        if (userId != null) {
            viewModelScope.launch { repository.syncAppUsageTime(userId) }
        }
    }

    fun refreshData() {
        val userId = currentUserId
        if (userId != null) {
            updateUsageTime()
            loadInitialData(userId)
        } else {
            _syncMessage.value = "로그인 정보가 없습니다."
        }
    }

    fun selectSong(songId: Long) { /* 생략 */ }
    fun searchSongs(query: String) = repository.searchSongs(query)
    fun savePracticeResult(history: PracticeHistory) {
        viewModelScope.launch { repository.savePracticeResult(history) }
    }
    fun clearSyncMessage() { _syncMessage.value = null }

    companion object {
        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    }
}