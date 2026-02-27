package com.example.poop.ui.screens.animation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AnimationUiState(
    val isFadeVisible: Boolean = false,
    val isScaleSelected: Boolean = false,
    val isHeartLiked: Boolean = false,
    val isCardRotated: Boolean = false,
    val isHeightExpanded: Boolean = false
) {
    fun toggleFadeVisibility() = copy(isFadeVisible = !isFadeVisible)
    fun toggleScaleSelection() = copy(isScaleSelected = !isScaleSelected)
    fun toggleHeartLike() = copy(isHeartLiked = !isHeartLiked)
    fun toggleCardRotation() = copy(isCardRotated = !isCardRotated)
    fun toggleHeightExpansion() = copy(isHeightExpanded = !isHeightExpanded)
}

class AnimationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnimationUiState())
    val uiState: StateFlow<AnimationUiState> = _uiState.asStateFlow()

    fun toggleFadeVisibility() {
        _uiState.update { it.toggleFadeVisibility() }
    }

    fun toggleScaleSelection() {
        _uiState.update { it.toggleScaleSelection() }
    }

    fun toggleHeartLike() {
        _uiState.update { it.toggleHeartLike() }
    }

    fun toggleCardRotation() {
        _uiState.update { it.toggleCardRotation() }
    }

    fun toggleHeightExpansion() {
        _uiState.update { it.toggleHeightExpansion() }
    }
}
