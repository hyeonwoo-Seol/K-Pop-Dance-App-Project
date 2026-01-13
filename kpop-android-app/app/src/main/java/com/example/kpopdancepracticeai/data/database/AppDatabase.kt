package com.example.kpopdancepracticeai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.SongDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.entity.*

// 수정 내역: Song::class, SongPart::class 주석 해제 및 SongDao 연결
@Database(
    entities = [
        UserStats::class,
        PracticeHistory::class,
        Achievement::class,
        User::class,
        Song::class,
        SongPart::class,
        LightStick::class,
        Badge::class,
        UserAchievementProgress::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun historyDao(): HistoryDao
    abstract fun achievementDao(): AchievementDao

    // SongDao 추가: 노래 데이터 접근을 위해 필수입니다.
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kpop_dance_db"
                )
                    .fallbackToDestructiveMigration() // 개발 중 스키마 변경 시 테이블 재생성
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}