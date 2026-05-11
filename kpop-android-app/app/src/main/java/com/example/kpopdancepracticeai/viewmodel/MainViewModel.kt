package com.example.kpopdancepracticeai.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.RealDataSource
import com.example.kpopdancepracticeai.data.dao.TopPracticedHistoryRow
import com.example.kpopdancepracticeai.data.entity.*
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

// UI용 업적 데이터 클래스
data class AchievementUiModel(
    val code: String,
    val title: String,
    val description: String,
    val currentStep: Int,
    val goalStep: Int,
    val isUnlocked: Boolean,
    val progress: Float, // 0.0 ~ 1.0
    val progressText: String
)

// UI용 배지 데이터 클래스
data class BadgeUiModel(
    val id: String,
    val name: String,
    val color: Color,
    val isSelected: Boolean
)

data class LightStickUiModel(
    val id: String,
    val name: String,
    val artist: String,
    val localImagePath: String
)


// 홈 화면 최근 연습 안무 UI 모델
data class RecentChoreoUiModel(
    val songId: Long,
    val partNumber: Int,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val practiceCount: Int,
    val lastPracticedAt: String
)

data class TopPracticedChoreoUiModel(
    val title: String,
    val artist: String,
    val partName: String
)

data class PracticeResultSummaryUiModel(
    val songTitle: String,
    val bestScore: Int,
    val achievements: List<AchievementUiModel>
)

// 로그인 상태 정의
sealed interface LoginState {
    object Idle : LoginState
    object Loading : LoginState
    object Success : LoginState // 기존 유저 -> 메인 화면 이동
    object NeedProfile : LoginState // 신규 유저 -> 프로필 입력 화면 이동
    data class Error(val message: String) : LoginState
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

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

    private val _recentChoreo = MutableStateFlow<List<RecentChoreoUiModel>>(emptyList())
    val recentChoreo: StateFlow<List<RecentChoreoUiModel>> = _recentChoreo.asStateFlow()

    private val _topPracticedChoreos = MutableStateFlow<List<TopPracticedChoreoUiModel>>(emptyList())
    val topPracticedChoreos: StateFlow<List<TopPracticedChoreoUiModel>> = _topPracticedChoreos.asStateFlow()

    private var topPracticedHistoryRowsCache: List<TopPracticedHistoryRow> = emptyList()

    // --- 계산된 레벨 및 경험치 정보 ---
    val userLevelInfo: StateFlow<Pair<Int, Pair<Long, Long>>> = _userStats.map { stats ->
        if (stats == null) Pair(1, Pair(0L, 1000L))
        else {
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

    private val _selectedBadge = MutableStateFlow<BadgeUiModel?>(null)
    val selectedBadge: StateFlow<BadgeUiModel?> = _selectedBadge.asStateFlow()

    private val _ownedLightSticks = MutableStateFlow<List<LightStickUiModel>>(emptyList())
    val ownedLightSticks: StateFlow<List<LightStickUiModel>> = _ownedLightSticks.asStateFlow()

    private val achievementMetaByCode = RealDataSource.getRealAchievements.associateBy { it.id }

    private var currentUserId: String? = null

    fun checkUserExists(userId: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val existingUser = repository.getUserProfileOneShot(userId)
                if (existingUser != null) {
                    loadInitialData(userId)
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.NeedProfile
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(e.message ?: "사용자 정보를 확인할 수 없습니다.")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun loadInitialData(userId: String) {
        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.prePopulateSongsIfNeeded()

                repository.fetchInitialData(userId)

                // 구독 설정
                launch { repository.getUserStats(userId).collect { _userStats.value = it } }
                launch { repository.getUserProfile(userId).collect { _currentUserProfile.value = it } }
                launch {
                    repository.allSongs.collect {
                        _songs.value = it
                        remapTopPracticedChoreos()
                    }
                }
                launch { repository.getRecentHistory(userId).collect { _recentHistory.value = it } }
                launch {
                    repository.getRecentChoreoRows(userId).collect { rows ->
                        _recentChoreo.value = rows.map { row ->
                            RecentChoreoUiModel(
                                songId = row.songId,
                                partNumber = row.partNumber,
                                title = row.titleKr,
                                artist = row.artistKr,
                                coverUrl = row.coverUrl,
                                practiceCount = row.practiceCount,
                                lastPracticedAt = row.lastPracticedAt
                            )
                        }
                    }
                }
                launch {
                    repository.getTopPracticedHistoryRows(userId).collect { rows ->
                        topPracticedHistoryRowsCache = rows
                        remapTopPracticedChoreos()
                    }
                }

                // 업적 구독 및 변환
                launch {
                    repository.getUserAchievements(userId).collect { list ->
                        val progressByCode = list.associateBy { it.achievementCode }
                        val uiModels = achievementMetaByCode.values.map { meta ->
                            val item = progressByCode[meta.id]
                            val currentStep = item?.currentStep ?: 0
                            val goalStep = item?.goalStep ?: meta.goalCount
                            val isUnlocked = item?.isCompleted ?: false
                            val progress = if (goalStep > 0) currentStep.toFloat() / goalStep else 0f
                            val safeProgress = progress.coerceIn(0f, 1f)
                            val progressText = "$currentStep / $goalStep (${(safeProgress * 100).toInt()}%)"

                            AchievementUiModel(
                                code = meta.id,
                                title = meta.title,
                                description = meta.description,
                                currentStep = currentStep,
                                goalStep = goalStep,
                                isUnlocked = isUnlocked,
                                progress = safeProgress,
                                progressText = progressText
                            )
                        }.sortedWith(
                            compareByDescending<AchievementUiModel> { it.isUnlocked }
                                .thenByDescending { it.progress }
                                .thenBy { it.title }
                        )

                        _achievementProgress.value = uiModels
                    }
                }

                // 배지 구독 및 변환
                launch {
                    repository.getUserBadges(userId).collect { list ->
                        _userBadges.value = list.map { badge ->
                            BadgeUiModel(badge.id, badge.name, badgeColorForName(badge.name), badge.isSelected)
                        }
                    }
                }

                launch {
                    repository.getSelectedBadge(userId).collect { badge ->
                        _selectedBadge.value = badge?.let {
                            BadgeUiModel(
                                id = it.id,
                                name = it.name,
                                color = badgeColorForName(it.name),
                                isSelected = true
                            )
                        }
                    }
                }

                launch {
                    repository.getOwnedLightSticks().collect { lightSticks ->
                        _ownedLightSticks.value = lightSticks.map {
                            LightStickUiModel(
                                id = it.id,
                                name = it.name,
                                artist = it.artist,
                                localImagePath = it.localImagePath
                            )
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

    fun selectBadge(badgeId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.selectBadge(userId, badgeId)
        }
    }

    private fun badgeColorForName(name: String): Color = when {
        name.contains("마스터") -> Color(0xFFEBEBFF)
        name.contains("팬") -> Color(0xFFD6F5FF)
        name.contains("전문가") -> Color(0xFFFFD6EB)
        else -> Color(0xFFD9FFE5)
    }

    fun registerUser(userId: String, email: String, passwordHash: String, name: String, birthDate: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                _isSyncing.value = true
                repository.registerUser(userId, email, passwordHash, name, birthDate)
                loadInitialData(userId)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(e.message ?: "회원 정보를 저장하지 못했습니다.")
            } finally {
                _isSyncing.value = false
            }
        }
    }

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
            viewModelScope.launch {
                repository.prePopulateSongsIfNeeded()
                _songs.value = repository.getAllSongsSync()
            }
            _syncMessage.value = "로그인 정보가 없습니다."
        }
    }

    // 💡 [핵심 수정됨] 비어있던 selectSong을 구현하여 DB에서 파트 목록 4개를 가져옵니다!
    fun selectSong(songId: Long) {
        viewModelScope.launch {
            repository.getSongParts(songId).collect { parts ->
                _currentSongParts.value = parts
            }
        }
    }

    fun searchSongs(query: String) = repository.searchSongs(query)

    fun savePracticeResult(history: PracticeHistory) {
        viewModelScope.launch { repository.savePracticeResult(history) }
    }

    fun markPracticePartCompleted(userId: String, songId: Long, partNumber: Int, artistName: String) {
        viewModelScope.launch { repository.markPracticePartCompleted(userId, songId, partNumber, artistName) }
    }

    suspend fun getPracticeResultSummary(userId: String, jsonFileName: String): PracticeResultSummaryUiModel? {
        val history = repository.getHistoryByJsonFileName(userId, jsonFileName) ?: return null
        val song = repository.getAllSongsSync().firstOrNull { it.songId == history.songId }
        val songTitle = song?.titleKr ?: "곡 ${history.songId}"
        val bestScore = repository.getBestScore(userId, history.songId) ?: history.totalScore
        val achievements = _achievementProgress.value.sortedWith(
            compareByDescending<AchievementUiModel> { it.isUnlocked }
                .thenByDescending { it.progress }
        ).take(3)

        return PracticeResultSummaryUiModel(
            songTitle = songTitle,
            bestScore = bestScore,
            achievements = achievements
        )
    }

    fun clearSyncMessage() { _syncMessage.value = null }

    private fun remapTopPracticedChoreos() {
        val songs = _songs.value
        _topPracticedChoreos.value = topPracticedHistoryRowsCache.map { row ->
            val resolvedSong = resolveSongForTopChoreo(
                songs = songs,
                historySongId = row.songId,
                historyArtistName = row.artistName
            )
            val partName = resolvePartNameForTopChoreo(
                songId = resolvedSong?.songId ?: row.songId,
                partNumber = row.partNumber
            )

            TopPracticedChoreoUiModel(
                title = resolvedSong?.titleKr ?: "곡 ${row.songId}",
                artist = resolvedSong?.artistKr ?: row.artistName.ifBlank { "Unknown" },
                partName = partName
            )
        }
    }

    private fun resolveSongForTopChoreo(
        songs: List<Song>,
        historySongId: Long,
        historyArtistName: String
    ): Song? {
        val candidates = songs.filter { it.songId in historySongId..(historySongId + 2) }
        if (candidates.isEmpty()) return songs.firstOrNull { it.songId == historySongId }

        val normalizedArtist = historyArtistName.trim().lowercase(Locale.getDefault())
        if (normalizedArtist.isNotBlank()) {
            candidates.firstOrNull { candidate ->
                candidate.artistKr.lowercase(Locale.getDefault()).contains(normalizedArtist)
            }?.let { return it }
        }

        return candidates.firstOrNull { it.songId == historySongId } ?: candidates.firstOrNull()
    }

    private fun resolvePartNameForTopChoreo(songId: Long, partNumber: Int): String {
        return RealDataSource.getRealSongParts
            .firstOrNull { it.songId == songId && it.partNumber == partNumber }
            ?.partName
            ?: "파트$partNumber"
    }

    companion object {
        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    }
}
