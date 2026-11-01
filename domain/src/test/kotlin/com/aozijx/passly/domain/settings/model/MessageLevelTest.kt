package com.aozijx.passly.domain.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLevelTest {
    @Test
    fun everyMessageLevelPairUsesExplicitSeverityThresholds() {
        val levels = MessageLevel.entries
        levels.forEach { actual ->
            levels.forEach { minimum ->
                assertEquals(
                    "actual=$actual minimum=$minimum",
                    severity(actual) >= severity(minimum),
                    actual.meetsMinimum(minimum),
                )
            }
        }
    }

    private fun severity(level: MessageLevel): Int = when (level) {
        MessageLevel.INFO -> 10
        MessageLevel.SUCCESS -> 20
        MessageLevel.WARNING -> 30
        MessageLevel.ERROR -> 40
        MessageLevel.CRITICAL -> 50
    }
}
