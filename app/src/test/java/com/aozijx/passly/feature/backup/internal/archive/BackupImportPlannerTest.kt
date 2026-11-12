package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupImportPlannerTest {
    @Test
    fun structuralFailuresHaveSameReasonInBothImportStrategiesAndExportValidation() {
        val source = bundle(entry("note", listOf(BackupFieldRecord("title", BackupFieldValue.text("entry")))))
        val cases = listOf(
            source.copy(document = source.document.copy(format = "unknown")) to Reasons.BackupDocumentFormatUnsupported,
            source.copy(document = source.document.copy(version = BackupDocument.CURRENT_VERSION + 1)) to Reasons.BackupDocumentVersionUnsupported,
            source.copy(document = source.document.copy(entries = source.document.entries + source.document.entries)) to Reasons.BackupDocumentDuplicateEntryId,
        )
        cases.forEach { (invalid, reason) ->
            BackupImportStrategy.entries.forEach { strategy ->
                val failure = assertThrows(BackupIntegrityException::class.java) { BackupImportPlanner.plan(invalid, strategy) }
                assertEquals(reason, failure.reasonCode)
            }
            val failure = assertThrows(BackupIntegrityException::class.java) { BackupBundleValidator.validate(invalid, false) }
            assertEquals(reason, failure.reasonCode)
        }
    }

    @Test
    fun candidateValidationSkipsOnlyExpectedValidationExceptions() {
        assertFalse(
            BackupImportPlanner.isCandidateValid {
                throw IllegalArgumentException("invalid entry")
            },
        )

        val ioFailure = IOException("candidate validation infrastructure failed")
        val propagatedIoFailure =
            assertThrows(IOException::class.java) {
                BackupImportPlanner.isCandidateValid { throw ioFailure }
            }
        assertSame(ioFailure, propagatedIoFailure)

        val fatalError = AssertionError("fatal validator failure")
        val propagated =
            assertThrows(AssertionError::class.java) {
                BackupImportPlanner.isCandidateValid { throw fatalError }
            }

        assertSame(fatalError, propagated)
    }

    @Test
    fun compatibleImportIgnoresUnknownFields() {
        val source =
            bundle(
                entry(
                    "login",
                    listOf(
                        BackupFieldRecord("title", BackupFieldValue.text("Example")),
                        BackupFieldRecord("password", BackupFieldValue.text("secret")),
                        BackupFieldRecord("future_field", BackupFieldValue.text("future")),
                    ),
                ),
            )
        val plan = BackupImportPlanner.plan(source, BackupImportStrategy.COMPATIBLE)

        assertEquals(
            listOf("title", "password"),
            plan.bundle.document.entries
                .single()
                .fields
                .map { it.key },
        )
        assertEquals(1, plan.summary.ignoredFieldCount)
        assertEquals(0, plan.summary.skippedEntryCount)
        assertThrows(IllegalArgumentException::class.java) {
            BackupImportPlanner.plan(source, BackupImportStrategy.STRICT)
        }
    }

    @Test
    fun compatibleImportSkipsUnknownEntryTypeButStrictRejectsIt() {
        val source =
            bundle(
                entry("future_type", listOf(BackupFieldRecord("title", BackupFieldValue.text("Future")))),
                entry("note", listOf(BackupFieldRecord("title", BackupFieldValue.text("Known")))),
            )

        val compatible = BackupImportPlanner.plan(source, BackupImportStrategy.COMPATIBLE)

        assertEquals(
            listOf("note"),
            compatible.bundle.document.entries
                .map { it.type },
        )
        assertEquals(2, compatible.bundle.sourceEntryCount)
        assertEquals(1, compatible.summary.skippedEntryCount)
        assertThrows(IllegalArgumentException::class.java) {
            BackupImportPlanner.plan(source, BackupImportStrategy.STRICT)
        }
    }

    @Test
    fun compatibleImportSkipsEntryRejectedByStructuralValidation() {
        val plan =
            BackupImportPlanner.plan(
                bundle(
                    entry("note", listOf(BackupFieldRecord("password", BackupFieldValue.text("invalid")))),
                    entry(
                        "note",
                        listOf(BackupFieldRecord("title", BackupFieldValue.text("Known"))),
                    ).copy(id = "valid-note"),
                ),
                BackupImportStrategy.COMPATIBLE,
            )

        assertEquals(
            listOf("valid-note"),
            plan.bundle.document.entries
                .map { it.id },
        )
        assertEquals(1, plan.summary.skippedEntryCount)
    }

    @Test
    fun compatibleImportStillRejectsDuplicateTopLevelLinkIds() {
        val known =
            entry(
                "note",
                listOf(BackupFieldRecord("title", BackupFieldValue.text("Known"))),
            )
        val source =
            bundle(known).let { bundle ->
                bundle.copy(
                    document =
                        bundle.document.copy(
                            links =
                                listOf(
                                    BackupLinkRecord("link-1", known.id, "missing-1", "related_to", 1, 1),
                                    BackupLinkRecord("link-1", known.id, "missing-2", "related_to", 1, 1),
                                ),
                        ),
                )
            }

        assertThrows(IllegalArgumentException::class.java) {
            BackupImportPlanner.plan(source, BackupImportStrategy.COMPATIBLE)
        }
    }

    private fun bundle(vararg entries: BackupEntryRecord) =
        BackupBundle(
            BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = 1L,
                entries = entries.toList(),
            ),
        )

    private fun entry(
        type: String,
        fields: List<BackupFieldRecord>,
    ) = BackupEntryRecord(
        id = "entry-${type.hashCode().toUInt()}",
        type = type,
        revision = 1,
        createdAt = 1L,
        updatedAt = 1L,
        fields = fields,
    )
}
