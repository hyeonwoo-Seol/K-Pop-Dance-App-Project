package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart
import com.example.kpopdancepracticeai.data.entity.User // [추가]
import com.example.kpopdancepracticeai.data.entity.UserStats
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// [추가] 로그인 상태 정의
sealed interface LoginState {
    object Idle : LoginState
    object Loading : LoginState
    object Success : LoginState // 기존 유저 -> 메인 화면 이동
    object NeedProfile : LoginState // 신규 유저 -> 프로필 입력 화면 이동
    data class Error(val message: String) : LoginState
}

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

    // [추가] ProfileScreen 호환을 위한 더미 데이터
    private val _achievements = MutableStateFlow<List<Any>>(emptyList())
    val achievements: StateFlow<List<Any>> = _achievements.asStateFlow()

    // [추가] 로그인 상태 관리
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // ----------------------------------------------------------------
    // [추가] 로그인 및 회원가입 관련 로직
    // ----------------------------------------------------------------

    // 1. 로그인 시도 (Firebase UID로 DB 조회)
    fun checkUserExists(uid: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val user = repository.getUserByUuid(uid)
                if (user != null) {
                    // 유저 정보가 있음 -> 로그인 성공 및 초기 데이터 로드
                    loadInitialData(uid)
                    _loginState.value = LoginState.Success
                } else {
                    // 유저 정보가 없음 -> 프로필 입력 필요
                    _loginState.value = LoginState.NeedProfile
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("로그인 확인 중 오류: ${e.message}")
            }
        }
    }

    // 2. 회원가입 완료 (유저 정보 저장)
    fun registerUser(user: User) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                repository.saveUser(user)
                // 저장 후 초기 데이터(통계 등) 세팅
                loadInitialData(user.userUuid)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("회원가입 저장 실패: ${e.message}")
            }
        }
    }

    // 로그인 상태 초기화 (화면 이동 후 호출)
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    // ----------------------------------------------------------------
    // 기존 기능 유지
    // ----------------------------------------------------------------

    // 앱 시작 시 또는 로그인 후 호출
    fun loadInitialData(userId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.fetchInitialData(userId)

                launch {
                    repository.getUserStats(userId).collect { stats ->
                        _userStats.value = stats
                    }
                }

                launch {
                    repository.allSongs.collect { songList ->
                        _songs.value = songList
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

    fun refreshData() {
        val userId = _userStats.value?.userUuid
        if (userId != null) {
            loadInitialData(userId)
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

    // 연습 결과 저장 (기존 함수)
    fun savePracticeResult(userId: String, songId: Long, partId: Long, score: Int, videoPath: String) {
        viewModelScope.launch {
            val history = PracticeHistory(
                userUuid = userId,
                songId = songId,
                partNumber = partId.toInt(),
                artistName = "Unknown",
                totalScore = score,
                grade = "PENDING",
                partAccuracies = emptyMap(),
                worstPoints = emptyList(),
                durationSec = 0.0,
                fps = 0.0,
                videoWidth = 0,
                videoHeight = 0,
                totalFrames = 0,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                fullJsonPath = "",
                userVideoPath = videoPath
            )
            repository.savePracticeResult(history)
        }
    }

    // [추가] 연습 결과 저장 (PracticeHistory 객체 직접 전달용 오버로딩)
    fun savePracticeResult(history: PracticeHistory) {
        viewModelScope.launch {
            repository.savePracticeResult(history)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // ViewModel Factory 정의
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