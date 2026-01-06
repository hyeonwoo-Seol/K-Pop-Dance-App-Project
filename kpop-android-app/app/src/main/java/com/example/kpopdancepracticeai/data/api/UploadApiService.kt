package com.example.kpopdancepracticeai.data.api

import com.example.kpopdancepracticeai.data.dto.PresignedUrlRequest
import com.example.kpopdancepracticeai.data.dto.PresignedUrlResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UploadApiService {
    // Lambda와 연결된 API Gateway의 엔드포인트 경로를 입력하세요 (예: /upload-url)
    @POST("YOUR_API_GATEWAY_ENDPOINT_PATH")
    suspend fun getPresignedUrl(@Body request: PresignedUrlRequest): Response<PresignedUrlResponse>
}