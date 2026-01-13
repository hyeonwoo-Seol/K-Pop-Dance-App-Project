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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // [추가] ProfileScreen 호환을 위한 더미 데이터 (추후 실제 엔티티로 교체 필요)
    private val _achievements = MutableStateFlow<List<Any>>(emptyList())
    val achievements: StateFlow<List<Any>> = _achievements.asStateFlow()

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

    // [추가] 데이터 새로고침 (ProfileScreen에서 사용)
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

    // 연습 결과 저장
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