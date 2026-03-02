package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.UserStats
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _recentHistory = MutableStateFlow<List<PracticeHistory>>(emptyList())
    val recentHistory: StateFlow<List<PracticeHistory>> = _recentHistory.asStateFlow()

    private var currentUserId: String? = null

    // [수정] 화면 진입 시 호출되는 함수
    fun loadData(userId: String) {
        if (userId.isBlank()) return

        // 1. [핵심] 프로필 진입 시점까지의 사용 시간을 DB에 반영 (실시간 갱신 효과)
        viewModelScope.launch {
            try {
                repository.syncAppUsageTime(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 이미 같은 ID로 데이터를 보고 있다면 중복 구독 방지
        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            // 2. 통계 데이터(시간, 점수 등) 실시간 구독
            // DB 값이 변경되면(위의 syncAppUsageTime 덕분에) 자동으로 새 값이 emit 됩니다.
            repository.getUserStats(userId).collectLatest { stats ->
                _userStats.value = stats
            }
        }

        viewModelScope.launch {
            // 3. 최근 기록 실시간 구독
            repository.getRecentHistory(userId).collectLatest { history ->
                _recentHistory.value = history
            }
        }
    }

    // 명시적으로 시간을 갱신하고 싶을 때 사용하는 함수 (예: 새로고침 버튼)
    fun refreshAppUsageTime() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.syncAppUsageTime(userId)
        }
    }

    companion object {
        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                    return ProfileViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}