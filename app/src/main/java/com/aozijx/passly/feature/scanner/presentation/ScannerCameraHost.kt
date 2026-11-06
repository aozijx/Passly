package com.aozijx.passly.feature.scanner.presentation

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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aozijx.passly.app.platform.permission.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.google.mlkit.vision.barcode.BarcodeScanning
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

@Composable
internal fun ScannerCameraHost(
    onBarcodeDetected: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    onFailure: (Throwable) -> Unit,
    isScanning: Boolean,
    viewfinderFrame: ScannerViewfinderFrame,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    val currentOnFailure by rememberUpdatedState(onFailure)
    val currentViewfinderFrame by rememberUpdatedState(viewfinderFrame)
    val permissionHost = rememberPermissionRequestHost("scanner.camera") { permission, result ->
        if (permission != RuntimePermission.CAMERA) return@rememberPermissionRequestHost
        hasPermission.value = result is PermissionRequestOutcome.Granted
        if (!hasPermission.value) currentOnPermissionDenied()
    }

    LaunchedEffect(permissionHost) {
        when (permissionHost.status(RuntimePermission.CAMERA)) {
            PermissionStatus.GRANTED -> hasPermission.value = true
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
        val disposed = AtomicBoolean(false)
        cameraSession.stop()
        if (hasPermission.value && isScanning) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    if (!disposed.get()) {
                        try {
                            bindScannerCamera(
                                provider = providerFuture.get(),
                                previewView = previewView,
                                lifecycleOwner = lifecycleOwner,
                                cameraSession = cameraSession,
                                imageAnalysis = createScannerImageAnalysis(
                                    executor = cameraExecutor,
                                    scanner = barcodeScanner,
                                    previewView = previewView,
                                    viewfinderBounds = { currentViewfinderFrame.boundsInPreview },
                                    isDisposed = disposed::get,
                                    onBarcodeDetected = { currentOnBarcodeDetected(it) },
                                    onFailure = { currentOnFailure(it) },
                                ),
                                isDisposed = disposed::get,
                                onFailure = { currentOnFailure(it) },
                            )
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            if (!disposed.get()) currentOnFailure(error)
                        }
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }

        onDispose {
            disposed.set(true)
            cameraSession.stop()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
    )
}

private fun bindScannerCamera(
    provider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraSession: ScannerCameraSession,
    imageAnalysis: ImageAnalysis,
    isDisposed: () -> Boolean,
    onFailure: (Throwable) -> Unit,
) {
    try {
        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }
        cameraSession.bind(
            provider = provider,
            previewView = previewView,
            lifecycleOwner = lifecycleOwner,
            preview = preview,
            imageAnalysis = imageAnalysis,
            isDisposed = isDisposed,
            onFailure = onFailure,
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (!isDisposed()) onFailure(error)
    }
}
