package com.luminine.app

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.luminine.app.auth.AuthResult
import com.luminine.app.data.SampleData
import com.luminine.app.data.session.Session
import com.luminine.app.di.LuminineDependencies
import com.luminine.app.domain.CompletionBucket
import com.luminine.app.domain.KakaoMessageParser
import com.luminine.app.domain.MealType
import com.luminine.app.domain.averageEnergy
import com.luminine.app.domain.averageScore
import com.luminine.app.domain.calculatePracticeScore
import com.luminine.app.domain.greetingFor
import com.luminine.app.domain.membersRequiringAttention
import com.luminine.app.domain.monthlyCompletionBuckets
import com.luminine.app.domain.seededRoutines
import com.luminine.app.model.Condition
import com.luminine.app.model.DailyRecord
import com.luminine.app.model.InbodyRecord
import com.luminine.app.model.LuminineSettings
import com.luminine.app.model.MemberDailySummary
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Routine
import com.luminine.app.model.RoutineCategory
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection
import com.luminine.app.onboarding.AuthScreen
import com.luminine.app.onboarding.SurveyFlow
import com.luminine.app.ui.IconLabel
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.IconTile
import com.luminine.app.ui.components.LuminineIconView
import com.luminine.app.ui.healthTopics
import com.luminine.app.ui.icon
import com.luminine.app.ui.topLevelDestinations
import com.luminine.app.ui.screens.MenuOverlay
import com.luminine.app.ui.screens.MyPageScreen
import com.luminine.app.ui.screens.ReadabilitySettingsScreen
import com.luminine.app.ui.screens.ShopScreen
import com.luminine.app.ui.screens.WebViewScreen
import com.luminine.app.content.HealthContent
import com.luminine.app.content.HealthTopicKey
import com.luminine.app.ui.theme.ReverseCoral
import com.luminine.app.ui.theme.ReverseEspresso
import com.luminine.app.ui.theme.ReverseGold
import com.luminine.app.ui.theme.ReverseGreen
import com.luminine.app.ui.theme.ReverseIvory
import com.luminine.app.ui.theme.LuminineTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

// Mockup-matched shape: soft 20dp rounded cards across all surfaces.
private val CardShape = RoundedCornerShape(20.dp)

private enum class MainTab(val label: String) {
    Home("홈"),
    Charts("차트"),
    Health("건강정보"),
    Care("1:1케어"),
    Shop("Shop"),
}

// Screens reachable from the top app bar (전체 메뉴 / 마이페이지) or a content tap, drawn over the
// current tab. Hand-rolled overlay routing — consistent with the existing when-based navigation.
private sealed interface Overlay {
    data object None : Overlay
    data object Menu : Overlay
    data object MyPage : Overlay
    data object Readability : Overlay
    data class Web(val url: String, val title: String) : Overlay
}

// Top-level app gate. The main tab UI is only reachable after auth + completed onboarding.
private sealed interface RootState {
    data object Checking : RootState
    data object Auth : RootState
    data class Survey(val resume: SurveyResponse?) : RootState
    data class Main(val session: Session, val survey: SurveyResponse?) : RootState
}

@Composable
fun App() {
    val settingsRepo = remember { LuminineDependencies.settingsRepository }
    val settings by settingsRepo.observe().collectAsState(initial = LuminineSettings())
    LuminineTheme(settings) {
        val scope = rememberCoroutineScope()
        val sessionRepo = remember { LuminineDependencies.sessionRepository }
        val surveyRepo = remember { LuminineDependencies.surveyRepository }
        val authClient = remember { LuminineDependencies.kakaoAuthClient }
        var root by remember { mutableStateOf<RootState>(RootState.Checking) }

        // Restore any persisted session/survey on launch (repos are suspend → LaunchedEffect).
        LaunchedEffect(Unit) {
            val session = sessionRepo.load()
            val survey = surveyRepo.load()
            root = when {
                session == null -> RootState.Auth
                session.onboardingComplete -> RootState.Main(session, survey)
                else -> RootState.Survey(survey) // logged in but onboarding unfinished
            }
        }

        when (val state = root) {
            RootState.Checking -> SplashGate()
            RootState.Auth -> AuthScreen(onKakaoLogin = {
                scope.launch {
                    when (val result = authClient.login()) {
                        is AuthResult.Success -> {
                            val account = result.account
                            val session = Session(
                                userId = "user-${account.kakaoId}",
                                kakaoId = account.kakaoId,
                                displayName = account.nickname,
                                onboardingComplete = false,
                            )
                            sessionRepo.save(session)
                            root = RootState.Survey(null)
                        }
                        AuthResult.Cancelled, is AuthResult.Error -> Unit // stay on AuthScreen
                    }
                }
            })
            is RootState.Survey -> SurveyFlow(onComplete = { response ->
                scope.launch {
                    surveyRepo.save(response)
                    val existing = sessionRepo.load()
                    val name = response.basicInfo.name.ifBlank { existing?.displayName ?: "" }
                    val updated = (existing ?: Session("user-1", "", name)).copy(
                        displayName = name,
                        onboardingComplete = true,
                    )
                    sessionRepo.save(updated)
                    root = RootState.Main(updated, response)
                }
            })
            is RootState.Main -> MainScaffold(
                session = state.session,
                survey = state.survey,
                onLogout = {
                    scope.launch {
                        sessionRepo.clear()
                        surveyRepo.clear()
                        root = RootState.Auth
                    }
                },
            )
        }
    }
}

@Composable
private fun SplashGate() {
    Box(Modifier.fillMaxSize().background(ReverseIvory).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Text("LUMÍNINE", color = ReverseEspresso, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(session: Session, survey: SurveyResponse?, onLogout: () -> Unit) {
    val userId = session.userId
    val goals = remember(survey) { survey?.goals?.orderedGoals ?: emptyList() }
    val routines = remember(survey) {
        (survey?.let { seededRoutines(it, SampleData.defaultRoutines(userId)) }
            ?: SampleData.defaultRoutines(userId)).toMutableStateList()
    }
        val records = remember { SampleData.records(userId).toMutableStateList() }
        var doneIds by remember { mutableStateOf(setOf<String>()) }
        var condition by remember { mutableStateOf(Condition(3, 3, 3, "😊")) }
        var selectedTab by remember { mutableStateOf(MainTab.Home) }
        var isAdmin by remember { mutableStateOf(false) }
        var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
        val settingsRepo = remember { LuminineDependencies.settingsRepository }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isAdmin) {
                            Column {
                                Text("관리자 대시보드", fontWeight = FontWeight.Bold)
                                Text("전담 코치 · 5월 20일", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Column {
                                Text(greetingFor(session.displayName), fontWeight = FontWeight.Bold)
                                Text("5월 20일 · 오늘도 빛나는 하루", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    actions = {
                        TextButton(onClick = { isAdmin = !isAdmin }) {
                            LuminineIconView(
                                icon = if (isAdmin) LuminineIcon.User else LuminineIcon.Admin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isAdmin) "회원" else "관리자")
                        }
                        IconButton(onClick = { overlay = Overlay.Menu }) {
                            LuminineIconView(
                                icon = LuminineIcon.Menu,
                                contentDescription = "전체 메뉴",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        IconButton(onClick = { overlay = Overlay.MyPage }) {
                            LuminineIconView(
                                icon = LuminineIcon.User,
                                contentDescription = "마이페이지",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
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
                                    LuminineIconView(
                                        icon = destination.icon,
                                        contentDescription = destination.contentDescription,
                                        tint = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        goals = goals,
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
                    MainTab.Health -> HealthInfoScreen(Modifier.padding(padding)) { key ->
                        overlay = Overlay.Web(HealthContent.urlFor(key), HealthContent.titleFor(key))
                    }
                    MainTab.Care -> CareScreen(Modifier.padding(padding), SampleData.latestInbody(userId))
                    MainTab.Shop -> ShopScreen(Modifier.padding(padding))
                }
            }
        }

        when (val ov = overlay) {
            Overlay.None -> Unit
            Overlay.Menu -> MenuOverlay(
                displayName = session.displayName,
                records = records,
                survey = survey,
                onOpenReadability = { overlay = Overlay.Readability },
                onLogout = onLogout,
                onClose = { overlay = Overlay.None },
            )
            Overlay.MyPage -> MyPageScreen(
                displayName = session.displayName,
                onOpenReadability = { overlay = Overlay.Readability },
                onClose = { overlay = Overlay.None },
            )
            Overlay.Readability -> ReadabilitySettingsScreen(
                settingsRepo = settingsRepo,
                onClose = { overlay = Overlay.None },
            )
            is Overlay.Web -> WebViewScreen(
                url = ov.url,
                title = ov.title,
                onClose = { overlay = Overlay.None },
            )
        }
    }

@Composable
private fun HomeScreen(
    modifier: Modifier,
    routines: List<Routine>,
    goals: List<PriorityGoal>,
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
    val activeRoutines = routines.filter { it.isActive }
    val score = calculatePracticeScore(routines, doneIds)
    val doneCount = activeRoutines.count { it.id in doneIds }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroScoreCard(score = score, done = doneCount, total = activeRoutines.size, streak = 12)
        }
        if (goals.isNotEmpty()) {
            item { GoalChipsRow(goals) }
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
            SectionTitle("오늘 컨디션", LuminineIcon.Energy)
        }
        item {
            ConditionCard(condition, onConditionChange)
        }
        item {
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CardShape) {
                LuminineIconView(
                    icon = LuminineIcon.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("오늘 기록 저장")
            }
        }
    }
}

@Composable
private fun GoalChipsRow(goals: List<PriorityGoal>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle("나의 관심 영역", LuminineIcon.Sparkles, ReverseGreen)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            goals.forEachIndexed { index, goal ->
                AssistChip(
                    onClick = {},
                    leadingIcon = {
                        Text("${index + 1}", color = ReverseGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    },
                    label = { Text(goal.label) },
                )
            }
        }
    }
}

@Composable
private fun Avatar(initial: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = ReverseEspresso, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScoreRing(score: Int, size: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$score", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("점", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun HeroScoreCard(score: Int, done: Int, total: Int, streak: Int) {
    val gradient = Brush.linearGradient(listOf(ReverseEspresso, ReverseGold))
    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = CardShape) {
        Row(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ScoreRing(score = score, size = 84.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("오늘의 실천지수", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "${total}개 중 ${done}개 완료" + if (done < total) " · 남은 루틴을 마저 채워보세요" else " · 오늘 완벽 달성!",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text("🔥 연속 $streak 일 실천 중", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
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
                LuminineIconView(LuminineIcon.Menu, null, Modifier.size(17.dp), tint = ReverseGold)
            },
            label = { Text("전체") },
        )
        RoutineCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
                leadingIcon = {
                    LuminineIconView(category.icon(), null, Modifier.size(17.dp), tint = ReverseGold)
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
    Card(shape = CardShape) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = if (checked) LuminineIcon.Check else routine.category.icon(),
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
                LuminineIconView(LuminineIcon.Trash, "삭제", Modifier.size(18.dp), tint = ReverseCoral)
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
    Card(shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(LuminineIcon.Plus, "루틴 추가", size = 36.dp, background = ReverseGreen.copy(alpha = 0.14f), tint = ReverseGreen)
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
                        leadingIcon = { LuminineIconView(item.icon(), null, Modifier.size(16.dp), tint = ReverseGold) },
                        label = { Text(item.label) },
                    )
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                LuminineIconView(LuminineIcon.Plus, null, Modifier.size(18.dp), tint = ReverseGold)
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
    Card(shape = CardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ConditionSlider("에너지", LuminineIcon.Energy, condition.energy) { onChange(condition.copy(energy = it)) }
            ConditionSlider("피부 상태", LuminineIcon.Skin, condition.skin) { onChange(condition.copy(skin = it)) }
            ConditionSlider("수면 품질", LuminineIcon.Sleep, condition.sleep) { onChange(condition.copy(sleep = it)) }
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
private fun ConditionSlider(label: String, icon: LuminineIcon, value: Int, onValueChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LuminineIconView(icon, null, Modifier.size(18.dp), tint = ReverseGold)
                Text(label)
            }
            Text("$value/5", fontWeight = FontWeight.Bold, color = ReverseGold)
        }
        // Thin track with discrete 1–5 tap targets, matching the mockup.
        Row(
            Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(percent = 50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(
                    Modifier
                        .fillMaxWidth(value / 5f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(ReverseGold),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { step ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (step <= value) ReverseGold.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onValueChange(step) },
                )
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
                MetricCard("달성률", "${averageScore(records)}%", Modifier.weight(1f), LuminineIcon.Chart)
                MetricCard("평균 에너지", "${(averageEnergy(records) * 10).toInt() / 10.0}", Modifier.weight(1f), LuminineIcon.Energy)
                MetricCard("연속", "5일", Modifier.weight(1f), LuminineIcon.Trophy)
            }
        }
        item { MonthlyCalendar(buckets) }
        item { SevenDayCondition(records.takeLast(7)) }
        // Only rank active routines — seeding may deactivate de-prioritized categories.
        item { RoutineRanking(routines.filter { it.isActive }) }
        item {
            InsightCard("최근 수면 점수가 낮은 날에는 실천지수가 함께 떨어졌습니다. 저녁 루틴을 먼저 완료하고 수면 준비 알림을 30분 앞당기는 전략이 좋습니다.")
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, icon: LuminineIcon = LuminineIcon.Chart) {
    Card(modifier = modifier, shape = CardShape) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTile(icon, label, size = 32.dp, background = ReverseGold.copy(alpha = 0.13f), tint = ReverseGold)
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthlyCalendar(buckets: Map<LocalDate, CompletionBucket>) {
    Card(shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("5월 달성 캘린더", LuminineIcon.Chart)
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
    Card(shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("7일 컨디션", LuminineIcon.Energy)
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
    Card(shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("루틴별 달성률", LuminineIcon.Trophy)
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)), shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle("AI 인사이트", LuminineIcon.Sparkles, ReverseGreen)
            Text(text)
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: LuminineIcon, tint: Color = ReverseGold) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        LuminineIconView(icon, null, Modifier.size(19.dp), tint = tint)
        Text(title, fontWeight = FontWeight.Bold, color = if (tint == ReverseGold) MaterialTheme.colorScheme.onSurface else tint)
    }
}

@Composable
private fun HealthInfoScreen(modifier: Modifier, onOpenTopic: (HealthTopicKey) -> Unit) {
    val cards = healthTopics()
    // healthTopics() order is fixed and 1:1 with HealthTopicKey.entries (산화 스트레스, 비타민C,
    // 운동 과학, 수면과 노화, 연구 백과); map by index so each card opens the matching content URL.
    val topicKeys = HealthTopicKey.entries
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(LuminineIcon.Book, "건강정보", size = 44.dp, background = ReverseGreen.copy(alpha = 0.14f), tint = ReverseGreen)
                Text("건강정보", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { colIndex, topic ->
                            val flatIndex = rowIndex * 2 + colIndex
                            val key = topicKeys.getOrNull(flatIndex)
                            TopicCard(
                                topic,
                                Modifier.weight(1f),
                                onClick = if (key != null) ({ onOpenTopic(key) }) else null,
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    IconLabel("스킨케어 성분", LuminineIcon.Drop, "스킨케어 성분"),
                    IconLabel("안티에이징 식단", LuminineIcon.Plate, "안티에이징 식단"),
                    IconLabel("마음건강", LuminineIcon.Mind, "마음건강"),
                ).forEach {
                    AssistChip(
                        onClick = {},
                        leadingIcon = { LuminineIconView(it.icon, null, Modifier.size(16.dp), tint = ReverseGold) },
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
            Card(shape = CardShape) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(LuminineIcon.Book, "아티클", size = 38.dp, background = ReverseGold.copy(alpha = 0.12f), tint = ReverseGold)
                    Column(Modifier.weight(1f)) {
                        Text(article, fontWeight = FontWeight.SemiBold)
                        Text("R의 건강로그 연구 노트", style = MaterialTheme.typography.labelMedium, color = ReverseGold)
                    }
                }
            }
        }
        item {
            Card(shape = CardShape) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("YouTube 최신 영상", LuminineIcon.Youtube)
                    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(8.dp)).background(ReverseEspresso), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LuminineIconView(LuminineIcon.Play, "재생", Modifier.size(42.dp), tint = Color.White)
                            Text("R의 건강로그", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: IconLabel, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val cardModifier = modifier
        .heightIn(min = 92.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Card(modifier = cardModifier, shape = CardShape) {
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
            Card(colors = CardDefaults.cardColors(containerColor = ReverseGreen), shape = CardShape) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("LUMÍNINE 1:1 케어", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("카카오 오픈채팅으로 식단, 인바디, 영양제 기록을 보내세요.", color = Color.White)
                        }
                        IconTile(LuminineIcon.Care, "1:1 케어", size = 54.dp, background = Color.White.copy(alpha = 0.18f), tint = Color.White)
                    }
                    OutlinedButton(onClick = {}, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))) {
                        LuminineIconView(LuminineIcon.Link, null, Modifier.size(18.dp), tint = Color.White)
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
            Card(shape = CardShape) {
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
    Card(shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("오늘 케어 현황", LuminineIcon.Check)
            listOf(
                IconLabel("식단 전송 완료", LuminineIcon.Plate, "식단 전송"),
                IconLabel("영양제 복용 확인", LuminineIcon.Supplement, "영양제 복용"),
                IconLabel("인바디 측정 결과 대기중", LuminineIcon.Body, "인바디 측정"),
                IconLabel("스킨케어 루틴 인증 완료", LuminineIcon.Drop, "스킨케어 인증"),
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
    Card(shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("최근 인바디", LuminineIcon.Body)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("체중", "${inbody.weight}kg", Modifier.weight(1f), LuminineIcon.Body)
                MetricCard("체지방", "${inbody.bodyFatPct}%", Modifier.weight(1f), LuminineIcon.Chart)
                MetricCard("골격근", "${inbody.muscleMass}kg", Modifier.weight(1f), LuminineIcon.Dumbbell)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("BMR", "${inbody.bmr ?: "-"}", Modifier.weight(1f), LuminineIcon.Energy)
                MetricCard("체수분", "${inbody.bodyWater ?: "-"}L", Modifier.weight(1f), LuminineIcon.Drop)
                MetricCard("내장지방", "${inbody.visceralFat ?: "-"}", Modifier.weight(1f), LuminineIcon.Report)
            }
        }
    }
}

@Composable
private fun StepsCard() {
    Card(shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("이용 방법", LuminineIcon.Link)
            listOf(
                IconLabel("카카오 오픈채팅 연결", LuminineIcon.Link, "연결"),
                IconLabel("인바디·식단·영양제 메시지 전송", LuminineIcon.Message, "메시지 전송"),
                IconLabel("자동 분석 결과 확인", LuminineIcon.Sparkles, "AI 분석"),
                IconLabel("다음 루틴 조정", LuminineIcon.Check, "루틴 조정"),
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
private fun AdminDashboard(modifier: Modifier, records: List<DailyRecord>) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("현황", "회원", "분석")
    val members = SampleData.memberSummaries()

    Column(modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                val icon = listOf(LuminineIcon.Chart, LuminineIcon.User, LuminineIcon.Sparkles)[index]
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LuminineIconView(icon, null, Modifier.size(16.dp), tint = if (selectedTab == index) ReverseGold else MaterialTheme.colorScheme.onSurfaceVariant)
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
                MetricCard("제출 수", "${members.size}", Modifier.weight(1f), LuminineIcon.Check)
                MetricCard("평균 지수", "${members.map { it.score }.average().toInt()}", Modifier.weight(1f), LuminineIcon.Chart)
                MetricCard("100점", "${members.count { it.score == 100 }}", Modifier.weight(1f), LuminineIcon.Trophy)
            }
        }
        item {
            Card(shape = CardShape) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("오늘 제출 회원", LuminineIcon.User)
                    members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconTile(LuminineIcon.User, member.name, size = 28.dp, background = ReverseGreen.copy(alpha = 0.12f), tint = ReverseGreen)
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
            Card(shape = CardShape) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(
                        LuminineIcon.User,
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
            Card(colors = CardDefaults.cardColors(containerColor = ReverseCoral.copy(alpha = 0.12f)), shape = CardShape) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("주목할 회원", LuminineIcon.Alert, ReverseCoral)
                    attention.forEach {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LuminineIconView(LuminineIcon.Alert, null, Modifier.size(17.dp), tint = ReverseCoral)
                            Text("${it.name} · ${it.score}점 · 에너지 ${it.energy}/5")
                        }
                    }
                }
            }
        }
        item {
            Card(shape = CardShape) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("카카오 수신 로그", LuminineIcon.Message)
                    SampleData.kakaoMessages().forEach {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LuminineIconView(LuminineIcon.Message, null, Modifier.size(17.dp), tint = ReverseGold)
                            Text("${it.parsedType} · ${it.parsedDataSummary}")
                        }
                    }
                }
            }
        }
    }
}
