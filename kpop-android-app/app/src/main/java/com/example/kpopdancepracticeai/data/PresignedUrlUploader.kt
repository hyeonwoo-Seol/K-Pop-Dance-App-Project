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

/**
 * [PresignedUrlUploader]
 * 영상을 S3에 업로드하고, AI 분석 상태를 실시간으로 확인(Polling)하며,
 * 최종 결과를 다운로드하여 핸드폰 내부 저장소에 보관하는 핵심 클래스입니다.
 */
class PresignedUrlUploader(private val context: Context) {

    private val apiService: UploadApiService

    init {
        // Retrofit 설정: AWS API Gateway 주소를 기본으로 설정합니다.
        val retrofit = Retrofit.Builder()
            .baseUrl("https://aujfpfdg6e.execute-api.ap-northeast-1.amazonaws.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(UploadApiService::class.java)
    }

    // ==========================================
    // 1. 영상 업로드 관련 함수들
    // ==========================================

    /**
     * 사용자가 선택한 영상을 S3에 업로드하는 메인 루틴
     */
    suspend fun uploadVideo(
        fileUri: Uri,
        filename: String,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // [Step 1] Uri로부터 실제 업로드 가능한 임시 파일 생성
                val file = createTempFileFromUri(fileUri)

                // [Step 2] API 서버에 S3 업로드용 '임시 허가 주소(Presigned URL)' 요청
                val requestPayload = PresignedUrlRequest(filename = filename)
                val response = apiService.getPresignedUrl(requestPayload)

                if (!response.isSuccessful || response.body() == null) {
                    throw Exception("URL 발급 실패: ${response.code()}")
                }

                val responseBody = response.body()!!
                val uploadUrl = responseBody.uploadUrl
                val s3Key = responseBody.s3Key // 나중에 DB 조회 시 기준이 되는 키값

                // [Step 3] 발급받은 URL을 이용해 S3로 직접 파일 전송
                uploadFileToS3(uploadUrl, file)

                // 업로드 성공 후 캐시 파일 삭제
                file.delete()

                withContext(Dispatchers.Main) {
                    onComplete(s3Key)
                }

            } catch (e: Exception) {
                Log.e("Upload", "업로드 실패", e)
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    /**
     * OkHttp를 사용하여 S3에 바이너리 데이터(영상)를 직접 PUT 방식으로 전송
     */
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
        if (!response.isSuccessful) throw Exception("S3 전송 실패: ${response.code}")
    }

    /**
     * 시스템 URI로부터 앱 내부 캐시 디렉토리에 임시 .mp4 파일 생성
     */
    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("URI 열기 실패")
        val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.mp4")
        FileOutputStream(tempFile).use { outputStream -> inputStream.copyTo(outputStream) }
        return tempFile
    }

    // ==========================================
    // 2. 분석 상태 확인 (Polling) 관련 함수
    // ==========================================

    /**
     * AI 분석이 완료되었는지 서버에 주기적으로 물어보는 함수
     */
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
                val maxAttempts = 60 // 최대 5분 대기

                // 업로드 직후 서버 처리를 기다리기 위한 초기 지연 시간
                delay(5000)

                while (attempts < maxAttempts) {
                    val response = apiService.checkAnalysisStatus(AnalysisStatusRequest(userId, timestamp))

                    // [404 처리] 아직 서버가 DB에 항목을 만들기 전이면 기다림
                    if (response.code() == 404) {
                        withContext(Dispatchers.Main) { onProgress("서버 응답 대기 중...") }
                    }
                    // [200 성공] 상태값이 정상적으로 올 경우
                    else if (response.isSuccessful && response.body() != null) {
                        val status = response.body()!!
                        when (status.status) {
                            "completed" -> { // 분석 완료 시 결과 파일 경로(Key) 반환
                                withContext(Dispatchers.Main) { onComplete(status.resultS3Key ?: "") }
                                return@withContext
                            }
                            "failed" -> throw Exception("분석 실패: ${status.errorMessage}")
                            "processing", "uploaded" -> {
                                withContext(Dispatchers.Main) { onProgress("분석 중... (${attempts * 5}초 경과)") }
                            }
                        }
                    } else {
                        throw Exception("서버 에러 발생: ${response.code()}")
                    }

                    // 5초 간격으로 재시도
                    delay(5000)
                    attempts++
                }
                throw Exception("타임아웃: 분석이 너무 오래 걸립니다")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    // ==========================================
    // 3. 결과 다운로드 및 로컬 저장 관련 함수
    // ==========================================

    /**
     * 분석 완료된 JSON 파일을 S3에서 다운로드하고 핸드폰 내부 저장소에 저장
     */
    suspend fun downloadResultJson(resultS3Key: String): String {
        return withContext(Dispatchers.IO) {
            val url = "https://kpop-dance-app-data.s3.ap-northeast-1.amazonaws.com/${resultS3Key}"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                // [Step 1] S3 URL로부터 JSON 텍스트 데이터 읽기
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }

                // [Step 2] S3 경로에서 순수 파일명만 추출
                val fileName = resultS3Key.split("/").last()

                // [Step 3] 추출된 파일명을 사용하여 핸드폰 내부 저장소에 파일 쓰기
                saveJsonToInternalStorage(fileName, jsonString)

                jsonString // UI에 표시하기 위해 JSON 텍스트 반환
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * 핸드폰의 내부 저장소(Internal Storage) 내 'analysis_results' 폴더에 파일 저장
     */
    private fun saveJsonToInternalStorage(fileName: String, jsonContent: String) {
        try {
            // 앱 전용 내부 파일 경로 설정 (/data/data/패키지명/files/analysis_results)
            val directory = File(context.filesDir, "analysis_results")

            // 폴더가 없으면 생성
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // 파일 스트림을 열어 JSON 내용 기록
            val file = File(directory, fileName)
            FileOutputStream(file).use { output ->
                output.write(jsonContent.toByteArray())
            }
            Log.d("Storage", "내부 저장소에 결과 파일 저장 완료: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("Storage", "파일 저장 중 오류 발생", e)
        }
    }
}