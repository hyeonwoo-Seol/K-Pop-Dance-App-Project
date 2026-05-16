package com.example.kpopdancepracticeai.data.network

import com.example.kpopdancepracticeai.data.entity.UserStats
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    // 3. 로컬 Room 사용자 데이터 -> AWS 동기화
    @POST("default/syncUserLocalData")
    suspend fun syncUserLocalData(@Body request: SyncUserLocalDataRequest): Response<SyncUserLocalDataResponse>
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

data class SyncUserLocalDataRequest(
    @SerializedName("user") val user: SyncUserDto,
    @SerializedName("user_stats") val userStats: SyncUserStatsDto,
    @SerializedName("achievements") val achievements: List<SyncAchievementDto>
)

data class SyncUserDto(
    @SerializedName("user_uuid") val userUuid: String,
    @SerializedName("login_id") val loginId: String,
    @SerializedName("email") val email: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("birth_date") val birthDate: String,
    @SerializedName("dance_skill") val danceSkill: String,
    @SerializedName("favorite_genres") val favoriteGenres: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("app_level") val appLevel: Int,
    @SerializedName("current_exp") val currentExp: Long,
    @SerializedName("join_date") val joinDate: String
)

data class SyncUserStatsDto(
    @SerializedName("user_uuid") val userUuid: String,
    @SerializedName("app_level") val appLevel: Int,
    @SerializedName("current_exp") val currentExp: Long,
    @SerializedName("total_play_time") val totalPlayTime: Long,
    @SerializedName("completed_parts") val completedParts: Int,
    @SerializedName("avg_accuracy") val avgAccuracy: Int,
    @SerializedName("badge_count") val badgeCount: Int,
    @SerializedName("lightstick_count") val lightstickCount: Int,
    @SerializedName("achievement_score") val achievementScore: Int,
    @SerializedName("last_updated") val lastUpdated: String
)

data class SyncAchievementDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("goal_count") val goalCount: Int,
    @SerializedName("reward_type") val rewardType: String,
    @SerializedName("reward_id") val rewardId: String,
    @SerializedName("current_count") val currentCount: Int,
    @SerializedName("is_unlocked") val isUnlocked: Boolean,
    @SerializedName("achieved_at") val achievedAt: Long?
)

data class SyncUserLocalDataResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("saved") val saved: SavedSummary? = null
)

data class SavedSummary(
    @SerializedName("user") val user: Boolean = false,
    @SerializedName("user_stats") val userStats: Boolean = false,
    @SerializedName("achievements_count") val achievementsCount: Int = 0
)
