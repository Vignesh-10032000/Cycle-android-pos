package dev.vignesh.cycleshop.ui.invoice

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity
import dev.vignesh.cycleshop.data.local.entity.SupplierProductMappingEntity
import dev.vignesh.cycleshop.data.local.model.InventoryUnit
import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus
import dev.vignesh.cycleshop.data.repository.PurchaseRepository
import java.io.File
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
     * [parsedInvoice] contains the structured extracted table and Room catalog matches.
     */
    data class Success(
        val imageUri: Uri,
        val result: OcrResult,
        val parsedInvoice: ParsedInvoice? = null
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

                // Step 3: Match extracted lines against local Room catalog
                val matchedInvoice = try {
                    val app = context.applicationContext as? dev.vignesh.cycleshop.VglApplication
                    val db = app?.database
                    if (db != null) {
                        val products = db.productDao().getActiveProductsWithVariants().first()
                        val brands = db.productDao().getAllBrands().first()
                        ProductMatchingEngine().matchInvoice(parsed, products, brands)
                    } else {
                        parsed
                    }
                } catch (_: Exception) {
                    parsed
                }

                _state.value = InvoiceScanState.Success(
                    imageUri = imageUri,
                    result = finalOcrResult,
                    parsedInvoice = matchedInvoice
                )
            } catch (e: Exception) {
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

    /**
     * Updates the current parsed invoice with user manual corrections.
     */
    fun updateParsedInvoice(updated: ParsedInvoice) {
        val curr = _state.value
        if (curr is InvoiceScanState.Success) {
            _state.value = curr.copy(parsedInvoice = updated)
        }
    }

    /**
     * Safely deletes the current temporary invoice capture image and resets state.
     */
    fun deleteCurrentImage(context: Context) {
        val curr = _state.value
        val uri = when (curr) {
            is InvoiceScanState.Success -> curr.imageUri
            is InvoiceScanState.Error -> curr.imageUri
            else -> null
        }
        InvoiceStorageManager.deleteInvoiceImage(context, uri)
        _state.value = InvoiceScanState.Idle
    }

    /**
     * Stages the parsed and reviewed invoice in Room database as CONFIRMED_STAGED.
     * Strict boundary: NO stock movements or inventory ledger updates are made in this step.
     */
    fun stageInvoiceToDatabase(
        context: Context,
        invoice: ParsedInvoice,
        imageUri: Uri?,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val app = context.applicationContext as? dev.vignesh.cycleshop.VglApplication
                val purchaseRepo = app?.purchaseRepository
                    ?: dev.vignesh.cycleshop.data.repository.PurchaseRepository(
                        dev.vignesh.cycleshop.data.local.VglDatabase.getInstance(context).purchaseDao()
                    )

                // 1. Validation check
                // v2 has no variant stock-unit field. The current staging target is provisional.
                val stagingUnit = InventoryUnit.PIECE
                val conversions = invoice.items.map { UnitConversionEngine.convert(it.unit, it.quantity, stagingUnit) }
                val lineValidationResults = invoice.items.mapIndexed { index, item ->
                    val variantId = item.selectedVariantId ?: item.bestCandidate?.variantId
                    PurchaseValidationEngine.validateLine(
                        lineNumber = item.lineNumber,
                        description = item.description,
                        variantId = variantId,
                        quantity = item.quantity,
                        unitRatePaise = item.unitRatePaise,
                        qualityFlags = item.qualityFlags,
                        lineAmountPaise = item.lineAmountPaise,
                        conversion = conversions[index]
                    )
                }

                val validationSummary = PurchaseValidationEngine.validateInvoice(
                    supplierName = invoice.supplierName,
                    invoiceNumber = invoice.invoiceNumber,
                    lineResults = lineValidationResults
                )

                if (!validationSummary.canStage) {
                    val issuesJoined = (validationSummary.globalIssues +
                        lineValidationResults.filterNot { it.isValid }.flatMap { it.issues }).joinToString("\n")
                    onResult(Result.failure(IllegalArgumentException("Cannot stage invoice: $issuesJoined")))
                    return@launch
                }

                // 2. Check duplicate
                val duplicateCheck = purchaseRepo.checkDuplicate(
                    supplierName = invoice.supplierName ?: "Unknown Supplier",
                    supplierGstin = invoice.supplierGstin,
                    invoiceNumber = invoice.invoiceNumber ?: "UNKNOWN",
                    supportingDocRef = null
                )

                if (duplicateCheck is DeduplicationResult.ExactDuplicate) {
                    onResult(Result.failure(IllegalStateException(duplicateCheck.message)))
                    return@launch
                }

                // 3. Build PurchaseInvoiceEntity
                val invoiceEntity = PurchaseInvoiceEntity(
                    supplierName = invoice.supplierName ?: "Unknown Supplier",
                    supplierGstin = invoice.supplierGstin,
                    invoiceNumber = invoice.invoiceNumber ?: "UNKNOWN",
                    invoiceDate = invoice.invoiceDate,
                    consigneeName = invoice.consigneeName,
                    consigneeGstin = invoice.consigneeGstin,
                    totalAmountPaise = invoice.totalAmountExtractedPaise,
                    totalQuantity = invoice.totalQuantityExtracted,
                    imageUri = imageUri?.toString(),
                    rawOcrText = invoice.rawText,
                    status = PurchaseInvoiceStatus.CONFIRMED_STAGED,
                    supportingDocRef = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // 4. Build PurchaseLineEntity list
                val lineEntities = invoice.items.mapIndexed { idx, item ->
                    val lineValidation = lineValidationResults[idx]
                    val variantId = item.selectedVariantId ?: item.bestCandidate?.variantId
                    val conversion = conversions[idx] as? UnitConversionResult.Success

                    PurchaseLineEntity(
                        invoiceId = 0, // Assigned inside DAO transaction
                        lineNumber = item.lineNumber,
                        variantId = variantId,
                        supplierItemCode = item.itemCode,
                        rawDescription = item.description,
                        correctedDescription = item.description,
                        hsnCode = item.hsnCode,
                        rawQuantity = requireNotNull(item.quantity),
                        correctedQuantity = item.quantity,
                        rawUnit = item.unit.orEmpty(),
                        inventoryUnit = conversion?.targetUnit ?: stagingUnit,
                        // v2 requires integers. Zero records an unresolved conversion in a staged row.
                        unitConversionFactor = conversion?.conversionFactor ?: 0,
                        effectiveQuantityBase = conversion?.effectiveBaseQuantity ?: 0,
                        rawUnitPricePaise = item.unitRatePaise,
                        correctedUnitPricePaise = item.unitRatePaise,
                        rawLineAmountPaise = item.lineAmountPaise,
                        correctedLineAmountPaise = item.lineAmountPaise,
                        gstRatePercent = item.gstRatePercent,
                        reviewStatus = lineValidation.reviewStatus,
                        rejectionReason = if (lineValidation.issues.isNotEmpty()) lineValidation.issues.joinToString(", ") else null,
                        createdAt = System.currentTimeMillis()
                    )
                }

                // 5. Build verified supplier mappings for items that have verified variantId
                val verifiedMappings = invoice.items.mapNotNull { item ->
                    val variantId = item.selectedVariantId ?: item.bestCandidate?.takeIf { it.confidenceScore >= 0.75f }?.variantId
                    if (variantId != null) {
                        SupplierProductMappingEntity(
                            supplierIdentifier = invoice.supplierGstin ?: invoice.supplierName ?: "UNKNOWN",
                            supplierItemCode = item.itemCode,
                            supplierDescPattern = item.description.trim(),
                            variantId = variantId,
                            verifiedByOwner = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else null
                }

                // 6. Stage to database
                val stagedResult = purchaseRepo.stageInvoice(
                    invoice = invoiceEntity,
                    lines = lineEntities,
                    verifiedMappings = verifiedMappings
                )

                onResult(stagedResult)
            } catch (e: Exception) {
                onResult(Result.failure(e))
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
