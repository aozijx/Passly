package com.aozijx.passly.core.platform.packageinfo

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberInstalledAppIconBitmap(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    val iconLoader = remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InstalledAppServicesProvider::class.java,
        ).getInstalledAppIconLoader()
    }
    val bitmapState = remember(iconLoader, packageName) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(iconLoader, packageName) {
        bitmapState.value = withContext(Dispatchers.IO) {
            packageName?.let(iconLoader::loadIcon)
        }
    }
    val bitmap by bitmapState
    return remember(bitmap) { bitmap?.asImageBitmap() }
}
