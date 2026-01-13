package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 배지 (Notion Section 4 - MD 파일 반영)
 * 테이블명: badges
 */
@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "iconResName")
    val iconResName: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "isUnlocked")
    val isUnlocked: Boolean = false,

    @ColumnInfo(name = "obtainedAt")
    val obtainedAt: Long? = null
)