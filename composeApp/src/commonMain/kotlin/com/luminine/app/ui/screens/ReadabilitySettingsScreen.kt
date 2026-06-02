package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.data.settings.SettingsRepository
import com.luminine.app.model.FontScale
import com.luminine.app.model.LuminineSettings
import com.luminine.app.model.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun ReadabilitySettingsScreen(settingsRepo: SettingsRepository, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by settingsRepo.observe().collectAsState(initial = LuminineSettings())
    fun mutate(transform: (LuminineSettings) -> LuminineSettings) {
        scope.launch { settingsRepo.update(transform) }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("닫기") }
                Text("화면/가독성 설정", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("미리보기", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("오늘의 루틴", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("비타민C 1000mg · 아침 스트레칭 · 수분 섭취", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("테마", fontWeight = FontWeight.SemiBold)
            ThemeMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.themeMode == mode, onClick = { mutate { it.copy(themeMode = mode) } })
                    Text(
                        when (mode) {
                            ThemeMode.Light -> "라이트"
                            ThemeMode.Dark -> "다크"
                            ThemeMode.System -> "시스템"
                        },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("고대비 모드", fontWeight = FontWeight.SemiBold)
                Switch(checked = settings.highContrast, onCheckedChange = { checked -> mutate { it.copy(highContrast = checked) } })
            }
            Text("글자 크기", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontScale.entries.forEach { fs ->
                    FilterChip(
                        selected = settings.fontScale == fs,
                        onClick = { mutate { it.copy(fontScale = fs) } },
                        label = {
                            Text(
                                when (fs) {
                                    FontScale.Small -> "작게"
                                    FontScale.Normal -> "보통"
                                    FontScale.Large -> "크게"
                                    FontScale.ExtraLarge -> "아주 크게"
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
