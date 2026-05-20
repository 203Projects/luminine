package com.reverse.healthtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
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
import com.reverse.healthtracker.ui.IconLabel
import com.reverse.healthtracker.ui.ReverseIcon
import com.reverse.healthtracker.ui.components.IconTile
import com.reverse.healthtracker.ui.components.ReverseIconView
import com.reverse.healthtracker.ui.healthTopics
import com.reverse.healthtracker.ui.icon
import com.reverse.healthtracker.ui.quickActions
import com.reverse.healthtracker.ui.topLevelDestinations
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            IconTile(
                                icon = ReverseIcon.Sparkles,
                                contentDescription = "REVERSE",
                                size = 42.dp,
                                background = ReverseEspresso,
                                tint = Color.White,
                            )
                            Column {
                                Text("REVERSE", fontWeight = FontWeight.Bold)
                                Text("안티에이징 루틴 트래커", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    },
                    actions = {
                        TextButton(onClick = { isAdmin = !isAdmin }) {
                            ReverseIconView(
                                icon = if (isAdmin) ReverseIcon.User else ReverseIcon.Admin,
                                contentDescription = null,
                                tint = ReverseGold,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isAdmin) "회원" else "관리자")
                        }
                    },
                )
            },
            bottomBar = {
                if (!isAdmin) {
                    val destinations = topLevelDestinations()
                    NavigationBar {
                        MainTab.entries.forEachIndexed { index, tab ->
                            val destination = destinations[index]
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    ReverseIconView(
                                        icon = destination.icon,
                                        contentDescription = destination.contentDescription,
                                        tint = if (selectedTab == tab) ReverseEspresso else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
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
                ReverseIconView(
                    icon = ReverseIcon.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("기록 저장")
            }
        }
    }
}

@Composable
private fun HeroScoreCard(score: Int, streak: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = ReverseEspresso), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("오늘의 실천지수", color = Color.White.copy(alpha = 0.8f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$score", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(" / 100점", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                IconTile(
                    icon = ReverseIcon.Trophy,
                    contentDescription = "연속 달성",
                    size = 58.dp,
                    background = ReverseGold,
                    tint = Color.White,
                )
            }
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                color = ReverseGreen,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniSignal("연속 $streak 일", ReverseIcon.Check)
                MiniSignal("AI 케어 대기", ReverseIcon.Care)
            }
        }
    }
}

@Composable
private fun NoticeCard(text: String) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(ReverseIcon.Sparkles, "공지", size = 38.dp, background = ReverseGold.copy(alpha = 0.18f), tint = ReverseGold)
            Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MiniSignal(label: String, icon: ReverseIcon) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ReverseIconView(icon = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
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
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            leadingIcon = {
                ReverseIconView(ReverseIcon.Menu, null, Modifier.size(17.dp), tint = ReverseGold)
            },
            label = { Text("전체") },
        )
        RoutineCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
                leadingIcon = {
                    ReverseIconView(category.icon(), null, Modifier.size(17.dp), tint = ReverseGold)
                },
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
            IconTile(
                icon = if (checked) ReverseIcon.Check else routine.category.icon(),
                contentDescription = routine.category.label,
                size = 42.dp,
                background = if (checked) ReverseGreen.copy(alpha = 0.15f) else ReverseGold.copy(alpha = 0.14f),
                tint = if (checked) ReverseGreen else ReverseGold,
            )
            Spacer(Modifier.width(10.dp))
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(Modifier.weight(1f)) {
                Text(
                    routine.name,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                )
                Text(routine.category.label, style = MaterialTheme.typography.labelMedium, color = ReverseGold)
            }
            TextButton(onClick = onDelete) {
                ReverseIconView(ReverseIcon.Trash, "삭제", Modifier.size(18.dp), tint = ReverseCoral)
            }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(ReverseIcon.Plus, "루틴 추가", size = 36.dp, background = ReverseGreen.copy(alpha = 0.14f), tint = ReverseGreen)
                Text("루틴 추가", fontWeight = FontWeight.Bold)
            }
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
                        leadingIcon = { ReverseIconView(item.icon(), null, Modifier.size(16.dp), tint = ReverseGold) },
                        label = { Text(item.label) },
                    )
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                ReverseIconView(ReverseIcon.Plus, null, Modifier.size(18.dp), tint = ReverseGold)
                Spacer(Modifier.width(6.dp))
                Text("추가")
            }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(ReverseIcon.Energy, "컨디션", size = 36.dp, background = ReverseCoral.copy(alpha = 0.13f), tint = ReverseCoral)
                Text("컨디션", fontWeight = FontWeight.Bold)
            }
            ConditionSlider("에너지", ReverseIcon.Energy, condition.energy) { onChange(condition.copy(energy = it)) }
            ConditionSlider("피부 상태", ReverseIcon.Skin, condition.skin) { onChange(condition.copy(skin = it)) }
            ConditionSlider("수면 품질", ReverseIcon.Sleep, condition.sleep) { onChange(condition.copy(sleep = it)) }
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
private fun ConditionSlider(label: String, icon: ReverseIcon, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReverseIconView(icon, null, Modifier.size(18.dp), tint = ReverseGold)
                Text(label)
            }
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
    val items = quickActions()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { action ->
                    QuickActionCard(action, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(action: IconLabel, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = {},
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconTile(action.icon, action.contentDescription, size = 34.dp, background = ReverseGold.copy(alpha = 0.12f), tint = ReverseGold)
            Spacer(Modifier.height(6.dp))
            Text(action.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                MetricCard("달성률", "${averageScore(records)}%", Modifier.weight(1f), ReverseIcon.Chart)
                MetricCard("평균 에너지", "${(averageEnergy(records) * 10).toInt() / 10.0}", Modifier.weight(1f), ReverseIcon.Energy)
                MetricCard("연속", "5일", Modifier.weight(1f), ReverseIcon.Trophy)
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
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, icon: ReverseIcon = ReverseIcon.Chart) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTile(icon, label, size = 32.dp, background = ReverseGold.copy(alpha = 0.13f), tint = ReverseGold)
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthlyCalendar(buckets: Map<LocalDate, CompletionBucket>) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("5월 달성 캘린더", ReverseIcon.Chart)
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
            SectionTitle("7일 컨디션", ReverseIcon.Energy)
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
            SectionTitle("루틴별 달성률", ReverseIcon.Trophy)
            routines.take(5).forEachIndexed { index, routine ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconTile(routine.category.icon(), routine.category.label, size = 30.dp, background = ReverseGold.copy(alpha = 0.11f), tint = ReverseGold)
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
            SectionTitle("AI 인사이트", ReverseIcon.Sparkles, ReverseGreen)
            Text(text)
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: ReverseIcon, tint: Color = ReverseGold) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ReverseIconView(icon, null, Modifier.size(19.dp), tint = tint)
        Text(title, fontWeight = FontWeight.Bold, color = if (tint == ReverseGold) MaterialTheme.colorScheme.onSurface else tint)
    }
}

@Composable
private fun HealthInfoScreen(modifier: Modifier) {
    val cards = healthTopics()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(ReverseIcon.Book, "건강정보", size = 44.dp, background = ReverseGreen.copy(alpha = 0.14f), tint = ReverseGreen)
                Text("건강정보", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { topic ->
                            TopicCard(topic, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    IconLabel("스킨케어 성분", ReverseIcon.Drop, "스킨케어 성분"),
                    IconLabel("안티에이징 식단", ReverseIcon.Plate, "안티에이징 식단"),
                    IconLabel("마음건강", ReverseIcon.Mind, "마음건강"),
                ).forEach {
                    AssistChip(
                        onClick = {},
                        leadingIcon = { ReverseIconView(it.icon, null, Modifier.size(16.dp), tint = ReverseGold) },
                        label = { Text(it.label) },
                    )
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
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(ReverseIcon.Book, "아티클", size = 38.dp, background = ReverseGold.copy(alpha = 0.12f), tint = ReverseGold)
                    Column(Modifier.weight(1f)) {
                        Text(article, fontWeight = FontWeight.SemiBold)
                        Text("R의 건강로그 연구 노트", style = MaterialTheme.typography.labelMedium, color = ReverseGold)
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("YouTube 최신 영상", ReverseIcon.Youtube)
                    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(8.dp)).background(ReverseEspresso), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReverseIconView(ReverseIcon.Play, "재생", Modifier.size(42.dp), tint = Color.White)
                            Text("R의 건강로그", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: IconLabel, modifier: Modifier) {
    Card(modifier = modifier.heightIn(min = 92.dp), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconTile(topic.icon, topic.contentDescription, size = 38.dp, background = ReverseGreen.copy(alpha = 0.12f), tint = ReverseGreen)
            Text(topic.label, fontWeight = FontWeight.Bold)
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
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("REVERSE 1:1 케어", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("카카오 오픈채팅으로 식단, 인바디, 영양제 기록을 보내세요.", color = Color.White)
                        }
                        IconTile(ReverseIcon.Care, "1:1 케어", size = 54.dp, background = Color.White.copy(alpha = 0.18f), tint = Color.White)
                    }
                    OutlinedButton(onClick = {}, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))) {
                        ReverseIconView(ReverseIcon.Link, null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("카카오 오픈채팅 연결", color = Color.White)
                    }
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
            SectionTitle("오늘 케어 현황", ReverseIcon.Check)
            listOf(
                IconLabel("식단 전송 완료", ReverseIcon.Plate, "식단 전송"),
                IconLabel("영양제 복용 확인", ReverseIcon.Supplement, "영양제 복용"),
                IconLabel("인바디 측정 결과 대기중", ReverseIcon.Body, "인바디 측정"),
                IconLabel("스킨케어 루틴 인증 완료", ReverseIcon.Drop, "스킨케어 인증"),
            ).forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(
                        it.icon,
                        it.contentDescription,
                        size = 30.dp,
                        background = if (it.label.contains("대기중")) ReverseGold.copy(alpha = 0.16f) else ReverseGreen.copy(alpha = 0.15f),
                        tint = if (it.label.contains("대기중")) ReverseGold else ReverseGreen,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(it.label)
                }
            }
        }
    }
}

@Composable
private fun InbodyCard(inbody: InbodyRecord) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("최근 인바디", ReverseIcon.Body)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("체중", "${inbody.weight}kg", Modifier.weight(1f), ReverseIcon.Body)
                MetricCard("체지방", "${inbody.bodyFatPct}%", Modifier.weight(1f), ReverseIcon.Chart)
                MetricCard("골격근", "${inbody.muscleMass}kg", Modifier.weight(1f), ReverseIcon.Dumbbell)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("BMR", "${inbody.bmr ?: "-"}", Modifier.weight(1f), ReverseIcon.Energy)
                MetricCard("체수분", "${inbody.bodyWater ?: "-"}L", Modifier.weight(1f), ReverseIcon.Drop)
                MetricCard("내장지방", "${inbody.visceralFat ?: "-"}", Modifier.weight(1f), ReverseIcon.Report)
            }
        }
    }
}

@Composable
private fun StepsCard() {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("이용 방법", ReverseIcon.Link)
            listOf(
                IconLabel("카카오 오픈채팅 연결", ReverseIcon.Link, "연결"),
                IconLabel("인바디·식단·영양제 메시지 전송", ReverseIcon.Message, "메시지 전송"),
                IconLabel("자동 분석 결과 확인", ReverseIcon.Sparkles, "AI 분석"),
                IconLabel("다음 루틴 조정", ReverseIcon.Check, "루틴 조정"),
            ).forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconTile(step.icon, step.contentDescription, size = 30.dp, background = ReverseGold.copy(alpha = 0.12f), tint = ReverseGold)
                    Text("${index + 1}. ${step.label}")
                }
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
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(ReverseIcon.User, "회원", size = 48.dp, background = ReverseEspresso, tint = Color.White)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("김민지", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("실천지수 포인트 ${records.sumOf { it.score }}P", color = ReverseGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        items(
            listOf(
                IconLabel("루틴 차트", ReverseIcon.Chart, "루틴 차트"),
                IconLabel("인바디 기록", ReverseIcon.Body, "인바디 기록"),
                IconLabel("사진 타임라인", ReverseIcon.Camera, "사진 타임라인"),
                IconLabel("실천지수 내역", ReverseIcon.Trophy, "실천지수 내역"),
                IconLabel("REVERSE Shop", ReverseIcon.Shop, "리버스 숍"),
                IconLabel("유어프라임", ReverseIcon.Sparkles, "유어프라임"),
                IconLabel("유튜브", ReverseIcon.Youtube, "유튜브"),
                IconLabel("카페", ReverseIcon.Cafe, "카페"),
                IconLabel("문의 및 설정", ReverseIcon.Message, "문의 및 설정"),
            ),
        ) { item ->
            Card(shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(item.icon, item.contentDescription, size = 34.dp, background = ReverseGold.copy(alpha = 0.12f), tint = ReverseGold)
                    Spacer(Modifier.width(12.dp))
                    Text(item.label, modifier = Modifier.weight(1f))
                    Text("›", color = ReverseGold, fontWeight = FontWeight.Bold)
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
                val icon = listOf(ReverseIcon.Chart, ReverseIcon.User, ReverseIcon.Sparkles)[index]
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReverseIconView(icon, null, Modifier.size(16.dp), tint = if (selectedTab == index) ReverseGold else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(title)
                        }
                    },
                )
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
                MetricCard("제출 수", "${members.size}", Modifier.weight(1f), ReverseIcon.Check)
                MetricCard("평균 지수", "${members.map { it.score }.average().toInt()}", Modifier.weight(1f), ReverseIcon.Chart)
                MetricCard("100점", "${members.count { it.score == 100 }}", Modifier.weight(1f), ReverseIcon.Trophy)
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("오늘 제출 회원", ReverseIcon.User)
                    members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconTile(ReverseIcon.User, member.name, size = 28.dp, background = ReverseGreen.copy(alpha = 0.12f), tint = ReverseGreen)
                            Text("${member.name} · 에너지 ${member.energy}/5 · ${member.score}점")
                        }
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
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(
                        ReverseIcon.User,
                        member.name,
                        size = 40.dp,
                        background = if (member.score <= 30 || member.energy <= 3) ReverseCoral.copy(alpha = 0.14f) else ReverseGold.copy(alpha = 0.12f),
                        tint = if (member.score <= 30 || member.energy <= 3) ReverseCoral else ReverseGold,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(member.name, fontWeight = FontWeight.Bold)
                        Text("오늘 기록: 실천지수 ${member.score}점, 에너지 ${member.energy}/5")
                        Text("메모: ${if (member.score <= 30) "저녁 루틴 리마인드 필요" else "특이사항 없음"}")
                    }
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
                    SectionTitle("주목할 회원", ReverseIcon.Alert, ReverseCoral)
                    attention.forEach {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReverseIconView(ReverseIcon.Alert, null, Modifier.size(17.dp), tint = ReverseCoral)
                            Text("${it.name} · ${it.score}점 · 에너지 ${it.energy}/5")
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("카카오 수신 로그", ReverseIcon.Message)
                    SampleData.kakaoMessages().forEach {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReverseIconView(ReverseIcon.Message, null, Modifier.size(17.dp), tint = ReverseGold)
                            Text("${it.parsedType} · ${it.parsedDataSummary}")
                        }
                    }
                }
            }
        }
    }
}
