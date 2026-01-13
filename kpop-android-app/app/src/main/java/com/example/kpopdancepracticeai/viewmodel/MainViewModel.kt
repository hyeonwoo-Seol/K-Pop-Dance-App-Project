package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
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

    // 앱 시작 시 또는 로그인 후 호출
    fun loadInitialData(userId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Repository에 해당 함수들이 존재하는지 확인해주세요 (AppRepository.kt 업데이트 필요)
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
                // historyId는 기본값이 0이므로 생략 가능 (DB 저장 시 자동 증가)
                userId = userId,
                songId = songId,
                partId = partId,
                score = score,
                date = System.currentTimeMillis(),
                videoPath = videoPath,

                // 나머지 필드는 기본값 또는 초기값 설정
                isSynced = false,
                analysisStatus = "PENDING",
                durationSec = 0.0,
                fps = 0.0,
                videoWidth = 0,
                videoHeight = 0,
                totalFrames = 0
            )
            repository.savePracticeResult(history)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}