package com.example.poop.ui.screens.vault.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.VaultViewModel

@Composable
fun DetailHeader(
    item: VaultEntry,
    onIconClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val hasCustomPath = !item.iconCustomPath.isNullOrEmpty()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        if (hasCustomPath) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onIconClick)) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(item.iconCustomPath).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (onMoreClick != null) {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape).size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable(onClick = onIconClick),
                    contentAlignment = Alignment.Center
                ) {
                    VaultItemIcon(item = item, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (onMoreClick != null) IconButton(onClick = onMoreClick) { Icon(Icons.Default.MoreVert, null) }
            }
        }
    }
}

@Composable
fun DetailActions(onDeleteClick: () -> Unit, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onDeleteClick, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("删除")
        }
        TextButton(onClick = onDismiss) { Text("关闭") }
    }
}

@Composable
fun EditTextField(value: String, onValueChange: (String) -> Unit, label: String, onSave: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = onSave) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryInputField(value: String, onValueChange: (String) -> Unit, viewModel: VaultViewModel, label: String = "分类") {
    val categories by viewModel.availableCategories.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {  expanded = it }
    ) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        if (categories.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onValueChange(it); expanded = false }) }
            }
        }
    }
}

@Composable
fun CategoryItem(viewModel: VaultViewModel, entry: VaultEntry) {
    if (viewModel.isEditingCategory) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryInputField(value = viewModel.editedCategory, onValueChange = { viewModel.editedCategory = it }, viewModel = viewModel, label = "修改分类")
            TextButton(onClick = { viewModel.saveCategoryEdit() }, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Check, null); Spacer(Modifier.width(4.dp)); Text("保存分类")
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.startEditingCategory() }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("分类", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(entry.category, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, isRevealed: Boolean, onCopy: () -> Unit, onEdit: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), letterSpacing = if (isRevealed) 0.sp else 2.sp)
            IconButton(onClick = if (isRevealed) onEdit else onCopy, modifier = Modifier.size(32.dp)) {
                Icon(if (isRevealed) Icons.Default.Edit else Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
