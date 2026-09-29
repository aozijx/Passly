package com.aozijx.passly.app.message.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
internal fun AppNoticeEffects() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<AppNoticeHostViewModel>()

    LaunchedEffect(viewModel, context) {
        viewModel.toastMessages.collect { message ->
            Toast.makeText(
                context,
                message.text,
                if (message.longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
