package com.aozijx.passly.presentation.ui.shared.components

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
import com.aozijx.passly.presentation.ui.shared.media.toLocalIconImageModel
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
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
    iconColor: String? = null,
) = VaultItemIcon(
    modifier = modifier,
    iconName = iconName,
    iconCustomPath = iconCustomPath,
    associatedAppPackage = associatedAppPackage,
    classificationInput = EntryClassificationInput(
        entryType = enumValues<EntryTypeUiModel>()
            .firstOrNull { it.name == entryTypeKey }
            ?: EntryTypeUiModel.LOGIN,
        title = title,
        username = username,
        urls = setOfNotNull(associatedDomain),
        domains = setOfNotNull(associatedDomain),
        packageNames = setOfNotNull(associatedAppPackage),
        appNames = setOfNotNull(title.takeIf { associatedAppPackage != null && associatedDomain == null }),
    ),
    iconColor = iconColor,
    tint = MaterialTheme.colorScheme.onSecondaryContainer,
)

@Composable
private fun VaultItemIcon(
    modifier: Modifier,
    iconName: String?,
    iconCustomPath: String?,
    associatedAppPackage: String?,
    classificationInput: EntryClassificationInput,
    iconColor: String?,
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
    val resolvedTint = if (explicitIconVector != null && customModel == null) {
        iconColorForStorageToken(iconColor, tint)
    } else {
        tint
    }
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
                    contentScale = ContentScale.Fit,
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
                    tint = resolvedTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
