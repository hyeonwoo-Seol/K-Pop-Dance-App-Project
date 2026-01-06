package com.example.kpopdancepracticeai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kpopdancepracticeai.data.dao.AchievementDao
import com.example.kpopdancepracticeai.data.dao.HistoryDao
import com.example.kpopdancepracticeai.data.dao.UserDao
import com.example.kpopdancepracticeai.data.entity.Achievement
import com.example.kpopdancepracticeai.data.entity.PracticeHistory
import com.example.kpopdancepracticeai.data.entity.UserStats

/**
 * 앱 전체 데이터베이스 클래스
 * 역할: Entity 테이블 생성 및 DAO 접근 포인트 제공
 */
@Database(
    entities = [UserStats::class, PracticeHistory::class, Achievement::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // 위에서 만든 변환기 등록
abstract class AppDatabase : RoomDatabase() {

    // DAO 접근 함수 (추상 메서드)
    abstract fun userDao(): UserDao
    abstract fun historyDao(): HistoryDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 싱글톤 인스턴스 반환
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kpop_dance_db" // DB 파일 이름
                )
                    // 개발 단계에서는 메인 스레드 쿼리 허용이 편할 수 있으나,
                    // 실제 배포 시에는 반드시 코루틴(비동기)을 사용해야 하므로 여기선 넣지 않음.
                    .fallbackToDestructiveMigration() // 버전 변경 시 기존 데이터 날리고 새로 생성 (개발용 옵션)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}