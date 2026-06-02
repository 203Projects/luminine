package com.luminine.app.content

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class HealthContentTest {
    @Test
    fun everyTopicMapsToAnHttpsUrl() {
        HealthTopicKey.entries.forEach { key ->
            assertTrue(HealthContent.urlFor(key).startsWith("https://"), "url for $key")
        }
    }

    @Test
    fun urlsAreDistinctPerTopic() {
        val urls = HealthTopicKey.entries.map { HealthContent.urlFor(it) }
        assertEquals(urls.size, urls.toSet().size)
    }
}
