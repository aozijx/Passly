package com.aozijx.passly.feature.vault.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.flowOf
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpKindUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultOtpUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface VaultOtpStateProvider {
    fun state(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    ): Flow<VaultOtpUiState>

    fun initialState(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    ): VaultOtpUiState

    fun subscribe(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    )

    fun unsubscribe(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    )
}

internal class VaultOtpStateProviderBindings(
    private val states: StateFlow<Map<String, OtpCodeState>>,
) : VaultOtpStateProvider {
    private var onSubscribe: (String) -> Unit = {}
    private var onUnsubscribe: (String) -> Unit = {}

    fun updateSubscriptions(
        onSubscribe: (String) -> Unit,
        onUnsubscribe: (String) -> Unit,
    ) {
        this.onSubscribe = onSubscribe
        this.onUnsubscribe = onUnsubscribe
    }

    override fun state(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    ): Flow<VaultOtpUiState> =
        if (!otpKind.isApplicable || !showCode) {
            flowOf(initialState(entryId, otpKind, showCode))
        } else {
            states.map { current -> current[entryId]?.toUiModel(otpKind) ?: VaultOtpUiState.NotRequested }
        }

    override fun initialState(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    ): VaultOtpUiState =
        when {
            !otpKind.isApplicable -> VaultOtpUiState.NotApplicable
            !showCode -> VaultOtpUiState.Hidden
            else -> states.value[entryId]?.toUiModel(otpKind) ?: VaultOtpUiState.NotRequested
        }

    override fun subscribe(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    ) {
        if (otpKind.isApplicable && showCode) onSubscribe(entryId)
    }

    override fun unsubscribe(
        entryId: String,
        otpKind: VaultOtpKindUiModel,
        showCode: Boolean,
    ) {
        if (otpKind.isApplicable && showCode) onUnsubscribe(entryId)
    }
}

@Composable
internal fun rememberVaultOtpStateProvider(
    states: StateFlow<Map<String, OtpCodeState>>,
    onSubscribe: (String) -> Unit,
    onUnsubscribe: (String) -> Unit,
): VaultOtpStateProvider {
    val currentOnSubscribe = rememberUpdatedState(onSubscribe)
    val currentOnUnsubscribe = rememberUpdatedState(onUnsubscribe)
    return remember(states) {
        VaultOtpStateProviderBindings(states).apply {
            updateSubscriptions(
                onSubscribe = { entryId -> currentOnSubscribe.value(entryId) },
                onUnsubscribe = { entryId -> currentOnUnsubscribe.value(entryId) },
            )
        }
    }
}
