package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: Long,

    // [수정] 검색 및 다국어 지원을 위한 필드 추가
    @ColumnInfo(name = "title_kr") val titleKr: String,
    @ColumnInfo(name = "title_en") val titleEn: String,

    @ColumnInfo(name = "artist_kr") val artistKr: String,
    @ColumnInfo(name = "artist_en") val artistEn: String,

    // [추가] 필터링용 메타데이터
    @ColumnInfo(name = "artist_gender") val artistGender: String, // "Male", "Female", "Mixed"
    @ColumnInfo(name = "genre") val genre: String,         // "Dance", "Hip-hop"
    @ColumnInfo(name = "tempo") val tempo: String,         // "Fast", "Normal"

    @ColumnInfo(name = "difficulty") val difficulty: String, // "Easy", "Hard"

    @ColumnInfo(name = "cover_url") val coverUrl: String?,

    // 신곡 정렬용 발매일
    @ColumnInfo(name = "release_date") val releaseDate: String? // "YYYY-MM-DD"
)


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

    @ColumnInfo(name = "part_number") val partNumber: Int,

    @ColumnInfo(name = "part_name") val partName: String, // "Verse 1", "Chorus"

    @ColumnInfo(name = "duration_sec") val durationSec: Int,

    @ColumnInfo(name = "video_url") val videoUrl: String?,

    @ColumnInfo(name = "skeleton_url") val skeletonUrl: String?,

    @ColumnInfo(name = "start_time_ms") val startTimeMs: Long,

    @ColumnInfo(name = "end_time_ms") val endTimeMs: Long
)