package com.example.kpopdancepracticeai.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.kpopdancepracticeai.data.api.UploadApiService
import com.example.kpopdancepracticeai.data.dto.PresignedUrlRequest
import com.example.kpopdancepracticeai.data.dto.AnalysisStatusRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PresignedUrlUploader(private val context: Context) {

    private val apiService: UploadApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://aujfpfdg6e.execute-api.ap-northeast-1.amazonaws.com/")  // ✅ 유지
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(UploadApiService::class.java)
    }

    suspend fun uploadVideo(
        fileUri: Uri,
        filename: String,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val file = createTempFileFromUri(fileUri)

                Log.d("Upload", "URL 요청 시작: $filename")
                val requestPayload = PresignedUrlRequest(filename = filename)
                val response = apiService.getPresignedUrl(requestPayload)

                if (!response.isSuccessful || response.body() == null) {
                    throw Exception("URL 발급 실패: ${response.code()} ${response.errorBody()?.string()}")
                }

                // ✅ 수정: 한 번만 읽기
                val responseBody = response.body()!!
                // ⭐ 여기서 서버가 보낸 '진짜 데이터'를 확인합니다.
                val body = response.body()
                val rawJson = response.errorBody()?.string() ?: response.body().toString()
                Log.d("CHECK_DEBUG", "서버가 보낸 진짜 데이터: $rawJson")

                val uploadUrl = responseBody.uploadUrl
                val s3Key = responseBody.s3Key
                // [추가할 로그 2] 변수에 값이 제대로 담겼는지 확인합니다.
                Log.d("Upload", "추출된 uploadUrl: $uploadUrl")
                Log.d("Upload", "추출된 s3Key: $s3Key")

                Log.d("Upload", "URL 발급 완료. S3 업로드 시작...")
                Log.d("Upload", "uploadUrl: $uploadUrl")
                Log.d("Upload", "s3Key: $s3Key")

                uploadFileToS3(uploadUrl, file)

                Log.d("Upload", "S3 업로드 성공!")
                file.delete()

                withContext(Dispatchers.Main) {
                    onComplete(s3Key)
                }

            } catch (e: Exception) {
                Log.e("Upload", "업로드 실패", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private fun uploadFileToS3(url: String, file: File) {
        val client = OkHttpClient()

        val mediaType = "video/mp4".toMediaTypeOrNull()
        val requestBody = file.asRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader("Content-Type", "video/mp4")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("S3 전송 실패: ${response.code} ${response.message}")
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("URI 열기 실패")
        val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.mp4")
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile
    }

    suspend fun pollAnalysisResult(
        userId: String,
        timestamp: Long,
        onProgress: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                var attempts = 0
                val maxAttempts = 60

                /// 서버가 아직 DB에 데이터를 쓰기 전이라 404를 보내면,
                // 여기서 바로 Exception을 던지고(throw) catch 블록으로 가서 폴링이 종료됩니다.
                delay(5000)

                while (attempts < maxAttempts) {
                    val response = apiService.checkAnalysisStatus(
                        AnalysisStatusRequest(userId, timestamp)
                    )

                    // ✅ 수정된 로직: 404는 실패가 아니라 '대기'로 처리합니다.
                    if (response.code() == 404) {
                        Log.d("Polling", "아직 데이터 생성 전입니다... ($attempts/$maxAttempts)")
                        withContext(Dispatchers.Main) {
                            onProgress("서버 응답 대기 중...")
                        }
                    }
                    else if (response.isSuccessful && response.body() != null) {
                        val status = response.body()!!
                        when (status.status) {
                            "completed" -> {
                                Log.d("Polling", "분석 완료!")
                                withContext(Dispatchers.Main) {
                                    onComplete(status.resultS3Key ?: "")
                                }
                                return@withContext
                            }
                            "failed" -> {
                                throw Exception("분석 실패: ${status.errorMessage}")
                            }
                            "processing", "uploaded" -> {
                                val elapsed = attempts * 5
                                withContext(Dispatchers.Main) {
                                    onProgress("분석 중... (${elapsed}초 경과)")
                                }
                            }
                        }
                    }
                    else {
                        // 404가 아닌 다른 에러(500 등)인 경우에만 중단
                        throw Exception("서버 에러 발생: ${response.code()}")
                    }

                    // 다음 시도까지 대기
                    delay(5000)
                    attempts++
                }

                throw Exception("타임아웃: 분석이 너무 오래 걸립니다")

            } catch (e: Exception) {
                Log.e("Polling", "폴링 실패", e)
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
    suspend fun downloadResultJson(resultS3Key: String): String {
        return withContext(Dispatchers.IO) {
            val url = "https://kpop-dance-app-data.s3.ap-northeast-1.amazonaws.com/${resultS3Key}"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}