package com.example.kpopdancepracticeai.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kpopdancepracticeai.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// 서버 API에 요청할 파라미터를 담기 위한 데이터 클래스
data class VideoRequestInfo(
    val classId: Int,
    val filename: String
)

// UI에 전달할 다운로드 상태 데이터 클래스
data class DownloadUiState(
    val isDownloading: Boolean = false,
    val currentProgress: Int = 0,
    val currentVideoIndex: Int = 0,
    val totalVideos: Int = 0,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 앱 초기 실행 시 전문가 댄스 영상을 다운로드하는 ViewModel
 * API Gateway를 통해 Presigned URL을 발급받아 OkHttp 스트림으로 다운로드합니다.
 */
class VideoDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val targetDirectory = File(application.filesDir, "expert_videos")

    // 대용량 파일 다운로드를 위해 재사용할 OkHttp 클라이언트 인스턴스
    private val okHttpClient = OkHttpClient()

    private val videosToDownload = listOf(
        VideoRequestInfo(classId = 450, filename = "450_ryujin_1.mp4"),
        VideoRequestInfo(classId = 451, filename = "451_ditto_1.mp4"),
        VideoRequestInfo(classId = 452, filename = "452_hypeboy_1.mp4")
    )

    init {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        checkIfAlreadyDownloaded()
    }

    private fun checkIfAlreadyDownloaded() {
        val isDownloaded = prefs.getBoolean("is_expert_video_downloaded", false)
        if (isDownloaded) {
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun startDownload() {
        if (_uiState.value.isDownloading || _uiState.value.isFinished) return

        _uiState.update {
            it.copy(
                isDownloading = true,
                totalVideos = videosToDownload.size,
                currentVideoIndex = 0,
                errorMessage = null
            )
        }

        downloadNext(0)
    }

    private fun downloadNext(index: Int) {
        if (index >= videosToDownload.size) {
            Log.d("VideoDownloadVM", "모든 영상 다운로드 완료!")
            prefs.edit().putBoolean("is_expert_video_downloaded", true).apply()

            _uiState.update {
                it.copy(isDownloading = false, isFinished = true, currentProgress = 100)
            }
            return
        }

        val videoInfo = videosToDownload[index]
        val localFile = File(targetDirectory, videoInfo.filename)

        if (localFile.exists() && localFile.length() > 0L) {
            _uiState.update { it.copy(currentVideoIndex = index + 1) }
            downloadNext(index + 1)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. API 서버에 classId와 filename을 보내어 Presigned URL 획득 (Step 3 연동)
                val downloadUrl = fetchPresignedUrlFromApi(videoInfo.classId, videoInfo.filename)

                // 2. 발급받은 URL로 실제 파일 스트림 다운로드 (Step 4 연동)
                downloadFileFromUrl(downloadUrl, localFile) { percent ->
                    _uiState.update { it.copy(currentProgress = percent) }
                }

                // 3. 완료 후 메인 스레드로 돌아와 다음 파일 진행
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(currentVideoIndex = index + 1, currentProgress = 0) }
                    downloadNext(index + 1)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("VideoDownloadVM", "다운로드 실패", e)
                    // 예외 발생 시 불완전하게 다운로드된 파일이 있다면 삭제하여 데이터 오염을 방지합니다.
                    if (localFile.exists()) localFile.delete()

                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            errorMessage = "다운로드 중 오류가 발생했습니다: ${e.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    fun retryDownload() {
        startDownload()
    }

    /**
     * API Gateway를 호출하여 실제 다운로드 가능한 임시 URL을 받아오는 함수입니다.
     */
    private suspend fun fetchPresignedUrlFromApi(classId: Int, filename: String): String {
        val response = ApiClient.downloadService.getPresignedUrl(
            classId = classId,
            filename = filename
        )
        return response.download_url
    }

    /**
     * OkHttp를 이용하여 URL로부터 대용량 파일을 안전하게 스트리밍 다운로드하는 함수입니다.
     */
    private suspend fun downloadFileFromUrl(url: String, targetFile: File, onProgress: (Int) -> Unit) {
        // 네트워크 및 디스크 I/O 작업이므로 명시적으로 Dispatchers.IO를 보장합니다.
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()

            // OkHttp 클라이언트를 통해 요청 실행
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("서버 응답 실패: HTTP 상태 코드 ${response.code}")
            }

            // 본문(Body) 추출
            val body = response.body ?: throw IOException("응답 본문이 비어 있습니다.")
            val contentLength = body.contentLength()

            var bytesCopied = 0L
            var lastProgress = 0

            // 스트림 읽기/쓰기 시작. use 블록을 사용하여 작업 완료 시 안전하게 스트림을 닫아줍니다(메모리 누수 방지).
            body.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024) // 8KB(8192 바이트) 버퍼 지정
                    var bytes = inputStream.read(buffer)

                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        bytesCopied += bytes

                        // 전체 파일 크기를 알 수 있는 경우 진행률 계산
                        if (contentLength > 0) {
                            val currentProgress = ((bytesCopied * 100) / contentLength).toInt()

                            // 1% 단위로 변경되었을 때만 UI 업데이트를 호출하여 부하(Overhead)를 줄입니다.
                            if (currentProgress != lastProgress) {
                                lastProgress = currentProgress
                                onProgress(currentProgress)
                            }
                        }

                        // 다음 버퍼 읽기
                        bytes = inputStream.read(buffer)
                    }
                    outputStream.flush()
                }
            }
        }
    }
}