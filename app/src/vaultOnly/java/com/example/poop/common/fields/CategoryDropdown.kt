package com.example.poop.common.fields

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.poop.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    availableCategories: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = onCategorySelected,
            label = { Text(stringResource(R.string.label_category_optional)) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        // 仅当可用分类不为空时显示下拉选项
        if (availableCategories.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(text = category) },
                        onClick = {
                            onCategorySelected(category)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
