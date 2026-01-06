package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.UserStats
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 앱 전체에서 공유하는 메인 뷰모델
 * 역할: Repository의 데이터를 UI가 관찰 가능한 형태(StateFlow)로 변환
 */
class MainViewModel(private val repository: AppRepository) : ViewModel() {

    // 현재 로그인한 사용자 ID (실제 앱에서는 로그인 정보에서 가져와야 함)
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

    // --- 2. 사용자 액션 처리 ---

    /**
     * 연습 결과 저장 요청
     * PracticeResultScreen에서 호출됨
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
                artistName = "Unknown", // 필요 시 파라미터로 받기
                partName = "Part 1",    // 필요 시 파라미터로 받기
                practiceDate = System.currentTimeMillis(),
                score = score,
                accuracy = accuracy,
                isSynced = false // 기본값 false (미동기화 상태)
            )
            repository.savePracticeResult(newHistory)
        }
    }

    /**
     * 앱 시작 시 초기 데이터 동기화
     */
    fun refreshData() {
        viewModelScope.launch {
            repository.fetchInitialData(currentUserId)
        }
    }
}

/**
 * ViewModel Factory
 * 역할: Repository를 주입받아 MainViewModel을 생성하는 공장 클래스
 * (Hilt 같은 DI 라이브러리를 안 쓸 때 필요)
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