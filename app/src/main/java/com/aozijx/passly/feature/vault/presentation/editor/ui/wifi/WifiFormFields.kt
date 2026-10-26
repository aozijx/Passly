package com.aozijx.passly.feature.vault.presentation.editor.ui.wifi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.EntryViolationCode
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.EntryEditorSection
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.SensitiveFieldVisibilityButton
import com.aozijx.passly.feature.vault.presentation.editor.wifi.AddWifiAction
import com.aozijx.passly.feature.vault.presentation.editor.wifi.AddWifiFormState
import com.aozijx.passly.presentation.shared.components.NextFocusTextField

@Composable
internal fun WifiFormFields(
    form: AddWifiFormState,
    fieldErrors: Map<FieldKey, EntryViolationCode>,
    onAction: (AddWifiAction) -> Unit,
) {
    EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
        WifiSecurityTypeDropdown(
            selected = form.securityType,
            onSelected = { onAction(AddWifiAction.SecurityTypeChanged(it)) },
        )
        if (form.securityType != WifiSecurityType.Open) {
            val passwordError = fieldErrors[FieldKey.PASSWORD]
            NextFocusTextField(
                value = form.password,
                onValueChange = { onAction(AddWifiAction.PasswordChanged(it)) },
                label = stringResource(R.string.wifi_password),
                visualTransformation = if (form.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardType = KeyboardType.Password,
                isError = passwordError != null,
                supportingText = passwordError?.let { error ->
                    {
                        Text(
                            stringResource(
                                if (error == EntryViolationCode.REQUIRED) {
                                    R.string.wifi_password_required
                                } else {
                                    R.string.wifi_password_invalid
                                },
                            ),
                        )
                    }
                },
                trailingIcon = {
                    SensitiveFieldVisibilityButton(
                        fieldLabelRes = R.string.wifi_password,
                        visible = form.isPasswordVisible,
                        onVisibilityChanged = { onAction(AddWifiAction.PasswordVisibilityChanged(it)) },
                    )
                },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.wifi_hidden))
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = form.isHidden,
                onCheckedChange = { onAction(AddWifiAction.HiddenChanged(it)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiSecurityTypeDropdown(
    selected: WifiSecurityType,
    onSelected: (WifiSecurityType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.wifi_encryption)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            WifiSecurityType.entries.forEach { securityType ->
                DropdownMenuItem(
                    text = { Text(securityType.label()) },
                    onClick = {
                        onSelected(securityType)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun WifiSecurityType.label(): String = stringResource(
    when (this) {
        WifiSecurityType.Open -> R.string.wifi_security_open
        WifiSecurityType.Wep -> R.string.wifi_security_wep
        WifiSecurityType.WpaPersonal -> R.string.wifi_security_wpa_personal
    },
)
