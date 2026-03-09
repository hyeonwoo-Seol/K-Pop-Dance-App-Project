package com.example.kpopdancepracticeai.data.repository

import com.example.kpopdancepracticeai.data.RealDataSource
import com.example.kpopdancepracticeai.data.dao.SongDao
import com.example.kpopdancepracticeai.data.entity.Song
import kotlinx.coroutines.flow.Flow

interface SongDataRepository {
    fun searchSongs(query: String): Flow<List<Song>>
    fun getAllSongs(): Flow<List<Song>>
    suspend fun prePopulateIfEmpty()
}

class RoomSongDataRepository(
    private val songDao: SongDao
) : SongDataRepository {
    override fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    override fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    override suspend fun prePopulateIfEmpty() {
        if (songDao.getSongCount() > 0) return
        songDao.insertSongs(RealDataSource.getRealSongs)
        songDao.insertSongParts(RealDataSource.getRealSongParts)
    }
}
