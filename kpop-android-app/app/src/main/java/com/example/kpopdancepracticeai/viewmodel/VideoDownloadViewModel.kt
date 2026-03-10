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
import org.json.JSONArray
import org.json.JSONException

data class VideoRequestInfo(
    val s3Key: String,
    val localFilename: String = s3Key.substringAfterLast("/")
)

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
    private val missingVideoKeys = mutableListOf<String>()
    private var downloadedVideoCount = 0

    private val videosToDownload by lazy { loadVideoManifest() }
    private val manifestSignature by lazy { videosToDownload.joinToString("|") { it.s3Key } }

    init {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        checkIfAlreadyDownloaded()
    }

    private fun loadVideoManifest(): List<VideoRequestInfo> {
        val fallback = listOf(
            VideoRequestInfo(s3Key = "expert_videos/450/450_류진_1.mp4"),
            VideoRequestInfo(s3Key = "expert_videos/450/450_류진_2.mp4")
        )

        return try {
            val jsonText = getApplication<Application>().assets
                .open("expert_video_manifest.json")
                .bufferedReader()
                .use { it.readText() }

            val root = JSONArray(jsonText)
            val parsed = mutableListOf<VideoRequestInfo>()

            for (i in 0 until root.length()) {
                val item = root.optJSONObject(i) ?: continue

                val s3Key = item.optString("s3Key")
                if (s3Key.isNotBlank()) {
                    parsed.add(VideoRequestInfo(s3Key = s3Key))
                    continue
                }

                val classId = item.optString("classId")
                val files = item.optJSONArray("files") ?: continue
                if (classId.isBlank()) continue

                for (j in 0 until files.length()) {
                    val fileName = files.optString(j)
                    if (fileName.isNotBlank()) {
                        parsed.add(VideoRequestInfo(s3Key = "expert_videos/$classId/$fileName"))
                    }
                }
            }

            if (parsed.isEmpty()) fallback else parsed
        } catch (e: IOException) {
            Log.w("VideoDownloadVM", "expert_video_manifest.json 로드 실패, fallback 사용", e)
            fallback
        } catch (e: JSONException) {
            Log.w("VideoDownloadVM", "expert_video_manifest.json 파싱 실패, fallback 사용", e)
            fallback
        }
    }

    private fun checkIfAlreadyDownloaded() {
        val isDownloaded = prefs.getBoolean("is_expert_video_downloaded", false)
        val savedSignature = prefs.getString("expert_video_manifest_signature", null)
        val allFilesExist = videosToDownload.all { video ->
            val file = File(targetDirectory, video.localFilename)
            file.exists() && file.length() > 0L
        }

        if (isDownloaded && savedSignature == manifestSignature && allFilesExist) {
            _uiState.update { it.copy(isFinished = true) }
        } else {
            prefs.edit()
                .putBoolean("is_expert_video_downloaded", false)
                .putString("expert_video_manifest_signature", manifestSignature)
                .apply()
        }
    }

    fun startDownload() {
        if (_uiState.value.isDownloading || _uiState.value.isFinished) return
        downloadedVideoCount = 0
        missingVideoKeys.clear()

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
            if (downloadedVideoCount == 0) {
                val missingMessage = buildMissingKeyErrorMessage()
                Log.e("VideoDownloadVM", "다운로드 가능한 영상이 없음. $missingMessage")
                prefs.edit()
                    .putBoolean("is_expert_video_downloaded", false)
                    .putString("expert_video_manifest_signature", manifestSignature)
                    .apply()

                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isFinished = false,
                        currentProgress = 0,
                        errorMessage = missingMessage
                    )
                }
            } else {
                Log.d("VideoDownloadVM", "영상 다운로드 완료: $downloadedVideoCount/${videosToDownload.size}")
                if (missingVideoKeys.isNotEmpty()) {
                    Log.w(
                        "VideoDownloadVM",
                        "일부 영상 누락으로 스킵됨: ${missingVideoKeys.joinToString()}"
                    )
                }
                prefs.edit()
                    .putBoolean("is_expert_video_downloaded", true)
                    .putString("expert_video_manifest_signature", manifestSignature)
                    .apply()

                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isFinished = true,
                        currentProgress = 100,
                        errorMessage = null
                    )
                }
            }
            return
        }

        val videoInfo = videosToDownload[index]
        val localFile = File(targetDirectory, videoInfo.localFilename)

        if (localFile.exists() && localFile.length() > 0L) {
            downloadedVideoCount += 1
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
                downloadedVideoCount += 1

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

                    if ((e.localizedMessage ?: "").contains("HTTP 404")) {
                        missingVideoKeys.add(videoInfo.s3Key)
                        Log.w("VideoDownloadVM", "S3 key 없음, 다음 파일로 진행: ${videoInfo.s3Key}")
                        _uiState.update { it.copy(currentVideoIndex = index + 1, currentProgress = 0) }
                        downloadNext(index + 1)
                        return@withContext
                    }

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
        downloadedVideoCount = 0
        missingVideoKeys.clear()
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

    private fun buildMissingKeyErrorMessage(): String {
        if (missingVideoKeys.isEmpty()) {
            return "다운로드 가능한 영상이 없습니다. 관리자에게 문의해주세요."
        }

        val preview = missingVideoKeys.take(3).joinToString(", ")
        val remain = missingVideoKeys.size - 3
        val suffix = if (remain > 0) " 외 ${remain}개" else ""
        return "S3에 영상 파일이 없습니다. 키 확인 필요: $preview$suffix"
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
