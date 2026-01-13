package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 노래 메타데이터 (Notion Section 5)
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: Long,

    val title: String,
    val artist: String,
    val albumCoverUrl: String?,
    val difficulty: String // "Easy", "Hard", "Expert"
)

/**
 * 노래 파트 정보 (Notion Section 6)
 * Song 테이블과 외래키로 연결됨
 */
@Entity(
    tableName = "song_parts",
    foreignKeys = [ForeignKey(
        entity = Song::class,
        parentColumns = ["song_id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SongPart(
    @PrimaryKey
    @ColumnInfo(name = "part_id")
    val partId: Long,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "part_name")
    val partName: String, // "Verse 1", "Chorus"

    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,

    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long,

    @ColumnInfo(name = "preview_video_url")
    val previewVideoUrl: String? // 비교 영상 URL
)