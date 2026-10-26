package com.aozijx.passly.feature.vault.presentation.editor

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Coordinates one create attempt without owning or copying a type-specific form. */
internal class EditorSaveSession(
    private val scope: CoroutineScope,
    private val access: SecureSessionAccessState,
    private val create: suspend (EntryDraft) -> AppResult<EntryId>,
    private val onSaving: () -> Unit,
    private val onIdle: () -> Unit,
    private val onClear: () -> Unit,
) {
    private sealed interface Phase {
        data object Closed : Phase
        data object Destroyed : Phase
        class Editing(val authentication: AuthenticationState.Authenticated) : Phase
        class Saving(val authentication: AuthenticationState.Authenticated) : Phase
        class Finished(val attempt: Saving) : Phase
    }

    private data class Delivery(val attempt: Phase.Saving, val effect: EditorSaveEffect)
    private var phase: Phase = Phase.Closed
    private var saveJob: Job? = null
    // A new edit or attempt invalidates previous outcomes, so only the latest delivery is useful.
    private val deliveries = Channel<Delivery>(Channel.CONFLATED)
    val effects: Flow<EditorSaveEffect> = deliveries.receiveAsFlow().filter { delivery ->
        (phase as? Phase.Finished)?.attempt === delivery.attempt &&
            authentication() === delivery.attempt.authentication
    }.map { it.effect }

    private val accessJob = scope.launch {
        access.authenticationState.collect {
            val current = authentication()
            val previous = owner()
            if (current == null || (previous != null && previous !== current)) clear()
        }
    }

    fun canEdit(): Boolean {
        if (phase == Phase.Destroyed) return false
        val current = authentication() ?: run { clear(); return false }
        if (owner()?.let { it !== current } == true) clear()
        if (phase is Phase.Saving) return false
        phase = Phase.Editing(current)
        return true
    }

    fun save(draft: () -> EntryDraft) {
        if (phase == Phase.Destroyed) return
        val current = authentication() ?: run { clear(); return }
        if (owner() !== current) {
            clear()
            return
        }
        if (phase is Phase.Saving) return
        val attempt = Phase.Saving(current)
        phase = attempt
        onSaving()
        saveJob = scope.launch {
            if (!isCurrent(attempt)) {
                if (phase === attempt) clear()
                return@launch
            }
            val effect = try {
                when (val result = create(draft())) {
                    is AppResult.Success -> EditorSaveEffect.Saved
                    is AppResult.Failure -> EditorSaveEffect.Failed(result.error.code)
                }
            } catch (cancelled: CancellationException) {
                if (phase === attempt) {
                    if (authentication() === attempt.authentication) {
                        phase = Phase.Editing(attempt.authentication)
                        onIdle()
                    } else clear()
                }
                throw cancelled
            } catch (_: Exception) {
                EditorSaveEffect.Failed(null)
            }
            if (!isCurrent(attempt) || !currentCoroutineContext().isActive) {
                if (phase === attempt) clear()
                return@launch
            }
            phase = Phase.Finished(attempt)
            when (effect) {
                EditorSaveEffect.Saved -> onClear()
                is EditorSaveEffect.Failed -> onIdle()
            }
            deliveries.trySend(Delivery(attempt, effect))
        }
    }

    fun clear() {
        if (phase == Phase.Destroyed) return
        phase = Phase.Closed
        saveJob?.cancel()
        saveJob = null
        onClear()
    }

    fun close() {
        if (phase == Phase.Destroyed) return
        phase = Phase.Destroyed
        accessJob.cancel()
        saveJob?.cancel()
        saveJob = null
        deliveries.close()
        onClear()
    }

    private fun isCurrent(attempt: Phase.Saving): Boolean =
        phase === attempt && authentication() === attempt.authentication

    private fun authentication(): AuthenticationState.Authenticated? =
        if (access.hasFullSecureSessionAccess()) access.authenticationState.value as? AuthenticationState.Authenticated else null

    private fun owner(): AuthenticationState.Authenticated? = when (val current = phase) {
        Phase.Closed, Phase.Destroyed -> null
        is Phase.Editing -> current.authentication
        is Phase.Saving -> current.authentication
        is Phase.Finished -> current.attempt.authentication
    }
}
