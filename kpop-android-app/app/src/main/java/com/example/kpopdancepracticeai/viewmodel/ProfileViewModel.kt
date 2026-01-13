package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.UserStats
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: AppRepository) : ViewModel() {

    // 사용자 통계 상태
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    // 최근 연습 기록 상태
    private val _recentHistory = MutableStateFlow<List<PracticeHistory>>(emptyList())
    val recentHistory: StateFlow<List<PracticeHistory>> = _recentHistory.asStateFlow()

    // 현재 로드된 사용자 ID (중복 로드 방지용)
    private var currentUserId: String? = null

    // ⭐️ [수정] 생성자가 아닌 이 함수를 통해 userId를 전달받아 데이터를 로드합니다.
    fun loadData(userId: String) {
        if (userId.isBlank()) return

        // 이미 같은 ID로 데이터를 보고 있다면 중복 호출 방지
        if (currentUserId == userId && _userStats.value != null) return
        currentUserId = userId

        viewModelScope.launch {
            // 1. 통계 데이터 실시간 구독
            repository.getUserStats(userId).collectLatest { stats ->
                _userStats.value = stats
            }
        }

        viewModelScope.launch {
            // 2. 최근 기록 실시간 구독
            repository.getRecentHistory(userId).collectLatest { history ->
                _recentHistory.value = history
            }
        }
    }

    // 앱 접속 시간 갱신 (화면 진입 시 호출 등)
    fun refreshAppUsageTime(userId: String) {
        viewModelScope.launch {
            repository.syncAppUsageTime(userId)
        }
    }
}