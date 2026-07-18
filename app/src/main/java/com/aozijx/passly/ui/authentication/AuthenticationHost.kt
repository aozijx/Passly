package com.aozijx.passly.ui.authentication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry

@Composable
fun AuthenticationHost(
    activity: FragmentActivity,
    registry: AuthenticationHostRegistry,
    content: @Composable () -> Unit
) {
    val owner: AuthenticationHostOwnerViewModel = viewModel(viewModelStoreOwner = activity)
    val host = remember(activity, owner.ownerId) { ActivityAuthUiHost(activity, owner.ownerId) }
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
                Lifecycle.Event.ON_PAUSE -> registration?.let(registry::unregister)
                Lifecycle.Event.ON_DESTROY -> {
                    registration?.let(registry::unregister)
                    host.cancelOwnedRequests()
                    if (!activity.isChangingConfigurations) {
                        registry.invalidateOwner(host.ownerId)
                    }
                }
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
            registration?.let(registry::unregister)
        }
    }

    Box { content() }
    when (val request = dialog) {
        is AuthenticationDialogRequest.ChooseMethod -> MethodDialog(
            methods = request.methods,
            onSelected = host::submitMethod,
            onCancel = { host.submitMethod(null) }
        )
        is AuthenticationDialogRequest.Secret -> SecretDialog(
            method = request.method,
            onSubmit = host::submitSecret,
            onCancel = { host.submitSecret(null) }
        )
        null -> Unit
    }
}

@Composable
private fun MethodDialog(
    methods: List<AuthenticationMethod>,
    onSelected: (AuthenticationMethod) -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("验证身份") },
        text = {
            Column {
                methods.forEach { method ->
                    TextButton(
                        onClick = { onSelected(method) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(method.label()) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}

@Composable
private fun SecretDialog(
    method: AuthenticationMethod,
    onSubmit: (CharArray) -> Unit,
    onCancel: () -> Unit
) {
    var secret by remember(method) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(method.label()) },
        text = {
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text(if (method == AuthenticationMethod.RECOVERY_CODE) "恢复码" else "应用密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        },
        confirmButton = {
            Button(
                enabled = secret.isNotEmpty(),
                onClick = {
                    val chars = secret.toCharArray()
                    secret = ""
                    onSubmit(chars)
                }
            ) { Text("继续") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}

private fun AuthenticationMethod.label(): String = when (this) {
    AuthenticationMethod.BIOMETRIC -> "生物识别"
    AuthenticationMethod.APP_PASSWORD -> "应用密码"
    AuthenticationMethod.RECOVERY_CODE -> "使用恢复码"
}
