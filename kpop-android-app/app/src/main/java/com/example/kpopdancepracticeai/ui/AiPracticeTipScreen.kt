package com.example.kpopdancepracticeai.ui

import com.example.kpopdancepracticeai.ui.motion.rememberIosLikeFlingBehavior

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kpopdancepracticeai.KpopApplication
import com.example.kpopdancepracticeai.data.repository.AuthRepository
import com.example.kpopdancepracticeai.viewmodel.AiPracticeTipViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private enum class AiAppTarget(val label: String) {
    CHAT_GPT("ChatGPT"),
    GEMINI("Gemini")
}

private data class AiAppIntentSpec(
    val packages: List<String>,
    val deepLink: (String) -> Uri
)

private val chatGptSpec = AiAppIntentSpec(
    packages = listOf("com.openai.chatgpt"),
    deepLink = { encodedPrompt -> Uri.parse("https://chatgpt.com/?q=$encodedPrompt") }
)

private val geminiSpec = AiAppIntentSpec(
    // 일부 디바이스/배포 채널에서 패키지명이 달라질 수 있어 후보를 순차 탐색
    packages = listOf("com.google.android.apps.bard", "com.google.android.apps.gemini"),
    deepLink = { encodedPrompt -> Uri.parse("https://gemini.google.com/app?prompt=$encodedPrompt") }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPracticeTipScreen(
    paddingValues: PaddingValues,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as KpopApplication
    val repository = app.repository

    val viewModel: AiPracticeTipViewModel = viewModel(
        factory = AiPracticeTipViewModel.provideFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf(AiAppTarget.CHAT_GPT) }

    LaunchedEffect(Unit) {
        val authRepo = AuthRepository(context)
        authRepo.getCurrentUser()?.uid?.let { userId ->
            viewModel.load(userId)
        }
    }

    val appGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFDDE3FF),
            Color(0xFFF0E8FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appGradient)
            .padding(paddingValues)
    ) {
        LazyColumn(
            flingBehavior = rememberIosLikeFlingBehavior(),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                    Text("AI 연습 팁 보내기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            item {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedLabel = uiState.choreoOptions
                        .firstOrNull { it.key == uiState.selectedChoreoKey }
                        ?.label
                        ?: "안무를 선택하세요"

                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("안무 필터") },
                        trailingIcon = { TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.choreoOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.selectChoreoFilter(option.key)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            uiState.selectedSummary?.let { summary ->
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${summary.title} / 파트${summary.partNumber}", fontWeight = FontWeight.Bold)
                            Text("아티스트: ${summary.artist}")
                            Text("연습 횟수: ${summary.practiceCount}회")
                            Text("평균 점수: ${"%.1f".format(summary.avgScore)}점")
                            Text("최고 점수: ${summary.bestScore}점")
                            Text("최근 점수: ${summary.recentScores.joinToString(", ").ifBlank { "기록 없음" }}")
                            Text(
                                "약점 포인트: ${summary.topWeakPoints.joinToString { "${it.first}(${it.second})" }.ifBlank { "데이터 없음" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    AppPicker(
                        selected = selectedApp,
                        onSelected = { selectedApp = it }
                    )
                }

                item {
                    Button(
                        onClick = {
                            openExternalAiApp(
                                target = selectedApp,
                                prompt = uiState.aiPrompt,
                                context = context
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("선택한 앱으로 연습 팁 요청하기")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPicker(selected: AiAppTarget, onSelected: (AiAppTarget) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiAppTarget.entries.forEach { app ->
            Button(onClick = { onSelected(app) }) {
                Text(if (selected == app) "✓ ${app.label}" else app.label)
            }
        }
    }
}

private fun openExternalAiApp(target: AiAppTarget, prompt: String, context: Context) {
    if (prompt.isBlank()) {
        Toast.makeText(context, "보낼 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }

    val encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString())
    val packageManager = context.packageManager

    val spec = when (target) {
        AiAppTarget.CHAT_GPT -> chatGptSpec
        AiAppTarget.GEMINI -> geminiSpec
    }

    val installedPackage = spec.packages.firstOrNull { packageName ->
        runCatching {
            packageManager.getPackageInfo(packageName, 0)
        }.isSuccess
    }

    if (installedPackage == null) {
        Toast.makeText(context, "선택한 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        return
    }

    // 1) 딥링크 실행 시도
    val deepLinkIntent = Intent(Intent.ACTION_VIEW, spec.deepLink(encodedPrompt)).apply {
        setPackage(installedPackage)
    }

    if (deepLinkIntent.resolveActivity(packageManager) != null) {
        context.startActivity(deepLinkIntent)
        return
    }

    // 2) 텍스트 공유 인텐트 실행 시도
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, prompt)
        setPackage(installedPackage)
    }

    if (shareIntent.resolveActivity(packageManager) != null) {
        context.startActivity(shareIntent)
        return
    }

    // 3) 앱 런처 인텐트 실행 + 프롬프트 클립보드 복사
    val launchIntent = packageManager.getLaunchIntentForPackage(installedPackage)
    if (launchIntent != null) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AI Practice Prompt", prompt))
        Toast.makeText(context, "프롬프트를 클립보드에 복사했습니다. 앱에서 붙여넣기 해주세요.", Toast.LENGTH_LONG).show()
        context.startActivity(launchIntent)
        return
    }

    Toast.makeText(context, "앱 실행 인텐트를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
}
