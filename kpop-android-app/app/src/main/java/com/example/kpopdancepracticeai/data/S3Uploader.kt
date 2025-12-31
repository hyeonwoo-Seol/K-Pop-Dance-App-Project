package com.example.kpopdancepracticeai.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File
import java.io.FileOutputStream

/**
 * T3-1: AWS S3 Multipart Upload 구현체
 * 기존 코드의 변경을 최소화하기 위해 독립적인 유틸리티 클래스로 구성함.
 */
class S3Uploader(private val context: Context) {

    // ⚠️ T1 팀에게 받은 정보로 교체 필수
    private val bucketName = "YOUR_S3_BUCKET_NAME"
    private val identityPoolId = "YOUR_COGNITO_IDENTITY_POOL_ID" // 예: ap-northeast-2:xxxx...
    private val region = Regions.AP_NORTHEAST_2 // 서울 리전

    private val transferUtility: TransferUtility by lazy {
        // 네트워크 연결 상태 감지 핸들러 초기화
        TransferNetworkLossHandler.getInstance(context)

        // Cognito 자격 증명 공급자 설정
        val credentialsProvider = CognitoCachingCredentialsProvider(
            context.applicationContext,
            identityPoolId,
            region
        )

        // S3 클라이언트 생성
        val s3Client = AmazonS3Client(credentialsProvider)

        // TransferUtility 빌드
        TransferUtility.builder()
            .context(context.applicationContext)
            .s3Client(s3Client)
            .build()
    }

    /**
     * 동영상 파일을 S3에 업로드합니다.
     * Android 13(API 33) 이상의 보안 정책을 준수하기 위해 Uri -> 임시 파일 변환 방식을 사용합니다.
     */
    fun uploadVideo(fileUri: Uri, s3Key: String, onComplete: () -> Unit, onError: (Exception?) -> Unit) {
        try {
            // 1. Uri -> 앱 전용 캐시 공간에 임시 파일로 복사
            val file = createTempFileFromUri(fileUri)

            // 2. 업로드 시작
            val observer = transferUtility.upload(
                bucketName,
                s3Key,
                file
            )

            // 3. 상태 리스너 등록
            observer.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    Log.d("S3Upload", "State Changed: $state")
                    if (state == TransferState.COMPLETED) {
                        Log.d("S3Upload", "업로드 성공: $s3Key")
                        // 업로드 완료 후 임시 파일 삭제하여 용량 확보
                        file.delete()
                        onComplete()
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    val percent = if (bytesTotal > 0) (bytesCurrent.toFloat() / bytesTotal) * 100 else 0f
                    Log.d("S3Upload", "Progress: ${percent.toInt()}%")
                }

                override fun onError(id: Int, ex: Exception?) {
                    Log.e("S3Upload", "업로드 실패", ex)
                    file.delete() // 실패 시에도 임시 파일 삭제
                    onError(ex)
                }
            })
        } catch (e: Exception) {
            onError(e)
        }
    }

    // ContentResolver를 통해 Uri의 스트림을 읽어 임시 파일 생성
    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("URI를 열 수 없습니다: $uri")

        // 캐시 디렉터리에 임시 파일 생성
        val tempFile = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.mp4")

        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile
    }
}