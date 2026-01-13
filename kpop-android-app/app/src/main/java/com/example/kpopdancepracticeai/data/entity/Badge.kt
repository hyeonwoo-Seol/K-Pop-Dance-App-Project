package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//뱃지 테이블

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // 예: badge_ive_master

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "icon_res_name")
    val iconResName: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false,

    @ColumnInfo(name = "obtained_at")
    val obtainedAt: Long? = null
)