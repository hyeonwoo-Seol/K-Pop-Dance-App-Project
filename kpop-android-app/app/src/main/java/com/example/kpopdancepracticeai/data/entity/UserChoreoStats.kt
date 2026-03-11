package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_choreo_stats")
data class UserChoreoStats(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_uuid")
    val userUuid: String,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "part_number")
    val partNumber: Int,

    @ColumnInfo(name = "practice_count")
    val practiceCount: Int,

    @ColumnInfo(name = "last_practiced_at")
    val lastPracticedAt: String
)
