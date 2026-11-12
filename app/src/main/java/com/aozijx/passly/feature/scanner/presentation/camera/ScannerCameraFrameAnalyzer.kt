package com.aozijx.passly.feature.scanner.presentation.camera

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

internal fun createScannerImageAnalysis(
    executor: Executor,
    scanner: BarcodeScanner,
    previewView: PreviewView,
    viewfinderBounds: () -> RectF,
    isDisposed: () -> Boolean,
    onBarcodeDetected: (String) -> Unit,
    onFailure: (Throwable) -> Unit,
): ImageAnalysis =
    ImageAnalysis
        .Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(executor) { imageProxy ->
                analyzeScannerFrame(
                    imageProxy = imageProxy,
                    scanner = scanner,
                    previewView = previewView,
                    viewfinderBounds = viewfinderBounds(),
                    isDisposed = isDisposed,
                    onBarcodeDetected = onBarcodeDetected,
                    onFailure = onFailure,
                )
            }
        }

@OptIn(ExperimentalGetImage::class)
private fun analyzeScannerFrame(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    previewView: PreviewView,
    viewfinderBounds: RectF,
    isDisposed: () -> Boolean,
    onBarcodeDetected: (String) -> Unit,
    onFailure: (Throwable) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null || isDisposed()) {
        imageProxy.close()
        return
    }

    val proxyClosed = AtomicBoolean(false)

    fun closeProxy() {
        if (proxyClosed.compareAndSet(false, true)) imageProxy.close()
    }
    var completionListenerRegistered = false
    try {
        val mainExecutor = ContextCompat.getMainExecutor(previewView.context)
        scanner
            .process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener(mainExecutor) { barcodes ->
                try {
                    if (isDisposed()) return@addOnSuccessListener
                    val region =
                        ScannerRecognitionRegion.create(imageProxy, previewView, viewfinderBounds)
                            ?: return@addOnSuccessListener
                    barcodes
                        .firstOrNull { barcode ->
                            !barcode.rawValue.isNullOrBlank() && region.contains(barcode)
                        }?.rawValue
                        ?.let(onBarcodeDetected)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (!isDisposed()) onFailure(error)
                } finally {
                    closeProxy()
                }
            }.addOnFailureListener(mainExecutor) { error ->
                try {
                    if (!isDisposed()) onFailure(error)
                } finally {
                    closeProxy()
                }
            }.addOnCompleteListener(mainExecutor) { closeProxy() }
        completionListenerRegistered = true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (!isDisposed()) onFailure(error)
    } finally {
        if (!completionListenerRegistered) closeProxy()
    }
}
