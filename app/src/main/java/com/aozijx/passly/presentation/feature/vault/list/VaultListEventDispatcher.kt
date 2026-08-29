package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEventHandler

internal class VaultListEventDispatcher : VaultListEventHandler {
    private var onEventCallback: (VaultListEvent) -> Unit = {}
    private var requestAuthenticationCallback: ((() -> Unit) -> Unit) = {}

    fun updateCallbacks(
        onEvent: (VaultListEvent) -> Unit,
        requestAuthentication: ((() -> Unit) -> Unit),
    ) {
        onEventCallback = onEvent
        requestAuthenticationCallback = requestAuthentication
    }

    override fun onEvent(event: VaultListEvent) = onEventCallback(event)

    override fun requestAuthentication(onSuccess: () -> Unit) =
        requestAuthenticationCallback(onSuccess)
}

@Composable
internal fun rememberVaultListEventHandler(
    onEvent: (VaultListEvent) -> Unit,
    requestAuthentication: ((() -> Unit) -> Unit),
): VaultListEventHandler {
    val dispatcher = remember { VaultListEventDispatcher() }
    SideEffect {
        dispatcher.updateCallbacks(onEvent, requestAuthentication)
    }
    return dispatcher
}
