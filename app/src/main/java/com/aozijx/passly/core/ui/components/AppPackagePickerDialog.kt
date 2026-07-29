package com.aozijx.passly.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.PackageUtils
import com.aozijx.passly.core.platform.PackageUtilsProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppPackagePickerDialog(
    onSelect: (PackageUtils.AppMetadata) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val packageUtils = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PackageUtilsProvider::class.java,
        ).getPackageUtils()
    }
    val apps by produceState(initialValue = emptyList(), packageUtils) {
        value = withContext(Dispatchers.IO) { packageUtils.getLaunchableApps() }
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val normalized = query.trim()
        if (normalized.isEmpty()) apps else apps.filter {
            it.appName.contains(normalized, ignoreCase = true) ||
                it.packageName.contains(normalized, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_package_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text(stringResource(R.string.app_package_picker_search)) },
                )
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = PackageUtils.AppMetadata::packageName) { app ->
                        val icon = remember(app.packageName) {
                            packageUtils.loadIcon(app.packageName)?.let(::BitmapPainter)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (icon != null) {
                                Image(
                                    painter = icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
