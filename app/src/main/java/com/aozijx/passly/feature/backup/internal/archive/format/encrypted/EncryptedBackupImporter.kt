package com.aozijx.passly.feature.backup.internal.archive.format.encrypted

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.BackupImportPlanner
import com.aozijx.passly.feature.backup.internal.archive.BackupIntegrityException
import com.aozijx.passly.feature.backup.internal.archive.BackupJson
import com.aozijx.passly.feature.backup.internal.archive.io.decodeStrictUtf8
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipException
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Composes the chunked decryptor, ZIP reader and document mapper for import. */
@Singleton
internal class EncryptedBackupImporter
    @Inject
    constructor() {
        fun import(
            input: InputStream,
            password: CharArray,
            strategy: BackupImportStrategy = BackupImportStrategy.STRICT,
        ): BackupBundle = import(input, password, strategy, KeyDerivation::deriveKeyBytesArgon2id)

        internal fun import(
            input: InputStream,
            password: CharArray,
            strategy: BackupImportStrategy,
            deriveKey: (CharArray, ByteArray, KeyDerivation.Argon2idParameters) -> ByteArray,
        ): BackupBundle {
            var archive: BackupZipCodec.ZipContent? = null
            var handedOff = false
            try {
                val bundle =
                    ChunkedBackupInputStream(input, password, deriveKey = deriveKey).use { decrypted ->
                        archive = BackupZipCodec.readZip(decrypted)
                        decrypted.copyTo(DISCARD_OUTPUT)
                        val content = requireNotNull(archive)
                        val document =
                            BackupJson.decodeFromString<BackupDocument>(
                                content.documentJson.decodeStrictUtf8("document.json"),
                            )
                        val resources =
                            content.resources.mapKeys { (name, _) ->
                                name.removePrefix(BackupZipCodec.RESOURCE_ENTRY_PREFIX)
                            }
                        BackupImportPlanner
                            .plan(
                                BackupBundle(document = document, resourceData = resources),
                                strategy,
                            ).bundle
                    }
                handedOff = true
                return bundle
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: BackupFailed) {
                throw error
            } catch (error: BackupTruncatedException) {
                throw BackupFailed(throwableType = "EOFException", reasonCode = error.reasonCode.value)
            } catch (error: BackupIntegrityException) {
                throw BackupFailed(reasonCode = error.reasonCode.value)
            } catch (error: AEADBadTagException) {
                throw BackupFailed(
                    throwableType = error.javaClass.simpleName,
                    reasonCode = Reasons.Backup.Authentication.BackupAuthenticationFailed.value,
                )
            } catch (error: ZipException) {
                throw BackupFailed(
                    throwableType = error.javaClass.simpleName,
                    reasonCode = Reasons.Backup.Archive.BackupZipMalformed.value,
                )
            } catch (error: Exception) {
                throw BackupFailed(
                    throwableType = error.javaClass.simpleName,
                    reasonCode = Reasons.Backup.Document.BackupDocumentMalformed.value,
                )
            } finally {
                archive?.documentJson?.fill(0)
                if (!handedOff) archive?.resources?.values?.forEach { it.fill(0) }
            }
        }

        private companion object {
            val DISCARD_OUTPUT =
                object : OutputStream() {
                    override fun write(value: Int) = Unit

                    override fun write(
                        buffer: ByteArray,
                        offset: Int,
                        length: Int,
                    ) = Unit
                }
        }
    }
