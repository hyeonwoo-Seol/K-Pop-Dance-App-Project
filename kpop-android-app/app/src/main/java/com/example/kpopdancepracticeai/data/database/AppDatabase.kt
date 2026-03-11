package com.example.kpopdancepracticeai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.SongDao
import com.example.kpopdancepracticeai.data.dao.UserChoreoStatsDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.entity.*

@Database(
    entities = [
        UserStats::class,
        PracticeHistory::class,
        Achievement::class,
        User::class,
        Song::class,
        SongPart::class,
        UserChoreoStats::class,
        LightStick::class,
        Badge::class,
        UserAchievementProgress::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun songDao(): SongDao
    abstract fun historyDao(): HistoryDao
    abstract fun userChoreoStatsDao(): UserChoreoStatsDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kpop_dance_database" // DB 이름
                )
                    // [중요] 스키마 버전 변경 시 마이그레이션 전략이 없으면 기존 DB를 파괴하고 재생성
                    // 개발 단계에서 스키마 충돌로 인한 앱 크래시를 방지합니다.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
