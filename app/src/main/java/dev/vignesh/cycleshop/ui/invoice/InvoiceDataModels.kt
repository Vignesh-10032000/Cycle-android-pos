package dev.vignesh.cycleshop.ui.invoice

/**
 * Quality status of an extracted invoice field.
 */
enum class FieldQuality {
    EXACT,       // Confidently extracted with matching checksum/regex/reconciliation
    PARTIAL,     // Substring or OCR noise present (e.g. slight character corruption)
    MISSING,     // Field was not found on the document
    INCORRECT    // Mathematical mismatch (e.g., qty * rate != amount)
}

/**
 * Matching status between a parsed invoice line and local Room inventory.
 */
enum class MatchConfidence {
    EXACT_SKU,        // Verified against variant SKU or supplierCode (1.0)
    HIGH_SIMILARITY,  // Brand + product model + specs match (>= 0.75)
    UNCERTAIN,        // Partial fuzzy candidate match (0.40 - 0.74)
    UNMATCHED         // No catalog candidate found (< 0.40)
}

/**
 * Known supplier invoice layout styles.
 */
enum class InvoiceLayoutType {
    MSTEEL_MBW,        // M.STEEL / MBW layout (spokes, grease, Gross/Pcs units)
    BICYCLE_SUPPLIER,  // Bicycle manufacturer invoice (models, frame sizes, color codes)
    GENERIC_GST        // Standard Indian GST tax invoice with tabular particulars
}

/**
 * Candidate product variant from the local Room database.
 */
data class ProductMatchCandidate(
    val variantId: Long,
    val productId: Long,
    val productName: String,
    val brandName: String,
    val variantSku: String,
    val supplierCode: String?,
    val variantSize: String?,
    val variantColor: String?,
    val costPricePaise: Long,
    val sellingPricePaise: Long,
    val confidenceScore: Float,
    val matchReason: String
) {
    val confidence: MatchConfidence
        get() = when {
            confidenceScore >= 0.95f -> MatchConfidence.EXACT_SKU
            confidenceScore >= 0.75f -> MatchConfidence.HIGH_SIMILARITY
            confidenceScore >= 0.40f -> MatchConfidence.UNCERTAIN
            else -> MatchConfidence.UNMATCHED
        }
}

/**
 * A single structured line item extracted from an invoice table.
 */
data class ParsedInvoiceItem(
    val lineNumber: Int,
    val itemCode: String? = null,
    val description: String,
    val hsnCode: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,             // e.g., "Gross", "Pcs", "Sets"
    val unitRatePaise: Long? = null,      // Stored in paise for integer precision
    val gstRatePercent: Double? = null,   // e.g., 5.0, 12.0, 18.0
    val lineAmountPaise: Long? = null,    // Stored in paise
    val rawLines: List<String> = emptyList(),
    val boundingBox: OcrBoundingBox? = null,
    val qualityFlags: Map<String, FieldQuality> = emptyMap(),
    val suggestedCandidates: List<ProductMatchCandidate> = emptyList(),
    val selectedVariantId: Long? = null,  // User-selected or auto-selected variant
    val isUserEdited: Boolean = false
) {
    val bestCandidate: ProductMatchCandidate?
        get() = suggestedCandidates.firstOrNull()

    val matchConfidence: MatchConfidence
        get() = when {
            suggestedCandidates.isEmpty() -> MatchConfidence.UNMATCHED
            suggestedCandidates.first().confidenceScore >= 0.95f -> MatchConfidence.EXACT_SKU
            suggestedCandidates.first().confidenceScore >= 0.75f -> MatchConfidence.HIGH_SIMILARITY
            else -> MatchConfidence.UNCERTAIN
        }

    /**
     * Checks if quantity * unitRate matches lineAmount within rounding tolerance.
     */
    val isMathReconciled: Boolean
        get() {
            if (quantity == null || unitRatePaise == null || lineAmountPaise == null) return false
            val expectedPaise = (quantity * unitRatePaise).toLong()
            val diff = kotlin.math.abs(expectedPaise - lineAmountPaise)
            // Allow up to ₹1.00 (100 paise) rounding discrepancy
            return diff <= 100L
        }
}

/**
 * Complete structured representation of a supplier invoice parsed from OCR.
 */
data class ParsedInvoice(
    val supplierName: String?,
    val invoiceNumber: String?,
    val invoiceDate: String?,
    val supplierGstin: String?,
    val consigneeName: String?,
    val consigneeGstin: String?,
    val items: List<ParsedInvoiceItem>,
    val totalQuantityExtracted: Double?,
    val totalAmountExtractedPaise: Long?,
    val layoutType: InvoiceLayoutType,
    val rawText: String,
    val processingTimeMs: Long = 0L
) {
    /**
     * Sum of parsed item quantities.
     */
    val sumItemQuantities: Double
        get() = items.mapNotNull { it.quantity }.sum()

    /**
     * Sum of parsed item amounts in paise.
     */
    val sumItemAmountsPaise: Long
        get() = items.mapNotNull { it.lineAmountPaise }.sum()

    /**
     * Validates whether parsed quantity total reconciles with document total.
     */
    val isQuantityReconciled: Boolean
        get() = totalQuantityExtracted != null &&
                kotlin.math.abs(sumItemQuantities - totalQuantityExtracted) < 0.01

    /**
     * Percentage of items with at least one high-confidence local inventory match.
     */
    val matchRatePercent: Int
        get() {
            if (items.isEmpty()) return 0
            val matched = items.count { it.matchConfidence != MatchConfidence.UNMATCHED }
            return ((matched.toDouble() / items.size) * 100).toInt()
        }
}
