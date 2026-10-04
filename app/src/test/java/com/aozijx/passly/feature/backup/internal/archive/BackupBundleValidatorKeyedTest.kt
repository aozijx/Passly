package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupBundleValidatorKeyedTest {
    @Test
    fun validKeyedLoginDocumentPassesValidation() {
        BackupBundleValidator.validate(
            bundle = bundle(
                fields = listOf(
                    BackupFieldRecord("title", BackupFieldValue.text("Example")),
                    BackupFieldRecord("password", BackupFieldValue.text("secret")),
                    BackupFieldRecord("favorite", BackupFieldValue.boolean(false)),
                ),
            ),
            requireResourceData = false,
        )
    }

    @Test
    fun duplicateFieldKeyIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupBundleValidator.validate(
                bundle = bundle(
                    fields = listOf(
                        BackupFieldRecord("title", BackupFieldValue.text("One")),
                        BackupFieldRecord("title", BackupFieldValue.text("Two")),
                    ),
                ),
                requireResourceData = false,
            )
        }
    }

    @Test
    fun fieldWithMultiplePayloadsIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupBundleValidator.validate(
                bundle = bundle(
                    fields = listOf(
                        BackupFieldRecord("title", BackupFieldValue.text("Example")),
                        BackupFieldRecord(
                            "favorite",
                            BackupFieldValue(
                                kind = BackupFieldValue.KIND_BOOLEAN,
                                text = "false",
                                booleanValue = false,
                            ),
                        ),
                    ),
                ),
                requireResourceData = false,
            )
        }
    }

    private fun bundle(fields: List<BackupFieldRecord>) = BackupBundle(
        document = BackupDocument(
            format = BackupDocument.FORMAT,
            version = BackupDocument.CURRENT_VERSION,
            exportedAt = 10L,
            entries = listOf(
                BackupEntryRecord(
                    id = "entry-1",
                    type = "login",
                    revision = 1,
                    createdAt = 1L,
                    updatedAt = 2L,
                    fields = fields,
                ),
            ),
        ),
    )
}
