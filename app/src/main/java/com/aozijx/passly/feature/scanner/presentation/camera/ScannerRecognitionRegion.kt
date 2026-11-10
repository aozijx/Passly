package com.aozijx.passly.feature.scanner.presentation.camera

import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import com.google.mlkit.vision.barcode.common.Barcode

internal data class ScannerRecognitionRegion(
    private val boundsInPreview: RectF,
    private val imageToPreview: Matrix,
) {
    fun contains(barcode: Barcode): Boolean {
        val corners = barcode.cornerPoints
        if (!corners.isNullOrEmpty()) {
            if (corners.size != BARCODE_CORNER_COUNT) return false
            val points = FloatArray(corners.size * POINT_COORDINATE_COUNT)
            corners.forEachIndexed { index, point ->
                val coordinateIndex = index * POINT_COORDINATE_COUNT
                points[coordinateIndex] = point.x.toFloat()
                points[coordinateIndex + 1] = point.y.toFloat()
            }
            return containsMappedPoints(points)
        }

        val bounds = barcode.boundingBox ?: return false
        return containsMappedPoints(
            floatArrayOf(
                bounds.left.toFloat(), bounds.top.toFloat(),
                bounds.right.toFloat(), bounds.top.toFloat(),
                bounds.right.toFloat(), bounds.bottom.toFloat(),
                bounds.left.toFloat(), bounds.bottom.toFloat(),
            ),
        )
    }

    private fun containsMappedPoints(points: FloatArray): Boolean {
        imageToPreview.mapPoints(points)
        return points.indices.step(POINT_COORDINATE_COUNT).all { index ->
            boundsInPreview.contains(points[index], points[index + 1])
        }
    }

    companion object {
        private const val BARCODE_CORNER_COUNT = 4
        private const val POINT_COORDINATE_COUNT = 2

        @OptIn(markerClass = [TransformExperimental::class])
        fun create(
            imageProxy: ImageProxy,
            previewView: PreviewView,
            viewfinderBounds: RectF,
        ): ScannerRecognitionRegion? {
            val previewOutputTransform = previewView.outputTransform ?: return null
            val imageOutputTransform =
                ImageProxyTransformFactory()
                    .apply {
                        // ML Kit returns barcode coordinates after rotationDegrees is applied.
                        setUsingRotationDegrees(true)
                    }
                    .getOutputTransform(imageProxy)
            val coordinateTransform =
                CoordinateTransform(imageOutputTransform, previewOutputTransform)
            val imageToPreview = Matrix().also(coordinateTransform::transform)
            return ScannerRecognitionRegion(RectF(viewfinderBounds), imageToPreview)
        }
    }
}
