package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import kotlinx.coroutines.flow.Flow

/**
 * 연습 기록 접근 객체
 * 역할: 연습 결과 저장 및 동기화 대상 조회
 */
@Dao
interface HistoryDao {

    // 연습 결과 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PracticeHistory): Long

    // 특정 곡의 연습 기록 조회 (최신순)
    @Query("SELECT * FROM practice_history WHERE songId = :songId ORDER BY practiceDate DESC")
    fun getHistoryBySong(songId: String): Flow<List<PracticeHistory>>

    // 특정 유저의 모든 연습 기록 조회 (날짜 오름차순 - 그래프용)
    @Query("SELECT * FROM practice_history WHERE userId = :userId ORDER BY practiceDate ASC")
    fun getAllHistoryByUser(userId: String): Flow<List<PracticeHistory>>

    // 특정 유저의 전체 연습 정확도 평균 계산 (DB 자체 집계 함수 사용)
    @Query("SELECT AVG(accuracy) FROM practice_history WHERE userId = :userId")
    suspend fun getAverageAccuracy(userId: String): Double?

    // AWS로 전송되지 않은(미동기화) 데이터만 조회
    @Query("SELECT * FROM practice_history WHERE isSynced = 0")
    suspend fun getUnsyncedData(): List<PracticeHistory>

    // 동기화 완료 처리 (isSynced = true로 변경)
    @Query("UPDATE practice_history SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}