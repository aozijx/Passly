package com.aozijx.passly.feature.scanner.presentation.camera

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
import com.aozijx.passly.feature.scanner.presentation.state.ScannerCameraState
import com.aozijx.passly.feature.scanner.presentation.viewfinder.ScannerViewfinderFrame
import com.google.mlkit.vision.barcode.BarcodeScanning
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

@Composable
internal fun ScannerCameraHost(
    onBarcodeDetected: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    onFailure: (Throwable) -> Unit,
    cameraState: ScannerCameraState,
    viewfinderFrame: ScannerViewfinderFrame,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView =
        remember {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val cameraSession = remember { ScannerCameraSession() }
    val permissionState = remember { mutableStateOf<ScannerCameraPermissionState>(ScannerCameraPermissionState.Checking) }
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)
    val currentOnFailure by rememberUpdatedState(onFailure)
    val currentViewfinderFrame by rememberUpdatedState(viewfinderFrame)
    val permissionHost =
        rememberPermissionRequestHost("scanner.camera") { permission, result ->
            if (permission != RuntimePermission.CAMERA) return@rememberPermissionRequestHost
            when (result) {
                PermissionRequestOutcome.Granted -> {
                    permissionState.value = ScannerCameraPermissionState.Granted
                }

                is PermissionRequestOutcome.Denied -> {
                    permissionState.value = ScannerCameraPermissionState.Denied
                    currentOnPermissionDenied()
                }
            }
        }

    LaunchedEffect(permissionHost) {
        when (permissionHost.status(RuntimePermission.CAMERA)) {
            PermissionStatus.GRANTED -> permissionState.value = ScannerCameraPermissionState.Granted
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

    DisposableEffect(lifecycleOwner, permissionState.value, cameraState) {
        val disposed = AtomicBoolean(false)
        cameraSession.stop()
        when (cameraState) {
            ScannerCameraState.Idle -> {
                Unit
            }

            ScannerCameraState.Active -> {
                when (permissionState.value) {
                    ScannerCameraPermissionState.Checking,
                    ScannerCameraPermissionState.Denied,
                    -> {
                        Unit
                    }

                    ScannerCameraPermissionState.Granted -> {
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
                                            imageAnalysis =
                                                createScannerImageAnalysis(
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
                }
            }
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

private sealed interface ScannerCameraPermissionState {
    data object Checking : ScannerCameraPermissionState

    data object Granted : ScannerCameraPermissionState

    data object Denied : ScannerCameraPermissionState
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
        val preview =
            Preview.Builder().build().apply {
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
