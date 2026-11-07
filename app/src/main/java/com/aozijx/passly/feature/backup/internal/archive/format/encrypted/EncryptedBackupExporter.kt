package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString

/** Composes the JSON document, ZIP archive and chunked container for export. */
@Singleton
class EncryptedBackupExporter
    @Inject
    constructor() {
        fun export(
            bundle: BackupBundle,
            password: CharArray,
            output: OutputStream,
        ) = export(bundle, password, output, KeyDerivation::deriveKeyBytesArgon2id)

        internal fun export(
            bundle: BackupBundle,
            password: CharArray,
            output: OutputStream,
            deriveKey: (CharArray, ByteArray, KeyDerivation.Argon2idParameters) -> ByteArray,
        ) {
            BackupBundleValidator.validate(bundle, requireResourceData = bundle.document.resources.isNotEmpty())
            val documentJson = BackupJson.encodeToString(bundle.document).toByteArray(Charsets.UTF_8)
            val resources = bundle.resourceData.mapKeys { (id, _) -> "${BackupZipCodec.RESOURCE_ENTRY_PREFIX}$id" }
            val encrypted =
                ChunkedBackupOutputStream(
                    output,
                    password,
                    KeyDerivation.BASELINE_ARGON2ID_PARAMETERS,
                    deriveKey,
                )
            var completed = false
            try {
                BackupZipCodec.buildZip(documentJson, resources, encrypted)
                encrypted.close()
                completed = true
            } catch (error: Throwable) {
                encrypted.abort()
                throw error
            } finally {
                if (!completed) encrypted.abort()
                documentJson.fill(0)
            }
        }
    }
