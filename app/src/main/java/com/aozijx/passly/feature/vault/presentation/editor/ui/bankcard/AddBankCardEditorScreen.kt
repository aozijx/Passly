package com.aozijx.passly.feature.vault.presentation.editor.ui.bankcard

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
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.AddEntryScaffold
import com.aozijx.passly.feature.vault.presentation.editor.ui.common.EntryEditorSection
import com.aozijx.passly.feature.vault.presentation.editor.bankcard.AddBankCardAction
import com.aozijx.passly.feature.vault.presentation.editor.bankcard.AddBankCardUiState
import com.aozijx.passly.feature.vault.presentation.editor.bankcard.AddBankCardSavePhase
import com.aozijx.passly.feature.vault.presentation.editor.bankcard.CardType
import com.aozijx.passly.feature.vault.presentation.editor.bankcard.CardTypeSelection
import com.aozijx.passly.domain.entry.policy.CardNumberInvalidReason
import com.aozijx.passly.domain.entry.policy.CardNumberValidationResult

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
        isSaving = state.savePhase == AddBankCardSavePhase.Saving,
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

        EntryEditorSection(title = stringResource(R.string.bank_card_bank_details)) {
            NextFocusTextField(
                value = form.issuingBank,
                onValueChange = { onAction(AddBankCardAction.IssuingBankChanged(it)) },
                label = stringResource(R.string.card_issuing_bank),
            )
            NextFocusTextField(
                value = form.bankBranch,
                onValueChange = { onAction(AddBankCardAction.BankBranchChanged(it)) },
                label = stringResource(R.string.card_bank_branch),
            )
            NextFocusTextField(
                value = form.cardProduct,
                onValueChange = { onAction(AddBankCardAction.CardProductChanged(it)) },
                label = stringResource(R.string.card_product),
            )
            NextFocusTextField(
                value = form.currency,
                onValueChange = { onAction(AddBankCardAction.CurrencyChanged(it)) },
                label = stringResource(R.string.card_currency),
            )
            NextFocusTextField(
                value = form.bankPhone,
                onValueChange = { onAction(AddBankCardAction.BankPhoneChanged(it)) },
                label = stringResource(R.string.card_bank_phone),
                keyboardType = KeyboardType.Phone,
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
            NextFocusTextField(
                value = form.cardNumber,
                onValueChange = { onAction(AddBankCardAction.CardNumberChanged(it)) },
                label = stringResource(R.string.card_number),
                visualTransformation = if (form.isCardNumberVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                isError = form.cardNumberValidation is CardNumberValidationResult.Invalid,
                supportingText = when (val validation = form.cardNumberValidation) {
                    is CardNumberValidationResult.Invalid -> {
                        { Text(validation.reason.label()) }
                    }
                    CardNumberValidationResult.Empty,
                    is CardNumberValidationResult.Valid,
                    -> null
                },
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
                isError = !form.isCvvValid,
                supportingText = if (!form.isCvvValid) {
                    { Text(stringResource(R.string.vault_card_cvv_invalid)) }
                } else null,
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
            CardExpiryField(
                selection = form.cardExpiry,
                onExpiryChanged = { selection ->
                    onAction(AddBankCardAction.ExpiryChanged(selection))
                },
            )
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
                        if (state.canSave) {
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
    selected: CardTypeSelection,
    onSelected: (CardType) -> Unit,
    label: String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = when (selected) {
                CardTypeSelection.Unselected -> ""
                is CardTypeSelection.Selected -> selected.type.label()
            },
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

@Composable
private fun CardNumberInvalidReason.label(): String = stringResource(
    when (this) {
        CardNumberInvalidReason.InvalidLength -> R.string.card_number_invalid_length
        CardNumberInvalidReason.InvalidCharacters -> R.string.card_number_invalid_characters
        CardNumberInvalidReason.ChecksumFailed -> R.string.card_number_checksum_failed
    },
)
