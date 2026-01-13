package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

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