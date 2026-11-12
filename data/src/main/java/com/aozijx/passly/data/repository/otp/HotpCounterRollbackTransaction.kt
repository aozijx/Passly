package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpType
import javax.inject.Inject

/** Rewinds one HOTP counter only while it still points at the value produced by the last copy. */
internal class HotpCounterRollbackTransaction
    @Inject
    constructor(
        private val secretFieldStore: SecretFieldStore,
    ) {
        suspend fun rollback(
            db: AppDatabase,
            entryId: String,
            expectedCurrentCounter: Long,
        ): Boolean {
            if (expectedCurrentCounter <= 0L) return false
            val entry =
                db
                    .entryQueryDao()
                    .getById(entryId)
                    ?.takeIf { it.deletedAt == null && it.entryType == EntryType.OTP }
                    ?: return false
            val bundle = secretFieldStore.readBundle(db, entryId, entry.entryType)
            val config =
                bundle.otp
                    ?.config
                    ?.takeIf { it.type == OtpType.HOTP && it.counter == expectedCurrentCounter }
                    ?: return false

            secretFieldStore.updateExisting(
                db,
                entryId,
                bundle.copy(
                    credential =
                        OtpCredential(
                            config.copy(
                                counter = expectedCurrentCounter - 1,
                                secret = null,
                            ),
                        ),
                ),
                entry.entryType,
            )
            return true
        }
    }
