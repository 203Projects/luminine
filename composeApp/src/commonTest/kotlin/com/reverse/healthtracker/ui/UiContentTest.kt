package com.reverse.healthtracker.ui

import com.reverse.healthtracker.model.RoutineCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UiContentTest {
    @Test
    fun everyTopLevelDestinationHasDistinctIconAndLabel() {
        val destinations = topLevelDestinations()

        assertEquals(5, destinations.size)
        assertEquals(destinations.size, destinations.map { it.icon }.toSet().size)
        assertTrue(destinations.all { it.label.isNotBlank() })
        assertTrue(destinations.all { it.contentDescription.isNotBlank() })
    }

    @Test
    fun everyRoutineCategoryHasARelevantIcon() {
        RoutineCategory.entries.forEach { category ->
            assertNotEquals(ReverseIcon.Sparkles, category.icon())
        }
    }

    @Test
    fun quickActionsAreIconFirstAndHaveStableDestinations() {
        val actions = quickActions()

        assertEquals(8, actions.size)
        assertEquals(actions.size, actions.map { it.icon }.toSet().size)
        assertTrue(actions.all { it.label.length <= 8 })
    }
}
