package com.aozijx.passly.ui.components.common

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.security.crypto.SecureString

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
    enabled: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    val submit = {
        if (enabled && !state.progress && !state.value.isEmpty) {
            onAction()
            // 提交时通常收起输入框，如果需要保持展开可由外部 state 控制
            onExpandedChange(false)
        }
    }

    BackHandler(enabled = state.expanded) {
        onExpandedChange(false)
    }

    LaunchedEffect(state.expanded) {
        if (state.expanded) focusRequester.requestFocus()
    }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = state.expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                OutlinedTextField(
                    value = state.value.toPlainString(),
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text(config.inputLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    enabled = enabled && !state.progress,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        ActionButton(
            modifier = Modifier.fillMaxWidth(),
            progress = state.progress,
            success = state.result == true,
            icon = when (state.result) {
                true -> config.successIcon
                false -> config.errorIcon
                else -> config.icon
            },
            containerColor = config.containerColor,
            text = if (state.expanded) config.expandedText else config.collapsedText,
            resultText = if (state.result == true) config.successText else config.errorText,
            enabled = enabled && (!state.expanded || !state.value.isEmpty),
            onClick = {
                if (state.expanded) submit() else onExpandedChange(true)
            }
        )
    }
}

/**
 * 便捷构造方法，兼容现有调用逻辑并支持扩展参数。
 */
@Composable
fun InputActionButton(
    value: SecureString,
    expanded: Boolean,
    progress: Boolean,
    collapsedText: String,
    expandedText: String,
    inputLabel: String,
    onValueChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Key,
    containerColor: Color? = null,
    result: Boolean? = null,
    successText: String = "Success",
    errorText: String = "Failed",
    showResultFooter: Boolean = false
) {
    InputActionButton(
        state = InputActionButtonState(value, expanded, progress, result),
        config = InputActionButtonConfig(
            collapsedText = collapsedText,
            expandedText = expandedText,
            inputLabel = inputLabel,
            icon = icon,
            containerColor = containerColor,
            successText = successText,
            errorText = errorText,
            showResultFooter = showResultFooter
        ),
        onValueChange = onValueChange,
        onExpandedChange = onExpandedChange,
        onAction = onAction,
        modifier = modifier,
        enabled = enabled
    )
}

data class InputActionButtonState(
    val value: SecureString = SecureString.EMPTY,
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
    val errorIcon: ImageVector = Icons.Default.Cancel,
    val showResultFooter: Boolean = false
)
