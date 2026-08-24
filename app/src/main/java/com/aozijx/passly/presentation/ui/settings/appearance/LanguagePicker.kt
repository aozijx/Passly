package com.aozijx.passly.presentation.ui.settings.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.settings.appearance.model.LanguageOptionUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePicker(
    currentKey: String,
    options: List<LanguageOptionUiModel>,
    onSelect: (String) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // 使用 LazyColumn 替代 Column + verticalScroll 获得更稳定的滑动性能
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentPadding = PaddingValues(top = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_language_choose),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(options, key = LanguageOptionUiModel::key) { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(language.key) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = language.key == currentKey,
                        onClick = { onSelect(language.key) }
                    )
                    Spacer(Modifier.padding(start = 12.dp))
                    Text(text = language.label)
                }
            }
        }
    }
}
