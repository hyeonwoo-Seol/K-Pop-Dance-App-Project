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