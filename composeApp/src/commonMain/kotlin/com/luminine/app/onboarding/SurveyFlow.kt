package com.luminine.app.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luminine.app.model.AllergenComponent
import com.luminine.app.model.ConsultingInterest
import com.luminine.app.model.ExerciseFrequency
import com.luminine.app.model.Gender
import com.luminine.app.model.JobType
import com.luminine.app.model.MonthlyBudget
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Region
import com.luminine.app.model.SkinSymptom
import com.luminine.app.model.Supplement
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection
import com.luminine.app.model.WalkingTime
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.IconTile
import com.luminine.app.ui.components.LuminineIconView
import com.luminine.app.ui.theme.ReverseEspresso
import com.luminine.app.ui.theme.ReverseGold
import com.luminine.app.ui.theme.ReverseGreen

private val CardShape = RoundedCornerShape(20.dp)

private fun String.digitsOnly(): String = filter { it.isDigit() }

/**
 * Multi-step onboarding survey. Required steps (S0/S1/S6) cannot be skipped; skippable steps
 * (S2–S5/S7) offer "나중에 입력". A sensitive-info notice precedes S2; a reward screen ends the flow.
 * Section state survives back/next via the single hoisted [SurveyDraft]. onComplete receives the
 * fully-built, canonical SurveyResponse.
 */
@Composable
fun SurveyFlow(
    modifier: Modifier = Modifier,
    onComplete: (SurveyResponse) -> Unit,
) {
    val draft = rememberSurveyDraft()
    var current by remember { mutableStateOf(SurveyStep.S0) }

    fun goNext() { nextStep(current)?.let { current = it } }
    fun goBack() { previousStep(current)?.let { current = it } }
    fun skip(section: SurveySection) { draft.markSkipped(section); goNext() }

    Column(modifier.fillMaxSize().safeDrawingPadding()) {
        SurveyProgressBar(current)
        Box(Modifier.fillMaxSize()) {
            when (current) {
                SurveyStep.S0 -> SectionBasicInfo(draft, onNext = ::goNext)
                SurveyStep.S1 -> SectionBodyInfo(draft, onNext = ::goNext, onBack = ::goBack)
                SurveyStep.Notice -> SensitiveNotice(onContinue = ::goNext, onBack = ::goBack)
                SurveyStep.S2 -> SectionConditions(draft, onNext = ::goNext, onBack = ::goBack, onSkip = { skip(SurveySection.S2) })
                SurveyStep.S3 -> SectionSymptoms(draft, onNext = ::goNext, onBack = ::goBack, onSkip = { skip(SurveySection.S3) })
                SurveyStep.S4 -> SectionLifestyle(draft, onNext = ::goNext, onBack = ::goBack, onSkip = { skip(SurveySection.S4) })
                SurveyStep.S5 -> SectionSupplements(draft, onNext = ::goNext, onBack = ::goBack, onSkip = { skip(SurveySection.S5) })
                SurveyStep.S6 -> SectionGoals(draft, onNext = ::goNext, onBack = ::goBack)
                SurveyStep.S7 -> SectionBudget(draft, onNext = ::goNext, onBack = ::goBack, onSkip = { skip(SurveySection.S7) })
                SurveyStep.Reward -> RewardScreen(onFinish = { onComplete(draft.toResponse()) })
            }
        }
    }
}

@Composable
private fun SurveyProgressBar(current: SurveyStep) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(current.title, fontWeight = FontWeight.Bold, color = ReverseEspresso)
            stepLabel(current)?.let {
                Text(it, color = ReverseGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        LinearProgressIndicator(
            progress = { progressFraction(current) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color = ReverseGold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// Shared back / next / skip footer.
@Composable
private fun SectionFooter(
    nextEnabled: Boolean,
    onNext: () -> Unit,
    nextLabel: String = "다음",
    onBack: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CardShape,
        ) { Text(nextLabel) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (onBack != null) TextButton(onClick = onBack) { Text("이전") } else Spacer(Modifier.width(1.dp))
            if (onSkip != null) TextButton(onClick = onSkip) { Text("나중에 입력", color = ReverseGold) }
        }
    }
}

@Composable
private fun SectionScaffold(content: LazyListScopeContent) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

// Alias so SectionScaffold reads cleanly without importing LazyListScope at every call site.
private typealias LazyListScopeContent = androidx.compose.foundation.lazy.LazyListScope.() -> Unit

@Composable
private fun FieldCard(title: String, icon: LuminineIcon, content: @Composable () -> Unit) {
    Card(shape = CardShape) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                LuminineIconView(icon, null, Modifier.size(19.dp), tint = ReverseGold)
                Text(title, fontWeight = FontWeight.Bold)
            }
            content()
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

// ---------------- S0 기본 인적사항 (required) ----------------
@Composable
private fun SectionBasicInfo(draft: SurveyDraft, onNext: () -> Unit) {
    SectionScaffold {
        item {
            FieldCard("기본 인적사항", LuminineIcon.User) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft.name = it },
                    label = { Text("이름 (닉네임 가능)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.birthYear?.toString() ?: "",
                    onValueChange = { draft.birthYear = it.digitsOnly().take(4).toIntOrNull() },
                    label = { Text("출생연도 (예: 1990)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleSelectChips("성별", Gender.entries, draft.gender, { it.label }) { draft.gender = it }
                SingleSelectChips("거주 지역", Region.entries, draft.region, { it.label }) { draft.region = it }
            }
        }
        item { SectionFooter(nextEnabled = draft.s0Valid, onNext = onNext) }
    }
}

// ---------------- S1 신체 기본정보 (required) ----------------
@Composable
private fun SectionBodyInfo(draft: SurveyDraft, onNext: () -> Unit, onBack: () -> Unit) {
    SectionScaffold {
        item {
            FieldCard("신체 기본정보", LuminineIcon.Body) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.heightCm,
                        onValueChange = { draft.heightCm = it.digitsOnly() },
                        label = { Text("키 (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.weightKg,
                        onValueChange = { draft.weightKg = it.digitsOnly() },
                        label = { Text("체중 (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                UnknownableField("체지방률 (%)", draft.bodyFatPct, draft.bodyFatUnknown,
                    onValue = { draft.bodyFatPct = it.digitsOnly() },
                    onUnknown = { draft.bodyFatUnknown = it; if (it) draft.bodyFatPct = "" })
                UnknownableField("근육량 (kg)", draft.muscleKg, draft.muscleUnknown,
                    onValue = { draft.muscleKg = it.digitsOnly() },
                    onUnknown = { draft.muscleUnknown = it; if (it) draft.muscleKg = "" })
            }
        }
        item { SectionFooter(nextEnabled = draft.s1Valid, onNext = onNext, onBack = onBack) }
    }
}

@Composable
private fun UnknownableField(label: String, value: String, unknown: Boolean, onValue: (String) -> Unit, onUnknown: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            label = { Text(label) },
            enabled = !unknown,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        FilterChip(selected = unknown, onClick = { onUnknown(!unknown) }, label = { Text("모름") })
    }
}

// ---------------- Notice (민감정보 안내) ----------------
@Composable
private fun SensitiveNotice(onContinue: () -> Unit, onBack: () -> Unit) {
    SectionScaffold {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)), shape = CardShape) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        LuminineIconView(LuminineIcon.Alert, null, Modifier.size(18.dp), tint = ReverseGreen)
                        Text("민감정보 안내", fontWeight = FontWeight.Bold, color = ReverseGreen)
                    }
                    Text("입력하신 건강 정보는 맞춤 추천에만 사용되며, 동의 없이 제3자에게 제공되지 않습니다.")
                    Text(
                        "이어지는 질환·증상·복용 정보는 모두 건너뛸 수 있고, 마이페이지에서 언제든 입력할 수 있습니다.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { SectionFooter(nextEnabled = true, onNext = onContinue, nextLabel = "이해했어요", onBack = onBack) }
    }
}

// ---------------- S2 질환·병력 (skippable) ----------------
@Composable
private fun SectionConditions(draft: SurveyDraft, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    SectionScaffold {
        item {
            FieldCard("질환·병력", LuminineIcon.Report) {
                Text("현재 진단·관리 중인 질환이 있다면 자유롭게 적어주세요. 영양제 추천 시 금기 성분을 걸러내는 데 사용됩니다.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = draft.conditionsCustom,
                    onValueChange = { draft.conditionsCustom = it },
                    label = { Text("질환·병력 (예: 고혈압, 갑상선)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item { SectionFooter(nextEnabled = true, onNext = onNext, onBack = onBack, onSkip = onSkip) }
    }
}

// ---------------- S3 체감 증상 (skippable) ----------------
@Composable
private fun SectionSymptoms(draft: SurveyDraft, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    SectionScaffold {
        item {
            FieldCard("걱정되는 건강 문제", LuminineIcon.Skin) {
                Text("요즘 가장 신경 쓰이는 피부·외모 증상을 선택하세요. 홈 화면 콘텐츠 노출에 반영됩니다.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MultiSelectChipGrid(SkinSymptom.entries, draft.skinSymptoms, { it.label })
            }
        }
        item { SectionFooter(nextEnabled = true, onNext = onNext, onBack = onBack, onSkip = onSkip) }
    }
}

// ---------------- S4 생활습관 (skippable) ----------------
@Composable
private fun SectionLifestyle(draft: SurveyDraft, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    SectionScaffold {
        item {
            FieldCard("운동 습관", LuminineIcon.Dumbbell) {
                SingleSelectChips("현재 운동 여부", ExerciseFrequency.entries, draft.exerciseFrequency, { it.label }) { draft.exerciseFrequency = it }
                Text("운동 수준은 루틴 난이도와 추천 콘텐츠 조정에 사용됩니다.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            FieldCard("수면·스트레스", LuminineIcon.Moon) {
                RatingRow("수면의 질", draft.sleepQuality) { draft.sleepQuality = it }
                RatingRow("스트레스 수준", draft.stressLevel) { draft.stressLevel = it }
            }
        }
        item { SectionFooter(nextEnabled = true, onNext = onNext, onBack = onBack, onSkip = onSkip) }
    }
}

@Composable
private fun RatingRow(label: String, value: Int?, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value ?: "-"}/5", color = ReverseGold, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { step ->
                Box(
                    Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (value != null && step <= value) ReverseGold else ReverseGold.copy(alpha = 0.12f))
                        .clickable { onChange(step) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$step", color = if (value != null && step <= value) Color.White else ReverseGold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ---------------- S5 복용 중 (skippable) ----------------
@Composable
private fun SectionSupplements(draft: SurveyDraft, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    SectionScaffold {
        item {
            FieldCard("복용 중인 영양제", LuminineIcon.Supplement) {
                Text("성분 중복·충돌을 제외한 추천을 위해 사용됩니다.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MultiSelectChipGrid(Supplement.entries, draft.supplements, { it.label })
            }
        }
        item {
            FieldCard("처방약 / 알레르기", LuminineIcon.Pill) {
                Text("처방약 복용 여부", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = draft.takingPrescription == false, onClick = { draft.takingPrescription = false; draft.prescriptionNote = "" }, label = { Text("없음") })
                    FilterChip(selected = draft.takingPrescription == true, onClick = { draft.takingPrescription = true }, label = { Text("있음") })
                }
                if (draft.takingPrescription == true) {
                    OutlinedTextField(
                        value = draft.prescriptionNote,
                        onValueChange = { draft.prescriptionNote = it },
                        label = { Text("복용 중인 처방약 종류") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("알레르기 성분", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MultiSelectChipGrid(AllergenComponent.entries, draft.allergens, { it.label })
            }
        }
        item { SectionFooter(nextEnabled = true, onNext = onNext, onBack = onBack, onSkip = onSkip) }
    }
}

// ---------------- S6 관심영역 우선순위 (required) ----------------
@Composable
private fun SectionGoals(draft: SurveyDraft, onNext: () -> Unit, onBack: () -> Unit) {
    SectionScaffold {
        item {
            Card(shape = CardShape) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        LuminineIconView(LuminineIcon.Sparkles, null, Modifier.size(19.dp), tint = ReverseGold)
                        Text("안티에이징 관심 영역", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "가장 중요한 순서대로 탭하세요. 탭한 순서대로 1·2·3순위가 매겨지고, 홈 화면과 스토어 노출 순위에 반영됩니다.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(PriorityGoal.entries) { goal ->
            val rank = draft.goalRanks[goal]
            Card(shape = CardShape) {
                Row(
                    Modifier.fillMaxWidth().clickable { draft.toggleGoal(goal) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(50))
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
        item { SectionFooter(nextEnabled = draft.s6Valid, onNext = onNext, nextLabel = "거의 다 왔어요", onBack = onBack) }
    }
}

// ---------------- S7 라이프스타일 & 예산 (skippable) ----------------
@Composable
private fun SectionBudget(draft: SurveyDraft, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    SectionScaffold {
        item {
            FieldCard("라이프스타일 & 예산", LuminineIcon.Shop) {
                SingleSelectChips("직업 유형", JobType.entries, draft.jobType, { it.label }) { draft.jobType = it }
                SingleSelectChips("하루 평균 보행 시간", WalkingTime.entries, draft.walkingTime, { it.label }) { draft.walkingTime = it }
                SingleSelectChips("건강관리 월 예산", MonthlyBudget.entries, draft.monthlyBudget, { it.label }) { draft.monthlyBudget = it }
                SingleSelectChips("1:1 전문 컨설팅 관심", ConsultingInterest.entries, draft.consultingInterest, { it.label }) { draft.consultingInterest = it }
            }
        }
        item { SectionFooter(nextEnabled = true, onNext = onNext, nextLabel = "완료", onBack = onBack, onSkip = onSkip) }
    }
}

// ---------------- Reward ----------------
@Composable
private fun RewardScreen(onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        IconTile(LuminineIcon.Trophy, "완료", size = 72.dp, background = ReverseEspresso, tint = Color.White)
        Text("설문 완료!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "첫 달 혜택이 적용되었어요 — 오늘부터 나에게 맞춘 루틴을 시작해보세요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CardShape) {
            Text("홈으로 가기")
        }
    }
}

// Multi-select chips backed by a SnapshotStateList, wrapping rows of 2 to avoid horizontal scroll.
@Composable
private fun <T> MultiSelectChipGrid(options: List<T>, selected: MutableList<T>, labelOf: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { opt ->
                    FilterChip(
                        selected = opt in selected,
                        onClick = { if (opt in selected) selected.remove(opt) else selected.add(opt) },
                        label = { Text(labelOf(opt)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
