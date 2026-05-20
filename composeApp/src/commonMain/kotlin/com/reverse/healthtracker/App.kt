package com.reverse.healthtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reverse.healthtracker.data.SampleData
import com.reverse.healthtracker.domain.CompletionBucket
import com.reverse.healthtracker.domain.KakaoMessageParser
import com.reverse.healthtracker.domain.MealType
import com.reverse.healthtracker.domain.averageEnergy
import com.reverse.healthtracker.domain.averageScore
import com.reverse.healthtracker.domain.calculatePracticeScore
import com.reverse.healthtracker.domain.membersRequiringAttention
import com.reverse.healthtracker.domain.monthlyCompletionBuckets
import com.reverse.healthtracker.model.Condition
import com.reverse.healthtracker.model.DailyRecord
import com.reverse.healthtracker.model.InbodyRecord
import com.reverse.healthtracker.model.MemberDailySummary
import com.reverse.healthtracker.model.Routine
import com.reverse.healthtracker.model.RoutineCategory
import com.reverse.healthtracker.ui.theme.ReverseCoral
import com.reverse.healthtracker.ui.theme.ReverseEspresso
import com.reverse.healthtracker.ui.theme.ReverseGold
import com.reverse.healthtracker.ui.theme.ReverseGreen
import com.reverse.healthtracker.ui.theme.ReverseTheme
import kotlinx.datetime.LocalDate

private enum class MainTab(val label: String) {
    Home("홈"),
    Charts("차트"),
    Health("건강정보"),
    Care("1:1케어"),
    Menu("메뉴"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    ReverseTheme {
        val userId = "user-1"
        val routines = remember { SampleData.defaultRoutines(userId).toMutableStateList() }
        val records = remember { SampleData.records(userId).toMutableStateList() }
        var doneIds by remember { mutableStateOf(setOf<String>()) }
        var condition by remember { mutableStateOf(Condition(3, 3, 3, "😊")) }
        var selectedTab by remember { mutableStateOf(MainTab.Home) }
        var isAdmin by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("REVERSE", fontWeight = FontWeight.Bold)
                            Text("안티에이징 루틴 트래커", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    actions = {
                        TextButton(onClick = { isAdmin = !isAdmin }) {
                            Text(if (isAdmin) "회원" else "관리자")
                        }
                    },
                )
            },
            bottomBar = {
                if (!isAdmin) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Text(tab.label.take(1)) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            if (isAdmin) {
                AdminDashboard(Modifier.padding(padding), records)
            } else {
                when (selectedTab) {
                    MainTab.Home -> HomeScreen(
                        modifier = Modifier.padding(padding),
                        routines = routines,
                        doneIds = doneIds,
                        condition = condition,
                        onDoneChange = { routineId, checked ->
                            doneIds = if (checked) doneIds + routineId else doneIds - routineId
                        },
                        onConditionChange = { condition = it },
                        onAddRoutine = { name, category ->
                            val nextOrder = routines.maxOfOrNull { it.order }?.plus(1) ?: 0
                            routines += Routine("custom-$nextOrder", userId, name, category, true, nextOrder)
                        },
                        onDeleteRoutine = { routineId ->
                            routines.removeAll { it.id == routineId }
                            doneIds = doneIds - routineId
                        },
                        onSave = {
                            val score = calculatePracticeScore(routines, doneIds)
                            records.removeAll { it.date == SampleData.today }
                            records += DailyRecord(
                                id = "today-${records.size}",
                                userId = userId,
                                date = SampleData.today,
                                score = score,
                                doneRoutineIds = doneIds,
                                totalRoutines = routines.count { it.isActive },
                                condition = condition,
                                memo = null,
                            )
                        },
                    )
                    MainTab.Charts -> ChartsScreen(Modifier.padding(padding), records, routines)
                    MainTab.Health -> HealthInfoScreen(Modifier.padding(padding))
                    MainTab.Care -> CareScreen(Modifier.padding(padding), SampleData.latestInbody(userId))
                    MainTab.Menu -> MenuScreen(Modifier.padding(padding), records)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    routines: List<Routine>,
    doneIds: Set<String>,
    condition: Condition,
    onDoneChange: (String, Boolean) -> Unit,
    onConditionChange: (Condition) -> Unit,
    onAddRoutine: (String, RoutineCategory) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    onSave: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<RoutineCategory?>(null) }
    var newRoutineName by remember { mutableStateOf("") }
    var newRoutineCategory by remember { mutableStateOf(RoutineCategory.InnerCare) }
    val visibleRoutines = routines
        .filter { it.isActive }
        .filter { selectedCategory == null || it.category == selectedCategory }
        .sortedBy { it.order }
    val score = calculatePracticeScore(routines, doneIds)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroScoreCard(score = score, streak = 5)
        }
        item {
            NoticeCard("오늘 카카오 오픈채팅에 식단과 인바디를 보내면 AI 케어 조언이 자동으로 정리됩니다.")
        }
        item {
            CategoryChips(selectedCategory) { selectedCategory = it }
        }
        items(visibleRoutines, key = { it.id }) { routine ->
            RoutineRow(
                routine = routine,
                checked = routine.id in doneIds,
                onCheckedChange = { onDoneChange(routine.id, it) },
                onDelete = { onDeleteRoutine(routine.id) },
            )
        }
        item {
            AddRoutineCard(
                name = newRoutineName,
                category = newRoutineCategory,
                onNameChange = { newRoutineName = it },
                onCategoryChange = { newRoutineCategory = it },
                onAdd = {
                    if (newRoutineName.isNotBlank()) {
                        onAddRoutine(newRoutineName.trim(), newRoutineCategory)
                        newRoutineName = ""
                    }
                },
            )
        }
        item {
            ConditionCard(condition, onConditionChange)
        }
        item {
            QuickMenuGrid()
        }
        item {
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("기록 저장")
            }
        }
    }
}

@Composable
private fun HeroScoreCard(score: Int, streak: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = ReverseEspresso), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("오늘의 실천지수", color = Color.White.copy(alpha = 0.8f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(" / 100점", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 8.dp))
            }
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                color = ReverseGreen,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
            Text("연속 달성 $streak 일", color = Color.White)
        }
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Text(text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CategoryChips(
    selected: RoutineCategory?,
    onSelected: (RoutineCategory?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text("전체") })
        RoutineCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
                label = { Text(category.label) },
            )
        }
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(Modifier.weight(1f)) {
                Text(
                    routine.name,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                )
                Text(routine.category.label, style = MaterialTheme.typography.labelMedium, color = ReverseGold)
            }
            TextButton(onClick = onDelete) { Text("삭제") }
        }
    }
}

@Composable
private fun AddRoutineCard(
    name: String,
    category: RoutineCategory,
    onNameChange: (String) -> Unit,
    onCategoryChange: (RoutineCategory) -> Unit,
    onAdd: () -> Unit,
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("루틴 추가", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("새 루틴") },
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutineCategory.entries.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { onCategoryChange(item) },
                        label = { Text(item.label) },
                    )
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("추가") }
        }
    }
}

@Composable
private fun ConditionCard(
    condition: Condition,
    onChange: (Condition) -> Unit,
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("컨디션", fontWeight = FontWeight.Bold)
            ConditionSlider("에너지", condition.energy) { onChange(condition.copy(energy = it)) }
            ConditionSlider("피부 상태", condition.skin) { onChange(condition.copy(skin = it)) }
            ConditionSlider("수면 품질", condition.sleep) { onChange(condition.copy(sleep = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("😫", "😕", "😊", "😄", "🔥").forEach { emoji ->
                    FilterChip(
                        selected = condition.emoji == emoji,
                        onClick = { onChange(condition.copy(emoji = emoji)) },
                        label = { Text(emoji) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConditionSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("$value/5", fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(1, 5)) },
            valueRange = 1f..5f,
            steps = 3,
        )
    }
}

@Composable
private fun QuickMenuGrid() {
    val items = listOf("인바디", "식단", "영양제", "스킨케어", "사진", "리포트", "Shop", "문의")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    AssistChip(onClick = {}, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChartsScreen(modifier: Modifier, records: List<DailyRecord>, routines: List<Routine>) {
    val buckets = monthlyCompletionBuckets(records)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("달성률", "${averageScore(records)}%", Modifier.weight(1f))
                MetricCard("평균 에너지", "${(averageEnergy(records) * 10).toInt() / 10.0}", Modifier.weight(1f))
                MetricCard("연속", "5일", Modifier.weight(1f))
            }
        }
        item { MonthlyCalendar(buckets) }
        item { SevenDayCondition(records.takeLast(7)) }
        item { RoutineRanking(routines) }
        item {
            InsightCard("최근 수면 점수가 낮은 날에는 실천지수가 함께 떨어졌습니다. 저녁 루틴을 먼저 완료하고 수면 준비 알림을 30분 앞당기는 전략이 좋습니다.")
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthlyCalendar(buckets: Map<LocalDate, CompletionBucket>) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("5월 달성 캘린더", fontWeight = FontWeight.Bold)
            (1..31).chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val color = when (buckets[LocalDate(2026, 5, day)]) {
                            CompletionBucket.Complete -> ReverseGreen
                            CompletionBucket.Partial -> ReverseGold
                            CompletionBucket.None -> MaterialTheme.colorScheme.surfaceVariant
                            null -> Color.White
                        }
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$day", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SevenDayCondition(records: List<DailyRecord>) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("7일 컨디션", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                records.forEach { record ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height((record.condition.energy * 20).dp).clip(RoundedCornerShape(6.dp)).background(ReverseGreen),
                        )
                        Text("${record.date.day}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineRanking(routines: List<Routine>) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("루틴별 달성률", fontWeight = FontWeight.Bold)
            routines.take(5).forEachIndexed { index, routine ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("${index + 1}", fontWeight = FontWeight.Bold, color = ReverseGold)
                    Spacer(Modifier.width(8.dp))
                    Text(routine.name, modifier = Modifier.weight(1f))
                    Text("${92 - index * 7}%")
                }
            }
        }
    }
}

@Composable
private fun InsightCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("AI 인사이트", fontWeight = FontWeight.Bold, color = ReverseGreen)
            Text(text)
        }
    }
}

@Composable
private fun HealthInfoScreen(modifier: Modifier) {
    val cards = listOf("산화 스트레스", "비타민C", "운동 과학", "수면과 노화", "연구 백과")
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("건강정보", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { label ->
                            TopicCard(label, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("스킨케어 성분", "안티에이징 식단", "마음건강").forEach {
                    AssistChip(onClick = {}, label = { Text(it) })
                }
            }
        }
        items(
            listOf(
                "비타민C 약동학으로 보는 복용 타이밍",
                "근육량 유지가 피부 노화 속도에 주는 영향",
                "수면 품질을 올리는 저녁 루틴 설계",
            ),
        ) { article ->
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(article, fontWeight = FontWeight.SemiBold)
                    Text("R의 건강로그 연구 노트", style = MaterialTheme.typography.labelMedium, color = ReverseGold)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YouTube 최신 영상", fontWeight = FontWeight.Bold)
                    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(8.dp)).background(ReverseEspresso), contentAlignment = Alignment.Center) {
                        Text("R의 건강로그", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCard(label: String, modifier: Modifier) {
    Card(modifier = modifier.heightIn(min = 92.dp), shape = RoundedCornerShape(8.dp)) {
        Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CareScreen(modifier: Modifier, inbody: InbodyRecord) {
    val parsed = remember {
        KakaoMessageParser.parse("오늘 인바디 62.4kg 체지방 22.1% 골격근 24.8kg\n아침 샐러드, 점심 현미밥+닭가슴살\n비타민C 3g, 오메가3 복용 완료")
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ReverseGreen), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("REVERSE 1:1 케어", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("카카오 오픈채팅으로 식단, 인바디, 영양제 기록을 보내세요.", color = Color.White)
                    OutlinedButton(onClick = {}) { Text("카카오 오픈채팅 연결") }
                }
            }
        }
        item {
            CareChecklist()
        }
        item {
            InbodyCard(inbody)
        }
        item {
            InsightCard(SampleData.aiAdvice().adviceText)
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("카카오 메시지 파싱 예시", fontWeight = FontWeight.Bold)
                    Text("인바디: ${parsed.inbody?.weight ?: "-"}kg / 체지방 ${parsed.inbody?.bodyFatPct ?: "-"}%")
                    Text("식단: ${parsed.diet.firstOrNull { it.mealType == MealType.Lunch }?.content ?: "대기중"}")
                    Text("영양제: ${parsed.supplements.joinToString()}")
                }
            }
        }
        item {
            StepsCard()
        }
    }
}

@Composable
private fun CareChecklist() {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("오늘 케어 현황", fontWeight = FontWeight.Bold)
            listOf("식단 전송 완료", "영양제 복용 확인", "인바디 측정 결과 대기중", "스킨케어 루틴 인증 완료").forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (it.contains("대기중")) ReverseGold else ReverseGreen))
                    Spacer(Modifier.width(8.dp))
                    Text(it)
                }
            }
        }
    }
}

@Composable
private fun InbodyCard(inbody: InbodyRecord) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("최근 인바디", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("체중", "${inbody.weight}kg", Modifier.weight(1f))
                MetricCard("체지방", "${inbody.bodyFatPct}%", Modifier.weight(1f))
                MetricCard("골격근", "${inbody.muscleMass}kg", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("BMR", "${inbody.bmr ?: "-"}", Modifier.weight(1f))
                MetricCard("체수분", "${inbody.bodyWater ?: "-"}L", Modifier.weight(1f))
                MetricCard("내장지방", "${inbody.visceralFat ?: "-"}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StepsCard() {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("이용 방법", fontWeight = FontWeight.Bold)
            listOf("1. 카카오 오픈채팅 연결", "2. 인바디·식단·영양제 메시지 전송", "3. 자동 분석 결과 확인", "4. 다음 루틴 조정").forEach {
                Text(it)
            }
        }
    }
}

@Composable
private fun MenuScreen(modifier: Modifier, records: List<DailyRecord>) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("김민지", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("실천지수 포인트 ${records.sumOf { it.score }}P", color = ReverseGold, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(listOf("루틴 차트", "인바디 기록", "사진 타임라인", "실천지수 내역", "REVERSE Shop", "유어프라임", "유튜브", "카페", "문의 및 설정")) { item ->
            Card(shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item)
                    Text("›")
                }
            }
        }
    }
}

@Composable
private fun AdminDashboard(modifier: Modifier, records: List<DailyRecord>) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("현황", "회원", "분석")
    val members = SampleData.memberSummaries()

    Column(modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> AdminStatus(records, members)
            1 -> AdminMembers(members)
            2 -> AdminAnalysis(members)
        }
    }
}

@Composable
private fun AdminStatus(records: List<DailyRecord>, members: List<MemberDailySummary>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("제출 수", "${members.size}", Modifier.weight(1f))
                MetricCard("평균 지수", "${members.map { it.score }.average().toInt()}", Modifier.weight(1f))
                MetricCard("100점", "${members.count { it.score == 100 }}", Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("오늘 제출 회원", fontWeight = FontWeight.Bold)
                    members.forEach { member ->
                        Text("${member.name} · 에너지 ${member.energy}/5 · ${member.score}점")
                    }
                }
            }
        }
        item { RoutineRanking(SampleData.defaultRoutines()) }
    }
}

@Composable
private fun AdminMembers(members: List<MemberDailySummary>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(members) { member ->
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(member.name, fontWeight = FontWeight.Bold)
                    Text("오늘 기록: 실천지수 ${member.score}점, 에너지 ${member.energy}/5")
                    Text("메모: ${if (member.score <= 30) "저녁 루틴 리마인드 필요" else "특이사항 없음"}")
                }
            }
        }
    }
}

@Composable
private fun AdminAnalysis(members: List<MemberDailySummary>) {
    val attention = membersRequiringAttention(members)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            InsightCard("오늘 평균 실천지수는 안정적이지만, 수면 관련 루틴 누락이 저점 회원에게 반복됩니다. 저녁 8시 리마인더와 수면 준비 루틴을 우선 점검하세요.")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ReverseCoral.copy(alpha = 0.12f)), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("주목할 회원", fontWeight = FontWeight.Bold, color = ReverseCoral)
                    attention.forEach { Text("${it.name} · ${it.score}점 · 에너지 ${it.energy}/5") }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("카카오 수신 로그", fontWeight = FontWeight.Bold)
                    SampleData.kakaoMessages().forEach {
                        Text("${it.parsedType} · ${it.parsedDataSummary}")
                    }
                }
            }
        }
    }
}
