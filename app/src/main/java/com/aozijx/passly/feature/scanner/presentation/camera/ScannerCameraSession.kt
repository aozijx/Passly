package com.aozijx.passly.feature.scanner.presentation.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException

internal class ScannerCameraSession {
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var previewView: PreviewView? = null
    private var layoutChangeListener: android.view.View.OnLayoutChangeListener? = null

    fun bind(
        provider: ProcessCameraProvider,
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        preview: Preview,
        imageAnalysis: ImageAnalysis,
        isDisposed: () -> Boolean,
        onFailure: (Throwable) -> Unit,
    ) {
        stop()
        this.provider = provider
        this.preview = preview
        this.previewView = previewView
        this.imageAnalysis = imageAnalysis

        if (!bindWhenReady(provider, previewView, lifecycleOwner, preview, imageAnalysis, isDisposed, onFailure)) {
            val listener =
                android.view.View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    if (bindWhenReady(provider, previewView, lifecycleOwner, preview, imageAnalysis, isDisposed, onFailure)) {
                        removeLayoutListener()
                    }
                }
            layoutChangeListener = listener
            previewView.addOnLayoutChangeListener(listener)
        }
    }

    fun stop() {
        val boundProvider = provider
        val boundPreview = preview
        val boundImageAnalysis = imageAnalysis
        boundImageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        removeLayoutListener()
        previewView = null
        provider = null
        preview = null
        if (boundProvider != null && boundPreview != null && boundImageAnalysis != null) {
            boundProvider.unbind(boundPreview, boundImageAnalysis)
        }
    }

    private fun bindWhenReady(
        provider: ProcessCameraProvider,
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        preview: Preview,
        imageAnalysis: ImageAnalysis,
        isDisposed: () -> Boolean,
        onFailure: (Throwable) -> Unit,
    ): Boolean {
        val viewPort = previewView.viewPort ?: return false
        if (previewView.width <= 0 || previewView.height <= 0 || isDisposed()) return false
        return try {
            val useCaseGroup =
                UseCaseGroup
                    .Builder()
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .setViewPort(viewPort)
                    .build()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (!isDisposed()) onFailure(error)
            true
        }
    }

    private fun removeLayoutListener() {
        layoutChangeListener?.let { listener -> previewView?.removeOnLayoutChangeListener(listener) }
        layoutChangeListener = null
    }
}
