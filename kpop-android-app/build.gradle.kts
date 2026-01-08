//build.gradle.kts(Project: kpopDancePracticeAI)
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Room Database를 위한 KSP 플러그인 추가
    // [수정됨] Kotlin 2.0.21 버전에 맞춰 호환되는 KSP 버전(2.0.21-1.0.28)으로 변경했습니다.
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false //firebase
}