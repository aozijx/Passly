package com.aozijx.passly.security.authentication.host

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.security.authentication.host.ActivityAuthUiHost
import com.aozijx.passly.security.authentication.host.AuthenticationDialogRequest
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry

@Composable
fun AuthenticationHost(
    activity: FragmentActivity,
    registry: AuthenticationHostRegistry,
    logger: Logger,
    content: @Composable () -> Unit,
) {
    val owner: AuthenticationHostOwnerViewModel = viewModel(viewModelStoreOwner = activity)
    val host = remember(activity, owner.ownerId, logger) {
        ActivityAuthUiHost(activity, owner.ownerId, logger)
    }
    val dialog by host.dialog.collectAsStateWithLifecycle()

    DisposableEffect(activity, host, registry) {
        var registration = if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            registry.register(host)
        } else {
            null
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> registration = registry.register(host)
                Lifecycle.Event.ON_PAUSE -> {
                    registration?.let(registry::unregister)
                    registration = null
                }
                Lifecycle.Event.ON_DESTROY -> {
                    registration?.let(registry::unregister)
                    registration = null
                    host.cancelOwnedRequests()
                    if (!activity.isChangingConfigurations) registry.invalidateOwner(host.ownerId)
                }
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
            registration?.let(registry::unregister)
            registration = null
        }
    }

    Box { content() }
    AuthenticationDialogs(
        state = dialog.toUiState(),
        events = remember(host) {
            object : AuthenticationDialogEventHandler {
                override fun onMethodSelected(method: AuthenticationMethodUiModel?) {
                    host.submitMethod(method?.toDomainModel())
                }

                override fun onSecretSubmitted(secret: CharArray?) {
                    host.submitSecret(secret)
                }
            }
        },
    )
}

private fun AuthenticationDialogRequest?.toUiState(): AuthenticationDialogUiState? = when (this) {
    is AuthenticationDialogRequest.ChooseMethod -> AuthenticationDialogUiState.ChooseMethod(
        methods.map(AuthenticationMethod::toUiModel),
    )
    is AuthenticationDialogRequest.Secret -> AuthenticationDialogUiState.Secret(
        method.toUiModel(),
    )
    null -> null
}

private fun AuthenticationMethod.toUiModel(): AuthenticationMethodUiModel = when (this) {
    AuthenticationMethod.BIOMETRIC -> AuthenticationMethodUiModel.BIOMETRIC
    AuthenticationMethod.APP_PASSWORD -> AuthenticationMethodUiModel.APP_PASSWORD
    AuthenticationMethod.RECOVERY_CODE -> AuthenticationMethodUiModel.RECOVERY_CODE
}

private fun AuthenticationMethodUiModel.toDomainModel(): AuthenticationMethod = when (this) {
    AuthenticationMethodUiModel.BIOMETRIC -> AuthenticationMethod.BIOMETRIC
    AuthenticationMethodUiModel.APP_PASSWORD -> AuthenticationMethod.APP_PASSWORD
    AuthenticationMethodUiModel.RECOVERY_CODE -> AuthenticationMethod.RECOVERY_CODE
}
