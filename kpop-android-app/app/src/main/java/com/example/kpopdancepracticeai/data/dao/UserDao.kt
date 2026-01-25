package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.User
import com.example.kpopdancepracticeai.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // ----------------------------------------------------------------
    // [추가] User 테이블 관련 쿼리 (로그인/회원가입 연동용)
    // ----------------------------------------------------------------

    // 사용자 정보 저장 (회원가입 또는 정보 갱신)
    // OnConflictStrategy.REPLACE: 이미 있는 ID면 덮어씌움
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Firebase UID로 사용자 정보 조회 (로그인 시 확인용)
    @Query("SELECT * FROM users WHERE user_uuid = :userUuid")
    suspend fun getUser(userUuid: String): User?

    // ----------------------------------------------------------------
    // 기존 UserStats 관련 쿼리 (유지)
    // ----------------------------------------------------------------

    // 테이블명 변경 반영: user_stats -> user_statistics
    @Query("SELECT * FROM user_statistics WHERE user_uuid = :userId")
    fun getUserStats(userId: String): Flow<UserStats?>

    @Query("SELECT * FROM user_statistics WHERE user_uuid = :userId")
    suspend fun getUserStatsOneShot(userId: String): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userStats: UserStats)

    // 개별 필드 업데이트 쿼리 (필요 시 사용, Repository에서는 객체 전체 업데이트 권장)
    @Query("UPDATE user_statistics SET total_play_time = total_play_time + :addTime, last_updated = :updatedAt WHERE user_uuid = :userId")
    suspend fun updateTotalTime(userId: String, addTime: Long, updatedAt: String)

    @Query("UPDATE user_statistics SET completed_parts = completed_parts + 1, last_updated = :updatedAt WHERE user_uuid = :userId")
    suspend fun incrementSongCount(userId: String, updatedAt: String)
}