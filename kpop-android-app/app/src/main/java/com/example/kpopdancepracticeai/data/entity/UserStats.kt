package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 사용자 통계 (Notion Section 7 & MD 파일 반영 통합본)
 * 테이블명: user_statistics
 */
@Entity(
    tableName = "user_statistics",
    indices = [Index(value = ["user_uuid"], unique = true)]
)
data class UserStats(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "stat_id")
    val statId: Long = 0,

    @ColumnInfo(name = "user_uuid")
    val userUuid: String,

    @ColumnInfo(name = "app_level")
    val appLevel: Int = 1,

    @ColumnInfo(name = "current_exp")
    val currentExp: Long = 0,

    @ColumnInfo(name = "total_play_time")
    val totalPlayTime: Long = 0L,

    @ColumnInfo(name = "completed_parts")
    val completedParts: Int = 0,

    @ColumnInfo(name = "avg_accuracy")
    val avgAccuracy: Double = 0.0,

    @ColumnInfo(name = "badge_count")
    val badgeCount: Int = 0,

    @ColumnInfo(name = "lightstick_count")
    val lightstickCount: Int = 0,

    @ColumnInfo(name = "achievement_score")
    val achievementScore: Int = 0,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: String = ""
)