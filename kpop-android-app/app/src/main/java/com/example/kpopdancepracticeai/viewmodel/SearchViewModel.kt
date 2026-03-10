package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.repository.SongDataRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Empty : SearchUiState
    data class Success(val songs: List<Song>) : SearchUiState
}

data class SearchFilters(
    val difficulty: String? = null,
    val artistGender: String? = null,
    val tempo: String? = null
)

class SearchViewModel(
    private val repository: SongDataRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(250)
        .flatMapLatest { keyword ->
            combine(repository.getAllSongs(), _filters) { songs, filters ->
                applyFilters(songs = songs, keyword = keyword, filters = filters)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun updateFilters(filters: SearchFilters) {
        _filters.value = filters
    }

    private fun applyFilters(
        songs: List<Song>,
        keyword: String,
        filters: SearchFilters
    ): SearchUiState {
        val normalizedKeyword = keyword.trim()

        val filteredSongs = songs.filter { song ->
            val matchesKeyword =
                normalizedKeyword.isBlank() ||
                        song.titleKr.contains(normalizedKeyword, ignoreCase = true) ||
                        song.titleEn.contains(normalizedKeyword, ignoreCase = true) ||
                        song.artistKr.contains(normalizedKeyword, ignoreCase = true) ||
                        song.artistEn.contains(normalizedKeyword, ignoreCase = true)

            val matchesDifficulty = filters.difficulty == null || song.difficulty == filters.difficulty
            val matchesArtist = filters.artistGender == null || song.artistGender == filters.artistGender
            val matchesTempo = filters.tempo == null || song.tempo == filters.tempo

            matchesKeyword && matchesDifficulty && matchesArtist && matchesTempo
        }

        return if (filteredSongs.isEmpty()) SearchUiState.Empty else SearchUiState.Success(filteredSongs)
    }

    companion object {
        fun provideFactory(repository: SongDataRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(repository) as T
                }
            }
    }
}
