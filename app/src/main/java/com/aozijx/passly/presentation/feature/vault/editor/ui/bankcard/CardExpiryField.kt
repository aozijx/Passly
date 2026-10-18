package com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.CardExpirySelection
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.cardExpiryYearRange
import java.time.LocalDate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardExpiryField(
    selection: CardExpirySelection,
    onExpiryChanged: (CardExpirySelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val value = when (selection) {
        CardExpirySelection.NotSet -> stringResource(R.string.card_expiry_not_set)
        is CardExpirySelection.Selected -> selection.storageValue
    }

    OutlinedCard(onClick = { showPicker = true }, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.card_expiration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showPicker) {
        CardExpiryPickerSheet(
            selection = selection,
            onSelected = { month, year ->
                onExpiryChanged(CardExpirySelection.Selected(month, year))
                showPicker = false
            },
            onClear = {
                onExpiryChanged(CardExpirySelection.NotSet)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardExpiryPickerSheet(
    selection: CardExpirySelection,
    onSelected: (month: Int, year: Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val years = remember(today.year) { cardExpiryYearRange(today.year).toList() }
    val selected = selection as? CardExpirySelection.Selected
    var workingMonth by remember(selection, today) {
        mutableIntStateOf(selected?.month ?: today.monthValue)
    }
    var workingYear by remember(selection, today) {
        mutableIntStateOf(selected?.year?.takeIf(years::contains) ?: today.year)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.card_expiration),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (selection is CardExpirySelection.Selected) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.card_expiry_clear))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WheelColumn(
                    label = stringResource(R.string.card_expiry_month),
                    items = (1..12).toList(),
                    selected = workingMonth,
                    itemLabel = { it.toString().padStart(2, '0') },
                    onSelected = { workingMonth = it },
                    modifier = Modifier.weight(1f),
                )
                WheelColumn(
                    label = stringResource(R.string.card_expiry_year),
                    items = years,
                    selected = workingYear,
                    itemLabel = Int::toString,
                    onSelected = { workingYear = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = { onSelected(workingMonth, workingYear) },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun <T> WheelColumn(
    label: String,
    items: List<T>,
    selected: T,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState, items) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> items.getOrNull(index)?.let(onSelected) }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(144.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(12.dp),
                    ),
            )
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = 48.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(items) { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { scope.launch { listState.animateScrollToItem(index) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = itemLabel(item),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
