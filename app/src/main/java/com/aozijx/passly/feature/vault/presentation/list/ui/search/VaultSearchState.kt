package com.aozijx.passly.feature.vault.presentation.list.ui.search

import androidx.compose.runtime.Immutable

enum class VaultSearchAction { MORE, CLEAR, NONE }

@Immutable
sealed interface VaultSearchState {
    data object Browsing : VaultSearchState

    data class Pulling(val progress: Float) : VaultSearchState

    data class Editing(val hasReceivedFocus: Boolean = false) : VaultSearchState

    data object Results : VaultSearchState

    companion object {
        fun initial(
            isSearchActive: Boolean,
            query: String,
        ): VaultSearchState =
            when {
                !isSearchActive -> Browsing
                query.isNotBlank() -> Results
                else -> Browsing
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

fun VaultSearchState.action(query: String): VaultSearchAction =
    when {
        this == VaultSearchState.Browsing || this is VaultSearchState.Pulling -> VaultSearchAction.MORE
        query.isNotEmpty() -> VaultSearchAction.CLEAR
        else -> VaultSearchAction.NONE
    }
fun VaultSearchState.onPullProgressChanged(progress: Float): VaultSearchState =
    when (this) {
        is VaultSearchState.Editing,
        VaultSearchState.Results,
        -> this

        else -> progress.coerceIn(0f, 1f).takeIf { it > 0f }?.let(VaultSearchState::Pulling) ?: VaultSearchState.Browsing
    }

fun VaultSearchState.startEditing(): VaultSearchState = VaultSearchState.Editing()

fun VaultSearchState.onFocusChanged(
    focused: Boolean,
    query: String,
): VaultSearchState =
    when {
        focused && this is VaultSearchState.Editing -> copy(hasReceivedFocus = true)
        focused -> this
        this is VaultSearchState.Editing && hasReceivedFocus -> settle(query)
        else -> this
    }

fun VaultSearchState.settle(query: String): VaultSearchState =
    query.takeIf(String::isNotBlank)?.let { VaultSearchState.Results } ?: VaultSearchState.Browsing

fun VaultSearchState.onScreenPaused(query: String): VaultSearchState = settle(query)

fun VaultSearchState.synchronize(
    isSearchActive: Boolean,
    query: String,
): VaultSearchState =
    when {
        !isSearchActive -> VaultSearchState.Browsing
        this is VaultSearchState.Editing || this is VaultSearchState.Pulling -> this
        query.isNotBlank() -> VaultSearchState.Results
        else -> VaultSearchState.Browsing
    }
