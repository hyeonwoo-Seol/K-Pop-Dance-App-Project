package com.example.kpopdancepracticeai.ui.test

import com.example.kpopdancepracticeai.ui.motion.rememberIosLikeFlingBehavior

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kpopdancepracticeai.ui.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


@Composable
fun IntegrationTestScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Assets에 있는 JSON 파일 목록
    val jsonFiles = listOf(
        "kim889_540_원영_1_result.json",
        "kim889_540_원영_2_result.json",
        "kim889_540_원영_3_result.json",
        "kim889_540_원영_4_result.json"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState, flingBehavior = rememberIosLikeFlingBehavior()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("오버레이 통합 테스트", style = MaterialTheme.typography.headlineMedium)

        HorizontalDivider()

        Text("1. 환경 설정", style = MaterialTheme.typography.titleMedium)
        Text("Assets 폴더의 JSON 파일을 내부 저장소로 복사합니다.", style = MaterialTheme.typography.bodySmall)

        Button(
            onClick = {
                scope.launch {
                    val success = copyAssetsToInternalStorage(context, jsonFiles)
                    if (success) {
                        Toast.makeText(context, "파일 복사 완료!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "파일 복사 실패 (로그 확인)", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("테스트 데이터 세팅 (필수)")
        }

        HorizontalDivider()

        Text("2. 결과 화면 이동 테스트", style = MaterialTheme.typography.titleMedium)
        Text("각 JSON 파일명에 맞는 영상을 자동으로 매칭합니다.", style = MaterialTheme.typography.bodySmall)

        jsonFiles.forEachIndexed { index, jsonName ->
            // [수정] JSON 파일명 기반으로 비디오 파일명 자동 생성
            // 예: "kim889_540_원영_1_result.json" -> "kim889_540_원영_1.mp4"
            val videoName = jsonName.replace("_result.json", ".mp4")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Case ${index + 1}", style = MaterialTheme.typography.titleSmall)
                    Text("JSON: $jsonName", style = MaterialTheme.typography.bodySmall)
                    Text("Video: $videoName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            // 자동 매칭된 비디오 경로 사용
                            val videoPath = "asset:///$videoName"

                            val encodedJson = Screen.encodeArg(jsonName)
                            val encodedVideo = Screen.encodeArg(videoPath)

                            navController.navigate("practiceResult/$encodedJson/$encodedVideo")
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("결과 보기")
                    }
                }
            }
        }
    }
}

/**
 * Assets 폴더에 있는 JSON 파일들을 앱 내부 저장소(filesDir/analysis_results)로 복사합니다.
 * JsonResultLoader가 이 경로에서 파일을 읽기 때문입니다.
 */
suspend fun copyAssetsToInternalStorage(context: Context, fileNames: List<String>): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val assetManager = context.assets
            val targetDir = File(context.filesDir, "analysis_results")

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            fileNames.forEach { fileName ->
                try {
                    // Assets에서 읽기
                    val inputStream = assetManager.open(fileName)
                    // 내부 저장소에 쓰기
                    val outFile = File(targetDir, fileName)
                    val outputStream = FileOutputStream(outFile)

                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d("TestSetup", "Copied: $fileName to ${outFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e("TestSetup", "Failed to copy $fileName: ${e.message}")
                    // 파일 하나 실패해도 나머지는 시도
                }
            }
            true
        } catch (e: Exception) {
            Log.e("TestSetup", "Asset Copy Process Failed", e)
            false
        }
    }
}
