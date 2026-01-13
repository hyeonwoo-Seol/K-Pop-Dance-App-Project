package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 업적 진행도 상세 (Notion Section 7 - MD 파일 반영)
 * 테이블명: user_achievement_progress
 */
@Entity(tableName = "user_achievement_progress")
data class UserAchievementProgress(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "progress_id")
    val progressId: Long = 0,

    // [수정] userId -> user_uuid
    @ColumnInfo(name = "user_uuid")
    val userUuid: String,

    @ColumnInfo(name = "achievement_code")
    val achievementCode: String,

    @ColumnInfo(name = "current_step")
    val currentStep: Int,

    @ColumnInfo(name = "goal_step")
    val goalStep: Int,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    @ColumnInfo(name = "achieved_date")
    val achievedDate: String?
)