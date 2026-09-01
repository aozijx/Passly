package com.aozijx.passly.app.entry.favicon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaviconBitmapTransformerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val transformer = FaviconBitmapTransformer()

    @Test
    fun transform_rejectsOversizedBoundsBeforeOutput() {
        val input = pngFile("wide.png", 4097, 1)
        val output = temporaryFolder.newFile("result.webp").apply(File::delete)

        val error = assertThrows(FaviconImageException::class.java) {
            transformer.transform(input, output, crop = null)
        }

        assertEquals(FaviconImageFailure.UNSUPPORTED_DIMENSIONS, error.reason)
        assertTrue(!output.exists())
    }

    @Test
    fun transform_noCropPreservesAspectRatioAndMaxEdge() {
        val input = pngFile("landscape.png", 1600, 800)
        val output = temporaryFolder.newFile("landscape.webp")

        transformer.transform(input, output, crop = null)

        val decoded = BitmapFactory.decodeFile(output.absolutePath)
        assertEquals(1024, decoded.width)
        assertEquals(512, decoded.height)
        decoded.recycle()
    }

    @Test
    fun transform_cropProducesSquareAndPreservesAlpha() {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            setPixel(width / 2, height / 2, Color.RED)
        }
        val input = writeBitmap("alpha.png", bitmap, Bitmap.CompressFormat.PNG)
        val output = temporaryFolder.newFile("alpha.webp")

        transformer.transform(input, output, FaviconCropRequest())

        val decoded = BitmapFactory.decodeFile(output.absolutePath)
        assertEquals(512, decoded.width)
        assertEquals(512, decoded.height)
        assertTrue(decoded.hasAlpha())
        assertEquals(0, Color.alpha(decoded.getPixel(0, 0)))
        decoded.recycle()
    }

    @Test
    fun transform_appliesExifOrientationBeforeScaling() {
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val input = writeBitmap("rotated.jpg", bitmap, Bitmap.CompressFormat.JPEG)
        ExifInterface(input.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val output = temporaryFolder.newFile("rotated.webp")

        transformer.transform(input, output, crop = null)

        val decoded = BitmapFactory.decodeFile(output.absolutePath)
        assertEquals(40, decoded.width)
        assertEquals(80, decoded.height)
        decoded.recycle()
    }

    @Test
    fun transform_animatedGifUsesFirstFrame() {
        val input = temporaryFolder.newFile("animated.gif").apply { writeBytes(twoFrameGif()) }
        val output = temporaryFolder.newFile("animated.webp")

        transformer.transform(input, output, crop = null)

        val decoded = BitmapFactory.decodeFile(output.absolutePath)
        assertEquals(1, decoded.width)
        assertEquals(1, decoded.height)
        assertTrue(Color.red(decoded.getPixel(0, 0)) < 20)
        decoded.recycle()
    }

    private fun pngFile(name: String, width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        return writeBitmap(name, bitmap, Bitmap.CompressFormat.PNG)
    }

    private fun writeBitmap(name: String, bitmap: Bitmap, format: Bitmap.CompressFormat): File {
        val file = temporaryFolder.newFile(name)
        FileOutputStream(file).use { output -> assertTrue(bitmap.compress(format, 100, output)) }
        bitmap.recycle()
        return file
    }

    private fun twoFrameGif(): ByteArray = byteArrayOf(
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
        0x01, 0x00, 0x01, 0x00, 0x80.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0x21, 0xff.toByte(), 0x0b, 0x4e, 0x45, 0x54, 0x53, 0x43, 0x41, 0x50, 0x45, 0x32, 0x2e, 0x30,
        0x03, 0x01, 0x00, 0x00, 0x00,
        0x21, 0xf9.toByte(), 0x04, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
        0x02, 0x02, 0x44, 0x01, 0x00,
        0x21, 0xf9.toByte(), 0x04, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
        0x02, 0x02, 0x4c, 0x01, 0x00,
        0x3b,
    )
}
