package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

/**
 * 업적 접근 객체
 * 역할: 업적 목록 조회 및 진행도 업데이트
 */
@Dao
interface AchievementDao {

    // 모든 업적 목록 조회
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    // 초기 업적 데이터 세팅용 (여러 개 동시 삽입)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Query("UPDATE achievements SET current_count = :progress, is_completed = :isCompleted, achieved_at = :achievedAt WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, isCompleted: Boolean, achievedAt: Long? = null)

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievement(id: String): Achievement?
}