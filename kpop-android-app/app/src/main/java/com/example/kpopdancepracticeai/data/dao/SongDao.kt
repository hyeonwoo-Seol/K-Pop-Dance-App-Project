package com.example.kpopdancepracticeai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.kpopdancepracticeai.data.entity.Song
import com.example.kpopdancepracticeai.data.entity.SongPart
import kotlinx.coroutines.flow.Flow

// 노래 및 파트 정보를 관리하는 DAO입니다.
@Dao
interface SongDao {

    // 모든 노래 목록 조회 (홈 화면, 노래 선택 화면용)
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    // 특정 노래 정보 조회
    @Query("SELECT * FROM songs WHERE song_id = :songId")
    suspend fun getSongById(songId: Long): Song?

    // 특정 노래의 모든 파트 조회 (단계별 학습용)
    @Query("SELECT * FROM song_parts WHERE song_id = :songId ORDER BY part_id ASC")
    fun getPartsBySongId(songId: Long): Flow<List<SongPart>>

    // 노래 검색 (제목 또는 아티스트)
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Song>>

    // 초기 데이터 세팅을 위한 Insert (관리자용)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongParts(parts: List<SongPart>)
}