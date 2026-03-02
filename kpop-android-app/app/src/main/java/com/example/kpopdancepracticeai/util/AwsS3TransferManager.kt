package com.example.kpopdancepracticeai.util

import android.content.Context
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File

/**
 * AWS S3에서 전문가 댄스 영상을 다운로드하는 매니저 클래스입니다.
 * Cognito를 통해 안전하게 권한을 얻고, TransferUtility를 사용해 백그라운드 다운로드를 수행합니다.
 */
class AwsS3TransferManager(context: Context) {

    companion object {
        private const val TAG = "AwsS3TransferManager"

        // TODO: 현우님의 실제 AWS 환경에 맞게 아래 정보들을 수정해야 합니다.
        private const val COGNITO_IDENTITY_POOL_ID = "ap-northeast-2:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
        private val AWS_REGION = Regions.AP_NORTHEAST_2 // 예: 서울 리전
        private const val BUCKET_NAME = "현우님의-s3-버킷-이름"
    }

    private val applicationContext = context.applicationContext
    private var transferUtility: TransferUtility

    init {
        // 1. Cognito 자격 증명 풀 초기화 (안전하게 S3에 접근하기 위한 임시 키 발급)
        val credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            COGNITO_IDENTITY_POOL_ID,
            AWS_REGION
        )

        // 2. S3 클라이언트 생성
        val s3Client = AmazonS3Client(credentialsProvider)
        s3Client.setRegion(Region.getRegion(AWS_REGION))

        // 3. TransferUtility 초기화 (대용량 파일 전송 최적화 도구)
        transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .s3Client(s3Client)
            .build()
    }

    /**
     * S3에서 특정 파일을 다운로드합니다.
     *
     * @param s3Key S3 버킷 내의 파일 경로 (예: "expert_videos/dynamite_expert.mp4")
     * @param localFile 기기에 저장될 로컬 File 객체
     * @param onProgress 다운로드 진행률(0~100)을 반환하는 콜백
     * @param onSuccess 다운로드 완료 시 호출되는 콜백
     * @param onError 에러 발생 시 호출되는 콜백
     */
    fun downloadVideo(
        s3Key: String,
        localFile: File,
        onProgress: (Int) -> Unit,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Log.d(TAG, "다운로드 시작: $s3Key -> ${localFile.absolutePath}")

        // S3에서 파일 다운로드 지시
        val downloadObserver = transferUtility.download(BUCKET_NAME, s3Key, localFile)

        // 다운로드 상태를 추적하는 리스너 부착
        downloadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                Log.d(TAG, "상태 변경: $state")
                when (state) {
                    TransferState.COMPLETED -> {
                        Log.d(TAG, "다운로드 완료: $s3Key")
                        onSuccess(localFile)
                    }
                    TransferState.FAILED, TransferState.CANCELED -> {
                        onError(Exception("다운로드가 실패하거나 취소되었습니다. 상태: $state"))
                    }
                    else -> {
                        // IN_PROGRESS 등의 상태는 무시
                    }
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                if (bytesTotal > 0) {
                    val percent = ((bytesCurrent.toDouble() / bytesTotal) * 100).toInt()
                    // UI 스레드가 아닐 수 있으므로 ViewModel에서 처리하도록 넘김
                    onProgress(percent)
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                Log.e(TAG, "다운로드 에러", ex)
                onError(ex ?: Exception("알 수 없는 S3 다운로드 에러"))
            }
        })
    }
}