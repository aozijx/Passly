package com.aozijx.passly.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.core.platform.packageinfo.InstalledAppRegistry
import com.aozijx.passly.core.platform.packageinfo.InstalledAppRegistryProvider
import dagger.hilt.android.EntryPointAccessors

@Composable
fun rememberAppIcon(packageName: String?): Painter? {
    val context = LocalContext.current
    val packageUtils = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InstalledAppRegistryProvider::class.java
        ).getInstalledAppRegistry()
    }
    return remember(packageName) {
        packageName?.let {
            packageUtils.loadIcon(it)?.asImageBitmap()?.let(::BitmapPainter)
        }
    }
}

@Composable
fun rememberAppMetadata(packageName: String?): InstalledAppRegistry.AppMetadata? {
    val context = LocalContext.current
    val packageUtils = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InstalledAppRegistryProvider::class.java
        ).getInstalledAppRegistry()
    }
    return remember(packageName) {
        packageName?.let(packageUtils::getAppMetadata)
    }
}
