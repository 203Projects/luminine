package com.luminine.app.ui

import com.luminine.app.model.RoutineCategory

enum class LuminineIcon {
    Home,
    Chart,
    Book,
    Care,
    Menu,
    Sparkles,
    Check,
    Plus,
    Trash,
    Pill,
    Dumbbell,
    Plate,
    Drop,
    Moon,
    Mind,
    Body,
    Camera,
    Report,
    Shop,
    Youtube,
    Cafe,
    Message,
    Alert,
    Admin,
    Energy,
    Skin,
    Sleep,
    Supplement,
    Link,
    Play,
    User,
    Trophy,
}

data class IconLabel(
    val label: String,
    val icon: LuminineIcon,
    val contentDescription: String,
)

fun topLevelDestinations(): List<IconLabel> = listOf(
    IconLabel("홈", LuminineIcon.Home, "오늘의 루틴"),
    IconLabel("차트", LuminineIcon.Chart, "기록 차트"),
    IconLabel("건강정보", LuminineIcon.Book, "건강 정보"),
    IconLabel("1:1케어", LuminineIcon.Care, "일대일 케어"),
    IconLabel("Shop", LuminineIcon.Shop, "루미닌 숍"),
)

fun RoutineCategory.icon(): LuminineIcon = when (this) {
    RoutineCategory.InnerCare -> LuminineIcon.Pill
    RoutineCategory.Exercise -> LuminineIcon.Dumbbell
    RoutineCategory.Diet -> LuminineIcon.Plate
    RoutineCategory.SkinCare -> LuminineIcon.Drop
    RoutineCategory.Sleep -> LuminineIcon.Moon
    RoutineCategory.Mind -> LuminineIcon.Mind
}

fun quickActions(): List<IconLabel> = listOf(
    IconLabel("인바디", LuminineIcon.Body, "인바디 기록"),
    IconLabel("식단", LuminineIcon.Plate, "식단 기록"),
    IconLabel("영양제", LuminineIcon.Supplement, "영양제 기록"),
    IconLabel("스킨케어", LuminineIcon.Drop, "스킨케어 인증"),
    IconLabel("사진", LuminineIcon.Camera, "사진 타임라인"),
    IconLabel("리포트", LuminineIcon.Report, "리포트"),
    IconLabel("Shop", LuminineIcon.Shop, "루미닌 숍"),
    IconLabel("문의", LuminineIcon.Message, "문의"),
)

fun healthTopics(): List<IconLabel> = listOf(
    IconLabel("산화 스트레스", LuminineIcon.Sparkles, "산화 스트레스 정보"),
    IconLabel("비타민C", LuminineIcon.Pill, "비타민C 정보"),
    IconLabel("운동 과학", LuminineIcon.Dumbbell, "운동 과학 정보"),
    IconLabel("수면과 노화", LuminineIcon.Moon, "수면과 노화 정보"),
    IconLabel("연구 백과", LuminineIcon.Book, "연구 백과"),
)
