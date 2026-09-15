package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailSectionActionHandlerTest {
    @Test
    fun copyDispatchesOnlyTheSemanticFieldKey() {
        val actions = mutableListOf<DetailUiAction>()
        val handler = DetailSectionActionHandler(actions::add)

        handler.copy(FieldKey.PASSWORD)

        assertEquals(listOf(DetailUiAction.CopyField(FieldKey.PASSWORD)), actions)
    }
}