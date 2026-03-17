package com.example.poop.common.fields

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.poop.R

@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
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
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.label_password)) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onVisibilityChange(!isVisible) }) {
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
