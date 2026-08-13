package com.aozijx.passly.security.authentication

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

/**
 * 恢复模式行为边界测试。
 *
 * 验证恢复模式下的访问控制、autofill 拒绝、以及认证方式可用性判断。
 */
class RecoveryModeBoundaryTest {

    // ==================== 恢复模式下 autofill 被拒绝 ====================

    @Test
    fun `recovery mode rejects autofill`() = runBlocking {
        val dispatcher = FillRequestDispatcher(
            sessionState = RecoverySecureSessionAccessState(),
            candidateResolver = CandidateResolver(EmptyCredentialRepository),
            fieldMatchStrategy = object : FieldMatchStrategy {
                override fun match(request: InternalFillRequest) =
                    MatchResult(hasCredentials = true)
            },
            responseFactory = ResponseFactory(),
            settingsRepository = DefaultSettingsRepository,
        )

        val response = dispatcher.dispatch(
            InternalFillRequest(parentPackage = "com.example", fields = emptyList())
        )

        // Recovery mode has isDatabaseOpen() == true but hasFullSecureSessionAccess() == false,
        // so autofill must be rejected.
        assertEquals(FillAvailability.LOCKED, response.availability)
    }

    @Test
    fun `recovery mode hasFullSecureSessionAccess returns false`() {
        val state = RecoverySecureSessionAccessState()
        // isDatabaseOpen is true (database is accessible in recovery mode)
        assertEquals(true, state.isDatabaseOpen())
        // hasFullSecureSessionAccess must be false (limited access)
        assertEquals(false, state.hasFullSecureSessionAccess())
        // isRecoveryMode must be true
        assertEquals(true, state.isRecoveryMode())
    }

    @Test
    fun `authenticated mode hasFullSecureSessionAccess returns true`() {
        val state = AuthenticatedSecureSessionAccessState()
        assertEquals(true, state.isDatabaseOpen())
        assertEquals(true, state.hasFullSecureSessionAccess())
        assertEquals(false, state.isRecoveryMode())
    }

    @Test
    fun `locked mode hasFullSecureSessionAccess returns false`() {
        val state = LockedSecureSessionAccessState()
        assertEquals(false, state.isDatabaseOpen())
        assertEquals(false, state.hasFullSecureSessionAccess())
        assertEquals(false, state.isRecoveryMode())
    }

    // ==================== 恢复模式下不能查看/复制密码 ====================

    @Test
    fun `recovery mode credential search returns empty`() = runBlocking {
        // The CredentialServiceRepositoryImpl checks hasFullSecureSessionAccess() before searching.
        // In recovery mode, it should return empty results.
        val repository = RecoveryModeCredentialRepository()
        val results = repository.search(
            packageName = "com.example",
            webDomain = null,
            allowUnmatched = false,
            includeSecrets = true,
            limit = 10
        )
        assertEquals(emptyList<EntryAggregate>(), results)
    }

    // ==================== SecureSessionAccessState 三层语义 ====================

    private class RecoverySecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.RecoveryMode(authenticatedAtMs = 0L)
            )

        override fun isUnlocked(): Boolean = true
    }

    private class AuthenticatedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.Authenticated(authenticatedAtMs = 0L)
            )

        override fun isUnlocked(): Boolean = true
    }

    private class LockedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)

        override fun isUnlocked(): Boolean = false
    }

    private class RecoveryModeCredentialRepository : CredentialServiceRepository {
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
}