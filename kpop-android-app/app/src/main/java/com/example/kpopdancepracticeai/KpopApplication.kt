package com.example.kpopdancepracticeai

import android.app.Application
import com.example.kpopdancepracticeai.data.database.AppDatabase
import com.example.kpopdancepracticeai.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KpopApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy {
        AppDatabase.getDatabase(this)
    }

    // Repository 생성 부분 수정: songDao 추가
    val repository by lazy {
        AppRepository(
            userDao = database.userDao(),
            songDao = database.songDao(), // 이 부분이 추가되어야 합니다.
            historyDao = database.historyDao(),
            achievementDao = database.achievementDao(),
            userChoreoStatsDao = database.userChoreoStatsDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.prePopulateSongsIfNeeded()
        }
    }
}
