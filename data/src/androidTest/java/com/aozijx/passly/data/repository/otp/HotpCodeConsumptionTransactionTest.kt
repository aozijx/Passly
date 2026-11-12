package com.aozijx.passly.data.repository.otp

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.crypto.AesGcmCryptoEngine
import com.aozijx.passly.core.crypto.FieldEncryptor
import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.entry.SecretFieldCodec
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.TestDatabaseFactory
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.data.testing.TestFieldKeyProvider
import com.aozijx.passly.data.testing.TestSensitiveFieldCryptor
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HotpCodeConsumptionTransactionTest {
    private lateinit var database: AppDatabase
    private lateinit var fieldKeyProvider: TestFieldKeyProvider
    private lateinit var secretFieldStore: SecretFieldStore
    private lateinit var transaction: HotpCodeConsumptionTransaction

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            TestDatabaseFactory
                .inMemoryDatabaseBuilder(context)
                .allowMainThreadQueries()
                .build()
        fieldKeyProvider = TestFieldKeyProvider(ByteArray(32) { (it + 17).toByte() })
        val bundleEncryptor = FieldEncryptor(fieldKeyProvider, AesGcmCryptoEngine())
        secretFieldStore =
            SecretFieldStore(
                SecretFieldCodec(TestSensitiveFieldCryptor),
                SecretBundleCodec(bundleEncryptor),
                DatabaseClock(),
            )
        transaction = HotpCodeConsumptionTransaction(secretFieldStore)
    }

    @After
    fun tearDown() {
        database.close()
        fieldKeyProvider.clear()
    }

    @Test
    fun successfulGenerationPersistsNextCounterWithoutCreatingEntryRevision() =
        runBlocking {
            seed(counter = 7)

            val result =
                database.withTransaction {
                    transaction.consume(database, ENTRY_ID, expectedCounter = 7) { material ->
                        val counter = (material.config as OtpGenerationConfig.Hotp).counter
                        OtpResult.Success(code = "123456", nextCounter = counter + 1)
                    }
                }

            assertTrue(result is OtpResult.Success)
            assertEquals(
                8L,
                secretFieldStore
                    .readBundle(database, ENTRY_ID, EntryType.OTP)
                    .otp
                    ?.config
                    ?.let { (it as OtpConfig.Hotp).counter },
            )
            assertEquals(1, database.entryQueryDao().getById(ENTRY_ID)?.version)
            assertTrue(
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId(ENTRY_ID)
                    .first()
                    .isEmpty(),
            )
        }

    @Test
    fun failedGenerationDoesNotAdvanceCounter() =
        runBlocking {
            seed(counter = 7)

            transaction.consume(database, ENTRY_ID, expectedCounter = 7) {
                OtpResult.Failure(OtpGenerationError.InvalidSecret)
            }

            assertEquals(
                7L,
                secretFieldStore
                    .readBundle(database, ENTRY_ID, EntryType.OTP)
                    .otp
                    ?.config
                    ?.let { (it as OtpConfig.Hotp).counter },
            )
        }

    @Test
    fun missingOrNonOtpEntryReturnsInvalidSecret() =
        runBlocking {
            assertEquals(
                OtpResult.Failure(OtpGenerationError.InvalidSecret),
                transaction.consume(database, ENTRY_ID, expectedCounter = 0),
            )

            database.entryCommandDao().insertStrict(
                EntryEntity(entryId = ENTRY_ID, entryType = EntryType.LOGIN, title = "Login"),
            )
            assertEquals(
                OtpResult.Failure(OtpGenerationError.InvalidSecret),
                transaction.consume(database, ENTRY_ID, expectedCounter = 0),
            )
        }

    @Test
    fun unrelatedSensitiveFieldIsNotDecryptedDuringHOTPConsumption() =
        runBlocking {
            seed(counter = 7)
            database.secretFieldCommandDao().upsert(
                EntrySecretFieldEntity(
                    entryId = ENTRY_ID,
                    fieldKey = SensitiveFieldKey.PASSWORD.toStorageKey(),
                    valueCipher = byteArrayOf(1),
                    keyVersion = 1,
                    updatedAt = 1,
                ),
            )

            val result =
                database.withTransaction {
                    transaction.consume(database, ENTRY_ID, expectedCounter = 7) {
                        OtpResult.Failure(OtpGenerationError.InvalidSecret)
                    }
                }

            assertEquals(OtpResult.Failure(OtpGenerationError.InvalidSecret), result)
        }

    @Test
    fun staleExpectedCounterDoesNotGenerateOrAdvanceCounter() =
        runBlocking {
            seed(counter = 8)
            var generated = false

            val result =
                database.withTransaction {
                    transaction.consume(database, ENTRY_ID, expectedCounter = 7) {
                        generated = true
                        OtpResult.Success(code = "123456", nextCounter = 9)
                    }
                }

            assertEquals(OtpResult.Failure(OtpGenerationError.CounterChanged), result)
            assertFalse(generated)
            assertEquals(
                8L,
                secretFieldStore
                    .readBundle(database, ENTRY_ID, EntryType.OTP)
                    .otp
                    ?.config
                    ?.let { (it as OtpConfig.Hotp).counter },
            )
        }

    private suspend fun seed(counter: Long) {
        val config =
            OtpConfig.Hotp(
                secret = null,
                counter = counter,
            )
        database.withTransaction {
            database.entryCommandDao().insertStrict(
                EntryEntity(
                    entryId = ENTRY_ID,
                    entryType = EntryType.OTP,
                    title = "HOTP",
                    otpType = OtpType.HOTP,
                ),
            )
            secretFieldStore.replaceAll(
                database,
                ENTRY_ID,
                EntrySecret(credential = OtpCredential(config)),
                EntryType.OTP,
            )
        }
    }

    companion object {
        private const val ENTRY_ID = "hotp-entry"
    }
}
