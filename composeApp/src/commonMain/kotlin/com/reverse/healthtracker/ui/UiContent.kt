package com.reverse.healthtracker.ui

import com.reverse.healthtracker.model.RoutineCategory

enum class ReverseIcon {
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
    val icon: ReverseIcon,
    val contentDescription: String,
)

fun topLevelDestinations(): List<IconLabel> = listOf(
    IconLabel("홈", ReverseIcon.Home, "오늘의 루틴"),
    IconLabel("차트", ReverseIcon.Chart, "기록 차트"),
    IconLabel("건강정보", ReverseIcon.Book, "건강 정보"),
    IconLabel("1:1케어", ReverseIcon.Care, "일대일 케어"),
    IconLabel("메뉴", ReverseIcon.Menu, "메뉴"),
)

fun RoutineCategory.icon(): ReverseIcon = when (this) {
    RoutineCategory.InnerCare -> ReverseIcon.Pill
    RoutineCategory.Exercise -> ReverseIcon.Dumbbell
    RoutineCategory.Diet -> ReverseIcon.Plate
    RoutineCategory.SkinCare -> ReverseIcon.Drop
    RoutineCategory.Sleep -> ReverseIcon.Moon
    RoutineCategory.Mind -> ReverseIcon.Mind
}

fun quickActions(): List<IconLabel> = listOf(
    IconLabel("인바디", ReverseIcon.Body, "인바디 기록"),
    IconLabel("식단", ReverseIcon.Plate, "식단 기록"),
    IconLabel("영양제", ReverseIcon.Supplement, "영양제 기록"),
    IconLabel("스킨케어", ReverseIcon.Drop, "스킨케어 인증"),
    IconLabel("사진", ReverseIcon.Camera, "사진 타임라인"),
    IconLabel("리포트", ReverseIcon.Report, "리포트"),
    IconLabel("Shop", ReverseIcon.Shop, "리버스 숍"),
    IconLabel("문의", ReverseIcon.Message, "문의"),
)

fun healthTopics(): List<IconLabel> = listOf(
    IconLabel("산화 스트레스", ReverseIcon.Sparkles, "산화 스트레스 정보"),
    IconLabel("비타민C", ReverseIcon.Pill, "비타민C 정보"),
    IconLabel("운동 과학", ReverseIcon.Dumbbell, "운동 과학 정보"),
    IconLabel("수면과 노화", ReverseIcon.Moon, "수면과 노화 정보"),
    IconLabel("연구 백과", ReverseIcon.Book, "연구 백과"),
)
