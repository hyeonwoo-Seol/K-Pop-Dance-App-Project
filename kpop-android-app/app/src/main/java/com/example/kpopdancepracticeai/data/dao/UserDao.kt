package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 통계 접근 객체
 * 역할: UserStats 테이블에 대한 CRUD 작업 정의
 */
@Dao
interface UserDao {

    // 사용자 통계 조회 (실시간 업데이트를 위해 Flow 반환)
    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    fun getUserStats(userId: String): Flow<UserStats?>

    // 1회성 조회 동기화 버튼 전용
    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    suspend fun getUserStatsOneShot(userId: String): UserStats?

    // 사용자 통계 저장 또는 업데이트 (충돌 시 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userStats: UserStats)

    // 총 연습 시간 누적 업데이트 (쿼리로 직접 계산하여 원자성 보장)
    @Query("UPDATE user_stats SET totalPracticeTimeSeconds = totalPracticeTimeSeconds + :addTime WHERE userId = :userId")
    suspend fun updateTotalTime(userId: String, addTime: Long)

    // 완료 곡 수 1 증가
    @Query("UPDATE user_stats SET completedSongCount = completedSongCount + 1 WHERE userId = :userId")
    suspend fun incrementSongCount(userId: String)

    // 사용자의 평균 정확도 업데이트
    @Query("UPDATE user_stats SET averageAccuracy = :newAverage WHERE userId = :userId")
    suspend fun updateAverageAccuracy(userId: String, newAverage: Double)
}