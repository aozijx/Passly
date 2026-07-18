package com.aozijx.passly.ui.components.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.security.crypto.SecureString

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
    success: Boolean = false,
    resultText: String = "Success"
) {
    val focusRequester = remember { FocusRequester() }
    val submit = {
        if (enabled && !progress && !value.isEmpty) {
            onAction()
            onExpandedChange(false)
        }
    }

    BackHandler(enabled = expanded) {
        onExpandedChange(false)
    }

    LaunchedEffect(expanded) {
        if (expanded) focusRequester.requestFocus()
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        OutlinedTextField(
            value = value.toPlainString(),
            onValueChange = onValueChange,
            singleLine = true,
            label = { Text(inputLabel) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            enabled = enabled && !progress,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (expanded) {
        Spacer(modifier = Modifier.height(12.dp))
    }

    ActionButton(
        modifier = modifier,
        progress = progress,
        success = success,
        icon = icon,
        text = if (expanded) expandedText else collapsedText,
        resultText = resultText,
        enabled = enabled && (!expanded || !value.isEmpty),
        onClick = {
            if (expanded) submit() else onExpandedChange(true)
        }
    )
}
