package com.aozijx.passly.feature.autofill.presentation.accessibility

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aozijx.passly.feature.autofill.presentation.AutofillHostCompletion

@Composable
internal fun AccessibilityAutofillEntry(onComplete: (AutofillHostCompletion) -> Unit) {
    val activity =
        LocalActivity.current as? ComponentActivity
            ?: error("AccessibilityAutofillEntry requires a ComponentActivity host")
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: AccessibilityFillViewModel = hiltViewModel(activity)

    DisposableEffect(activity, lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY && activity.isFinishing) viewModel.cancel()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AccessibilityFillRoute(
        viewModel = viewModel,
        onComplete = { onComplete(AutofillHostCompletion(resultCode = null, hideIme = false)) },
    )
}
