package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 업적 시스템 (Notion Section 2)
 */
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    @ColumnInfo(name = "achievement_id")
    val id: String, // 예: "ach_ive_10"

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "goal_count")
    val goalCount: Int,

    @ColumnInfo(name = "current_count")
    val currentCount: Int = 0,

    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false, // 해금 여부

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false, // 달성 여부

    @ColumnInfo(name = "achieved_date")
    val achievedDate: Long? = null
)