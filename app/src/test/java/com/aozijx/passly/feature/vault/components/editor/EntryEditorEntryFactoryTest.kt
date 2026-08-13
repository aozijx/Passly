package com.aozijx.passly.feature.vault.components.editor

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.model.AddType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryEditorEntryFactoryTest {

    @Test
    fun `toEntryAggregate stores user category in summary tags without changing entry type`() {
        val schema = AddType.BANK_CARD.toEntryEditorSchema()
        val state = EntryEditorFormState().apply {
            update(EntryEditorFieldKey.TITLE, "Card")
            update(EntryEditorFieldKey.SUMMARY, "User")
            update(EntryEditorFieldKey.TAGS, " Finance, 工作，finance ; 个人 ")
            update(EntryEditorFieldKey.SECRET, "4111111111111111")
        }

        val entry = schema.toEntryAggregate(state)

        assertEquals(EntryType.BANK_CARD, entry.entryType)
        assertEquals(listOf("Finance", "工作", "个人"), entry.summary.tags)
    }
}
