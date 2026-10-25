package com.aozijx.passly.feature.vault.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
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
    init {
        viewModelScope.launch {
            sessionAccess.authenticationState.collect {
                if (!sessionAccess.hasFullSecureSessionAccess()) store.clear()
            }
        }
    }

    override fun stage(seed: EntryEditorSeed): EntryEditorSeedStageResult {
        if (!sessionAccess.hasFullSecureSessionAccess()) {
            store.clear()
            return EntryEditorSeedStageResult.AccessDenied
        }
        return EntryEditorSeedStageResult.Staged(store.stage(seed))
    }

    fun consume(token: EditorSeedToken): EntryEditorSeedConsumeResult {
        if (!sessionAccess.hasFullSecureSessionAccess()) {
            store.clear()
            return EntryEditorSeedConsumeResult.AccessDenied
        }
        return store.consume(token)
    }

    override fun onCleared() {
        store.clear()
    }
}
