package com.luminine.app.domain

enum class ParsedMessageType {
    Inbody,
    Diet,
    Supplement,
    Routine,
    Unknown,
}

enum class MealType(val label: String) {
    Breakfast("아침"),
    Lunch("점심"),
    Dinner("저녁"),
    Snack("간식"),
}

data class ParsedInbody(
    val weight: Double?,
    val bodyFatPct: Double?,
    val muscleMass: Double?,
    val bmr: Int?,
)

data class ParsedDiet(
    val mealType: MealType,
    val content: String,
)

data class ParsedCareMessage(
    val primaryType: ParsedMessageType,
    val inbody: ParsedInbody?,
    val diet: List<ParsedDiet>,
    val supplements: List<String>,
)

object KakaoMessageParser {
    private val decimal = """(\d+(?:\.\d+)?)"""
    private val inbodyKeywords = listOf("체중", "kg", "체지방", "근육", "골격근", "기초대사량", "BMR", "인바디")
    private val supplementKeywords = listOf("비타민", "영양제", "오메가", "마그네슘", "CoQ10", "코큐텐", "복용")

    fun parse(rawText: String): ParsedCareMessage {
        val text = rawText.trim()
        val inbody = parseInbody(text)
        val diet = parseDiet(text)
        val supplements = parseSupplements(text)
        val primaryType = when {
            inbody != null -> ParsedMessageType.Inbody
            diet.isNotEmpty() -> ParsedMessageType.Diet
            supplements.isNotEmpty() -> ParsedMessageType.Supplement
            else -> ParsedMessageType.Unknown
        }

        return ParsedCareMessage(
            primaryType = primaryType,
            inbody = inbody,
            diet = diet,
            supplements = supplements,
        )
    }

    private fun parseInbody(text: String): ParsedInbody? {
        if (inbodyKeywords.none { text.contains(it, ignoreCase = true) }) return null

        val weight = Regex("""(?:체중|인바디|오늘 인바디)?\s*:? ?$decimal\s*kg?""").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val bodyFat = Regex("""체지방(?:률)?\s*:? ?$decimal\s*%?""").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val muscle = Regex("""(?:골격근|근육량|근육)\s*:? ?$decimal\s*kg?""").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val bmr = Regex("""(?:기초대사량|BMR)\s*:? ?(\d+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()

        return if (weight != null || bodyFat != null || muscle != null || bmr != null) {
            ParsedInbody(weight = weight, bodyFatPct = bodyFat, muscleMass = muscle, bmr = bmr)
        } else {
            null
        }
    }

    private fun parseDiet(text: String): List<ParsedDiet> {
        val mealMap = mapOf(
            "아침" to MealType.Breakfast,
            "점심" to MealType.Lunch,
            "저녁" to MealType.Dinner,
            "간식" to MealType.Snack,
        )
        val regex = Regex("""(아침|점심|저녁|간식)\s*[:：]?\s*([^,\n/]+)""")
        return regex.findAll(text)
            .mapNotNull { match ->
                val mealType = mealMap[match.groupValues[1]] ?: return@mapNotNull null
                val content = match.groupValues[2]
                    .replace("식단:", "")
                    .replace("식단：", "")
                    .trim()
                if (content.isBlank()) null else ParsedDiet(mealType, content)
            }
            .toList()
    }

    private fun parseSupplements(text: String): List<String> {
        val lines = text.lines().filter { line ->
            supplementKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) }
        }
        return lines.flatMap { it.split(",", "/", "\n") }
            .map { item ->
                item.replace(Regex("""\b(복용|먹었어요|먹었습니다|완료)\b|!"""), "")
                    .trim()
            }
            .filter { item ->
                item.isNotBlank() && supplementKeywords.any { keyword -> item.contains(keyword, ignoreCase = true) }
            }
            .distinct()
    }
}
