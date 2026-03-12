package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import kotlinx.coroutines.flow.Flow

data class TopPracticedHistoryRow(
    val songId: Long,
    val partNumber: Int,
    val artistName: String,
    val practiceCount: Int,
    val lastPracticedAt: String
)

@Dao
interface HistoryDao {

    // 테이블명: PracticeResults

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PracticeHistory)

    // 특정 유저의 모든 연습 기록 조회 (최신순: created_at 기준)
    @Query("SELECT * FROM PracticeResults WHERE user_uuid = :userId ORDER BY created_at DESC")
    fun getAllHistory(userId: String): Flow<List<PracticeHistory>>

    // 특정 유저의 최근 연습 기록 (홈 화면용, 5개 제한)
    @Query("SELECT * FROM PracticeResults WHERE user_uuid = :userId ORDER BY created_at DESC LIMIT 5")
    fun getRecentHistory(userId: String): Flow<List<PracticeHistory>>

    // 특정 곡에 대한 최고 점수 조회 (컬럼명 score -> total_score 변경)
    @Query("SELECT MAX(total_score) FROM PracticeResults WHERE user_uuid = :userId AND song_id = :songId")
    suspend fun getBestScore(userId: String, songId: Long): Int?

    @Query(
        """
        SELECT
            song_id AS songId,
            part_number AS partNumber,
            artist_name AS artistName,
            COUNT(*) AS practiceCount,
            MAX(created_at) AS lastPracticedAt
        FROM PracticeResults
        WHERE user_uuid = :userId
        GROUP BY song_id, part_number, artist_name
        ORDER BY practiceCount DESC, lastPracticedAt DESC
        LIMIT 3
        """
    )
    fun getTopPracticedHistoryRows(userId: String): Flow<List<TopPracticedHistoryRow>>
}
