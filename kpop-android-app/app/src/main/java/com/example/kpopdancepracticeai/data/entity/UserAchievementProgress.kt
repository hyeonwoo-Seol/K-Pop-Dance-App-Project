package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


// 업적 상세 진행도 테이블 입니두

@Entity(tableName = "user_achievement_progress")
data class UserAchievementProgress(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "progress_id")
    val progressId: Long = 0,

    @ColumnInfo(name = "user_uuid")
    val userId: String,

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