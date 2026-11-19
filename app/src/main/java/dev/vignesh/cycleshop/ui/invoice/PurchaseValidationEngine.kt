package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.model.LineReviewStatus

data class LineValidationResult(
    val lineNumber: Int,
    val isValid: Boolean,
    val reviewStatus: LineReviewStatus,
    val issues: List<String>
)

data class InvoiceValidationSummary(
    val canStage: Boolean,
    val totalLines: Int,
    val verifiedLinesCount: Int,
    val unverifiedLinesCount: Int,
    val linesWithIssues: List<LineValidationResult>,
    val globalIssues: List<String>
)

/**
 * Strict validator for supplier purchase invoice staging and inventory readiness.
 *
 * Rules:
 * 1. A row CANNOT update inventory without a selected, non-null variantId ([LineReviewStatus.VERIFIED_MATCH]).
 * 2. Incomplete or obscured OCR fields are flagged for manual review.
 * 3. Quantities and prices must be strictly positive (> 0).
 */
object PurchaseValidationEngine {

    fun validateLine(
        lineNumber: Int,
        description: String,
        variantId: Long?,
        quantity: Double?,
        unitRatePaise: Long?,
        qualityFlags: Map<String, FieldQuality> = emptyMap(),
        lineAmountPaise: Long? = null,
        conversion: UnitConversionResult? = null
    ): LineValidationResult {
        val issues = mutableListOf<String>()

        if (description.isBlank()) {
            issues.add("Line $lineNumber has an empty product description.")
        }

        val invalidQuantity = InvoiceNumbers.quantityDecimal(quantity) == null
        val invalidRate = unitRatePaise == null || unitRatePaise <= 0L
        val invalidAmount = lineAmountPaise != null && lineAmountPaise < 0
        if (invalidQuantity) {
            issues.add("Line $lineNumber has invalid quantity ($quantity).")
        }

        if (invalidRate) {
            issues.add("Line $lineNumber has invalid or missing unit rate.")
        }

        if (invalidAmount) issues.add("Line $lineNumber has a negative line amount.")
        when (conversion) {
            is UnitConversionResult.Invalid -> issues.add(conversion.errorMessage)
            is UnitConversionResult.RequiresReview -> issues.add(conversion.warningMessage)
            else -> Unit
        }
        val hasOcrIssues = qualityFlags.values.any { it != FieldQuality.EXACT }
        if (hasOcrIssues) {
            issues.add("Line $lineNumber has uncertain or partial OCR fields requiring review.")
        }

        val isValid = description.isNotBlank() && !invalidQuantity && !invalidRate && !invalidAmount &&
            conversion !is UnitConversionResult.Invalid
        val reviewStatus = when {
            !isValid -> LineReviewStatus.UNMATCHED_REJECTED
            variantId == null -> {
                issues.add("Line $lineNumber does not have a verified catalog product variant assigned.")
                LineReviewStatus.PENDING_MATCH
            }
            hasOcrIssues || conversion is UnitConversionResult.RequiresReview -> LineReviewStatus.MANUAL_CORRECTED
            else -> LineReviewStatus.VERIFIED_MATCH
        }

        return LineValidationResult(
            lineNumber = lineNumber,
            isValid = isValid,
            reviewStatus = reviewStatus,
            issues = issues
        )
    }

    fun validateInvoice(
        supplierName: String?,
        invoiceNumber: String?,
        lineResults: List<LineValidationResult>
    ): InvoiceValidationSummary {
        val globalIssues = mutableListOf<String>()

        if (supplierName.isNullOrBlank()) {
            globalIssues.add("Supplier name is required for invoice staging.")
        }

        if (invoiceNumber.isNullOrBlank()) {
            globalIssues.add("Invoice number is required for invoice staging.")
        }

        if (lineResults.isEmpty()) {
            globalIssues.add("Invoice must contain at least one line item.")
        }

        val verifiedCount = lineResults.count { it.reviewStatus == LineReviewStatus.VERIFIED_MATCH }
        val unverifiedCount = lineResults.size - verifiedCount

        // An invoice can be staged as long as header exists, quantities are valid,
        // and lines with issues are saved with PENDING_MATCH for review.
        val canStage = globalIssues.isEmpty() && lineResults.all { it.isValid }

        return InvoiceValidationSummary(
            canStage = canStage,
            totalLines = lineResults.size,
            verifiedLinesCount = verifiedCount,
            unverifiedLinesCount = unverifiedCount,
            linesWithIssues = lineResults.filter { it.issues.isNotEmpty() },
            globalIssues = globalIssues
        )
    }
}
