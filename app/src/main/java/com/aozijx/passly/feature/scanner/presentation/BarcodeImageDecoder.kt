package com.aozijx.passly.feature.scanner.presentation

import android.content.Context
import androidx.core.net.toUri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

sealed interface BarcodeImageResult {
    data class Decoded(val rawValue: String) : BarcodeImageResult
    data object NoBarcode : BarcodeImageResult
    data class Failed(val error: Throwable) : BarcodeImageResult
}

class BarcodeImageDecoder @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun decode(ref: ImageRef): BarcodeImageResult = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, ref.value.toUri())
            currentCoroutineContext().ensureActive()
            suspendCancellableCoroutine<BarcodeImageResult> { continuation ->
                val scanner = BarcodeScanning.getClient()
                try {
                    scanner.process(image).addOnCompleteListener { task ->
                        // Native processing can outlive cancellation; always release its client.
                        scanner.close()
                        if (!continuation.isActive) return@addOnCompleteListener
                        val result = if (task.isSuccessful) {
                            task.result.firstNotNullOfOrNull { barcode ->
                                barcode.rawValue?.takeIf(String::isNotBlank)
                            }?.let(BarcodeImageResult::Decoded) ?: BarcodeImageResult.NoBarcode
                        } else {
                            BarcodeImageResult.Failed(task.exception ?: IllegalStateException("Barcode decoding failed"))
                        }
                        continuation.resume(result)
                    }
                } catch (error: Exception) {
                    scanner.close()
                    if (continuation.isActive) continuation.resume(BarcodeImageResult.Failed(error))
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            BarcodeImageResult.Failed(error)
        }
    }
}
