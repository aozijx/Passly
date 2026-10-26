package com.aozijx.passly.feature.backup.internal.archive.format.bitwarden

import com.aozijx.passly.domain.entry.policy.EntryValidation
import com.aozijx.passly.feature.backup.internal.archive.mapper.KeyedEntryArchiveMapper
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitwardenCardImportTest {
    private val adapter = BitwardenJsonImportAdapter()

    @Test
    fun completeExpiryUsesDomainFormatAndProducesAValidCard() {
        for (month in listOf("3", "03", " 3 ")) {
            val record = importCard(mapOf("expMonth" to month, "expYear" to "2030"))
            assertEquals("03/2030", record.fields.single { it.key == "card_expiration" }.value.text)
            assertTrue(EntryValidation.validate(KeyedEntryArchiveMapper.toEntry(record)).isEmpty())
            assertFalse(record.fields.any { it.key == "custom_fields" })
        }
    }

    @Test
    fun incompleteOrInvalidPartsRemainHiddenCustomFieldsWithoutGuessingADate() {
        val cases = listOf(
            mapOf("expMonth" to "3"),
            mapOf("expYear" to "2030"),
            mapOf("expMonth" to "3", "expYear" to "30"),
            mapOf("expMonth" to " 13 ", "expYear" to "2030"),
            mapOf("expMonth" to "0", "expYear" to "2030"),
            mapOf("expMonth" to "3", "expYear" to "0000"),
            mapOf("expMonth" to "０３", "expYear" to "2030"),
        )
        for (strategy in BackupImportStrategy.entries) {
            for (parts in cases) {
                val record = importCard(parts, strategy)
                assertFalse(record.fields.any { it.key == "card_expiration" })
                val preserved = record.fields.single { it.key == "custom_fields" }.value.customFields.orEmpty()
                assertEquals(parts.mapKeys { "Bitwarden.${it.key}" }, preserved.associate { it.name to it.value })
                assertTrue(preserved.all { it.kind == BackupCustomFieldValue.KIND_HIDDEN })
                assertTrue(EntryValidation.validate(KeyedEntryArchiveMapper.toEntry(record)).isEmpty())
            }
        }
    }

    @Test
    fun absentOrBlankExpiryCreatesNoSyntheticDateOrCustomFields() {
        for (parts in listOf(emptyMap(), mapOf("expMonth" to " ", "expYear" to ""))) {
            val record = importCard(parts)
            assertFalse(record.fields.any { it.key == "card_expiration" || it.key == "custom_fields" })
        }
    }

    @Test
    fun preservingPartialExpiryAppendsToExistingCustomFields() {
        val record = importCard(mapOf("expMonth" to "3"), includeCustomField = true)
        val preserved = record.fields.single { it.key == "custom_fields" }.value.customFields.orEmpty()
        assertEquals(mapOf("Existing" to "keep", "Bitwarden.expMonth" to "3"), preserved.associate { it.name to it.value })
        assertEquals(BackupCustomFieldValue.KIND_TEXT, preserved.single { it.name == "Existing" }.kind)
    }

    private fun importCard(
        parts: Map<String, String>,
        strategy: BackupImportStrategy = BackupImportStrategy.COMPATIBLE,
        includeCustomField: Boolean = false,
    ): BackupEntryRecord {
        val card = buildJsonObject {
            put("number", JsonPrimitive("4111111111111111"))
            parts.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
        }
        val custom = if (includeCustomField) """, "fields":[{"name":"Existing","value":"keep","type":0}]""" else ""
        val payload = """{"items":[{"id":"card-1","type":3,"name":"Card","card":$card$custom}]}"""
            .toByteArray(Charsets.UTF_8)
        return adapter.decode(payload, password = null, strategy = strategy).document.entries.single { it.type == "bank_card" }
    }
}
