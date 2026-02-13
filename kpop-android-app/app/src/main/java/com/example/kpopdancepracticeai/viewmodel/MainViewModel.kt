package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart
import com.example.kpopdancepracticeai.data.entity.UserStats
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // UI 상태 관리
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSongParts = MutableStateFlow<List<SongPart>>(emptyList())
    val currentSongParts: StateFlow<List<SongPart>> = _currentSongParts.asStateFlow()

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _recentHistory = MutableStateFlow<List<PracticeHistory>>(emptyList())
    val recentHistory: StateFlow<List<PracticeHistory>> = _recentHistory.asStateFlow()

    private val _achievements = MutableStateFlow<List<Any>>(emptyList())
    val achievements: StateFlow<List<Any>> = _achievements.asStateFlow()

    private var currentUserId: String? = null

    // 앱 시작 시 또는 로그인 후 호출
    fun loadInitialData(userId: String) {
        // 이미 같은 ID로 로드된 상태라면 중복 호출 방지
        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // 1. 초기 데이터 세팅 (DB 없을 시 생성)
                repository.fetchInitialData(userId)

                // 2. 유저 통계 실시간 구독
                launch {
                    repository.getUserStats(userId).collect { stats ->
                        _userStats.value = stats
                    }
                }

                // 3. 전체 곡 목록 실시간 구독
                launch {
                    repository.allSongs.collect { songList ->
                        _songs.value = songList
                    }
                }

                // 4. 최근 기록 실시간 구독
                launch {
                    repository.getRecentHistory(userId).collect { history ->
                        _recentHistory.value = history
                    }
                }

                // _syncMessage.value = "데이터 동기화 완료" // 자동 로드 시엔 메시지 생략
            } catch (e: Exception) {
                e.printStackTrace()
                _syncMessage.value = "동기화 실패: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // [수정] ProfileViewModel로 기능이 이관되었으나, 하위 호환성 및 다른 화면에서의 호출을 위해 유지
    // [개선] 외부에서 userId를 명시적으로 전달받을 수 있도록 변경 (로그인 정보 유실 시 대응)
    fun updateUsageTime(explicitUserId: String? = null) {
        // 1. 파라미터로 받은 ID, 2. 현재 로드된 통계의 ID, 3. 저장된 currentUserId 순으로 유효한 ID 탐색
        val userId = explicitUserId ?: _userStats.value?.userUuid ?: currentUserId

        if (userId != null) {
            viewModelScope.launch {
                repository.syncAppUsageTime(userId)
            }
        } else {
            // 디버깅을 위해 로그 출력 (사용자에게 보이지 않음)
            println("MainViewModel: updateUsageTime failed - No User ID found")
        }
    }

    // [핵심 수정] 외부에서 ID를 주입받아 갱신할 수 있도록 매개변수 추가
    fun refreshData(explicitUserId: String? = null) {
        // 1. 명시적 ID -> 2. 이미 로드된 통계의 ID -> 3. 내부 저장 ID 순으로 확인
        val userId = explicitUserId ?: _userStats.value?.userUuid ?: currentUserId

        if (userId != null && userId.isNotBlank()) {
            // ID를 찾았다면 현재 ID로 업데이트 (누락 방지)
            currentUserId = userId

            // 데이터 갱신 시 시간도 같이 동기화
            updateUsageTime(userId)

            // 데이터 재로딩
            loadInitialData(userId)
            _syncMessage.value = "데이터를 새로고침했습니다."
        } else {
            _syncMessage.value = "로그인 정보가 없습니다."
        }
    }

    // 노래 선택 시 파트 정보 로드
    fun selectSong(songId: Long) {
        viewModelScope.launch {
            repository.getSongParts(songId).collect { parts ->
                _currentSongParts.value = parts
            }
        }
    }

    // 검색 기능
    fun searchSongs(query: String) = repository.searchSongs(query)

    // 연습 결과 저장
    fun savePracticeResult(history: PracticeHistory) {
        viewModelScope.launch {
            repository.savePracticeResult(history)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // ViewModel Factory
    companion object {
        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    return MainViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}