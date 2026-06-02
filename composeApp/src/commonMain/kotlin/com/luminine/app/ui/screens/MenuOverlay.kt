package com.luminine.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.model.DailyRecord
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection
import com.luminine.app.ui.IconLabel
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.IconTile
import com.luminine.app.ui.components.LuminineIconView

// Full-screen overlay opened from the top-bar 전체 메뉴 (☰). Migrated from the former MenuScreen tab,
// plus a 화면/가독성 설정 entry. Colors route through MaterialTheme so it works in dark/high-contrast.
private val CardShape = RoundedCornerShape(20.dp)

private val serviceLinks = listOf(
    IconLabel("루틴 차트", LuminineIcon.Chart, "루틴 차트"),
    IconLabel("인바디 기록", LuminineIcon.Body, "인바디 기록"),
    IconLabel("사진 타임라인", LuminineIcon.Camera, "사진 타임라인"),
    IconLabel("실천지수 내역", LuminineIcon.Trophy, "실천지수 내역"),
    IconLabel("LUMÍNINE Shop", LuminineIcon.Shop, "루미닌 숍"),
    IconLabel("유어프라임", LuminineIcon.Sparkles, "유어프라임"),
    IconLabel("유튜브", LuminineIcon.Youtube, "유튜브"),
    IconLabel("카페", LuminineIcon.Cafe, "카페"),
)

@Composable
fun MenuOverlay(
    displayName: String,
    records: List<DailyRecord>,
    survey: SurveyResponse?,
    onOpenReadability: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) { Text("닫기") }
                    Text("전체 메뉴", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Card(shape = CardShape) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconTile(LuminineIcon.User, "회원", size = 48.dp, background = MaterialTheme.colorScheme.primary, tint = MaterialTheme.colorScheme.onPrimary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(displayName.ifBlank { "회원" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("실천지수 포인트 ${records.sumOf { it.score }}P", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { SurveySummaryCard(survey) }
            item { MenuLinkRow(IconLabel("화면/가독성 설정", LuminineIcon.Sparkles, "화면/가독성 설정"), onClick = onOpenReadability) }
            items(serviceLinks) { link -> MenuLinkRow(link, onClick = {}) }
            item {
                Card(shape = CardShape) {
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onLogout).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(LuminineIcon.Link, "로그아웃", size = 34.dp, background = MaterialTheme.colorScheme.errorContainer, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("로그아웃", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                        Text("›", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuLinkRow(item: IconLabel, onClick: () -> Unit) {
    Card(shape = CardShape) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(item.icon, item.contentDescription, size = 34.dp, background = MaterialTheme.colorScheme.primaryContainer, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(item.label, modifier = Modifier.weight(1f))
            Text("›", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SurveySummaryCard(survey: SurveyResponse?) {
    Card(shape = CardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LuminineIconView(LuminineIcon.Report, "내 건강 설문", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text("내 건강 설문", fontWeight = FontWeight.Bold)
            }
            if (survey == null) {
                Text("아직 설문 정보가 없습니다.")
            } else {
                survey.basicInfo.birthYear?.let { Text("출생연도 · $it") }
                survey.basicInfo.gender?.let { Text("성별 · ${it.label}") }
                survey.basicInfo.region?.let { Text("지역 · ${it.label}") }
                survey.bodyInfo.heightCm?.let { h ->
                    survey.bodyInfo.weightKg?.let { w -> Text("신체 · ${h.toInt()}cm / ${w.toInt()}kg") }
                }
                if (survey.goals.orderedGoals.isNotEmpty()) {
                    Text(
                        "관심 영역 · " + survey.goals.orderedGoals.take(3)
                            .mapIndexed { i, g -> "${i + 1}.${g.label}" }.joinToString("  "),
                    )
                }
                if (survey.skippedSections.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = CardShape) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("나중에 입력하기로 한 항목 ${survey.skippedSections.size}개", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(survey.skippedSections.map { it.summaryLabel() }.joinToString(", "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun SurveySection.summaryLabel(): String = when (this) {
    SurveySection.S0 -> "기본 인적사항"
    SurveySection.S1 -> "신체 정보"
    SurveySection.S2 -> "질환·병력"
    SurveySection.S3 -> "체감 증상"
    SurveySection.S4 -> "생활습관"
    SurveySection.S5 -> "복용 중"
    SurveySection.S6 -> "관심 영역"
    SurveySection.S7 -> "예산"
}
