package com.example.poop.ui.screens.scanner

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.R
import com.example.poop.ui.navigation.TopBarConfig
import com.example.poop.ui.screens.components.ScannerView
import com.example.poop.ui.screens.profile.ImageType
import com.example.poop.util.rememberImagePicker
import kotlinx.coroutines.launch

/**
 * 通用扫码页面实现：
 * 专注于扫描并识别二维码内容，支持点击复制。
 */
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scanResult by viewModel.scanResult.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

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
            // 使用提取的公共扫码组件
            ScannerView(
                onBarcodeDetected = { barcode ->
                    viewModel.onBarcodeDetected(context, barcode)
                },
                onPermissionDenied = {
                    Toast.makeText(context, "需要相机权限才能扫码", Toast.LENGTH_SHORT).show()
                }
            )

            // 识别结果显示与操作区
            if (scanResult.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 通用识别结果显示卡片（支持点击复制）
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable {
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("scan_result", scanResult)))
                                }
                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = scanResult,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
