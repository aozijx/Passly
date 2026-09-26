package com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.components.NextFocusTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.AddEntryScaffold
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.EntryEditorSection
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.AddBankCardAction
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.AddBankCardUiState
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.CardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankCardEditorScreen(
    state: AddBankCardUiState,
    onAction: (AddBankCardAction) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    saveActionModifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val form = state.form

    fun save() {
        keyboardController?.hide()
        onAction(AddBankCardAction.Save)
    }

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_bank_card_title),
        canSave = state.canSave,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = ::save,
        modifier = modifier,
        saveActionModifier = saveActionModifier,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            NextFocusTextField(
                value = form.title,
                onValueChange = { onAction(AddBankCardAction.TitleChanged(it)) },
                label = stringResource(R.string.field_title),
            )
            CardTypeDropdown(
                selected = form.cardType,
                onSelected = { onAction(AddBankCardAction.CardTypeChanged(it)) },
                label = stringResource(R.string.card_type),
            )
            NextFocusTextField(
                value = form.cardholder,
                onValueChange = { onAction(AddBankCardAction.CardholderChanged(it)) },
                label = stringResource(R.string.cardholder),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
            NextFocusTextField(
                value = form.cardNumber,
                onValueChange = { onAction(AddBankCardAction.CardNumberChanged(it)) },
                label = stringResource(R.string.card_number),
                visualTransformation = if (form.isCardNumberVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                isError = form.cardNumberError != null,
                supportingText = form.cardNumberError?.let { { Text(it) } },
                trailingIcon = {
                    IconButton(onClick = {
                        onAction(AddBankCardAction.CardNumberVisibilityChanged(!form.isCardNumberVisible))
                    }) {
                        Icon(
                            imageVector = if (form.isCardNumberVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )
            NextFocusTextField(
                value = form.paymentPin,
                onValueChange = { onAction(AddBankCardAction.PaymentPinChanged(it)) },
                label = stringResource(R.string.payment_pin),
                visualTransformation = if (form.isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                trailingIcon = {
                    IconButton(onClick = {
                        onAction(AddBankCardAction.PinVisibilityChanged(!form.isPinVisible))
                    }) {
                        Icon(
                            imageVector = if (form.isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )
            NextFocusTextField(
                value = form.cardCvv,
                onValueChange = { onAction(AddBankCardAction.CvvChanged(it)) },
                label = stringResource(R.string.card_cvv),
                visualTransformation = if (form.isCvvVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                trailingIcon = {
                    IconButton(onClick = {
                        onAction(AddBankCardAction.CvvVisibilityChanged(!form.isCvvVisible))
                    }) {
                        Icon(
                            imageVector = if (form.isCvvVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NextFocusTextField(
                    value = form.cardExpiryMonth,
                    onValueChange = { onAction(AddBankCardAction.ExpiryMonthChanged(it)) },
                    label = stringResource(R.string.card_expiry_month),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                NextFocusTextField(
                    value = form.cardExpiryYear,
                    onValueChange = { onAction(AddBankCardAction.ExpiryYearChanged(it)) },
                    label = stringResource(R.string.card_expiry_year),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
            NextFocusTextField(
                value = form.tags,
                onValueChange = { onAction(AddBankCardAction.TagsChanged(it)) },
                label = stringResource(R.string.field_category),
            )
            NextFocusTextField(
                value = form.billingAddress,
                onValueChange = { onAction(AddBankCardAction.BillingAddressChanged(it)) },
                label = stringResource(R.string.billing_address),
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = { onAction(AddBankCardAction.NotesChanged(it)) },
                label = { Text(stringResource(R.string.field_notes)) },
                singleLine = false,
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (form.isValid) {
                            focusManager.clearFocus(force = true)
                            save()
                        }
                    },
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardTypeDropdown(
    selected: CardType?,
    onSelected: (CardType) -> Unit,
    label: String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.label().orEmpty(),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CardType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun CardType.label(): String = stringResource(labelRes)
