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

    // 특정 업적의 진행도 업데이트
    @Query("UPDATE achievements SET currentProgress = :progress, isCompleted = :isCompleted WHERE achievementId = :id")
    suspend fun updateProgress(id: String, progress: Int, isCompleted: Boolean)

    // 특정 업적 조회
    @Query("SELECT * FROM achievements WHERE achievementId = :id")
    suspend fun getAchievement(id: String): Achievement?
}