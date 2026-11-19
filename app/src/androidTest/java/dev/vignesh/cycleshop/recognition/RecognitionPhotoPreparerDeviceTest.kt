package dev.vignesh.cycleshop.recognition

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.vignesh.cycleshop.ui.recognition.RecognitionPhotoState
import dev.vignesh.cycleshop.ui.recognition.RecognitionPhotoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile

@RunWith(AndroidJUnit4::class)
class RecognitionPhotoPreparerDeviceTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val files = RecognitionPhotoStore(app.cacheDir)
    private val preparer = RecognitionPhotoPreparer(app.contentResolver, files)

    @Test
    fun normalizesAllExifOrientationsAndStripsSourceMetadata(): Unit = runBlocking {
        // Corner IDs in top-left, top-right, bottom-left, bottom-right order.
        val corners = listOf(
            listOf(0, 1, 2, 3), listOf(1, 0, 3, 2), listOf(3, 2, 1, 0), listOf(2, 3, 0, 1),
            listOf(0, 2, 1, 3), listOf(2, 0, 3, 1), listOf(3, 1, 2, 0), listOf(1, 3, 0, 2)
        )
        for (orientation in 1..8) {
            val source = files.createCapture()
            try {
                writeQuadrants(source)
                ExifInterface(source).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    setAttribute(ExifInterface.TAG_MAKE, "Private device name")
                    setAttribute(ExifInterface.TAG_GPS_LATITUDE, "11/1,0/1,0/1")
                    setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                    saveAttributes()
                }
                assertEquals("N", ExifInterface(source).getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
                val ready = preparer.prepare(RecognitionPhotoSource.Capture(source))
                try {
                    assertEquals(if (orientation >= 5) 80 else 120, ready.width)
                    assertEquals(if (orientation >= 5) 120 else 80, ready.height)
                    val bitmap = BitmapFactory.decodeFile(ready.file.path)
                    try {
                        assertEquals("Orientation $orientation", corners[orientation - 1], readCorners(bitmap))
                    } finally {
                        bitmap.recycle()
                    }
                    val exif = ExifInterface(ready.file)
                    // The framework reader synthesizes undefined orientation for plain JPEGs.
                    assertEquals(
                        ExifInterface.ORIENTATION_UNDEFINED,
                        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                    )
                    assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
                    assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
                    assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
                } finally {
                    files.delete(ready.file)
                }
            } finally {
                files.delete(source)
            }
        }
    }

    @Test
    fun preparesLargePhotoWithinOutputBoundsAndRejectsOversizedInput(): Unit = runBlocking {
        val source = files.createCapture()
        try {
            val large = Bitmap.createBitmap(4500, 3000, Bitmap.Config.RGB_565)
            try {
                large.eraseColor(Color.BLUE)
                source.outputStream().use { assertTrue(large.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            } finally {
                large.recycle()
            }
            val ready = preparer.prepare(RecognitionPhotoSource.Capture(source))
            try {
                assertEquals(1125, ready.width)
                assertEquals(750, ready.height)
                assertTrue(ready.file.length() in 1..(1536L * 1024))
            } finally {
                files.delete(ready.file)
            }
            RandomAccessFile(source, "rw").use { it.setLength(33L * 1024 * 1024) }
            val failure = runCatching { preparer.prepare(RecognitionPhotoSource.Capture(source)) }.exceptionOrNull()
            assertTrue(failure is PhotoPreparationException)
            assertTrue(failure?.message?.contains("too large") == true)
        } finally {
            files.delete(source)
        }
    }

    @Test
    fun changingPhotoAndRemovingItCleansTheOwnedFiles(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val owner = ViewModelStore()
        val model = RecognitionPhotoViewModel(app)
        owner.put("recognition", model)
        val first = files.createCapture()
        val second = files.createCapture()
        writeQuadrants(second)
        try {
            instrumentation.runOnMainSync {
                model.onCameraPermission(true)
                model.onCapture(first)
                model.onCameraPermission(true)
                model.onCapture(second)
            }
            val result = withTimeout(5_000) {
                model.state.first { it is RecognitionPhotoState.Ready } as RecognitionPhotoState.Ready
            }
            assertFalse(first.exists())
            assertFalse(second.exists())
            assertTrue(result.photo.file.exists())
            instrumentation.runOnMainSync { model.remove() }
            assertEquals(RecognitionPhotoState.Empty, model.state.value)
            assertFalse(result.photo.file.exists())
        } finally {
            instrumentation.runOnMainSync { owner.clear() }
            files.delete(first)
            files.delete(second)
        }
    }

    @Test
    fun aFailedCaptureCanBeRetriedAndClearedWhenTheSessionEnds(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val owner = ViewModelStore()
        val model = RecognitionPhotoViewModel(app)
        owner.put("recognition", model)
        val file = files.createCapture()
        try {
            instrumentation.runOnMainSync {
                model.onCameraPermission(true)
                model.onCapture(file)
            }
            val error = withTimeout(5_000) {
                model.state.first { it is RecognitionPhotoState.Error } as RecognitionPhotoState.Error
            }
            assertTrue(error.canRetryPhoto)
            assertTrue(file.exists())

            writeQuadrants(file)
            instrumentation.runOnMainSync { model.retryPhoto() }
            val ready = withTimeout(5_000) {
                model.state.first { it is RecognitionPhotoState.Ready } as RecognitionPhotoState.Ready
            }
            assertFalse(file.exists())
            instrumentation.runOnMainSync { owner.clear() }
            assertFalse(ready.photo.file.exists())
        } finally {
            instrumentation.runOnMainSync { owner.clear() }
            files.delete(file)
        }
    }

    private fun writeQuadrants(file: File) {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        try {
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    bitmap.setPixel(x, y, colours[(if (y < 40) 0 else 2) + if (x < 60) 0 else 1])
                }
            }
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun readCorners(bitmap: Bitmap) = listOf(
        bitmap.getPixel(10, 10), bitmap.getPixel(bitmap.width - 11, 10),
        bitmap.getPixel(10, bitmap.height - 11), bitmap.getPixel(bitmap.width - 11, bitmap.height - 11)
    ).map { pixel ->
        colours.indices.minBy { index ->
            val colour = colours[index]
            square(Color.red(pixel) - Color.red(colour)) +
                square(Color.green(pixel) - Color.green(colour)) +
                square(Color.blue(pixel) - Color.blue(colour))
        }
    }

    private fun square(value: Int) = value * value
    private val colours = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
}
