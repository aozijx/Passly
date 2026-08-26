package com.aozijx.passly.presentation.ui.vault.editor.bankcard

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryEditorSection
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryEditorTextField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryNotesField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryPasswordField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryTagsField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryTitleField
import com.aozijx.passly.presentation.ui.vault.editor.common.EntryUsernameField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankCardEditorScreen(
    state: BankCardEditorState,
    onEvent: BankCardEditorEventHandler,
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    AddEntryScaffold(
        title = stringResource(R.string.vault_add_bank_card_title),
        canSave = state.canSave,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onEvent.onBack,
        onSave = onEvent.onSave,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    ) {
        EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
            EntryTitleField(
                value = state.title,
                onValueChange = onEvent.onTitleChange,
                label = stringResource(R.string.field_title),
            )
            CardTypeDropdown(
                selected = state.cardType,
                onSelected = onEvent.onCardTypeChange,
                label = stringResource(R.string.card_type),
            )
            EntryUsernameField(
                value = state.cardholder,
                onValueChange = onEvent.onCardholderChange,
                label = stringResource(R.string.cardholder),
            )
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
            EntryPasswordField(
                password = state.cardNumber,
                onPasswordChange = onEvent.onCardNumberChange,
                isVisible = state.isCardNumberVisible,
                onVisibilityChange = onEvent.onCardNumberVisibilityChange,
                label = stringResource(R.string.card_number),
                isError = state.cardNumberError != null,
                supportingText = state.cardNumberError,
            )
            EntryPasswordField(
                password = state.paymentPin,
                onPasswordChange = onEvent.onPaymentPinChange,
                isVisible = state.isPinVisible,
                onVisibilityChange = onEvent.onPinVisibilityChange,
                label = stringResource(R.string.payment_pin),
            )
            EntryPasswordField(
                password = state.cardCvv,
                onPasswordChange = onEvent.onCvvChange,
                isVisible = state.isCvvVisible,
                onVisibilityChange = onEvent.onCvvVisibilityChange,
                label = stringResource(R.string.card_cvv),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EntryEditorTextField(
                    value = state.cardExpiryMonth,
                    onValueChange = onEvent.onExpiryMonthChange,
                    label = stringResource(R.string.card_expiry_month),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f),
                )
                EntryEditorTextField(
                    value = state.cardExpiryYear,
                    onValueChange = onEvent.onExpiryYearChange,
                    label = stringResource(R.string.card_expiry_year),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
            EntryTagsField(
                value = state.tags,
                onValueChange = onEvent.onTagsChange,
                label = stringResource(R.string.field_category),
            )
            EntryUsernameField(
                value = state.billingAddress,
                onValueChange = onEvent.onBillingAddressChange,
                label = stringResource(R.string.billing_address),
            )
            EntryNotesField(
                value = state.notes,
                onValueChange = onEvent.onNotesChange,
                label = stringResource(R.string.field_notes),
                keyboardActions = KeyboardActions(
                    onDone = { if (state.isFormValid) onEvent.onSave() },
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardTypeDropdown(
    selected: BankCardTypeUi?,
    onSelected: (BankCardTypeUi) -> Unit,
    label: String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        EntryEditorTextField(
            value = selected?.label().orEmpty(),
            onValueChange = {},
            label = label,
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
            BankCardTypeUi.entries.forEach { type ->
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
private fun BankCardTypeUi.label(): String = stringResource(
    when (this) {
        BankCardTypeUi.DEBIT -> R.string.card_type_debit
        BankCardTypeUi.CREDIT -> R.string.card_type_credit
    },
)
