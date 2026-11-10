package com.aozijx.passly.feature.autofill.presentation

import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

@Composable
internal fun rememberTargetAppIcon(packageName: String): ImageBitmap? {
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
