package com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.bankcard.cardExpiryYearRange
import java.time.Month
import java.time.Year
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardExpiryField(
    month: Int?,
    year: Int?,
    onExpiryChanged: (month: Int?, year: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val value = if (month in 1..12 && year != null) {
        "%02d/%d".format(month, year)
    } else {
        stringResource(R.string.card_expiry_not_set)
    }

    OutlinedCard(
        onClick = { showPicker = true },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPicker) {
        CardExpiryPickerSheet(
            selectedMonth = month,
            selectedYear = year,
            onSelected = { selectedMonth, selectedYear ->
                onExpiryChanged(selectedMonth, selectedYear)
                showPicker = false
            },
            onClear = {
                onExpiryChanged(null, null)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardExpiryPickerSheet(
    selectedMonth: Int?,
    selectedYear: Int?,
    onSelected: (month: Int, year: Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val currentYear = remember { Year.now().value }
    val years = remember(currentYear) {
        cardExpiryYearRange(currentYear).toList()
    }
    var workingYear by remember(selectedYear, currentYear) {
        mutableIntStateOf(selectedYear ?: currentYear)
    }
    val initialYearIndex = remember(years, workingYear) {
        years.indexOf(workingYear).coerceAtLeast(0)
    }
    val yearListState = rememberLazyListState(initialFirstVisibleItemIndex = initialYearIndex)
    val locale = LocalConfiguration.current.locales[0]
    val months = remember(locale) {
        Month.values().map { month ->
            month.value to month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
        }
    }

    LaunchedEffect(workingYear) {
        val index = years.indexOf(workingYear)
        if (index >= 0) yearListState.animateScrollToItem(index)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.card_expiration),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (selectedMonth in 1..12 && selectedYear != null) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.card_expiry_clear))
                    }
                }
            }

            Text(
                text = stringResource(R.string.card_expiry_year),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            )
            LazyRow(
                state = yearListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(years.size, key = { years[it] }) { index ->
                    val year = years[index]
                    FilterChip(
                        selected = workingYear == year,
                        onClick = { workingYear = year },
                        label = { Text(year.toString()) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.card_expiry_month),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                months.chunked(3).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowMonths.forEach { (month, label) ->
                            FilterChip(
                                selected = selectedMonth == month && selectedYear == workingYear,
                                onClick = { onSelected(month, workingYear) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
