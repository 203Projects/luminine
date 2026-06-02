package com.luminine.app.content

// Stable keys for health-info topics. Placeholder URLs this phase; swap to real content URLs later
// by editing urlFor() only — the single integration seam (README "next-phase boundary").
enum class HealthTopicKey { OxidativeStress, VitaminC, ExerciseScience, SleepAging, ResearchWiki }

object HealthContent {
    private const val BASE = "https://luminine.example.com/health"

    fun urlFor(key: HealthTopicKey): String = when (key) {
        HealthTopicKey.OxidativeStress -> "$BASE/oxidative-stress"
        HealthTopicKey.VitaminC -> "$BASE/vitamin-c"
        HealthTopicKey.ExerciseScience -> "$BASE/exercise-science"
        HealthTopicKey.SleepAging -> "$BASE/sleep-aging"
        HealthTopicKey.ResearchWiki -> "$BASE/research-wiki"
    }

    fun titleFor(key: HealthTopicKey): String = when (key) {
        HealthTopicKey.OxidativeStress -> "산화 스트레스"
        HealthTopicKey.VitaminC -> "비타민C"
        HealthTopicKey.ExerciseScience -> "운동 과학"
        HealthTopicKey.SleepAging -> "수면과 노화"
        HealthTopicKey.ResearchWiki -> "연구 백과"
    }
}
