package com.aozijx.passly.feature.scanner.presentation

import android.graphics.Matrix
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.common.Barcode

internal data class ScannerRecognitionRegion(
    private val boundsInImage: RectF,
) {
    fun contains(barcode: Barcode): Boolean {
        val corners = barcode.cornerPoints
        if (!corners.isNullOrEmpty()) {
            return corners.size == BARCODE_CORNER_COUNT && corners.all { corner ->
                boundsInImage.contains(corner.x.toFloat(), corner.y.toFloat())
            }
        }
        val bounds = barcode.boundingBox ?: return false
        return boundsInImage.contains(bounds.left.toFloat(), bounds.top.toFloat()) &&
            boundsInImage.contains(bounds.right.toFloat(), bounds.top.toFloat()) &&
            boundsInImage.contains(bounds.right.toFloat(), bounds.bottom.toFloat()) &&
            boundsInImage.contains(bounds.left.toFloat(), bounds.bottom.toFloat())
    }

    companion object {
        private const val BARCODE_CORNER_COUNT = 4

        fun create(
            imageProxy: ImageProxy,
            previewView: PreviewView,
            viewfinderBounds: RectF,
        ): ScannerRecognitionRegion? {
            if (previewView.width <= 0 || previewView.height <= 0) return null
            val previewToImage = Matrix()
            val imageToPreview = imageToPreviewTransform(imageProxy, previewView) ?: return null
            if (!imageToPreview.invert(previewToImage)) return null

            val boundsInImage = RectF(viewfinderBounds)
            previewToImage.mapRect(boundsInImage)
            return ScannerRecognitionRegion(boundsInImage)
        }

        private fun imageToPreviewTransform(
            imageProxy: ImageProxy,
            previewView: PreviewView,
        ): Matrix? {
            val cropRect = imageProxy.cropRect
            val source = floatArrayOf(
                cropRect.left.toFloat(), cropRect.top.toFloat(),
                cropRect.right.toFloat(), cropRect.top.toFloat(),
                cropRect.right.toFloat(), cropRect.bottom.toFloat(),
                cropRect.left.toFloat(), cropRect.bottom.toFloat(),
            )
            val destination = floatArrayOf(
                0f, 0f,
                previewView.width.toFloat(), 0f,
                previewView.width.toFloat(), previewView.height.toFloat(),
                0f, previewView.height.toFloat(),
            )
            val shiftOffset = when (imageProxy.imageInfo.rotationDegrees) {
                0 -> 0
                90 -> 2
                180 -> 4
                270 -> 6
                else -> return null
            }
            val originalDestination = destination.clone()
            for (toIndex in destination.indices) {
                destination[toIndex] = originalDestination[(toIndex + shiftOffset) % destination.size]
            }

            return Matrix().takeIf { matrix -> matrix.setPolyToPoly(source, 0, destination, 0, BARCODE_CORNER_COUNT) }
        }
    }
}
