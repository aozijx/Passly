package com.aozijx.passly.ui.screens.scanner

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.R
import com.aozijx.passly.core.media.ImageType
import com.aozijx.passly.core.media.rememberImagePicker
import com.aozijx.passly.ui.navigation.TopBarConfig
import com.aozijx.passly.ui.screens.components.ScannerView
import com.aozijx.passly.ui.screens.scanner.ScannerViewModel

/**
 * 通用扫码页面实现：
 * 专注于扫描并识别二维码内容。
 * 识别结果显示逻辑已下沉至 ScannerView 组件，支持自动链接跳转与点击复制。
 */
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scanResult by viewModel.scanResult.collectAsState()

    // 图片识别逻辑（从相册选择）
    val pickPhoto = rememberImagePicker { uri, _ ->
        viewModel.decodeImage(context, uri)
    }

    TopBarConfig(
        title = "扫一扫",
        actions = {
            IconButton(onClick = { pickPhoto(ImageType.SCREEN) }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_photo_24),
                    contentDescription = "选择图片",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 使用封装了结果显示逻辑的公共扫码组件
            ScannerView(
                scanResult = scanResult,
                onBarcodeDetected = { barcode ->
                    viewModel.onBarcodeDetected(context, barcode)
                },
                onPermissionDenied = {
                    Toast.makeText(context, "需要相机权限才能扫码", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}


