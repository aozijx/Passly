package com.aozijx.passly.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata
import com.aozijx.passly.core.platform.packageinfo.InstalledAppServicesProvider
import dagger.hilt.android.EntryPointAccessors

@Composable
fun rememberAppIcon(packageName: String?): Painter? {
    val context = LocalContext.current
    val iconLoader = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InstalledAppServicesProvider::class.java
        ).getInstalledAppIconLoader()
    }
    return remember(packageName) {
        packageName?.let {
            iconLoader.loadIcon(it)?.asImageBitmap()?.let(::BitmapPainter)
        }
    }
}

@Composable
fun rememberAppMetadata(packageName: String?): InstalledAppMetadata? {
    val context = LocalContext.current
    val appCatalog = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InstalledAppServicesProvider::class.java
        ).getInstalledAppCatalog()
    }
    return remember(packageName) {
        packageName?.let(appCatalog::getAppMetadata)
    }
}
