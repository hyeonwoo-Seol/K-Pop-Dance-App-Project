//build.gradle.kts(Project: kpopDancePracticeAI)
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Room Database를 위한 KSP 플러그인 추가 (버전은 프로젝트의 Kotlin 버전에 맞춰 조정 필요, 여기선 1.9.0 기준 예시)
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}