package com.luminine.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KakaoMessageParserTest {
    @Test
    fun parsesInbodyDietAndSupplementsFromCareMessage() {
        val result = KakaoMessageParser.parse(
            """
            오늘 인바디 72.4kg 체지방 22.1% 골격근 31.8kg
            식단: 아침 샐러드, 점심 현미밥+닭가슴살, 저녁 미역국
            비타민C 3g, 오메가3 복용 완료!
            """.trimIndent(),
        )

        val inbody = assertNotNull(result.inbody)
        assertEquals(72.4, inbody.weight)
        assertEquals(22.1, inbody.bodyFatPct)
        assertEquals(31.8, inbody.muscleMass)
        assertEquals("샐러드", result.diet.first { it.mealType == MealType.Breakfast }.content)
        assertEquals("현미밥+닭가슴살", result.diet.first { it.mealType == MealType.Lunch }.content)
        assertEquals(listOf("비타민C 3g", "오메가3"), result.supplements)
    }

    @Test
    fun returnsUnknownWhenMessageHasNoSupportedHealthPattern() {
        val result = KakaoMessageParser.parse("오늘도 좋은 하루입니다")

        assertEquals(ParsedMessageType.Unknown, result.primaryType)
    }
}
