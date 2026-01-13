package com.example.kpopdancepracticeai.data.network

import com.example.kpopdancepracticeai.data.entity.UserStats
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 서버 통신 인터페이스
 * AWS API Gateway와 통신하여 유저 통계 및 연습 기록을 받아옵니다.
 */
interface ApiService {

    // 1. 사용자 통계 조회
    @GET("users/{userId}/stats")
    suspend fun getUserStats(@Path("userId") userId: String): Response<UserStats>

    // 2. 연습 기록 목록 조회 (Notion Section 8 데이터 수신)
    @GET("users/{userId}/history")
    suspend fun getPracticeHistories(@Path("userId") userId: String): Response<List<PracticeHistoryDto>>
}


data class PracticeHistoryDto(
    @SerializedName("result_id") val result_id: Long,
    @SerializedName("song_id") val song_id: Long,
    @SerializedName("song_title") val song_title: String?,
    @SerializedName("artist_name") val artist_name: String?,
    @SerializedName("part_number") val part_number: Int,
    @SerializedName("total_score") val total_score: Int,

    // --- Notion Section 8 핵심 분석 데이터 (추가됨) ---
    @SerializedName("grade") val grade: String?,
    @SerializedName("part_accuracies") val part_accuracies: Map<String, Int>?, // JSON Object -> Map 자동 변환
    @SerializedName("worst_points") val worst_points: List<String>?,           // JSON Array -> List 자동 변환

    @SerializedName("created_at") val created_at: Long?
)