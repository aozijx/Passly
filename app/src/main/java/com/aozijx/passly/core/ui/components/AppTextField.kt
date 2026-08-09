package com.aozijx.passly.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.aozijx.passly.R

@Composable
fun PasslyOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = passlyTextFieldShape(),
    colors: TextFieldColors = passlyTextFieldColors(),
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors
    )
}

@Composable
fun PasslyOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = passlyTextFieldShape(),
    colors: TextFieldColors = passlyTextFieldColors(),
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    PasslyOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon
    )
}

@Composable
fun PasswordInput(
    password: String,
    onPasswordChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.password),
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    PasslyOutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = enabled,
                    onClick = { onVisibilityChange(!isVisible) }
                ) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isVisible) {
                            stringResource(R.string.hide_password)
                        } else {
                            stringResource(R.string.show_password)
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun passlyTextFieldShape(): Shape = MaterialTheme.shapes.large

@Composable
fun passlyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error
)
