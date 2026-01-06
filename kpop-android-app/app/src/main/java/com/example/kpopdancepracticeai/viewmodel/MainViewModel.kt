package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.UserStats
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 앱 전체에서 공유하는 메인 뷰모델
 * 역할: Repository의 데이터를 UI가 관찰 가능한 형태(StateFlow)로 변환
 */
class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // 현재 로그인한 사용자 ID (실제 앱에서는 Auth 시스템에서 가져와야 함)
    private val currentUserId = "user_test_01"

    // --- 1. UI 상태 (StateFlow) ---

    // 사용자 통계: DB가 변경되면 자동으로 UI에 전파됨
    val userStats: StateFlow<UserStats?> = repository.getUserStatsStream(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // UI가 안 보일 땐 5초 뒤 구독 중단
            initialValue = null
        )

    // 업적 목록
    val achievements = repository.getAllAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // [3단계 추가] 동기화 로딩 상태 (true = 로딩 중)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // [3단계 추가] 동기화 결과 메시지 (Toast용)
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    // 메시지 초기화 (Toast 표시 후 호출)
    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // --- 2. 사용자 액션 처리 ---

    /**
     * 연습 결과 저장 요청
     */
    fun savePracticeResult(
        title: String,
        score: Int,
        accuracy: Float,
        songId: String
    ) {
        viewModelScope.launch {
            val newHistory = PracticeHistory(
                userId = currentUserId,
                songId = songId,
                songTitle = title,
                artistName = "Unknown",
                partName = "Part 1",
                practiceDate = System.currentTimeMillis(),
                score = score,
                accuracy = accuracy,
                isSynced = false // 기본값 false (미동기화 상태)
            )
            repository.savePracticeResult(newHistory)
        }
    }

    /**
     * [3단계 구현] 데이터 동기화 요청 (새로고침)
     * ProfileScreen의 '동기화' 버튼 클릭 시 호출
     */
    fun refreshData() {
        // 이미 동기화 중이라면 중복 실행 방지
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "서버에서 최신 데이터를 가져오는 중..."

            try {
                // Repository 호출 -> 서버 데이터 Fetch -> 로컬 DB Update
                repository.fetchInitialData(currentUserId)
                _syncMessage.value = "최신 데이터 동기화 완료!"
            } catch (e: Exception) {
                e.printStackTrace()
                _syncMessage.value = "동기화 실패: 네트워크 상태를 확인해주세요."
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

/**
 * ViewModel Factory
 */
class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}