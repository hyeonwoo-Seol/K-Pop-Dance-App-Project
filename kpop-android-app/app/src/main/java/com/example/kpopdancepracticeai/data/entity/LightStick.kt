package com.example.kpopdancepracticeai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 응원봉 (Notion Section 3 - MD 파일 반영)
 * 테이블명: light_sticks
 */
@Entity(tableName = "light_sticks")
data class LightStick(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    // [수정] MD 파일에 명시된 컬럼명 그대로 적용 (localImage_path)
    @ColumnInfo(name = "localImage_path")
    val localImagePath: String,

    @ColumnInfo(name = "artist")
    val artist: String,

    @ColumnInfo(name = "is_owned")
    val isOwned: Boolean = false,

    @ColumnInfo(name = "obtained_at")
    val obtainedAt: Long? = null
)