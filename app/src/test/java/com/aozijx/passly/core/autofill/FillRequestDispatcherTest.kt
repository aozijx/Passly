package com.aozijx.passly.core.autofill

import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy
import com.aozijx.passly.core.autofill.matcher.MatchResult
import com.aozijx.passly.core.autofill.model.FillAvailability
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.core.autofill.pipeline.ResponseFactory
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.repository.CredentialServiceRepository
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.lookup.CredentialCandidate
import com.aozijx.passly.domain.notice.model.AppMessageSettings
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FillRequestDispatcherTest {

    @Test
    fun `locked vault does not offer unlock when page has no credential fields`() = runBlocking {
        val dispatcher = FillRequestDispatcher(
            sessionState = LockedSecureSessionAccessState(),
            candidateResolver = CandidateResolver(EmptyCredentialRepository),
            fieldMatchStrategy = object : FieldMatchStrategy {
                override fun match(request: InternalFillRequest) =
                    MatchResult(hasCredentials = false)
            },
            responseFactory = ResponseFactory(),
            settingsRepository = DefaultSettingsRepository,
        )

        val response = dispatcher.dispatch(
            InternalFillRequest(parentPackage = "com.example", fields = emptyList())
        )

        assertEquals(FillAvailability.UNSUPPORTED_FIELDS, response.availability)
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
                messages = AppMessageSettings(),
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

        override suspend fun getById(entryId: String): EntryAggregate? = null
        override suspend fun getByIds(
            entryIds: List<String>,
            includeSecrets: Boolean
        ): List<EntryAggregate> = emptyList()
        override suspend fun save(
            packageName: String?,
            webDomain: String?,
            pageTitle: String?,
            usernameValue: String,
            passwordValue: String,
        ): Boolean = false
    }
}
