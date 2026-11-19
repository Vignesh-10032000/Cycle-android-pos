package dev.vignesh.cycleshop.recognition

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.max

internal sealed interface RecognitionPhotoSource {
    data class Capture(val file: File) : RecognitionPhotoSource
    data class Gallery(val uri: Uri) : RecognitionPhotoSource
}

internal data class PreparedRecognitionPhoto(val file: File, val width: Int, val height: Int)

internal class PhotoPreparationException(message: String) : IOException(message)

internal class RecognitionPhotoPreparer(
    private val resolver: ContentResolver,
    private val store: RecognitionPhotoStore
) {
    suspend fun prepare(source: RecognitionPhotoSource): PreparedRecognitionPhoto {
        var imported: File? = null
        var output: File? = null
        var delivered = false
        try {
            val prepared = withContext(Dispatchers.IO) {
                // A canceled native decode can finish asynchronously with respect to UI state.
                // Serialize decodes so replacing photos cannot stack their bitmap allocations.
                decodeLock.withLock {
                    val input = when (source) {
                        is RecognitionPhotoSource.Capture -> source.file
                        is RecognitionPhotoSource.Gallery -> {
                            val local = store.createImport().also { imported = it }
                            val stream = resolver.openInputStream(source.uri)
                                ?: throw PhotoPreparationException("Could not open the selected photo.")
                            stream.use { copyBounded(it, local) }
                            local
                        }
                    }
                    if (input.length() > maxSourceBytes) throw oversized()
                    val jpeg = store.createPrepared().also { output = it }
                    normalize(input, jpeg)
                }
            }
            delivered = true
            return prepared
        } finally {
            imported?.let(store::delete)
            if (!delivered) output?.let(store::delete)
        }
    }

    private suspend fun copyBounded(stream: InputStream, file: File) {
        file.outputStream().use { destination ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = stream.read(buffer)
                if (count == -1) break
                total += count
                if (total > maxSourceBytes) throw oversized()
                destination.write(buffer, 0, count)
            }
        }
    }

    private suspend fun normalize(input: File, output: File): PreparedRecognitionPhoto {
        currentCoroutineContext().ensureActive()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(input.path, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) {
            throw PhotoPreparationException("This photo could not be decoded. Try a different image.")
        }
        if (max(width, height) > maxSourceEdge || width.toLong() * height > maxSourcePixels) throw oversized()
        val orientation = try {
            ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        currentCoroutineContext().ensureActive()
        val options = BitmapFactory.Options().apply {
            inSampleSize = recognitionDecodeSample(max(width, height))
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(input.path, options)
            ?: throw PhotoPreparationException("This photo could not be decoded. Try a different image.")
        try {
            currentCoroutineContext().ensureActive()
            val transform = orientationMatrix(orientation)
            val scale = minOf(1f, maxOutputEdge.toFloat() / max(decoded.width, decoded.height))
            transform.postScale(scale, scale)
            val normalized = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, transform, true)
            try {
                for (quality in listOf(88, 76, 64)) {
                    currentCoroutineContext().ensureActive()
                    // A new JPEG encoded from pixels carries none of the source's EXIF/GPS metadata.
                    val written = output.outputStream().buffered().use {
                        normalized.compress(Bitmap.CompressFormat.JPEG, quality, it)
                    }
                    if (!written) throw PhotoPreparationException("Could not save the prepared photo.")
                    if (output.length() in 1..maxOutputBytes) {
                        currentCoroutineContext().ensureActive()
                        return PreparedRecognitionPhoto(output, normalized.width, normalized.height)
                    }
                }
                throw PhotoPreparationException("Could not reduce the image size. Try a smaller photo.")
            } finally {
                if (normalized !== decoded) normalized.recycle()
            }
        } finally {
            decoded.recycle()
        }
    }

    private fun oversized() = PhotoPreparationException("The photo is too large. Choose a smaller image.")

    private companion object {
        val decodeLock = Mutex()
        const val maxSourceBytes = 32L * 1024 * 1024
        const val maxSourceEdge = 20_000
        const val maxSourcePixels = 100_000_000L
        const val maxOutputEdge = 1600
        const val maxOutputBytes = 1536L * 1024
    }
}

internal fun recognitionDecodeSample(longEdge: Int): Int {
    require(longEdge > 0)
    var sample = 1
    while ((longEdge.toLong() + sample - 1) / sample > 2048) sample *= 2
    return sample
}

private fun orientationMatrix(orientation: Int) = Matrix().apply {
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            setRotate(90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            setRotate(270f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
    }
}
