package com.aozijx.passly.core.ui.components.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 输入操作按钮：将“触发操作”和“输入数据”合并为一个控件。
 *
 * @param state 组件的动态状态
 * @param config 组件的静态配置
 */
@Composable
fun InputActionButton(
    state: InputActionButtonState,
    config: InputActionButtonConfig,
    onValueChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onResultConsumed: () -> Unit = {},
    enabled: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissInput = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onExpandedChange(false)
    }
    val submit = {
        if (enabled && !state.progress && state.value.isNotEmpty()) {
            onAction()
            dismissInput()
        }
    }

    BackHandler(enabled = state.expanded) {
        dismissInput()
    }

    LaunchedEffect(state.expanded) {
        if (state.expanded) {
            focusRequester.requestFocus()
            delay(BRING_INTO_VIEW_DELAY_MS)
            bringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(state.result) {
        if (state.result != null) {
            delay(RESULT_DISPLAY_DURATION_MS)
            onResultConsumed()
        }
    }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = state.expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                OutlinedTextField(
                    value = state.value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text(config.inputLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    enabled = enabled && !state.progress,
                    modifier = Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        ActionButton(
            modifier = Modifier.fillMaxWidth(),
            progress = state.progress,
            result = state.result,
            icon = when (state.result) {
                true -> config.successIcon
                false -> config.errorIcon
                else -> config.icon
            },
            containerColor = config.containerColor,
            text = if (state.expanded) config.expandedText else config.collapsedText,
            resultText = if (state.result == true) config.successText else config.errorText,
            enabled = enabled && (!state.expanded || state.value.isNotEmpty()),
            onClick = {
                if (state.expanded) submit() else onExpandedChange(true)
            }
        )
    }
}

data class InputActionButtonState(
    val value: String = "",
    val expanded: Boolean = false,
    val progress: Boolean = false,
    val result: Boolean? = null
)

data class InputActionButtonConfig(
    val collapsedText: String,
    val expandedText: String,
    val inputLabel: String,
    val containerColor: Color? = null,
    val icon: ImageVector = Icons.Default.Key,
    val successText: String = "Success",
    val errorText: String = "Failed",
    val successIcon: ImageVector = Icons.Default.CheckCircle,
    val errorIcon: ImageVector = Icons.Default.Cancel
)

private const val RESULT_DISPLAY_DURATION_MS = 2_500L
private const val BRING_INTO_VIEW_DELAY_MS = 120L
