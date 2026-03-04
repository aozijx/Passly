package com.example.poop.ui.screens.scanner

import android.Manifest
import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.R
import com.example.poop.ui.navigation.TopBarConfig
import com.example.poop.ui.screens.profile.ImageType
import com.example.poop.util.Logcat
import com.example.poop.util.rememberImagePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalGetImage::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val parsedTotp by viewModel.parsedTotp.collectAsState()
    val previewView = remember { PreviewView(context) }
    val scope = rememberCoroutineScope()

    val pickPhoto = rememberImagePicker { uri, _ ->
        viewModel.decodeImage(context, uri)
    }
    val clipboard = LocalClipboard.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(viewModel.cameraExecutor, viewModel.getAnalyzer(context))
                }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Logcat.e("ScannerScreen", "相机生命周期绑定失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

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
                        // 如果是 TOTP，显示专用保存卡片
                        parsedTotp?.let { totp ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "发现 TOTP 密钥",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "账户: ${totp.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.saveToVault(context) {
                                                Toast.makeText(context, "已成功保存到保险库", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("保存到保险库")
                                    }
                                }
                            }
                        }

                        // 始终显示识别结果提示框（普通的或 TOTP 的原始内容）
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .clickable {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(ClipData.newPlainText("scan_result", scanResult))
                                        )
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
            } else {
                Text(
                    text = "请授予相机权限以使用扫码功能",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
