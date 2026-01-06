package com.example.kpopdancepracticeai.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Room Type Converters
 * 역할: Room이 이해할 수 없는 타입(List, Date 등)을 저장 가능한 타입(String, Long)으로 변환
 */
class Converters {
    private val gson = Gson()

    // --- 날짜(Date) <-> 타임스탬프(Long) 변환 ---
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // --- 리스트(List<String>) <-> JSON 문자열(String) 변환 ---
    // 예: ["badge1", "badge2"] <-> "[\"badge1\", \"badge2\"]"
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}