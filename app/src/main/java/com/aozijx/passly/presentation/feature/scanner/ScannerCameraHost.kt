package com.aozijx.passly.presentation.feature.scanner

import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.aozijx.passly.presentation.ui.scanner.ScannerContent
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
internal fun ScannerCameraHost(
    onBarcodeDetected: (String) -> Unit,
    onCopyResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPermissionDenied: () -> Unit = {},
    isScanning: Boolean = true,
    scanResult: String = "",
    showResultCard: Boolean = true,
    autoHandleLinks: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val openLinkFailedText = stringResource(R.string.scanner_open_link_failed)
    val copySucceededText = stringResource(R.string.scanner_copy_succeeded)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val cameraSession = remember { ScannerCameraSession() }
    val hasPermission = remember { mutableStateOf(false) }
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)
    val permissionHost = rememberPermissionRequestHost("scanner.camera") { permission, result ->
        if (permission != RuntimePermission.CAMERA) return@rememberPermissionRequestHost
        val isGranted = result is PermissionRequestOutcome.Granted
        hasPermission.value = isGranted
        if (!isGranted) currentOnPermissionDenied()
    }

    LaunchedEffect(permissionHost) {
        val cameraPermission = permissionHost.status(RuntimePermission.CAMERA)
        hasPermission.value = cameraPermission == PermissionStatus.GRANTED
        when (cameraPermission) {
            PermissionStatus.GRANTED -> Unit
            PermissionStatus.DENIED -> permissionHost.request(RuntimePermission.CAMERA)
            else -> currentOnPermissionDenied()
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
                                try {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees,
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (disposed) return@addOnSuccessListener
                                            barcodes.firstOrNull()?.rawValue?.let { value ->
                                                AppTelemetry.d("ScannerCameraHost", "Barcode detected")
                                                currentOnBarcodeDetected(value)
                                            }
                                        }
                                        .addOnFailureListener { error ->
                                            AppTelemetry.e(
                                                "ScannerCameraHost",
                                                "Barcode analysis failed",
                                                error,
                                            )
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } catch (error: Exception) {
                                    imageProxy.close()
                                    AppTelemetry.e(
                                        "ScannerCameraHost",
                                        "Barcode analysis submission failed",
                                        error,
                                    )
                                }
                            }
                        }

                    cameraSession.provider = provider
                    cameraSession.imageAnalysis = imageAnalysis
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (error: Exception) {
                    AppTelemetry.e("ScannerCameraHost", "Camera binding failed", error)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            disposed = true
            cameraSession.stop()
        }
    }

    val isLinkResult = autoHandleLinks && scanResult.isWebLink()
    ScannerContent(
        hasPermission = hasPermission.value,
        scanResult = scanResult,
        showResultCard = showResultCard,
        isLinkResult = isLinkResult,
        onResultClick = {
            if (isLinkResult) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, scanResult.toUri()))
                } catch (_: Exception) {
                    Toast.makeText(context, openLinkFailedText, Toast.LENGTH_SHORT).show()
                }
            } else {
                onCopyResult(scanResult)
                Toast.makeText(context, copySucceededText, Toast.LENGTH_SHORT).show()
            }
        },
        cameraPreview = {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        },
        modifier = modifier,
    )
}

private fun String.isWebLink(): Boolean = startsWith("http://") || startsWith("https://")
