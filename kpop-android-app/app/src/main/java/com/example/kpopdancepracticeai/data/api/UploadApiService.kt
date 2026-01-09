package com.example.kpopdancepracticeai.data.api

import com.example.kpopdancepracticeai.data.dto.PresignedUrlRequest
import com.example.kpopdancepracticeai.data.dto.PresignedUrlResponse
import com.example.kpopdancepracticeai.data.dto.AnalysisStatusRequest
import com.example.kpopdancepracticeai.data.dto.AnalysisStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UploadApiService {
    // Lambda와 연결된 API Gateway의 엔드포인트 경로를 입력하세요 (예: /upload-url)
    @POST("default/preSignedUrl")
    suspend fun getPresignedUrl(@Body request: PresignedUrlRequest): Response<PresignedUrlResponse>

    @POST("default/checkAnalysisStatus")
    suspend fun checkAnalysisStatus(@Body request: AnalysisStatusRequest): Response<AnalysisStatusResponse>
}