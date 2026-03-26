package com.example.kpopdancepracticeai.data.network

import com.example.kpopdancepracticeai.data.entity.UserStats
import com.google.gson.JsonElement
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

    // 3. 프로필 설정 > 최신 데이터 동기화 버튼에서만 호출할 사용자 원본 데이터
    @GET("users/{userId}")
    suspend fun getRemoteUser(@Path("userId") userId: String): Response<RemoteUserDto>

    @GET("users/{userId}/statistics")
    suspend fun getRemoteUserStatistics(@Path("userId") userId: String): Response<RemoteUserStatisticsDto>

    @GET("users/{userId}/achievements")
    suspend fun getRemoteAchievements(@Path("userId") userId: String): Response<List<RemoteAchievementDto>>
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

data class RemoteUserDto(
    @SerializedName("user_uuid") val userUuid: String,
    @SerializedName("login_id") val loginId: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("password_hash") val passwordHash: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("birth_date") val birthDate: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("dance_skill") val danceSkill: String?,
    @SerializedName("favorite_genres") val favoriteGenres: JsonElement?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("join_date") val joinDate: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?
)

data class RemoteUserStatisticsDto(
    @SerializedName("stat_id") val statId: Long?,
    @SerializedName("user_uuid") val userUuid: String,
    @SerializedName("total_play_time") val totalPlayTime: Long?,
    @SerializedName("completed_parts") val completedParts: Int?,
    @SerializedName("avg_accuracy") val avgAccuracy: Double?,
    @SerializedName("badge_count") val badgeCount: Int?,
    @SerializedName("lightstick_count") val lightstickCount: Int?,
    @SerializedName("achievement_score") val achievementScore: Int?,
    @SerializedName("last_updated") val lastUpdated: String?,
    @SerializedName("app_level") val appLevel: Int?,
    @SerializedName("current_exp") val currentExp: Long?
)

data class RemoteAchievementDto(
    @SerializedName("id") val id: String,
    @SerializedName("goal_count") val goalCount: Int?,
    @SerializedName("current_count") val currentCount: Int?,
    @SerializedName("is_unlocked") val isUnlocked: Boolean?,
    @SerializedName("achieved_at") val achievedAt: JsonElement?
)
