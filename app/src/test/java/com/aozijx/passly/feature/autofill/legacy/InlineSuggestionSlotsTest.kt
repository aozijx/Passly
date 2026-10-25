package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.feature.autofill.legacy.service.builder.InlineSuggestionSlots
import org.junit.Assert.assertEquals
import org.junit.Test

class InlineSuggestionSlotsTest {
    @Test
    fun limitsCandidatesToKeyboardCapacity() {
        assertEquals(2, InlineSuggestionSlots.candidateCount(5, 2, 3))
        assertEquals(0, InlineSuggestionSlots.candidateCount(5, 0, 3))
        assertEquals(0, InlineSuggestionSlots.candidateCount(5, 2, 0))
    }

    @Test
    fun reusesLastSpecWhenKeyboardProvidesFewerSpecsThanSlots() {
        assertEquals(0, InlineSuggestionSlots.specIndex(0, 1))
        assertEquals(0, InlineSuggestionSlots.specIndex(2, 1))
        assertEquals(1, InlineSuggestionSlots.specIndex(2, 2))
    }
}
