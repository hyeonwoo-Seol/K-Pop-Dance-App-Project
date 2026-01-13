package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "PracticeResults")
data class PracticeHistory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "result_id")
    val resultId: Long = 0,

    @ColumnInfo(name = "user_uuid")
    val userUuid: String,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "part_number")
    val partNumber: Int,

    @ColumnInfo(name = "artist_name")
    val artistName: String,

    @ColumnInfo(name = "total_score")
    val totalScore: Int,

    @ColumnInfo(name = "grade")
    val grade: String,

    @ColumnInfo(name = "part_accuracies")
    val partAccuracies: Map<String, Int>?,

    @ColumnInfo(name = "worst_points")
    val worstPoints: List<String>?,

    @ColumnInfo(name = "duration_sec")
    val durationSec: Double,

    @ColumnInfo(name = "fps")
    val fps: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: String, // Date format String

    @ColumnInfo(name = "full_json_path")
    val fullJsonPath: String,

    @ColumnInfo(name = "user_video_path")
    val userVideoPath: String,

    @ColumnInfo(name = "video_width")
    val videoWidth: Int,

    @ColumnInfo(name = "video_height")
    val videoHeight: Int,

    @ColumnInfo(name = "total_frames")
    val totalFrames: Int
)