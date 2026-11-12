package com.aozijx.passly.feature.vault.presentation.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.PasslySheetTitle

@Composable
internal fun SheetTitleSearchHeader(
    title: String,
    searchLabel: String,
    query: String,
    onQueryChanged: (String) -> Unit,
    searchEnabled: Boolean = true,
    searchFieldModifier: Modifier = Modifier,
) {
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    AnimatedContent(
        targetState = searchEnabled && isSearchExpanded,
        label = "sheet title and search",
    ) { showSearch ->
        if (showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth().then(searchFieldModifier),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onQueryChanged("")
                            isSearchExpanded = false
                        },
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                label = { Text(searchLabel) },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PasslySheetTitle(
                    text = title,
                    modifier = Modifier.weight(1f),
                )
                if (searchEnabled) {
                    IconButton(onClick = { isSearchExpanded = true }) {
                        Icon(Icons.Default.Search, contentDescription = searchLabel)
                    }
                }
            }
        }
    }
}
