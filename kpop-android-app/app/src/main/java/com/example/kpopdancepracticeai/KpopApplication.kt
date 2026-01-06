package com.example.kpopdancepracticeai

import android.app.Application
import com.example.kpopdancepracticeai.data.database.AppDatabase
import com.example.kpopdancepracticeai.data.repository.AppRepository

/**
 * 앱의 진입점 (Application Class)
 * 역할: 앱 실행 시 딱 한 번만 실행되며, 전역적으로 사용할 DB와 Repository를 생성하고 관리합니다.
 * 주의: 이 클래스를 만든 후 반드시 AndroidManifest.xml의 <application> 태그에 android:name 속성을 추가해야 합니다.
 */
class KpopApplication : Application() {

    // 1. 데이터베이스 인스턴스 (Lazy: 처음 사용할 때 생성됨)
    val database by lazy {
        AppDatabase.getDatabase(this)
    }

    // 2. 저장소(Repository) 인스턴스
    // ViewModel에서 이 인스턴스를 가져다 사용합니다.
    val repository by lazy {
        AppRepository(
            userDao = database.userDao(),
            historyDao = database.historyDao(),
            achievementDao = database.achievementDao()
        )
    }
}