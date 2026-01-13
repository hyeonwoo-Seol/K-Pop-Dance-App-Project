package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_history")
data class PracticeHistory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "history_id")
    val historyId: Long = 0, // 기본값 0 지정으로 생성 시 생략 가능

    @ColumnInfo(name = "user_uuid")
    val userId: String,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "part_id")
    val partId: Long,

    @ColumnInfo(name = "score")
    val score: Int,

    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "video_path")
    val videoPath: String,

    // --- 추가 필드 (오류 해결을 위해 기본값 지정) ---
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "analysis_result_json")
    val analysisResultJson: String? = null,

    @ColumnInfo(name = "server_result_id")
    val serverResultId: String? = null,

    @ColumnInfo(name = "uploaded_url")
    val uploadedUrl: String? = null,

    @ColumnInfo(name = "analysis_status")
    val analysisStatus: String = "PENDING",

    @ColumnInfo(name = "duration_sec")
    val durationSec: Double = 0.0,

    @ColumnInfo(name = "fps")
    val fps: Double = 0.0,

    @ColumnInfo(name = "video_width")
    val videoWidth: Int = 0,

    @ColumnInfo(name = "video_height")
    val videoHeight: Int = 0,

    @ColumnInfo(name = "total_frames")
    val totalFrames: Int = 0
)