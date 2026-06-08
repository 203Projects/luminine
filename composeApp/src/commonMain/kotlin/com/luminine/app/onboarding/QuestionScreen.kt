package com.luminine.app.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luminine.app.model.Gender
import com.luminine.app.model.ImmuneAllergyCondition
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Region
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.IconTile
import com.luminine.app.ui.components.LuminineIconView
import com.luminine.app.ui.theme.ReverseEspresso
import com.luminine.app.ui.theme.ReverseGold
import com.luminine.app.ui.theme.ReverseGreen
import kotlinx.coroutines.delay

// Calm-gamification motion: deliberately short + subtle so no reduce-motion toggle is needed.
private const val ANIM_MS = 300            // progress fills + scale entrances
private const val AUTO_ADVANCE_MS = 250L   // single-choice "snap" delay before advancing
private val CardShape = RoundedCornerShape(20.dp)

private fun String.digitsOnly(): String = filter { it.isDigit() }

/**
 * Renders ONE [SurveyQuestion] full-screen (the Duolingo-style one-question-per-screen flow).
 * Shared chrome: a segmented progress bar, a section eyebrow, a big prompt + optional helper, a
 * type-specific answer area, and a footer (계속 / 이전 / 나중에 입력). SingleChoice auto-advances on tap;
 * everything else uses 계속. All state lives in the hoisted [draft].
 */
@Composable
fun QuestionScreen(
    question: SurveyQuestion,
    draft: SurveyDraft,
    onNext: () -> Unit,
    onBack: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SegmentedProgressBar(question)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header (prompt + helper), hidden for the bare checkpoint/reward which provide their own.
            val info = question as? SurveyQuestion.Info
            if (info?.kind != InfoKind.Checkpoint && info?.kind != InfoKind.Reward) {
                item { QuestionHeader(question) }
            }
            // Answer area.
            item { AnswerArea(question, draft, onNext) }
            // Footer.
            item {
                QuestionFooter(
                    question = question,
                    enabled = isAnswered(question, draft),
                    onNext = onNext,
                    onBack = onBack,
                    onSkip = onSkip,
                )
            }
        }
    }
}

// ---- chrome ----

@Composable
private fun SegmentedProgressBar(question: SurveyQuestion) {
    val (activeIdx, frac) = segmentInfo(question)
    val animFrac by animateFloatAsState(targetValue = frac, animationSpec = tween(ANIM_MS), label = "seg")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(8) { i ->
            val fill = when {
                i < activeIdx -> 1f
                i == activeIdx -> animFrac
                else -> 0f
            }
            Box(
                Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50))
                    .background(ReverseGold.copy(alpha = 0.12f)),
            ) {
                Box(Modifier.fillMaxWidth(fill).height(6.dp).clip(RoundedCornerShape(50)).background(ReverseGold))
            }
        }
    }
}

@Composable
private fun QuestionHeader(question: SurveyQuestion) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (question !is SurveyQuestion.Info) {
            Text(sectionEyebrow(question), style = MaterialTheme.typography.labelMedium, color = ReverseGold, fontWeight = FontWeight.Bold)
        }
        Text(question.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ReverseEspresso)
        question.helper?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuestionFooter(
    question: SurveyQuestion,
    enabled: Boolean,
    onNext: () -> Unit,
    onBack: (() -> Unit)?,
    onSkip: (() -> Unit)?,
) {
    val ctaLabel = (question as? SurveyQuestion.Info)?.ctaLabel ?: "계속"
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = onNext, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CardShape) {
            Text(ctaLabel)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (onBack != null) TextButton(onClick = onBack) { Text("이전") } else Spacer(Modifier.width(1.dp))
            if (onSkip != null) TextButton(onClick = onSkip) { Text("나중에 입력", color = ReverseGold) }
        }
    }
}

// ---- answer areas (dispatched by type) ----

@Composable
private fun AnswerArea(question: SurveyQuestion, draft: SurveyDraft, onNext: () -> Unit) {
    when (question) {
        is SurveyQuestion.SingleChoice<*> -> SingleChoiceArea(question, draft, onNext)
        is SurveyQuestion.MultiChoice<*> -> MultiChoiceArea(question, draft)
        is SurveyQuestion.Numeric -> NumericArea(question, draft)
        is SurveyQuestion.NumericPair -> NumericPairArea(question, draft)
        is SurveyQuestion.Rating -> RatingArea(question, draft)
        is SurveyQuestion.Ranked -> RankedArea(draft)
        is SurveyQuestion.Identity -> IdentityArea(draft)
        is SurveyQuestion.Info -> InfoArea(question, draft)
    }
}

// SingleChoice: large answer cards; tapping selects (gold fill + check + scale-pop) then auto-advances.
@Composable
private fun <T : Enum<T>> SingleChoiceArea(q: SurveyQuestion.SingleChoice<T>, draft: SurveyDraft, onNext: () -> Unit) {
    var justPicked by remember { mutableStateOf(false) }
    LaunchedEffect(justPicked) {
        if (justPicked) { delay(AUTO_ADVANCE_MS); onNext() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        q.options.forEach { opt ->
            val selected = q.get(draft) == opt
            AnswerCard(label = q.labelOf(opt), selected = selected) {
                q.set(draft, opt)
                justPicked = true
            }
        }
    }
}

// MultiChoice: tap toggles; no auto-advance. Conditional free-text appears when its chip is selected.
@Composable
private fun <T : Enum<T>> MultiChoiceArea(q: SurveyQuestion.MultiChoice<T>, draft: SurveyDraft) {
    val selected = q.selected(draft)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        q.options.forEach { opt ->
            val isOn = opt in selected
            AnswerCard(label = q.labelOf(opt), selected = isOn) {
                if (isOn) selected.remove(opt) else selected.add(opt)
            }
        }
        // Registry-declared conditional free-text (e.g. 식품 알레르기 직접 입력).
        q.conditional?.let { c ->
            if (c.whenSelected in selected) {
                ConditionalTextField(c.label, c.get(draft)) { c.set(draft, it) }
            }
        }
        // Special case: the 면역·알레르기 group has a SECOND conditional (환경성 알레르기) the single
        // `conditional` slot can't hold — render it directly off the draft (typed list, no generic).
        if (q.id == "s2.immune" && ImmuneAllergyCondition.EnvironmentalAllergy in draft.immuneAllergy) {
            ConditionalTextField("환경성 알레르기 직접 입력 (예: 꽃가루)", draft.environmentalAllergyText) {
                draft.environmentalAllergyText = it
            }
        }
    }
}

@Composable
private fun ConditionalTextField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumericArea(q: SurveyQuestion.Numeric, draft: SurveyDraft) {
    val unknown = q.unknownGet(draft)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = q.get(draft),
            onValueChange = { q.set(draft, it.digitsOnly()) },
            label = { Text(q.unit) },
            enabled = !unknown,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        if (q.unknownable) {
            FilterChip(selected = unknown, onClick = { q.unknownSet(draft, !unknown) }, label = { Text("모름") })
        }
    }
}

@Composable
private fun NumericPairArea(q: SurveyQuestion.NumericPair, draft: SurveyDraft) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = q.leftGet(draft), onValueChange = { q.leftSet(draft, it.digitsOnly()) },
            label = { Text(q.leftLabel) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = q.rightGet(draft), onValueChange = { q.rightSet(draft, it.digitsOnly()) },
            label = { Text(q.rightLabel) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RatingArea(q: SurveyQuestion.Rating, draft: SurveyDraft) {
    val value = q.get(draft)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("매우 나쁨", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value ?: "-"}/5", color = ReverseGold, fontWeight = FontWeight.Bold)
            Text("매우 좋음", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { step ->
                val on = value != null && step <= value
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (on) ReverseGold else ReverseGold.copy(alpha = 0.12f))
                        .clickable { q.set(draft, step) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$step", color = if (on) Color.White else ReverseGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Tap-to-rank goal list (copied idiom from the former SectionGoals).
@Composable
private fun RankedArea(draft: SurveyDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PriorityGoal.entries.forEach { goal ->
            val rank = draft.goalRanks[goal]
            Card(shape = CardShape) {
                Row(
                    Modifier.fillMaxWidth().clickable { draft.toggleGoal(goal) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(50))
                            .background(if (rank != null) ReverseGold else ReverseGold.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(rank?.toString() ?: "", color = if (rank != null) Color.White else ReverseGold, fontWeight = FontWeight.Bold)
                    }
                    Text(goal.label, Modifier.weight(1f), fontWeight = if (rank != null) FontWeight.Bold else FontWeight.Normal)
                    if (rank != null) LuminineIconView(LuminineIcon.Check, null, Modifier.size(18.dp), tint = ReverseGold)
                }
            }
        }
    }
}

// S0 mixed identity screen (copied idiom from the former SectionBasicInfo).
@Composable
private fun IdentityArea(draft: SurveyDraft) {
    Card(shape = CardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.name, onValueChange = { draft.name = it },
                label = { Text("이름 (닉네임 가능)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.birthYear?.toString() ?: "",
                    onValueChange = { draft.birthYear = it.digitsOnly().take(4).toIntOrNull() },
                    label = { Text("출생연도") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = draft.birthMonth?.toString() ?: "",
                    onValueChange = { draft.birthMonth = it.digitsOnly().take(2).toIntOrNull()?.coerceIn(1, 12) },
                    label = { Text("월") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.birthDay?.toString() ?: "",
                    onValueChange = { draft.birthDay = it.digitsOnly().take(2).toIntOrNull()?.coerceIn(1, 31) },
                    label = { Text("일") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
            }
            SingleSelectChips("성별", Gender.entries, draft.gender, { it.label }) { draft.gender = it }
            SingleSelectChips("거주 지역", Region.entries, draft.region, { it.label }) { draft.region = it }
        }
    }
}

// Info screens: Notice (incl. the special 처방약 screen), Checkpoint, Reward.
@Composable
private fun InfoArea(q: SurveyQuestion.Info, draft: SurveyDraft) {
    when (q.kind) {
        InfoKind.Notice -> if (q.id == "s5.prescription") PrescriptionArea(draft) else NoticeArea(q)
        InfoKind.Checkpoint -> CelebrationArea(q, icon = LuminineIcon.Sparkles)
        InfoKind.Reward -> CelebrationArea(q, icon = LuminineIcon.Trophy, big = true)
    }
}

@Composable
private fun NoticeArea(q: SurveyQuestion.Info) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
        shape = CardShape,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                LuminineIconView(LuminineIcon.Alert, null, Modifier.size(18.dp), tint = ReverseGreen)
                Text(q.prompt, fontWeight = FontWeight.Bold, color = ReverseGreen)
            }
            Text(q.body)
        }
    }
}

// 처방약 복용 여부 (있음/없음 + conditional note). Copied idiom from the former SectionSupplements.
@Composable
private fun PrescriptionArea(draft: SurveyDraft) {
    Card(shape = CardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("영양제 추천 충돌을 막기 위해 사용돼요.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = draft.takingPrescription == false, onClick = { draft.takingPrescription = false; draft.prescriptionNote = "" }, label = { Text("없음") })
                FilterChip(selected = draft.takingPrescription == true, onClick = { draft.takingPrescription = true }, label = { Text("있음") })
            }
            if (draft.takingPrescription == true) {
                OutlinedTextField(
                    value = draft.prescriptionNote, onValueChange = { draft.prescriptionNote = it },
                    label = { Text("복용 중인 처방약 종류") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Checkpoint + reward share a centered, scale-in celebration. Calm checkpoints (empty prompt) skip
// the celebratory headline.
@Composable
private fun CelebrationArea(q: SurveyQuestion.Info, icon: LuminineIcon, big: Boolean = false) {
    var shown by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (shown) 1f else 0.7f, animationSpec = spring(dampingRatio = 0.5f), label = "pop")
    LaunchedEffect(Unit) { shown = true }
    Column(
        Modifier.fillMaxWidth().heightIn(min = 360.dp).padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.scale(scale)) {
            IconTile(icon, q.prompt.ifBlank { "완료" }, size = if (big) 72.dp else 56.dp, background = if (big) ReverseEspresso else ReverseGold, tint = Color.White)
        }
        if (q.prompt.isNotBlank()) {
            Text(q.prompt, style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(q.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---- shared bits (copied idioms) ----

@Composable
private fun AnswerCard(label: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (selected) 1f else 0.98f, animationSpec = spring(dampingRatio = 0.45f), label = "card")
    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = if (selected) ReverseGold else MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(label, Modifier.weight(1f), color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) LuminineIconView(LuminineIcon.Check, null, Modifier.size(18.dp), tint = Color.White)
        }
    }
}

@Composable
private fun <T> SingleSelectChips(label: String, options: List<T>, selected: T?, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(selected = selected == opt, onClick = { onSelect(opt) }, label = { Text(labelOf(opt)) })
            }
        }
    }
}
