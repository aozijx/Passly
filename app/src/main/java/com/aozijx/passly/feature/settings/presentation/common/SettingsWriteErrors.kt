package com.aozijx.passly.feature.settings.presentation.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ObserveSettingsWriteErrors(errors: Flow<UiErrorMessage>) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(errors, context, resources, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            errors.collect { error ->
                Toast.makeText(context, resources.getString(error.resourceId), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
