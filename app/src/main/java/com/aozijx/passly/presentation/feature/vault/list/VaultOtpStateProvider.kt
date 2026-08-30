package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpStateProvider
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

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

    override fun state(entryId: String): Flow<VaultOtpUiState?> =
        states.map { current -> current[entryId]?.toUiModel() }

    override fun subscribe(entryId: String) = onSubscribe(entryId)

    override fun unsubscribe(entryId: String) = onUnsubscribe(entryId)
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
