package com.example.kpopdancepracticeai.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.util.AwsS3TransferManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

// UI에 전달할 다운로드 상태 데이터 클래스
data class DownloadUiState(
    val isDownloading: Boolean = false,     // 다운로드 진행 중 여부
    val currentProgress: Int = 0,           // 현재 영상의 다운로드 진행률 (0~100)
    val currentVideoIndex: Int = 0,         // 현재 다운로드 중인 영상의 인덱스 (0부터 시작)
    val totalVideos: Int = 0,               // 전체 다운로드해야 할 영상 개수
    val isFinished: Boolean = false,        // 모든 다운로드가 완료되었는지 여부
    val errorMessage: String? = null        // 에러 발생 시 출력할 메시지
)

/**
 * 앱 초기 실행 시 AWS S3에서 전문가 댄스 영상을 다운로드하는 ViewModel
 */
class VideoDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val transferManager = AwsS3TransferManager(application)

    // 다운로드 완료 상태를 로컬에 저장하기 위한 SharedPreferences
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // 다운로드할 영상들을 저장할 내부 디렉토리 경로 (앱 삭제 시 함께 지워집니다)
    private val targetDirectory = File(application.filesDir, "expert_videos")

    // TODO: AWS S3 버킷 내에 있는 실제 영상들의 Key(경로) 리스트로 수정해야 합니다.
    // (향후 AppRepository를 통해 DB에서 다운로드할 리스트를 가져오도록 확장할 수도 있습니다.)
    private val videosToDownload = listOf(
        "expert_videos/dynamite_expert.mp4",
        "expert_videos/ditto_expert.mp4",
        "expert_videos/hypeboy_expert.mp4"
    )

    init {
        // 폴더가 없으면 생성
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }

        // 이미 다운로드를 완료했는지 체크하여 UI 상태 업데이트
        checkIfAlreadyDownloaded()
    }

    /**
     * 이전에 이미 모든 다운로드를 마쳤는지 확인합니다.
     */
    private fun checkIfAlreadyDownloaded() {
        val isDownloaded = prefs.getBoolean("is_expert_video_downloaded", false)
        if (isDownloaded) {
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    /**
     * 다운로드를 시작하는 퍼블릭 함수입니다. 화면(Screen)에서 버튼 클릭 시 호출됩니다.
     */
    fun startDownload() {
        // 이미 진행 중이거나 끝났으면 무시
        if (_uiState.value.isDownloading || _uiState.value.isFinished) return

        _uiState.update {
            it.copy(
                isDownloading = true,
                totalVideos = videosToDownload.size,
                currentVideoIndex = 0,
                errorMessage = null
            )
        }

        // 첫 번째 영상부터 다운로드 시작
        downloadNext(0)
    }

    /**
     * 재귀적으로 다음 영상을 순차 다운로드합니다.
     */
    private fun downloadNext(index: Int) {
        if (index >= videosToDownload.size) {
            // 모든 다운로드 완료
            Log.d("VideoDownloadVM", "모든 영상 다운로드 완료!")

            // 다시 다운로드하지 않도록 SharedPreferences에 완료 상태 저장
            prefs.edit().putBoolean("is_expert_video_downloaded", true).apply()

            _uiState.update {
                it.copy(isDownloading = false, isFinished = true, currentProgress = 100)
            }
            return
        }

        val s3Key = videosToDownload[index]
        // S3 파일 이름만 추출하여 로컬 파일명으로 사용 (예: dynamite_expert.mp4)
        val fileName = s3Key.substringAfterLast("/")
        val localFile = File(targetDirectory, fileName)

        // 만약 파일이 이미 존재하고 크기가 0이 아니라면 건너뛰고 다음 파일 진행 (최적화)
        if (localFile.exists() && localFile.length() > 0) {
            _uiState.update { it.copy(currentVideoIndex = index + 1) }
            downloadNext(index + 1)
            return
        }

        // 실제 S3 다운로드 요청
        transferManager.downloadVideo(
            s3Key = s3Key,
            localFile = localFile,
            onProgress = { percent ->
                // UI 스레드 안전성을 위해 viewModelScope 내에서 StateFlow 업데이트
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { it.copy(currentProgress = percent) }
                }
            },
            onSuccess = {
                // 다운로드 성공 시 다음 인덱스 진행
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { it.copy(currentVideoIndex = index + 1, currentProgress = 0) }
                    downloadNext(index + 1)
                }
            },
            onError = { exception ->
                // 에러 발생 시 UI 상태 업데이트
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            errorMessage = "다운로드 중 오류가 발생했습니다: ${exception.localizedMessage}"
                        )
                    }
                }
            }
        )
    }

    /**
     * 다운로드 실패 후 재시도할 때 사용합니다.
     */
    fun retryDownload() {
        startDownload()
    }
}