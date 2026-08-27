package com.aozijx.passly.presentation.ui.scanner

import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aozijx.passly.R
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.app.platform.permission.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private class ScannerCameraSession {
    var provider: ProcessCameraProvider? = null
    var imageAnalysis: ImageAnalysis? = null

    fun stop() {
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        provider?.unbindAll()
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun ScannerContent(
    onBarcodeDetected: (String) -> Unit,
    onCopyResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPermissionDenied: () -> Unit = {},
    isScanning: Boolean = true,
    scanResult: String = "",
    showResultCard: Boolean = true,
    autoHandleLinks: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val waitingForPermissionText = stringResource(R.string.scanner_waiting_for_permission)
    val openLinkFailedText = stringResource(R.string.scanner_open_link_failed)
    val copySucceededText = stringResource(R.string.scanner_copy_succeeded)
    val openLinkActionText = stringResource(R.string.scanner_open_link_action)
    val copyResultActionText = stringResource(R.string.scanner_copy_result_action)

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val cameraSession = remember { ScannerCameraSession() }
    val hasPermission = remember { mutableStateOf(false) }

    val permissionHost = rememberPermissionRequestHost("scanner.camera") { permission, result ->
        if (permission != RuntimePermission.CAMERA) return@rememberPermissionRequestHost
        val isGranted = result is PermissionRequestOutcome.Granted
        hasPermission.value = isGranted
        if (!isGranted) onPermissionDenied()
    }

    LaunchedEffect(permissionHost) {
        val cameraPermission = permissionHost.status(RuntimePermission.CAMERA)
        hasPermission.value = cameraPermission == PermissionStatus.GRANTED
        when (cameraPermission) {
            PermissionStatus.GRANTED -> Unit
            PermissionStatus.DENIED ->
                permissionHost.request(RuntimePermission.CAMERA)

            else -> onPermissionDenied()
        }
    }

    DisposableEffect(cameraExecutor, barcodeScanner) {
        onDispose {
            cameraSession.stop()
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    DisposableEffect(lifecycleOwner, hasPermission.value, isScanning) {
        var disposed = false
        cameraSession.stop()

        if (hasPermission.value && isScanning) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                if (disposed) return@addListener
                try {
                    val provider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage == null || disposed) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (disposed) return@addOnSuccessListener
                                        barcodes.firstOrNull()?.rawValue?.let { value ->
                                            AppTelemetry.d(
                                                "ScannerContent",
                                                "Barcode detected"
                                            )
                                            onBarcodeDetected(value)
                                        }
                                    }
                                    .addOnFailureListener { error ->
                                        AppTelemetry.e(
                                            "ScannerContent",
                                            "Barcode analysis failed",
                                            error
                                        )
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            }
                        }

                    cameraSession.provider = provider
                    cameraSession.imageAnalysis = imageAnalysis
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    AppTelemetry.e("ScannerContent", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            disposed = true
            cameraSession.stop()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasPermission.value) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                waitingForPermissionText,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedVisibility(
            visible = showResultCard && scanResult.isNotEmpty(),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            val isUrl = remember(scanResult) {
                scanResult.startsWith("http://") || scanResult.startsWith("https://")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.largeIncreased)
                        .clickable {
                            if (autoHandleLinks && isUrl) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, scanResult.toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, openLinkFailedText, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            } else {
                                onCopyResult(scanResult)
                                Toast.makeText(context, copySucceededText, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (autoHandleLinks && isUrl) {
                                openLinkActionText
                            } else {
                                copyResultActionText
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
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
