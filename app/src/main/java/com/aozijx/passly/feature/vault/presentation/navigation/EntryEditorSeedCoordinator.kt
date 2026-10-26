package com.aozijx.passly.feature.vault.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.feature.vault.entry.EditorSeedToken
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface EntryEditorSeedStageResult {
    data class Staged(val token: EditorSeedToken) : EntryEditorSeedStageResult
    data object AccessDenied : EntryEditorSeedStageResult
}

internal fun interface EntryEditorSeedStager {
    fun stage(seed: EntryEditorSeed): EntryEditorSeedStageResult
}

@HiltViewModel
internal class EntryEditorSeedCoordinator @Inject constructor(
    private val sessionAccess: SecureSessionAccessState,
    private val store: EntryEditorSeedStore,
) : ViewModel(), EntryEditorSeedStager {
    private sealed interface Ownership {
        data object Empty : Ownership
        class Staged(val authentication: AuthenticationState.Authenticated) : Ownership
        data object Destroyed : Ownership
    }

    private sealed interface Access {
        data class Granted(val authentication: AuthenticationState.Authenticated) : Access
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

    override fun stage(seed: EntryEditorSeed): EntryEditorSeedStageResult {
        return when (val access = currentAccess()) {
            Access.Denied -> EntryEditorSeedStageResult.AccessDenied
            is Access.Granted -> {
                ownership = Ownership.Staged(access.authentication)
                EntryEditorSeedStageResult.Staged(store.stage(seed))
            }
        }
    }

    fun consume(token: EditorSeedToken): EntryEditorSeedConsumeResult {
        return when (currentAccess()) {
            Access.Denied -> EntryEditorSeedConsumeResult.AccessDenied
            is Access.Granted -> store.consume(token)
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
