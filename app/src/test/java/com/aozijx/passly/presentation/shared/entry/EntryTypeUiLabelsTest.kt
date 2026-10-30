package com.aozijx.passly.presentation.shared.entry

import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryTypeUiLabelsTest {
    @Test
    fun everyDomainEntryTypeHasALocalizedLabel() {
        assertTrue(EntryType.entries.all { it.labelRes != 0 })
    }
}
