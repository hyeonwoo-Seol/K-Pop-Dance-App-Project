package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.UserChoreoStats
import kotlinx.coroutines.flow.Flow

data class RecentChoreoRow(
    val songId: Long,
    val partNumber: Int,
    val practiceCount: Int,
    val lastPracticedAt: String,
    val titleKr: String,
    val artistKr: String,
    val coverUrl: String?
)

data class TopPracticedChoreoRow(
    val titleKr: String,
    val totalPracticeCount: Int
)

@Dao
interface UserChoreoStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserChoreoStats)

    @Query("SELECT * FROM user_choreo_stats WHERE user_uuid = :userId AND song_id = :songId AND part_number = :partNumber LIMIT 1")
    suspend fun getOne(userId: String, songId: Long, partNumber: Int): UserChoreoStats?

    @Query(
        """
        SELECT
            s.song_id AS songId,
            u.part_number AS partNumber,
            u.practice_count AS practiceCount,
            u.last_practiced_at AS lastPracticedAt,
            s.title_kr AS titleKr,
            s.artist_kr AS artistKr,
            s.cover_url AS coverUrl
        FROM user_choreo_stats u
        INNER JOIN songs s ON s.song_id = u.song_id
        WHERE u.user_uuid = :userId
          AND u.last_practiced_at IS NOT NULL
          AND u.last_practiced_at != ''
        ORDER BY u.last_practiced_at DESC
        LIMIT 4
        """
    )
    fun getRecentChoreoRows(userId: String): Flow<List<RecentChoreoRow>>

    @Query(
        """
        SELECT
            s.title_kr AS titleKr,
            SUM(u.practice_count) AS totalPracticeCount
        FROM user_choreo_stats u
        INNER JOIN songs s ON s.song_id = u.song_id
        WHERE u.user_uuid = :userId
        GROUP BY s.song_id, s.title_kr
        ORDER BY totalPracticeCount DESC, s.title_kr ASC
        LIMIT 3
        """
    )
    fun getTopPracticedChoreoRows(userId: String): Flow<List<TopPracticedChoreoRow>>
}
