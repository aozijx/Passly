package com.aozijx.passly.feature.database.presentation.reset.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.database.presentation.reset.DatabaseResetUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DatabaseResetSheet(
    state: DatabaseResetUiState,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.database_reset_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.database_reset_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isButtonEnabled,
                onClick = { showConfirm = true },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                val motionScheme = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                            slideInVertically(animationSpec = motionScheme.defaultSpatialSpec()) { it / 3 } togetherWith
                            fadeOut(animationSpec = motionScheme.defaultEffectsSpec()) +
                            slideOutVertically(animationSpec = motionScheme.defaultSpatialSpec()) { -it / 3 }
                    },
                    label = "database_reset_button_content",
                ) { currentState ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentState.isLoading) {
                            LoadingIndicator(modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Text(stringResource(currentState.titleRes))
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isLoading) showConfirm = false
            },
            title = { Text(stringResource(R.string.database_reset_confirm_title)) },
            text = { Text(stringResource(R.string.database_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    enabled = !state.isLoading,
                    onClick = {
                        showConfirm = false
                        onReset()
                    },
                ) {
                    Text(
                        stringResource(R.string.database_reset_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isLoading,
                    onClick = { showConfirm = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
