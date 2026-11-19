package dev.vignesh.cycleshop.ui.invoice

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dev.vignesh.cycleshop.VglApplication
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.measureTimeMillis

/**
 * State machine for the invoice scanning screen.
 *
 * Transitions:
 *   Idle → PermissionDenied         (user denies CAMERA permission)
 *   Idle → Previewing               (permission granted, camera preview live)
 *   Idle → Processing               (image imported from Photo Picker)
 *   Previewing → CapturePending     (shutter pressed, ImageCapture.takePicture in flight)
 *   CapturePending → Processing     (file written, ML Kit launched)
 *   Processing → Success            (ML Kit returned Text)
 *   Processing → Error              (exception from ML Kit or IO)
 *   Success → Idle                  (Retake/Reset pressed)
 *   Error → Idle                    (Retry pressed)
 */
sealed class InvoiceScanState {
    /** No image, awaiting user action. */
    object Idle : InvoiceScanState()

    /** User denied camera permission; show rationale. */
    object PermissionDenied : InvoiceScanState()

    /**
     * Camera preview is active. The shutter button is enabled.
     * [captureFilePath] is a pre-allocated file path ready to receive the JPEG.
     */
    data class Previewing(val captureFilePath: String) : InvoiceScanState()

    /** Image capture request is in flight (waiting for CameraX callback). */
    object CapturePending : InvoiceScanState()

    /**
     * ML Kit OCR is running on [imageUri].
     * The UI shows a full-screen loading indicator with an estimated progress.
     */
    data class Processing(val imageUri: Uri) : InvoiceScanState()

    /**
     * OCR completed successfully.
     * [imageUri] is the app-private file URI for the preview.
     * [result] contains the full recognition output.
     * [review] contains the extracted evidence and current owner review.
     */
    internal data class Success(
        val imageUri: Uri,
        val result: OcrResult,
        val review: InvoiceReviewSession,
        val catalog: List<ReviewVariant>,
        val saving: Boolean = false,
        val stagedInvoiceId: Long? = null
    ) : InvoiceScanState()

    /**
     * Something went wrong.
     * [imageUri] may be non-null if capture succeeded but OCR failed.
     * [message] is a user-facing description — never contains raw supplier data.
     */
    data class Error(
        val message: String,
        val imageUri: Uri? = null
    ) : InvoiceScanState()
}

/**
 * ViewModel managing CameraX capture, Photo Picker import, and offline ML Kit OCR
 * for the VGL Cycle Shop invoice scanner.
 *
 * ### CameraX Lifecycle
 * CameraX exposes a [ProcessCameraProvider] that is bound to the Activity/Fragment
 * [LifecycleOwner]. The [PreviewView] in [InvoiceScanScreen] creates and binds the
 * [Preview] and [ImageCapture] use-cases via [CameraProvider.bindToLifecycle].
 * When the owner transitions to DESTROYED, CameraX automatically unbinds all
 * use-cases and releases the sensor — no manual cleanup needed in this ViewModel.
 *
 * ### ML Kit InputImage
 * [InputImage.fromFilePath] reads the file off the main thread and respects EXIF
 * rotation, making it the safest factory for captured JPEG files.
 * [InputImage.fromBitmap] requires the caller to handle rotation manually.
 * We use [InputImage.fromMediaUri] for Photo Picker URIs (content://) because
 * CameraX images are saved to private files and photo picker returns content URIs.
 *
 * ### Image Rotation
 * Android sensors mount cameras at varying physical angles. CameraX normalises
 * rotation through [ImageCapture.Builder.setTargetRotation], which tags the saved
 * JPEG with the correct EXIF orientation. ML Kit reads EXIF automatically when
 * the InputImage is created with [InputImage.fromFilePath] or [fromMediaUri].
 * For bitmaps we read EXIF manually and rotate before passing to ML Kit.
 *
 * ### Error Handling
 * - IO failures (file unreadable, storage full) → [InvoiceScanState.Error]
 * - ML Kit failures (model not ready, out of memory) → [InvoiceScanState.Error]
 * - Zero recognized characters → [InvoiceScanState.Success] with empty [OcrResult.rawText]
 *   (the UI shows a "No text found" message in this case)
 * - Supplier text is NEVER written to Logcat/crash logs.
 */
class InvoiceScanViewModel : ViewModel() {

    private val _state = MutableStateFlow<InvoiceScanState>(InvoiceScanState.Idle)
    val state: StateFlow<InvoiceScanState> = _state.asStateFlow()

    // ── Camera setup ──────────────────────────────────────────────────────────

    /**
     * Called by the UI once the CAMERA permission is granted and a capture
     * output file has been allocated. Transitions to [InvoiceScanState.Previewing].
     *
     * @param captureFilePath Absolute path of the pre-created JPEG file that
     *                        CameraX will write upon capture.
     */
    fun onCameraReady(captureFilePath: String) {
        _state.value = InvoiceScanState.Previewing(captureFilePath)
    }

    /** Transitions to [InvoiceScanState.PermissionDenied]. */
    fun onPermissionDenied() {
        _state.value = InvoiceScanState.PermissionDenied
    }

    // ── Capture / Import ──────────────────────────────────────────────────────

    /**
     * Called when the CameraX capture to [captureFileUri] has completed.
     * Immediately launches OCR on the written file.
     */
    fun onCaptureSuccess(context: Context, captureFileUri: Uri) {
        _state.value = InvoiceScanState.Processing(captureFileUri)
        runOcr(context, captureFileUri)
    }

    /** Called when CameraX fails to write the capture file. */
    fun onCaptureFailed(reason: String) {
        // reason is a generic error class name — never raw supplier content
        _state.value = InvoiceScanState.Error("Capture failed: $reason")
    }

    /**
     * Called when the user imports an existing image via Android Photo Picker.
     * The returned [uri] is a content:// URI with read-only access granted
     * for the duration of this call.
     */
    fun onPhotoPickerResult(context: Context, uri: Uri) {
        _state.value = InvoiceScanState.Processing(uri)
        runOcr(context, uri)
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    /** User pressed Retake / Reset. Returns to [InvoiceScanState.Idle]. */
    fun reset() {
        _state.value = InvoiceScanState.Idle
    }

    // ── OCR engine ────────────────────────────────────────────────────────────

    /**
     * Runs ML Kit Latin text recognition on [imageUri] in the ViewModel scope
     * (IO-safe; ML Kit tasks marshal themselves onto a background thread).
     *
     * ### Processing pipeline
     * 1. Create [InputImage] from URI (ML Kit reads EXIF rotation automatically).
     * 2. Obtain a [TextRecognizer] with [TextRecognizerOptions.DEFAULT_OPTIONS]
     *    (Latin script, offline model bundled in APK via mlkit-text-recognition).
     * 3. Call [TextRecognizer.process] and suspend via [Task.await].
     * 4. Map ML Kit's [Text] → [OcrResult] preserving block/line/element hierarchy.
     * 5. Post [InvoiceScanState.Success] or [InvoiceScanState.Error].
     * 6. Close the recognizer to release its native resources.
     *
     * ### Why TextRecognizerOptions.DEFAULT_OPTIONS?
     * The default options select the Latin-script model which is bundled directly
     * in the APK. No Play Services model download is required, making this fully
     * offline on first launch — critical for a shop with unreliable connectivity.
     */
    private fun runOcr(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                val image = InputImage.fromFilePath(context, imageUri)
                val imageWidthPx = image.width
                val imageHeightPx = image.height
                val rotationDegrees = image.rotationDegrees

                val ocrResult: OcrResult
                val processingMs = measureTimeMillis {
                    val mlKitText = recognizer.process(image).await()

                    ocrResult = mlKitText.toInvoiceOcrResult(imageWidthPx, imageHeightPx, rotationDegrees)
                }

                val finalOcrResult = ocrResult.copy(processingMs = processingMs)

                // Step 2: Structured rule-based table parsing
                val tableParser = InvoiceTableParser()
                val parsed = tableParser.parse(finalOcrResult)

                val app = context.applicationContext as VglApplication
                val products = app.database.productDao().getActiveProductsWithVariants().first()
                val brands = app.database.productDao().getAllBrands().first()
                val matchedInvoice = ProductMatchingEngine().matchInvoice(parsed, products, brands)
                val catalog = products.flatMap { product ->
                    product.variants.map { ReviewVariant(it.id, product.product.name, it.sku) }
                }
                if (_state.value == InvoiceScanState.Processing(imageUri)) {
                    _state.value = InvoiceScanState.Success(
                        imageUri, finalOcrResult.immutableSnapshot(), InvoiceReviewSession.from(matchedInvoice), catalog
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_state.value != InvoiceScanState.Processing(imageUri)) return@launch
                // Map exception type to a user-safe message.
                // We deliberately do NOT log e.message, as it may contain file
                // path segments that could reveal invoice storage structure.
                val userMessage = when {
                    e is OutOfMemoryError -> "Image too large to process. Try a smaller resolution."
                    e.javaClass.simpleName.contains("IOException") ->
                        "Could not read the image file. Please retry."
                    e.javaClass.simpleName.contains("MlKit") ->
                        "OCR engine error. Try retaking the photo."
                    else -> "OCR failed. Please retry with a clearer image."
                }
                _state.value = InvoiceScanState.Error(
                    message = userMessage,
                    imageUri = imageUri
                )
            } finally {
                recognizer.close()
            }
        }
    }

    internal fun editLine(index: Int, change: (ReviewLine) -> ReviewLine) =
        editReview { it.updateLine(index, change) }

    internal fun editReview(change: (InvoiceReviewSession) -> InvoiceReviewSession) {
        val current = _state.value as? InvoiceScanState.Success ?: return
        if (current.saving || current.stagedInvoiceId != null) return
        _state.value = current.copy(review = change(current.review))
    }

    internal fun verifyLine(context: Context, index: Int, onError: (String) -> Unit) {
        val current = _state.value as? InvoiceScanState.Success ?: return
        val selected = current.review.lines.getOrNull(index)?.values?.variantId ?: return
        runReviewCheck(context, current, setOf(selected), onError) { active ->
            current.review.updateLine(index) { it.verify(active, System.currentTimeMillis()) }
        }
    }

    internal fun approveReview(context: Context, onError: (String) -> Unit) {
        val current = _state.value as? InvoiceScanState.Success ?: return
        runReviewCheck(context, current, current.review.selectedVariants(), onError) { active ->
            current.review.approve(active, System.currentTimeMillis())
        }
    }

    private fun runReviewCheck(
        context: Context,
        current: InvoiceScanState.Success,
        variants: Set<Long>,
        onError: (String) -> Unit,
        action: (Set<Long>) -> InvoiceReviewSession
    ) {
        if (current.saving || current.stagedInvoiceId != null) return
        viewModelScope.launch {
            try {
                val app = context.applicationContext as VglApplication
                val active = app.database.withTransaction {
                    app.database.productDao().activeReviewVariantIds(variants)
                }
                if (_state.value !== current) return@launch
                _state.value = current.copy(
                    review = action(active),
                    catalog = current.catalog.filter { it.id !in variants || it.id in active }
                )
                if (active != variants) onError("A selected catalog variant is no longer active.")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onError("Could not check the current catalog. Try again.")
            }
        }
    }

    fun deleteCurrentImage(context: Context) {
        val current = _state.value
        val uri = when (current) {
            is InvoiceScanState.Success -> {
                if (current.saving || current.stagedInvoiceId != null) return
                current.imageUri
            }
            is InvoiceScanState.Error -> current.imageUri
            else -> null
        }
        InvoiceStorageManager.deleteInvoiceImage(context, uri)
        _state.value = InvoiceScanState.Idle
    }

    internal fun stageInvoiceToDatabase(context: Context, onResult: (Result<Long>) -> Unit) {
        val current = _state.value as? InvoiceScanState.Success ?: return
        if (current.saving || current.stagedInvoiceId != null) return
        val saving = current.copy(saving = true)
        _state.value = saving
        viewModelScope.launch {
            var stageId: Long? = null
            try {
                val app = context.applicationContext as VglApplication
                stageId = stageReviewedInvoiceV2(
                    app.database, app.purchaseRepository, current.review, current.imageUri.toString()
                )
                onResult(Result.success(stageId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(Result.failure(e))
            } finally {
                if (_state.value === saving) {
                    _state.value = current.copy(stagedInvoiceId = stageId)
                }
            }
        }
    }

    /**
     * Allocates a new private JPEG file for CameraX capture output via [InvoiceStorageManager].
     */
    fun allocateCaptureFile(context: Context): File {
        return InvoiceStorageManager.allocateInvoiceFile(context)
    }
}

/** Simple factory; ViewModel has no constructor parameters. */
class InvoiceScanViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InvoiceScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InvoiceScanViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
