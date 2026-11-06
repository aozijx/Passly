package com.aozijx.passly.feature.autofill.presentation.legacy.ui

import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutofillCandidateBottomSheet(
    candidates: List<AutofillCandidateSummary>,
    targetAppPackageName: String,
    onCandidateSelected: (EntryId) -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val targetAppIcon = rememberTargetApplicationIcon(targetAppPackageName)

    BackHandler { onCancel() }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                pluralStringResource(
                                    id = R.plurals.autofill_entry_compact_summary,
                                    count = candidates.size,
                                    candidates.size,
                                ),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.autofill_cancel))
                        }
                    }
                    Text(
                        text = stringResource(R.string.autofill_select_account_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = candidates,
                        key = { candidate -> candidate.entry.id.value },
                    ) { candidate ->
                        CandidateItem(
                            candidate = candidate,
                            targetAppIcon = targetAppIcon,
                            onClick = { onCandidateSelected(candidate.entry.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateItem(
    candidate: AutofillCandidateSummary,
    targetAppIcon: ImageBitmap?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (targetAppIcon != null) {
                Image(
                    bitmap = targetAppIcon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = candidate.entry.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun rememberTargetApplicationIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(context, packageName) {
        try {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Resources.NotFoundException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
