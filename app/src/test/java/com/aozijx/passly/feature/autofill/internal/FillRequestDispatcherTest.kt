package com.aozijx.passly.feature.autofill.internal

import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import com.aozijx.passly.domain.autofill.port.AutofillHintProvider
import com.aozijx.passly.domain.autofill.model.AutofillStatus
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.port.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.CredentialCandidate
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FillRequestDispatcherTest {

    private fun createDispatcher(
        sessionState: SecureSessionAccessState,
        fieldMatchStrategy: FieldMatchStrategy,
    ) = FillRequestDispatcher(
        sessionState = sessionState,
        candidateRetriever = CandidateRetriever(EmptyCredentialRepository),
        settingsRepository = DefaultSettingsRepository,
        grantStore = object : AutofillGrantStore {
            override fun grant(context: AutofillGrantContext) = Unit
            override fun isGranted(context: AutofillGrantContext) = false
            override fun clear() = Unit
        },
        fieldMatchStrategy = fieldMatchStrategy
    )

    @Test
    fun `locked vault does not offer unlock when page has no credential fields`() = runBlocking {
        val dispatcher = createDispatcher(
            sessionState = LockedSecureSessionAccessState(),
            fieldMatchStrategy = object : FieldMatchStrategy {
                override fun match(request: AutofillRequest) =
                    MatchResult(hasCredentials = false)
            }
        )

        val response = dispatcher.dispatch(
            AutofillRequest(packageName = "com.example", domain = null, fields = emptyList(), source = AutofillSource.AUTOFILL_SERVICE)
        )

        assertEquals(AutofillStatus.UNSUPPORTED_FIELDS, response.status)
    }

    @Test
    fun `plain text field without credential hints does not trigger autofill`() = runBlocking {
        val dispatcher = createDispatcher(
            sessionState = LockedSecureSessionAccessState(),
            fieldMatchStrategy = HeuristicMatchStrategy(object : AutofillHintProvider {
                override fun getUsernamePattern() = Regex("(?!)")
                override fun getPasswordPattern() = Regex("(?!)")
                override fun getOtpPattern() = Regex("(?!)")
                override fun getSubmitPattern() = Regex("(?!)")
                override fun getSearchPattern() = Regex("search")
                override fun getConfirmationPattern() = Regex("(?!)")
                override fun getHintRoleMap() = emptyMap<String, FieldRole>()
            })
        )

        val response = dispatcher.dispatch(
            AutofillRequest(
                packageName = "com.example",
                domain = null,
                fields = listOf(
                    AutofillField(
                        id = "id/et_1",
                        hints = emptySet(),
                        inputType = "TYPE_CLASS_TEXT",
                        isFocused = true
                    )
                ),
                source = AutofillSource.AUTOFILL_SERVICE
            )
        )

        assertEquals(AutofillStatus.UNSUPPORTED_FIELDS, response.status)
    }

    @Test
    fun `stylized password field triggers unlock by inputType`() = runBlocking {
        val dispatcher = createDispatcher(
            sessionState = LockedSecureSessionAccessState(),
            fieldMatchStrategy = HeuristicMatchStrategy(object : AutofillHintProvider {
                override fun getUsernamePattern() = Regex("(?!)")
                override fun getPasswordPattern() = Regex("(?!)")
                override fun getOtpPattern() = Regex("(?!)")
                override fun getSubmitPattern() = Regex("(?!)")
                override fun getSearchPattern() = Regex("search")
                override fun getConfirmationPattern() = Regex("(?!)")
                override fun getHintRoleMap() = emptyMap<String, FieldRole>()
            })
        )

        val response = dispatcher.dispatch(
            AutofillRequest(
                packageName = "com.example",
                domain = null,
                fields = listOf(
                    AutofillField(
                        id = "id/et_1",
                        hints = emptySet(),
                        inputType = "TYPE_CLASS_TEXT TEXT_VARIATION_PASSWORD",
                        isFocused = true
                    )
                ),
                source = AutofillSource.AUTOFILL_SERVICE
            )
        )

        assertEquals(AutofillStatus.LOCKED, response.status)
    }

    @Test
    fun `dispatch propagates role map for two-phase filling`() = runBlocking {
        val dispatcher = createDispatcher(
            sessionState = UnlockedSecureSessionAccessState(),
            fieldMatchStrategy = HeuristicMatchStrategy(object : AutofillHintProvider {
                override fun getUsernamePattern() = Regex("user")
                override fun getPasswordPattern() = Regex("pass")
                override fun getOtpPattern() = Regex("otp")
                override fun getSubmitPattern() = Regex("submit")
                override fun getSearchPattern() = Regex("search")
                override fun getConfirmationPattern() = Regex("(?!)")
                override fun getHintRoleMap() = mapOf("username" to FieldRole.USERNAME)
            })
        )

        val response = dispatcher.dispatch(
            AutofillRequest(
                packageName = "com.example",
                domain = null,
                fields = listOf(
                    AutofillField(
                        id = "id/user",
                        hints = setOf("username"),
                        inputType = "TYPE_CLASS_TEXT",
                        isFocused = true
                    ),
                    AutofillField(
                        id = "id/pwd",
                        hints = emptySet(),
                        inputType = "TYPE_CLASS_TEXT TEXT_VARIATION_PASSWORD",
                        isFocused = false
                    ),
                ),
                source = AutofillSource.AUTOFILL_SERVICE
            )
        )

        assertEquals(FieldRole.USERNAME, response.roleMap["id/user"])
        assertEquals(FieldRole.PASSWORD, response.roleMap["id/pwd"])
    }

    private class UnlockedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
        override fun isUnlocked(): Boolean = true
    }

    private class LockedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
        override fun isUnlocked(): Boolean = false
    }

    private object DefaultSettingsRepository : AppSettingsRepository {
        override val settings: Flow<AppSettingsSnapshot> = flowOf(
            AppSettingsSnapshot(
                appearance = AppearanceSettings(),
                interfacePrefs = InterfaceSettings(),
                security = SecuritySettings(),
                interaction = InteractionSettings(),
                messages = MessageSettings(),
                vault = LibraryViewSettings(),
                backup = BackupSettings(),
            )
        )
        override val isLockOnBackground: Flow<Boolean> = flowOf(false)
        override val lockTimeout: Flow<Long> = flowOf(60_000L)
        override suspend fun update(command: SettingsCommand) = Unit
    }

    private object EmptyCredentialRepository : CredentialServiceRepository {
        override suspend fun search(
            packageName: String?,
            webDomain: String?,
            allowUnmatched: Boolean,
            includeSecrets: Boolean,
            limit: Int,
        ): List<CredentialCandidate> = emptyList()

        override suspend fun getById(entryId: String): Entry? = null
        override suspend fun getByIds(
            entryIds: List<String>,
            includeSecrets: Boolean
        ): List<Entry> = emptyList()
        override suspend fun save(
            packageName: String?,
            webDomain: String?,
            pageTitle: String?,
            usernameValue: String,
            passwordValue: String,
        ): Boolean = false
    }
}
