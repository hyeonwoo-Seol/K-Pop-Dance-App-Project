package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    // 정렬 기준 명시 (title_kr 기준 오름차순 예시)
    @Query("SELECT * FROM songs ORDER BY title_kr ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE song_id = :songId")
    suspend fun getSongById(songId: Long): Song?

    @Query("SELECT * FROM song_parts WHERE song_id = :songId ORDER BY part_id ASC")
    fun getPartsBySongId(songId: Long): Flow<List<SongPart>>

    // 검색 쿼리 강화: 한국어/영어 제목 및 아티스트 모두 검색
    @Query("""
        SELECT * FROM songs 
        WHERE title_kr LIKE '%' || :query || '%' 
           OR title_en LIKE '%' || :query || '%' 
           OR artist_kr LIKE '%' || :query || '%' 
           OR artist_en LIKE '%' || :query || '%'
    """)
    fun searchSongs(query: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongParts(parts: List<SongPart>)
}