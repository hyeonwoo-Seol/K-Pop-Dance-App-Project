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
import java.util.Locale

class AiPracticeTipViewModel(
    private val repository: AppRepository
) : ViewModel() {

    data class ChoreoFilterOption(
        val key: String,
        val songId: Long,
        val partNumber: Int,
        val artistName: String,
        val label: String
    )

    data class SelectedChoreoSummary(
        val title: String,
        val artist: String,
        val partNumber: Int,
        val practiceCount: Int,
        val avgScore: Double,
        val bestScore: Int,
        val latestGrade: String,
        val avgDurationSec: Double,
        val topWeakPoints: List<Pair<String, Int>>,
        val recentScores: List<Int>
    )

    data class UiState(
        val choreoOptions: List<ChoreoFilterOption> = emptyList(),
        val selectedChoreoKey: String? = null,
        val selectedSummary: SelectedChoreoSummary? = null,
        val aiPrompt: String = "",
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var allHistoryCache: List<PracticeHistory> = emptyList()
    private var songMetaById: Map<Long, Pair<String, String>> = emptyMap()

    fun load(userId: String) {
        if (userId.isBlank()) return
        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            repository.allSongs.collectLatest { songs ->
                songMetaById = songs.associate { song ->
                    song.songId to (song.titleKr to song.artistKr)
                }
                recalculate()
            }
        }

        viewModelScope.launch {
            repository.getAllHistory(userId).collectLatest { history ->
                allHistoryCache = history
                recalculate()
            }
        }
    }

    fun selectChoreoFilter(key: String) {
        _uiState.value = _uiState.value.copy(selectedChoreoKey = key)
        recalculate()
    }

    private fun recalculate() {
        val grouped = allHistoryCache.groupBy { Triple(it.songId, it.partNumber, it.artistName) }

        val options = grouped.keys.map { key ->
            val songMeta = songMetaById[key.first]
            val title = songMeta?.first ?: "곡 ${key.first}"
            val artist = songMeta?.second ?: key.third.ifBlank { "Unknown" }
            ChoreoFilterOption(
                key = "${key.first}_${key.second}_${key.third}",
                songId = key.first,
                partNumber = key.second,
                artistName = key.third,
                label = "$title ($artist) 파트${key.second}"
            )
        }.sortedBy { it.label }

        val selectedKey = _uiState.value.selectedChoreoKey?.takeIf { selected ->
            options.any { it.key == selected }
        } ?: options.firstOrNull()?.key

        val selectedOption = options.firstOrNull { it.key == selectedKey }
        val selectedHistory = if (selectedOption == null) {
            emptyList()
        } else {
            grouped[Triple(selectedOption.songId, selectedOption.partNumber, selectedOption.artistName)].orEmpty()
        }

        val summary = selectedOption?.let { option ->
            buildSummary(option, selectedHistory)
        }

        _uiState.value = _uiState.value.copy(
            choreoOptions = options,
            selectedChoreoKey = selectedKey,
            selectedSummary = summary,
            aiPrompt = summary?.let(::buildPrompt) ?: "",
            isLoading = false
        )
    }

    private fun buildSummary(
        option: ChoreoFilterOption,
        history: List<PracticeHistory>
    ): SelectedChoreoSummary {
        val songMeta = songMetaById[option.songId]
        val title = songMeta?.first ?: "곡 ${option.songId}"
        val artist = songMeta?.second ?: option.artistName.ifBlank { "Unknown" }
        val avgScore = if (history.isEmpty()) 0.0 else history.map { it.totalScore }.average()
        val bestScore = history.maxOfOrNull { it.totalScore } ?: 0
        val latestGrade = history.firstOrNull()?.grade ?: "-"
        val avgDurationSec = if (history.isEmpty()) 0.0 else history.map { it.durationSec }.average()

        val topWeakPoints = history
            .flatMap { it.worstPoints ?: emptyList() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val recentScores = history.take(5).map { it.totalScore }

        return SelectedChoreoSummary(
            title = title,
            artist = artist,
            partNumber = option.partNumber,
            practiceCount = history.size,
            avgScore = avgScore,
            bestScore = bestScore,
            latestGrade = latestGrade,
            avgDurationSec = avgDurationSec,
            topWeakPoints = topWeakPoints,
            recentScores = recentScores
        )
    }

    private fun buildPrompt(summary: SelectedChoreoSummary): String {
        val weakPointStr = if (summary.topWeakPoints.isEmpty()) {
            "- 약점 포인트 데이터 없음"
        } else {
            summary.topWeakPoints.joinToString("\n") { "- ${it.first}: ${it.second}회" }
        }

        val recentScoresStr = if (summary.recentScores.isEmpty()) "기록 없음"
        else summary.recentScores.joinToString(", ")

        return """
            너는 K-POP 안무 코치야. 아래 사용자 데이터를 바탕으로 한국어로 개인화된 연습 팁을 제안해줘.
            
            [선택 안무]
            - 곡: ${summary.title}
            - 아티스트: ${summary.artist}
            
            [사용자 연습 통계]
            - 총 연습 횟수: ${summary.practiceCount}회
            - 평균 점수: ${"%.1f".format(Locale.getDefault(), summary.avgScore)}점
            - 최고 점수: ${summary.bestScore}점
            - 최근 등급: ${summary.latestGrade}
            - 평균 연습 길이: ${"%.1f".format(Locale.getDefault(), summary.avgDurationSec)}초
            - 최근 5회 점수: $recentScoresStr
            - 자주 틀리는 포인트:
            $weakPointStr
            
            [요청]
            1) 이 안무를 출 때 알면 좋은 팁을 알려줘.
            2) 가장 효과적인 교정 포인트 3가지를 우선순위와 함께 알려줘.
            3) 이 안무를 연습할 때 어떻게 해야 할지 설명해줘.
        """.trimIndent()
    }

    companion object {
        fun provideFactory(repository: AppRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AiPracticeTipViewModel(repository) as T
                }
            }
    }
}
