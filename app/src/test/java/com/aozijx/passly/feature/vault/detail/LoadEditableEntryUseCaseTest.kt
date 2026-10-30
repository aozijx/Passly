package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadEditableEntryUseCaseTest {
    @Test
    fun revealsEveryPresentLoginSecretUnderOneExactScope() = runTest {
        val entry = entry(EntryType.LOGIN, LoginCredential(password = null))
        val harness = Harness(entry, setOf(SensitiveFieldKey.PASSWORD))
        harness.repository.values[SensitiveFieldKey.PASSWORD] = "login-secret"

        val result = harness.useCase()(ENTRY_ID)

        val loaded = result as EditableEntryLoadResult.Loaded
        assertEquals("login-secret", loaded.entry.secret.login?.password)
        assertEquals(
            AuthorizationScope.SensitiveFields(
                ENTRY_ID,
                setOf(SensitiveFieldKey.PASSWORD),
                SensitiveAccessAction.REVEAL,
            ),
            harness.gate.scope,
        )
        assertEquals(listOf(ActivityType.VIEW), harness.recorder.activities)
    }

    @Test
    fun revealsAllCardSecretsAndMapsThemToTheirTypedFields() = runTest {
        val entry = entry(EntryType.BANK_CARD, CardCredential())
        val keys = setOf(
            SensitiveFieldKey.CARD_NUMBER,
            SensitiveFieldKey.CARD_CVV,
            SensitiveFieldKey.CARD_PAYMENT_PIN,
        )
        val harness = Harness(entry, keys)
        harness.repository.values.putAll(
            mapOf(
                SensitiveFieldKey.CARD_NUMBER to "4111111111111111",
                SensitiveFieldKey.CARD_CVV to "123",
                SensitiveFieldKey.CARD_PAYMENT_PIN to "9876",
            ),
        )

        val loaded = harness.useCase()(ENTRY_ID) as EditableEntryLoadResult.Loaded

        assertEquals("4111111111111111", loaded.entry.secret.card?.cardNumber)
        assertEquals("123", loaded.entry.secret.card?.cardCvv)
        assertEquals("9876", loaded.entry.secret.card?.paymentPin)
        assertEquals(keys, (harness.gate.scope as AuthorizationScope.SensitiveFields).fieldKeys)
    }

    @Test
    fun loadsOtpHighSensitivityValueAndWifiBundleValueIntoTheirOwnCredentialTypes() = runTest {
        val otp = Harness(entry(EntryType.OTP, OtpCredential(OtpConfig(secret = null))), setOf(SensitiveFieldKey.OTP_SECRET))
        otp.repository.values[SensitiveFieldKey.OTP_SECRET] = "JBSWY3DPEHPK3PXP"
        val wifi = Harness(entry(EntryType.WIFI, WifiCredential("network", password = "wifi-secret")), emptySet())

        val loadedOtp = otp.useCase()(ENTRY_ID) as EditableEntryLoadResult.Loaded
        val loadedWifi = wifi.useCase()(ENTRY_ID) as EditableEntryLoadResult.Loaded

        assertEquals("JBSWY3DPEHPK3PXP", loadedOtp.entry.secret.otp?.config?.secret)
        assertEquals("wifi-secret", loadedWifi.entry.secret.wifi?.password)
    }

    @Test
    fun deniedOrIncompleteRevealDoesNotReturnAnEditableEntry() = runTest {
        val entry = entry(EntryType.LOGIN, LoginCredential())
        val denied = Harness(entry, setOf(SensitiveFieldKey.PASSWORD), allow = false)
        assertEquals(EditableEntryLoadResult.AccessDenied, denied.useCase()(ENTRY_ID))

        val partial = Harness(entry, setOf(SensitiveFieldKey.PASSWORD)).apply {
            repository.returnedKeys = emptySet()
        }
        assertEquals(EditableEntryLoadResult.Unavailable, partial.useCase()(ENTRY_ID))
        assertTrue(partial.recorder.activities.isEmpty())

        val cancelled = Harness(entry, setOf(SensitiveFieldKey.PASSWORD), cancel = true)
        assertEquals(EditableEntryLoadResult.Cancelled, cancelled.useCase()(ENTRY_ID))
    }

    @Test
    fun sessionReplacementDuringRevealRejectsTheLoadedValues() = runTest {
        val entry = entry(EntryType.LOGIN, LoginCredential())
        val harness = Harness(entry, setOf(SensitiveFieldKey.PASSWORD))
        harness.repository.onReveal = {
            harness.access.authenticationState.value = AuthenticationState.Authenticated(2)
        }

        assertEquals(EditableEntryLoadResult.Unavailable, harness.useCase()(ENTRY_ID))
        assertTrue(harness.recorder.activities.isEmpty())
    }

    @Test
    fun unsupportedAndMissingEntriesFailClosed() = runTest {
        val unsupported = Harness(entry(EntryType.NOTE, EntryCredential.None), emptySet())
        assertEquals(EditableEntryLoadResult.UnsupportedType, unsupported.useCase()(ENTRY_ID))
        assertEquals(EditableEntryLoadResult.NotFound, Harness(null, emptySet()).useCase()(ENTRY_ID))
    }

    private class Harness(
        entry: Entry?,
        keys: Set<SensitiveFieldKey>,
        allow: Boolean = true,
        cancel: Boolean = false,
    ) {
        val access = TestAccess()
        val repository = TestSensitiveRepository(entry?.id ?: ENTRY_ID, keys)
        val gate = TestAuthorizationGate(allow, cancel)
        val recorder = TestActivityRecorder()
        private val query = TestQuery(entry)

        fun useCase() = LoadEditableEntryUseCase(query, repository, gate, access, recorder)
    }

    private class TestAccess : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1))
        override fun isUnlocked() = true
    }

    private class TestQuery(var current: Entry?) : EntryQueryRepository {
        override suspend fun getById(entryId: EntryId) = current
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun count() = if (current == null) 0 else 1
    }

    private class TestAuthorizationGate(private val allow: Boolean, private val cancel: Boolean) : AuthorizationGate {
        var scope: AuthorizationScope? = null

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            this.scope = scope
            if (cancel) return AuthorizationResult.Cancelled
            return if (allow) AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            else AuthorizationResult.Denied(
                AuthenticationFailure(AuthenticationFailureCode.HOST_UNAVAILABLE),
            )
        }
    }

    private class TestSensitiveRepository(
        private val entryId: EntryId,
        private val present: Set<SensitiveFieldKey>,
    ) : SensitiveFieldRepository {
        val values = mutableMapOf<SensitiveFieldKey, String>()
        var returnedKeys: Set<SensitiveFieldKey> = present
        var onReveal: (() -> Unit)? = null

        override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, present)

        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> {
            onReveal?.invoke()
            return (keys intersect returnedKeys).mapNotNull { key ->
                values[key]?.let { RevealedSensitiveField(entryId, key, OwnedChars.fromString(it)) }
            }
        }

        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = null

        override suspend fun readBundle(entryId: EntryId) = EntrySecret()
        override suspend fun readAll(entryId: EntryId) = EntrySecret()
    }

    private class TestActivityRecorder : ActivityRecorder {
        val activities = mutableListOf<ActivityType>()
        override suspend fun recordUsage(entryId: String, type: ActivityType): AppResult<Unit> {
            activities += type
            return AppResult.Success(Unit)
        }
        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private fun entry(type: EntryType, credential: EntryCredential) = Entry(
        identity = EntryIdentity(ENTRY_ID, type, EntryVersion(3), EntryTimestamps(1, 2)),
        profile = EntryProfile("Entry"),
        secret = EntrySecret(credential = credential),
    )

    private companion object {
        val ENTRY_ID = EntryId("editable-entry")
    }
}
