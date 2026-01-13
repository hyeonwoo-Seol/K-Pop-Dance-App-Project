package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 응원봉
@Entity(tableName = "light_sticks")
data class LightStick(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // 예: stick_ive_v1

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "local_image_path")
    val localImagePath: String,

    @ColumnInfo(name = "artist")
    val artist: String,

    @ColumnInfo(name = "is_owned")
    val isOwned: Boolean = false,

    @ColumnInfo(name = "obtained_at")
    val obtainedAt: Long? = null
)