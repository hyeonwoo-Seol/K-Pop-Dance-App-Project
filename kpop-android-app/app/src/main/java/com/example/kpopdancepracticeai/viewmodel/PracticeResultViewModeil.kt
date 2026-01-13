package com.example.kpopdancepracticeai.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.data.dto.AnalysisResultResponse
import com.example.kpopdancepracticeai.data.mapper.AnalysisMapper
import com.example.kpopdancepracticeai.data.repository.AppRepository
import com.example.kpopdancepracticeai.util.FilenameParser
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class PracticeResultViewModel(
    private val repository: AppRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState

    // 결과 파일(JSON)을 로드하고 DB에 저장하는 함수
    fun loadAndSaveResult(context: Context, jsonFileName: String, videoPath: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ResultUiState.Loading

                // 1. JSON 파일 읽기
                val file = File(context.filesDir, jsonFileName)
                if (!file.exists()) {
                    _uiState.value = ResultUiState.Error("분석 결과 파일을 찾을 수 없습니다.")
                    return@launch
                }

                val jsonString = file.readText()
                val response = Gson().fromJson(jsonString, AnalysisResultResponse::class.java)

                // 2. 파일명 파싱 (Metadata 추출)
                val metadata = FilenameParser.parse(jsonFileName)
                if (metadata == null) {
                    _uiState.value = ResultUiState.Error("파일 형식이 올바르지 않습니다.")
                    return@launch
                }

                // 3. Mapper를 통해 DB Entity로 변환
                val historyEntity = AnalysisMapper.mapToPracticeHistory(
                    analysisResult = response,
                    metadata = metadata,
                    videoPath = videoPath,
                    fullJsonPath = file.absolutePath
                )

                // 4. DB 저장 (Repository가 History 저장 및 UserStats 갱신 수행)
                repository.savePracticeResult(historyEntity)

                // 5. UI 업데이트 (로딩 완료)
                _uiState.value = ResultUiState.Success(response, videoPath)

            } catch (e: Exception) {
                Log.e("PracticeResultVM", "Error loading result", e)
                _uiState.value = ResultUiState.Error(e.message ?: "알 수 없는 오류 발생")
            }
        }
    }
}

// UI 상태 관리용 Sealed Class
sealed class ResultUiState {
    object Loading : ResultUiState()
    data class Success(val data: AnalysisResultResponse, val videoPath: String) : ResultUiState()
    data class Error(val message: String) : ResultUiState()
}