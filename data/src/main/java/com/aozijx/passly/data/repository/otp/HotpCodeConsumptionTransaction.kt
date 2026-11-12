package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerator
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.otp.generation.toGenerationConfig
import javax.inject.Inject

/** Consumes one HOTP value and writes its next counter in the caller's database transaction. */
internal class HotpCodeConsumptionTransaction
    @Inject
    constructor(
        private val secretFieldStore: SecretFieldStore,
    ) {
        suspend fun consume(
            db: AppDatabase,
            entryId: String,
            expectedCounter: Long,
            generate: (OtpGenerationMaterial) -> OtpResult = { OtpGenerator.generate(it) },
        ): OtpResult {
            val entry =
                db
                    .entryQueryDao()
                    .getById(entryId)
                    ?.takeIf { it.deletedAt == null && it.entryType == EntryType.OTP }
                    ?: return OtpResult.Failure(OtpGenerationError.InvalidSecret)
            val bundle = secretFieldStore.readBundle(db, entryId, entry.entryType)
            val storedConfig = bundle.otp?.config as? OtpConfig.Hotp
                ?: return OtpResult.Failure(OtpGenerationError.InvalidSecret)
            if (storedConfig.counter != expectedCounter) {
                return OtpResult.Failure(OtpGenerationError.CounterChanged)
            }
            val generationConfig = storedConfig.toGenerationConfig()
            val config = storedConfig.copy(secret = null)
            val secret =
                secretFieldStore.readFieldBytes(db, entryId, SensitiveFieldKey.OTP_SECRET)
                    ?: return OtpResult.Failure(OtpGenerationError.InvalidSecret)
            val material =
                try {
                    OtpGenerationMaterial(generationConfig, secret)
                } catch (failure: Throwable) {
                    secret.fill(0)
                    throw failure
                }
            return try {
                when (val result = generate(material)) {
                    is OtpResult.Failure -> {
                        result
                    }

                    is OtpResult.CounterSuccess -> {
                        val nextCounter = result.nextCounter
                        secretFieldStore.updateExisting(
                            db,
                            entryId,
                            bundle.copy(
                                credential = OtpCredential(config.copy(counter = nextCounter, secret = null)),
                            ),
                            entry.entryType,
                        )
                        result
                    }

                    is OtpResult.Success -> OtpResult.Failure(OtpGenerationError.InvalidCounter)
                }
            } finally {
                material.close()
            }
        }
    }
