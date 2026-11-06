package com.aozijx.passly.feature.scanner.presentation

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalGetImage::class)
internal fun createScannerImageAnalysis(
    executor: Executor,
    scanner: BarcodeScanner,
    previewView: PreviewView,
    viewfinderBounds: () -> RectF,
    isDisposed: () -> Boolean,
    onBarcodeDetected: (String) -> Unit,
    onFailure: (Throwable) -> Unit,
): ImageAnalysis =
    ImageAnalysis.Builder()
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

    val region = ScannerRecognitionRegion.create(imageProxy, previewView, viewfinderBounds)
    if (region == null) {
        imageProxy.close()
        return
    }

    val proxyClosed = AtomicBoolean(false)
    fun closeProxy() {
        if (proxyClosed.compareAndSet(false, true)) imageProxy.close()
    }
    var completionListenerRegistered = false
    try {
        scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                if (isDisposed()) return@addOnSuccessListener
                barcodes.firstOrNull { barcode ->
                    !barcode.rawValue.isNullOrBlank() && region.contains(barcode)
                }?.rawValue?.let(onBarcodeDetected)
            }
            .addOnFailureListener { error -> if (!isDisposed()) onFailure(error) }
            .addOnCompleteListener { closeProxy() }
        completionListenerRegistered = true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (!isDisposed()) onFailure(error)
    } finally {
        if (!completionListenerRegistered) closeProxy()
    }
}
