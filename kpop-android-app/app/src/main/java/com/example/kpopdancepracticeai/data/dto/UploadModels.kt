package com.example.kpopdancepracticeai.data.dto

import com.google.gson.annotations.SerializedName

// 1. 요청: Lambda에게 보낼 데이터
data class PresignedUrlRequest(
    @SerializedName("filename") val filename: String,
    @SerializedName("content_type") val contentType: String = "video/mp4"
)

// 2. 응답: Lambda에게 받을 데이터
data class PresignedUrlResponse(
    @SerializedName("upload_url") val uploadUrl: String,
    @SerializedName("s3_key") val s3Key: String,
    @SerializedName("filename") val filename: String
)

//  3. 분석 상태 확인 요청
data class AnalysisStatusRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("timestamp") val timestamp: Long
)

//  4. 분석 상태 확인 응답
data class AnalysisStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("result_s3_key") val resultS3Key: String?,
    @SerializedName("error_message") val errorMessage: String?
)

//  5. 회원 탈퇴(사용자 데이터 삭제) 요청
data class WithdrawalRequest(
    @SerializedName("user_id") val userId: String
)

//  6. 회원 탈퇴(사용자 데이터 삭제) 응답
data class WithdrawalResponse(
    @SerializedName("message") val message: String,
    @SerializedName("deleted_s3_objects") val deletedS3Objects: Int = 0,
    @SerializedName("deleted_dynamodb_items") val deletedDynamodbItems: Int = 0
)
