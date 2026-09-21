package com.aozijx.passly.presentation.feature.vault.list.ui.search

enum class VaultSearchPhase {
    BROWSING,
    PULLING,
    EDITING,
    RESULTS,
}

data class VaultSearchState(
    val phase: VaultSearchPhase,
    val pullProgress: Float = 0f,
    val focusAcquired: Boolean = false,
) {
    val isEditing: Boolean
        get() = phase == VaultSearchPhase.EDITING

    val isDirectManipulation: Boolean
        get() = phase == VaultSearchPhase.PULLING

    val layoutProgress: Float
        get() = when (phase) {
            VaultSearchPhase.PULLING -> pullProgress.coerceIn(0f, 1f)
            VaultSearchPhase.EDITING -> 1f
            VaultSearchPhase.BROWSING,
            VaultSearchPhase.RESULTS,
            -> 0f
        }

    fun onPullProgressChanged(progress: Float): VaultSearchState {
        if (phase == VaultSearchPhase.EDITING || phase == VaultSearchPhase.RESULTS) return this

        val boundedProgress = progress.coerceIn(0f, 1f)
        return if (boundedProgress > 0f) {
            copy(
                phase = VaultSearchPhase.PULLING,
                pullProgress = boundedProgress,
                focusAcquired = false,
            )
        } else {
            browsing()
        }
    }

    fun startEditing(): VaultSearchState = copy(
        phase = VaultSearchPhase.EDITING,
        pullProgress = 0f,
        focusAcquired = false,
    )

    fun onFocusChanged(focused: Boolean, query: String): VaultSearchState = when {
        focused -> copy(
            phase = VaultSearchPhase.EDITING,
            pullProgress = 0f,
            focusAcquired = true,
        )

        phase == VaultSearchPhase.EDITING && focusAcquired -> settle(query)
        else -> this
    }

    fun settle(query: String): VaultSearchState = if (query.isNotBlank()) {
        results()
    } else {
        browsing()
    }

    fun onScreenPaused(query: String): VaultSearchState = settle(query)

    fun synchronize(isSearchActive: Boolean, query: String): VaultSearchState = when {
        !isSearchActive -> browsing()
        phase == VaultSearchPhase.EDITING || phase == VaultSearchPhase.PULLING -> this
        query.isNotBlank() -> results()
        else -> browsing()
    }

    private fun browsing(): VaultSearchState = VaultSearchState(VaultSearchPhase.BROWSING)

    private fun results(): VaultSearchState = VaultSearchState(VaultSearchPhase.RESULTS)

    companion object {
        fun initial(isSearchActive: Boolean, query: String): VaultSearchState = when {
            !isSearchActive -> VaultSearchState(VaultSearchPhase.BROWSING)
            query.isNotBlank() -> VaultSearchState(VaultSearchPhase.RESULTS)
            else -> VaultSearchState(VaultSearchPhase.BROWSING)
        }
    }
}
