package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalysisViewModel(
    private val repository: AppRepository
) : ViewModel() {

    // UI 상태: 로딩 중, 에러, 데이터 표시 등
    data class StatisticsUiState(
        val totalPlayTimeStr: String = "0시간 0분",
        val completedCountsStr: String = "0곡 / 0파트",
        val avgAccuracyStr: String = "0%",
        val heatmapData: List<Int> = List(84) { 0 }, // 12주 * 7일 = 84칸
        val graphData: List<Float> = emptyList(), // 최근 7개 점수 (0.0 ~ 1.0)
        val graphLabels: List<String> = emptyList(), // 최근 7개 날짜 (MM/dd)
        val songMasteryData: List<Float> = emptyList(),
        val songMasteryLabels: List<String> = emptyList()
    )

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    // 화면 진입 시 호출
    fun loadStatistics(userId: String) {
        if (userId.isBlank()) return

        // [핵심] 화면 진입 시 현재까지 쌓인 시간을 DB에 반영 (실시간 갱신 효과)
        viewModelScope.launch {
            try {
                repository.syncAppUsageTime(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            // 1. 유저 통계 (누적 시간, 평균 정확도) 구독
            repository.getUserStats(userId).collectLatest { stats ->
                if (stats != null) {
                    val hours = stats.totalPlayTime / 3600
                    val minutes = (stats.totalPlayTime % 3600) / 60

                    _uiState.value = _uiState.value.copy(
                        totalPlayTimeStr = "${hours}시간 ${minutes}분",
                        completedCountsStr = "${stats.completedParts}파트 완료",
                        avgAccuracyStr = String.format("%.1f%%", stats.avgAccuracy)
                    )
                }
            }
        }

        viewModelScope.launch {
            // 2. 전체 연습 기록 (히트맵, 그래프용) 구독
            repository.getAllHistory(userId).collectLatest { historyList ->
                processHistoryData(historyList)
            }
        }
    }

    private fun processHistoryData(historyList: List<PracticeHistory>) {
        // --- A. 히트맵 데이터 처리 (최근 12주) ---
        val heatmapLevels = calculateHeatmap(historyList)

        // --- B. 성장 그래프 데이터 처리 (최근 7건) ---
        // historyList는 DB 쿼리에서 created_at DESC로 정렬되어 있다고 가정
        // 그래프는 왼쪽(과거) -> 오른쪽(최신)으로 그려야 하므로 take(7) 후 reverse() 합니다.
        val recent7 = historyList.take(7).reversed()

        val graphScores = recent7.map { it.totalScore / 100f } // 0.0 ~ 1.0 정규화
        val graphLabels = recent7.map { formatDate(it.createdAt) }

        _uiState.value = _uiState.value.copy(
            heatmapData = heatmapLevels,
            graphData = graphScores,
            graphLabels = graphLabels
        )
    }

    // 히트맵 레벨 계산 로직 (0: 없음, 1: 적음 ~ 4: 많음)
    private fun calculateHeatmap(historyList: List<PracticeHistory>): List<Int> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 날짜별 연습 횟수 Map 생성
        val frequencyMap = mutableMapOf<String, Int>()

        historyList.forEach { item ->
            try {
                // "2025-05-10 14:30:00" -> "2025-05-10"
                val dateStr = item.createdAt.substringBefore(" ")
                frequencyMap[dateStr] = (frequencyMap[dateStr] ?: 0) + 1
            } catch (e: Exception) {
                // 파싱 에러 무시
            }
        }

        // 오늘 기준으로 84일(12주) 전까지 순회하며 데이터 채우기
        val levels = IntArray(84)
        val calendar = Calendar.getInstance()

        // 오늘 날짜부터 역순으로 채울지, 과거부터 채울지에 따라 인덱스 방향 결정
        // 여기서는 [0]이 12주 전(가장 과거), [83]이 오늘(최신)이 되도록 설정
        for (i in 0 until 84) {
            val dateKey = sdf.format(calendar.time)
            val count = frequencyMap[dateKey] ?: 0

            // levels 배열의 뒤에서부터 채움 (오늘이 마지막 인덱스 83)
            val index = 83 - i

            val level = when {
                count == 0 -> 0
                count <= 1 -> 1
                count <= 3 -> 2
                count <= 5 -> 3
                else -> 4
            }
            levels[index] = level

            // 하루 전으로 이동
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return levels.toList()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "-"
        }
    }

    companion object {
        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
                    return AnalysisViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}