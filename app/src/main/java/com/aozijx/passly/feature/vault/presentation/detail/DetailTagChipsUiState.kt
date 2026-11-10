package com.aozijx.passly.feature.vault.presentation.detail

sealed interface DetailTagChipsUiState {
    data object Empty : DetailTagChipsUiState

    data class Items(val tags: List<String>) : DetailTagChipsUiState {
        init {
            require(tags.isNotEmpty())
        }
    }
}

internal fun Collection<String>.toDetailTagChipsUiState(): DetailTagChipsUiState =
    takeIf(Collection<String>::isNotEmpty)
        ?.toList()
        ?.let(DetailTagChipsUiState::Items)
        ?: DetailTagChipsUiState.Empty
