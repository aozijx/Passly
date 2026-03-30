package com.example.poop.core.designsystem.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poop.R
import com.example.poop.core.designsystem.state.VaultEditState
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.VaultViewModel

/**
 * 分类选择输入框 (内部组件)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryInputField(
    value: String,
    onValueChange: (String) -> Unit,
    viewModel: VaultViewModel,
    label: String = stringResource(R.string.label_category)
) {
    val categories by viewModel.availableCategories.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        if (categories.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onValueChange(category)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 通用的分类展示与编辑条目
 */
@Composable
fun CategoryItem(
    viewModel: VaultViewModel,
    entry: VaultEntry,
    editState: VaultEditState
) {
    if (editState.isEditingCategory) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryInputField(
                value = editState.editedCategory,
                onValueChange = { editState.editedCategory = it },
                viewModel = viewModel,
                label = stringResource(R.string.label_category)
            )
            TextButton(
                onClick = {
                    val updatedEntry = editState.applyTo(entry)
                    viewModel.updateVaultEntry(updatedEntry)
                    editState.isEditingCategory = false
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_save))
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { editState.isEditingCategory = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.label_category),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(entry.category, fontWeight = FontWeight.SemiBold)
        }
    }
}
