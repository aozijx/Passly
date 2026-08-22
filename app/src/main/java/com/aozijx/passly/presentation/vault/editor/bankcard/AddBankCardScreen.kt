package com.aozijx.passly.presentation.vault.editor.bankcard

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.editor.bankcard.AddBankCardAction
import com.aozijx.passly.feature.vault.editor.bankcard.AddBankCardFormState
import com.aozijx.passly.feature.vault.editor.bankcard.AddBankCardViewModel
import com.aozijx.passly.feature.vault.editor.bankcard.CardType
import com.aozijx.passly.feature.vault.editor.common.CreateEntryEffect
import com.aozijx.passly.presentation.vault.editor.common.AddEntryScaffold
import com.aozijx.passly.presentation.vault.editor.common.EntryEditorSection
import com.aozijx.passly.presentation.vault.editor.common.EntryEditorTextField
import com.aozijx.passly.presentation.vault.editor.common.EntryNotesField
import com.aozijx.passly.presentation.vault.editor.common.EntryPasswordField
import com.aozijx.passly.presentation.vault.editor.common.EntryTagsField
import com.aozijx.passly.presentation.vault.editor.common.EntryTitleField
import com.aozijx.passly.presentation.vault.editor.common.EntryUsernameField

@Composable
fun AddBankCardScreen(
    viewModel: AddBankCardViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUserInteraction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveFailedMessage = stringResource(R.string.vault_add_bank_card_save_failed)
    val latestOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel, snackbarHostState, saveFailedMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CreateEntryEffect.Saved -> latestOnSaved()
                is CreateEntryEffect.SaveFailed -> {
                    snackbarHostState.showSnackbar(effect.message ?: saveFailedMessage)
                }
            }
        }
    }

    AddEntryScaffold(
        title = stringResource(R.string.vault_add_bank_card_title),
        canSave = uiState.canSave,
        isSaving = uiState.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = {
            keyboardController?.hide()
            viewModel.onAction(AddBankCardAction.Save)
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    ) {
        BankCardForm(
            state = uiState.form,
            callbacks = BankCardFormCallbacks(
                onAction = { action ->
                    onUserInteraction()
                    viewModel.onAction(action)
                },
                onVisibilityAction = { action ->
                    viewModel.onAction(action)
                },
                onSave = {
                    keyboardController?.hide()
                    viewModel.onAction(AddBankCardAction.Save)
                }
            )
        )
    }
}

data class BankCardFormCallbacks(
    val onAction: (AddBankCardAction) -> Unit,
    val onVisibilityAction: (AddBankCardAction) -> Unit,
    val onSave: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankCardForm(
    state: AddBankCardFormState,
    callbacks: BankCardFormCallbacks
) {
    EntryEditorSection(title = stringResource(R.string.vault_editor_section_basic_info)) {
        EntryTitleField(
            value = state.title,
            onValueChange = { callbacks.onAction(AddBankCardAction.TitleChanged(it)) },
            label = stringResource(R.string.field_title)
        )
        CardTypeDropdown(
            selected = state.cardType,
            onSelected = { callbacks.onAction(AddBankCardAction.CardTypeChanged(it)) },
            label = stringResource(R.string.card_type)
        )
        EntryUsernameField(
            value = state.cardholder,
            onValueChange = { callbacks.onAction(AddBankCardAction.CardholderChanged(it)) },
            label = stringResource(R.string.cardholder)
        )
    }

    EntryEditorSection(title = stringResource(R.string.vault_editor_section_credentials)) {
        EntryPasswordField(
            password = state.cardNumber,
            onPasswordChange = {
                callbacks.onAction(AddBankCardAction.CardNumberChanged(it))
            },
            isVisible = state.isCardNumberVisible,
            onVisibilityChange = {
                callbacks.onVisibilityAction(
                    AddBankCardAction.CardNumberVisibilityChanged(it)
                )
            },
            label = stringResource(R.string.card_number),
            isError = state.cardNumberError != null,
            supportingText = state.cardNumberError
        )
        EntryPasswordField(
            password = state.paymentPin,
            onPasswordChange = {
                callbacks.onAction(AddBankCardAction.PaymentPinChanged(it))
            },
            isVisible = state.isPinVisible,
            onVisibilityChange = {
                callbacks.onVisibilityAction(AddBankCardAction.PinVisibilityChanged(it))
            },
            label = stringResource(R.string.payment_pin)
        )
        EntryPasswordField(
            password = state.cardCvv,
            onPasswordChange = { callbacks.onAction(AddBankCardAction.CvvChanged(it)) },
            isVisible = state.isCvvVisible,
            onVisibilityChange = {
                callbacks.onVisibilityAction(AddBankCardAction.CvvVisibilityChanged(it))
            },
            label = stringResource(R.string.card_cvv)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EntryEditorTextField(
                value = state.cardExpiryMonth,
                onValueChange = {
                    callbacks.onAction(AddBankCardAction.ExpiryMonthChanged(it))
                },
                label = stringResource(R.string.card_expiry_month),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            EntryEditorTextField(
                value = state.cardExpiryYear,
                onValueChange = {
                    callbacks.onAction(AddBankCardAction.ExpiryYearChanged(it))
                },
                label = stringResource(R.string.card_expiry_year),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    EntryEditorSection(title = stringResource(R.string.vault_editor_section_details)) {
        EntryTagsField(
            value = state.tags,
            onValueChange = { callbacks.onAction(AddBankCardAction.TagsChanged(it)) },
            label = stringResource(R.string.field_category)
        )
        EntryUsernameField(
            value = state.billingAddress,
            onValueChange = {
                callbacks.onAction(AddBankCardAction.BillingAddressChanged(it))
            },
            label = stringResource(R.string.billing_address)
        )
        EntryNotesField(
            value = state.notes,
            onValueChange = { callbacks.onAction(AddBankCardAction.NotesChanged(it)) },
            label = stringResource(R.string.field_notes),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (state.isValid) callbacks.onSave()
                }
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardTypeDropdown(
    selected: CardType?,
    onSelected: (CardType) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        EntryEditorTextField(
            value = selected?.let { stringResource(it.labelRes) } ?: "",
            onValueChange = {},
            label = label,
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CardType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.labelRes)) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
