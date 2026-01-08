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
        // 1. Retrofit 초기화 (API Gateway 주소 설정)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://7v1ery3x1g.execute-api.ap-northeast-1.amazonaws.com/") // ⚠️ API Gateway 주소 입력
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(UploadApiService::class.java)
    }

    /**
     * 메인 업로드 함수
     */
    suspend fun uploadVideo(
        fileUri: Uri,
        filename: String, // 예: user01_song01_BTS_Part2.mp4
        onComplete: (String) -> Unit, // s3Key 반환
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 1. 임시 파일 생성 (Android 13+ 호환)
                val file = createTempFileFromUri(fileUri)

                // 2. Lambda에게 Pre-signed URL 요청
                Log.d("Upload", "URL 요청 시작: $filename")
                val requestPayload = PresignedUrlRequest(filename = filename)
                val response = apiService.getPresignedUrl(requestPayload)

                if (!response.isSuccessful || response.body() == null) {
                    throw Exception("URL 발급 실패: ${response.code()} ${response.errorBody()?.string()}")
                }

                val uploadUrl = response.body()!!.uploadUrl
                val s3Key = response.body()!!.s3Key
                Log.d("Upload", "URL 발급 완료. S3 업로드 시작...")

                // 3. 발급받은 URL로 파일 PUT 업로드 (OkHttp 사용)
                uploadFileToS3(uploadUrl, file)

                // 4. 완료 처리
                Log.d("Upload", "S3 업로드 성공!")
                file.delete() // 임시 파일 삭제

                // UI 업데이트(콜백)는 반드시 Main 스레드에서 실행
                withContext(Dispatchers.Main) {
                    onComplete(s3Key)
                }

            } catch (e: Exception) {
                Log.e("Upload", "업로드 실패", e)
                // 에러 콜백도 Main 스레드에서 실행
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    // OkHttp를 사용하여 S3로 직접 파일 전송
    private fun uploadFileToS3(url: String, file: File) {
        val client = OkHttpClient()

        // Lambda에서 설정한 Content-Type과 반드시 일치해야 함 (video/mp4)
        val mediaType = "video/mp4".toMediaTypeOrNull()
        val requestBody = file.asRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .put(requestBody) // PUT 메서드 중요
            .addHeader("Content-Type", "video/mp4") // 헤더 추가
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("S3 전송 실패: ${response.code} ${response.message}")
        }
    }

    // Uri -> File 변환 유틸리티 (기존 로직 재사용)
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
        onProgress: (String) -> Unit,  // ✅ 진행 상태 콜백 추가
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                var attempts = 0
                val maxAttempts = 60

                while (attempts < maxAttempts) {
                    val response = apiService.checkAnalysisStatus(
                        AnalysisStatusRequest(userId, timestamp)
                    )

                    if (!response.isSuccessful || response.body() == null) {
                        throw Exception("상태 확인 실패: ${response.code()}")
                    }

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
                            Log.d("Polling", "분석 중... ($attempts/$maxAttempts)")

                            // ✅ 진행 상태 콜백 호출
                            withContext(Dispatchers.Main) {
                                onProgress("분석 중... (${elapsed}초 경과)")
                            }

                            delay(5000)
                            attempts++
                        }
                    }
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