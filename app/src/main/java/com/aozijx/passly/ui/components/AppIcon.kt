package com.aozijx.passly.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.core.platform.PackageUtilsProvider
import dagger.hilt.android.EntryPointAccessors

@Composable
fun rememberAppIcon(packageName: String?): Painter? {
    val context = LocalContext.current
    val packageUtils = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PackageUtilsProvider::class.java
        ).getPackageUtils()
    }
    return remember(packageName) {
        packageName?.let { packageUtils.loadIcon(it)?.let { BitmapPainter(it) } }
    }
}