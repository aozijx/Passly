package com.aozijx.passly.data.repository.otp

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.OtpGenerator
import com.aozijx.passly.domain.entry.otp.OtpResult
import javax.inject.Inject

/** Consumes one HOTP value and writes its next counter in the caller's database transaction. */
internal class HotpCodeConsumptionTransaction @Inject constructor(
    private val secretFieldStore: SecretFieldStore,
) {
    suspend fun consume(
        db: AppDatabase,
        entryId: String,
        generate: (OtpConfig) -> OtpResult = OtpGenerator::generate,
    ): OtpResult {
        val secret = secretFieldStore.readAll(db, entryId)
        val config = secret.otp?.config
            ?.takeIf { it.type == OtpType.HOTP }
            ?: return OtpResult.Failure(OtpGenerationError.InvalidSecret)

        return when (val result = generate(config)) {
            is OtpResult.Failure -> result
            is OtpResult.Success -> {
                val nextCounter = result.nextCounter
                    ?: return OtpResult.Failure(OtpGenerationError.InvalidCounter)
                secretFieldStore.updateExisting(
                    db,
                    entryId,
                    secret.copy(credential = OtpCredential(config.copy(counter = nextCounter))),
                )
                result
            }
        }
    }
}
