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

data class VideoRequestInfo(
    val classId: Int,
    val filename: String
) {
    val s3Key: String
        get() = "expert_videos/$classId/$filename"
}

data class DownloadUiState(
    val isDownloading: Boolean = false,
    val currentProgress: Int = 0,
    val currentVideoIndex: Int = 0,
    val totalVideos: Int = 0,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
)

class VideoDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val targetDirectory = File(application.filesDir, "expert_videos")
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
                currentProgress = 0,
                errorMessage = null
            )
        }

        downloadNext(0)
    }

    private fun downloadNext(index: Int) {
        if (index >= videosToDownload.size) {
            Log.d("VideoDownloadVM", "모든 영상 다운로드 완료")
            prefs.edit().putBoolean("is_expert_video_downloaded", true).apply()

            _uiState.update {
                it.copy(
                    isDownloading = false,
                    isFinished = true,
                    currentProgress = 100
                )
            }
            return
        }

        val videoInfo = videosToDownload[index]
        val localFile = File(targetDirectory, videoInfo.filename)

        if (localFile.exists() && localFile.length() > 0L) {
            _uiState.update { it.copy(currentVideoIndex = index + 1, currentProgress = 0) }
            downloadNext(index + 1)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("VideoDownloadVM", "Presigned URL 요청 key=${videoInfo.s3Key}")

                val downloadUrl = fetchPresignedUrlFromApi(videoInfo.s3Key)

                Log.d("VideoDownloadVM", "Presigned URL 발급 성공: $downloadUrl")

                downloadFileFromUrl(downloadUrl, localFile) { percent ->
                    _uiState.update { it.copy(currentProgress = percent) }
                }

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            currentVideoIndex = index + 1,
                            currentProgress = 0
                        )
                    }
                    downloadNext(index + 1)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("VideoDownloadVM", "다운로드 실패", e)

                    if (localFile.exists()) {
                        localFile.delete()
                    }

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
        _uiState.update {
            it.copy(
                isDownloading = false,
                isFinished = false,
                currentProgress = 0,
                currentVideoIndex = 0,
                errorMessage = null
            )
        }
        startDownload()
    }

    private suspend fun fetchPresignedUrlFromApi(s3Key: String): String {
        val response = ApiClient.downloadService.getPresignedUrl(s3Key)

        Log.d(
            "VideoDownloadVM",
            "Presign API 응답: key=${response.key}, expiresIn=${response.expiresIn}"
        )

        return response.url
    }

    private suspend fun downloadFileFromUrl(
        url: String,
        targetFile: File,
        onProgress: (Int) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("서버 응답 실패: HTTP ${response.code}")
                }

                val body = response.body ?: throw IOException("응답 본문이 비어 있습니다.")
                val contentLength = body.contentLength()

                var bytesCopied = 0L
                var lastProgress = 0

                body.byteStream().use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytes = inputStream.read(buffer)

                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            bytesCopied += bytes

                            if (contentLength > 0) {
                                val currentProgress = ((bytesCopied * 100) / contentLength).toInt()
                                if (currentProgress != lastProgress) {
                                    lastProgress = currentProgress
                                    onProgress(currentProgress)
                                }
                            }

                            bytes = inputStream.read(buffer)
                        }

                        outputStream.flush()
                    }
                }
            }
        }
    }
}