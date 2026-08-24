package com.aozijx.passly.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aozijx.passly.core.media.toLocalIconImageModel
import com.aozijx.passly.domain.entry.model.query.EntryListItem

@Composable
fun VaultItemIcon(
    modifier: Modifier = Modifier,
    iconable: EntryListItem,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) = VaultItemIcon(
    modifier = modifier,
    iconName = iconable.profile.icon.name,
    iconCustomPath = iconable.profile.icon.customReference,
    associatedAppPackage = iconable.profile.associations.applicationIds.firstOrNull(),
    classificationInput = EntryClassificationInput(
        entryType = iconable.entryType,
        title = iconable.title,
        username = iconable.username,
        urls = setOfNotNull(iconable.profile.associations.primaryUrl),
        domains = iconable.profile.associations.domains,
        packageNames = iconable.profile.associations.applicationIds,
        appNames = setOfNotNull(
            iconable.title.takeIf {
                iconable.profile.associations.applicationIds.isNotEmpty() &&
                    iconable.profile.associations.primaryUrl == null &&
                    iconable.profile.associations.domains.isEmpty()
            },
        ),
    ),
    tint = tint,
)

@Composable
fun VaultItemIcon(
    modifier: Modifier = Modifier,
    iconName: String?,
    iconCustomPath: String?,
    associatedAppPackage: String?,
    entryTypeKey: String,
    title: String,
    username: String,
    associatedDomain: String?,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) = VaultItemIcon(
    modifier = modifier,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    associatedAppPackage = associatedAppPackage,
    classificationInput = EntryClassificationInput(
        entryType = enumValues<com.aozijx.passly.domain.entry.model.EntryType>()
            .firstOrNull { it.name == entryTypeKey }
            ?: com.aozijx.passly.domain.entry.model.EntryType.LOGIN,
        title = title,
        username = username,
        urls = setOfNotNull(associatedDomain),
        domains = setOfNotNull(associatedDomain),
        packageNames = setOfNotNull(associatedAppPackage),
        appNames = setOfNotNull(title.takeIf { associatedAppPackage != null && associatedDomain == null }),
    ),
    tint = tint,
)

@Composable
private fun VaultItemIcon(
    modifier: Modifier,
    iconName: String?,
    iconCustomPath: String?,
    associatedAppPackage: String?,
    classificationInput: EntryClassificationInput,
    tint: Color,
) {
    val appIconPainter = rememberAppIcon(associatedAppPackage)
    val explicitIconVector = remember(iconName) {
        iconName
            ?.takeIf { it.isNotBlank() }
            ?.let(VaultIcons::getIconByName)
    }
    val visualCategory = remember(classificationInput) {
        EntryVisualCategoryClassifier.classify(classificationInput)
    }
    val fallbackIconVector = explicitIconVector ?: VaultIcons.getIconByCategory(visualCategory)
    val fallbackPainter = rememberVectorPainter(fallbackIconVector)
    val placeholderPainter = appIconPainter ?: fallbackPainter

    val customModel = remember(iconCustomPath) { toLocalIconImageModel(iconCustomPath) }
    Box(
        modifier = modifier.size(36.dp), contentAlignment = Alignment.Center
    ) {
        when {
            customModel != null -> {
                AsyncImage(
                    model = customModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholderPainter,
                    error = placeholderPainter
                )
            }

            appIconPainter != null -> {
                Image(
                    painter = appIconPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            else -> {
                Icon(
                    imageVector = fallbackIconVector,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
