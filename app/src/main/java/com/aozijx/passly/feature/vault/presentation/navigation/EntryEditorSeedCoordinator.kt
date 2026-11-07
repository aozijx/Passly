package com.aozijx.passly.feature.vault.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.fields
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.entry.EditorSeedToken
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface EntryEditorSeedNavigationResult {
    data object Navigated : EntryEditorSeedNavigationResult

    data object AccessDenied : EntryEditorSeedNavigationResult

    data object UnsupportedDestination : EntryEditorSeedNavigationResult

    data class NavigationFailed(
        val failure: Exception,
    ) : EntryEditorSeedNavigationResult
}

@HiltViewModel
internal class EntryEditorSeedCoordinator
    @Inject
    constructor(
        private val sessionAccess: SecureSessionAccessState,
        private val store: EntryEditorSeedStore,
        private val logger: Logger,
    ) : ViewModel() {
        private sealed interface Ownership {
            data object Empty : Ownership

            class Staged(
                val authentication: AuthenticationState.Authenticated,
            ) : Ownership

            data object Destroyed : Ownership
        }

        private sealed interface Access {
            data class Granted(
                val authentication: AuthenticationState.Authenticated,
            ) : Access

            data object Denied : Access
        }

        private var ownership: Ownership = Ownership.Empty

        init {
            viewModelScope.launch {
                sessionAccess.authenticationState.collect {
                    currentAccess()
                }
            }
        }

        fun navigateWithSeed(
            seed: EntryEditorSeed,
            routeForToken: (EditorSeedToken) -> String?,
            navigate: (String) -> Unit,
        ): EntryEditorSeedNavigationResult {
            val authentication =
                when (val access = currentAccess()) {
                    Access.Denied -> return EntryEditorSeedNavigationResult.AccessDenied
                    is Access.Granted -> access.authentication
                }
            if (seed is EntryEditorSeed.Editable && seed.authentication !== authentication) {
                return EntryEditorSeedNavigationResult.AccessDenied
            }

            ownership = Ownership.Staged(authentication)
            val token = store.stage(seed)
            try {
                val route = routeForToken(token) ?: run {
                    discard(token)
                    return EntryEditorSeedNavigationResult.UnsupportedDestination
                }
                navigate(route)
                return EntryEditorSeedNavigationResult.Navigated
            } catch (cancelled: CancellationException) {
                discardAfterFailure(token, cancelled)
                throw cancelled
            } catch (fatal: Error) {
                discardAfterFailure(token, fatal)
                throw fatal
            } catch (failure: Exception) {
                discardAfterFailure(token, failure)
                logger.error(
                    Events.Vault.Error,
                    ErrorCodes.Security.VaultOperationFailed,
                    Reasons.UnknownFailure,
                    failure,
                    fields {
                        operation(LogFieldKeys.Operation, OperationCode("vault_seed_navigation"))
                    },
                )
                return EntryEditorSeedNavigationResult.NavigationFailed(failure)
            }
        }

        fun consume(token: EditorSeedToken): EntryEditorSeedConsumeResult =
            when (currentAccess()) {
                Access.Denied -> EntryEditorSeedConsumeResult.AccessDenied
                is Access.Granted -> store.consume(token)
            }

        fun discard(token: EditorSeedToken) {
            if (store.discard(token) && ownership is Ownership.Staged) ownership = Ownership.Empty
        }

        private fun discardAfterFailure(
            token: EditorSeedToken,
            primaryFailure: Throwable,
        ) {
            try {
                discard(token)
            } catch (cleanupFailure: Throwable) {
                if (cleanupFailure is Error && primaryFailure !is Error) {
                    if (cleanupFailure !== primaryFailure) cleanupFailure.addSuppressed(primaryFailure)
                    throw cleanupFailure
                }
                if (cleanupFailure !== primaryFailure) primaryFailure.addSuppressed(cleanupFailure)
            }
        }

        private fun currentAccess(): Access {
            if (ownership == Ownership.Destroyed) return Access.Denied
            val authentication = sessionAccess.authenticationState.value
            if (!sessionAccess.hasFullSecureSessionAccess() || authentication !is AuthenticationState.Authenticated) {
                clearSeed()
                return Access.Denied
            }
            val previous = ownership
            if (previous is Ownership.Staged && previous.authentication !== authentication) clearSeed()
            return Access.Granted(authentication)
        }

        private fun clearSeed() {
            store.clear()
            ownership = Ownership.Empty
        }

        override fun onCleared() {
            store.clear()
            ownership = Ownership.Destroyed
        }
    }
