package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kpopdancepracticeai.data.entity.User
import com.example.kpopdancepracticeai.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // --- UserStats (기존 코드) ---
    @Query("SELECT * FROM user_statistics WHERE user_uuid = :userId")
    fun getUserStats(userId: String): Flow<UserStats?>

    @Query("SELECT * FROM user_statistics WHERE user_uuid = :userId")
    suspend fun getUserStatsOneShot(userId: String): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userStats: UserStats)

    @Query("UPDATE user_statistics SET total_play_time = total_play_time + :addTime, last_updated = :updatedAt WHERE user_uuid = :userId")
    suspend fun updateTotalTime(userId: String, addTime: Long, updatedAt: String)

    @Query("UPDATE user_statistics SET completed_parts = completed_parts + 1, last_updated = :updatedAt WHERE user_uuid = :userId")
    suspend fun incrementSongCount(userId: String, updatedAt: String)
    
    @Query("SELECT * FROM users WHERE user_uuid = :userId")
    fun getUserProfile(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE user_uuid = :userId")
    suspend fun getUserProfileOneShot(userId: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}