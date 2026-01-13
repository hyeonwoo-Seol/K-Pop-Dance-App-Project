package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import kotlinx.coroutines.flow.Flow

// 연습 기록을 관리하는 DAO입니다.
@Dao
interface HistoryDao {

    // 연습 결과 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PracticeHistory)

    // 특정 유저의 모든 연습 기록 조회 (최신순)
    @Query("SELECT * FROM practice_history WHERE user_uuid = :userId ORDER BY date DESC")
    fun getAllHistory(userId: String): Flow<List<PracticeHistory>>

    // 특정 유저의 최근 연습 기록 (홈 화면용, 5개 제한)
    @Query("SELECT * FROM practice_history WHERE user_uuid = :userId ORDER BY date DESC LIMIT 5")
    fun getRecentHistory(userId: String): Flow<List<PracticeHistory>>

    // 특정 곡에 대한 최고 점수 조회
    @Query("SELECT MAX(score) FROM practice_history WHERE user_uuid = :userId AND song_id = :songId")
    suspend fun getBestScore(userId: String, songId: Long): Int?
}