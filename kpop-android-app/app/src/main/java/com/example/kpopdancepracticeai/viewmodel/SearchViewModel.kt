package com.example.kpopdancepracticeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.repository.SongDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Empty : SearchUiState
    data class Success(val songs: List<Song>) : SearchUiState
}

class SearchViewModel(
    private val repository: SongDataRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(250)
        .flatMapLatest { keyword ->
            if (keyword.isBlank()) {
                repository.getAllSongs().map { songs ->
                    if (songs.isEmpty()) SearchUiState.Empty else SearchUiState.Success(songs)
                }
            } else {
                repository.searchSongs(keyword).map { songs ->
                    if (songs.isEmpty()) SearchUiState.Empty else SearchUiState.Success(songs)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    fun updateQuery(value: String) {
        _query.value = value
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
