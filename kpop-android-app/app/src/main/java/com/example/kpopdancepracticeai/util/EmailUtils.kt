package com.example.kpopdancepracticeai.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast

/**
 * 이메일 발송 관련 확장 함수
 */
fun Context.sendSupportEmail(
    recipient: String = "shw8128@tukorea.ac.kr", // 관리자 이메일 주소
    subject: String = "[Dance Practice App] 문의사항"
) {
    // 기기 정보 및 기본 양식 생성
    val deviceInfo = """
        
        --------------------------------------------------
        Device Model: ${Build.MODEL}
        OS Version: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        App Version: 1.0.0
        --------------------------------------------------
        문의 내용을 아래에 작성해 주세요.
        
    """.trimIndent()

    // 이메일 인텐트 생성 (mailto 스키마 사용으로 이메일 앱만 필터링)
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, deviceInfo)
    }

    try {
        // 이메일 앱 실행
        startActivity(intent)
    } catch (e: Exception) {
        // 이메일 앱이 없는 경우 예외 처리
        Toast.makeText(this, "이메일 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}