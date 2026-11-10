package com.aozijx.passly.feature.vault.presentation.list.ui.search

import androidx.compose.runtime.Immutable
import com.aozijx.passly.feature.vault.presentation.list.VaultSearchInput

enum class VaultSearchAction { MORE, CLEAR, NONE }

@Immutable
sealed interface VaultSearchState {
    data object Browsing : VaultSearchState

    data class Pulling(val progress: Float) : VaultSearchState

    data class Editing(val hasReceivedFocus: Boolean = false) : VaultSearchState

    data object Results : VaultSearchState

    companion object {
        fun initial(search: VaultSearchInput): VaultSearchState =
            when (search) {
                VaultSearchInput.Closed -> Browsing
                is VaultSearchInput.Open -> search.toSearchState()
            }
    }
}

val VaultSearchState.isEditing: Boolean
    get() = this is VaultSearchState.Editing

val VaultSearchState.isDirectManipulation: Boolean
    get() = this is VaultSearchState.Pulling

val VaultSearchState.isResults: Boolean
    get() = this is VaultSearchState.Results

val VaultSearchState.layoutProgress: Float
    get() =
        when (this) {
            is VaultSearchState.Pulling -> progress.coerceIn(0f, 1f)
            is VaultSearchState.Editing -> 1f
            VaultSearchState.Browsing,
            VaultSearchState.Results,
            -> 0f
        }

fun VaultSearchState.action(search: VaultSearchInput): VaultSearchAction =
    when (this) {
        VaultSearchState.Browsing,
        is VaultSearchState.Pulling,
        -> VaultSearchAction.MORE

        is VaultSearchState.Editing,
        VaultSearchState.Results,
        -> if (search.query.isNotEmpty()) VaultSearchAction.CLEAR else VaultSearchAction.NONE
    }
fun VaultSearchState.onPullProgressChanged(progress: Float): VaultSearchState =
    when (this) {
        is VaultSearchState.Editing,
        VaultSearchState.Results,
        -> this

        VaultSearchState.Browsing,
        is VaultSearchState.Pulling,
        -> progress.coerceIn(0f, 1f).takeIf { it > 0f }?.let(VaultSearchState::Pulling) ?: VaultSearchState.Browsing
    }

fun VaultSearchState.startEditing(): VaultSearchState = VaultSearchState.Editing()

fun VaultSearchState.onFocusChanged(
    focused: Boolean,
    query: String,
): VaultSearchState =
    when (this) {
        is VaultSearchState.Editing ->
            when {
                focused -> copy(hasReceivedFocus = true)
                hasReceivedFocus -> settle(query)
                else -> this
            }

        VaultSearchState.Browsing,
        is VaultSearchState.Pulling,
        VaultSearchState.Results,
        -> this
    }

fun VaultSearchState.settle(query: String): VaultSearchState =
    query.takeIf(String::isNotBlank)?.let { VaultSearchState.Results } ?: VaultSearchState.Browsing

fun VaultSearchState.onScreenPaused(query: String): VaultSearchState = settle(query)

fun VaultSearchState.synchronize(search: VaultSearchInput): VaultSearchState =
    when (search) {
        VaultSearchInput.Closed -> VaultSearchState.Browsing
        is VaultSearchInput.Open ->
            when (this) {
                is VaultSearchState.Editing,
                is VaultSearchState.Pulling,
                -> this

                VaultSearchState.Browsing,
                VaultSearchState.Results,
                -> search.toSearchState()
            }
    }

private fun VaultSearchInput.Open.toSearchState(): VaultSearchState =
    if (query.isNotBlank()) VaultSearchState.Results else VaultSearchState.Browsing
