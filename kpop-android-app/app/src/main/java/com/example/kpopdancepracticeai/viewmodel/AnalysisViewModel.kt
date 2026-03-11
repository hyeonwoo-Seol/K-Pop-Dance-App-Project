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

    data class ChoreoFilterOption(
        val key: String,
        val songId: Long,
        val partNumber: Int,
        val label: String
    )

    enum class TrendRange { RECENT_7_DAYS, FULL_1_MONTH }

    // UI 상태: 로딩 중, 에러, 데이터 표시 등
    data class StatisticsUiState(
        val totalPlayTimeStr: String = "0시간 0분",
        val completedCountsStr: String = "0곡 / 0파트",
        val avgAccuracyStr: String = "0%",
        val heatmapData: List<Int> = List(84) { 0 }, // 12주 * 7일 = 84칸
        val graphData: List<Float> = emptyList(), // 최근 7개 점수 (0.0 ~ 1.0)
        val graphLabels: List<String> = emptyList(), // 최근 7개 날짜 (MM/dd)
        val songMasteryData: List<Float> = emptyList(),
        val songMasteryLabels: List<String> = emptyList(),
        val choreoOptions: List<ChoreoFilterOption> = emptyList(),
        val selectedChoreoKey: String? = null,
        val selectedRange: TrendRange = TrendRange.RECENT_7_DAYS,
        val filteredTrendData: List<Float> = emptyList(),
        val filteredTrendLabels: List<String> = emptyList(),
        val bestGrade: String = "-",
        val topWorstPoints: List<Pair<String, Int>> = emptyList(),
        val filteredPracticeCount: Int = 0
    )

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var allHistoryCache: List<PracticeHistory> = emptyList()
    private var songMetaMap: Map<Long, Pair<String, String>> = emptyMap()

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
            repository.allSongs.collectLatest { songs ->
                songMetaMap = songs.associate { it.songId to (it.titleKr to it.artistKr) }
                recalculateChoreoInsights()
            }
        }

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
                allHistoryCache = historyList
                processHistoryData(historyList)
                recalculateChoreoInsights()
            }
        }
    }

    fun selectChoreoFilter(key: String) {
        _uiState.value = _uiState.value.copy(selectedChoreoKey = key)
        recalculateChoreoInsights()
    }

    fun updateTrendRange(range: TrendRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        recalculateChoreoInsights()
    }

    private fun recalculateChoreoInsights() {
        val grouped = allHistoryCache.groupBy {
            it.songId to it.partNumber
        }

        val options = grouped.keys.map { key ->
            val songMeta = songMetaMap[key.first]
            val songTitle = songMeta?.first ?: "곡 ${key.first}"
            val artist = songMeta?.second ?: "Unknown"
            val label = "$songTitle ($artist) 파트${key.second}"
            ChoreoFilterOption(
                key = "${key.first}_${key.second}",
                songId = key.first,
                partNumber = key.second,
                label = label
            )
        }.sortedBy { it.label }

        val selectedKey = _uiState.value.selectedChoreoKey?.takeIf { selected ->
            options.any { it.key == selected }
        } ?: options.firstOrNull()?.key

        val selectedOption = options.firstOrNull { it.key == selectedKey }
        val selectedHistory = if (selectedOption == null) {
            emptyList()
        } else {
            grouped[selectedOption.songId to selectedOption.partNumber] ?: emptyList()
        }

        val bestGrade = getBestGrade(selectedHistory)
        val topWorstPoints = selectedHistory
            .flatMap { it.worstPoints ?: emptyList() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        val days = when (_uiState.value.selectedRange) {
            TrendRange.RECENT_7_DAYS -> 7
            TrendRange.FULL_1_MONTH -> 30
        }

        val (trendData, trendLabels) = buildDailyAverageTrend(selectedHistory, days)

        _uiState.value = _uiState.value.copy(
            choreoOptions = options,
            selectedChoreoKey = selectedKey,
            filteredTrendData = trendData,
            filteredTrendLabels = trendLabels,
            bestGrade = bestGrade,
            topWorstPoints = topWorstPoints,
            filteredPracticeCount = selectedHistory.size
        )
    }

    private fun buildDailyAverageTrend(history: List<PracticeHistory>, days: Int): Pair<List<Float>, List<String>> {
        val byDate = history.groupBy { it.createdAt.substringBefore(" ") }
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(days - 1)) }

        val scores = mutableListOf<Float>()
        val labels = mutableListOf<String>()

        repeat(days) {
            val date = calendar.time
            val key = keyFormat.format(date)
            val list = byDate[key].orEmpty()
            val avg = if (list.isEmpty()) 0f else (list.map { it.totalScore }.average().toFloat() / 100f)

            scores += avg
            labels += labelFormat.format(date)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return scores to labels
    }

    private fun getBestGrade(history: List<PracticeHistory>): String {
        val priority = mapOf("S" to 5, "A" to 4, "B" to 3, "C" to 2, "F" to 1)
        return history
            .map { it.grade.uppercase(Locale.getDefault()) }
            .maxByOrNull { priority[it] ?: 0 }
            ?: "-"
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
