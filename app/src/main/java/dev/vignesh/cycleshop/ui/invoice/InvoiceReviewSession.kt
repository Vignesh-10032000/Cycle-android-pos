package dev.vignesh.cycleshop.ui.invoice

import java.util.Collections

internal enum class OwnerReviewStatus(val label: String) {
    OCR_EXTRACTED("OCR extracted"),
    SUGGESTED_MATCH("Suggested match"),
    OWNER_SELECTED("Owner selected"),
    NEEDS_REVIEW("Needs review"),
    OWNER_VERIFIED("Owner verified"),
    REJECTED("Rejected")
}

internal data class ReviewVariant(val id: Long, val name: String, val sku: String)

internal data class ReviewValues(
    val description: String,
    val quantity: Double?,
    val unit: String?,
    val ratePaise: Long?,
    val amountPaise: Long?,
    val variantId: Long? = null
)

internal data class LineVerification(
    val revision: Long,
    val verifiedAtMillis: Long,
    val values: ReviewValues,
    val conversion: UnitConversionResult.Success
)

internal class ReviewLine private constructor(
    val evidence: ParsedInvoiceItem,
    val original: ReviewValues,
    val values: ReviewValues,
    val revision: Long,
    private val lastVerification: LineVerification?,
    val rejected: Boolean
) {
    val verification: LineVerification?
        get() = lastVerification?.takeIf { !rejected && it.revision == revision && it.values == values }

    fun conversion(): UnitConversionResult = UnitConversionEngine.convert(values.unit, values.quantity)

    fun issues(activeVariantIds: Set<Long>): List<String> = fieldIssues().let {
        when {
            values.variantId == null -> it + "Choose a catalog variant."
            values.variantId !in activeVariantIds -> it + "The selected variant is no longer active."
            else -> it
        }
    }

    private fun fieldIssues(): List<String> = buildList {
        if (values.description.isBlank()) add("Enter a description.")
        if (InvoiceNumbers.quantityDecimal(values.quantity) == null) add("Enter a valid quantity.")
        if (values.ratePaise == null || values.ratePaise <= 0) add("Enter a valid rate.")
        if (values.amountPaise == null || values.amountPaise < 0) add("Enter the line amount.")
        when (val result = conversion()) {
            is UnitConversionResult.RequiresReview -> add(result.warningMessage)
            is UnitConversionResult.Invalid -> if (InvoiceNumbers.quantityDecimal(values.quantity) != null) {
                add(result.errorMessage)
            }
            is UnitConversionResult.Success -> Unit
        }
    }

    fun status(activeVariantIds: Set<Long>): OwnerReviewStatus = when {
        rejected -> OwnerReviewStatus.REJECTED
        verification != null && values.variantId in activeVariantIds -> OwnerReviewStatus.OWNER_VERIFIED
        lastVerification != null || fieldIssues().isNotEmpty() -> OwnerReviewStatus.NEEDS_REVIEW
        values.variantId != null -> if (values.variantId in activeVariantIds) OwnerReviewStatus.OWNER_SELECTED
            else OwnerReviewStatus.NEEDS_REVIEW
        evidence.suggestedCandidates.isNotEmpty() -> OwnerReviewStatus.SUGGESTED_MATCH
        else -> OwnerReviewStatus.OCR_EXTRACTED
    }

    fun edit(updated: ReviewValues): ReviewLine {
        if (updated == values) return this
        return ReviewLine(evidence, original, updated, revision + 1, lastVerification, rejected)
    }

    fun setRejected(reject: Boolean): ReviewLine {
        if (reject == rejected) return this
        return ReviewLine(evidence, original, values, revision + 1, lastVerification, reject)
    }

    fun verify(activeVariantIds: Set<Long>, now: Long): ReviewLine {
        if (rejected || issues(activeVariantIds).isNotEmpty()) return this
        val conversion = conversion() as? UnitConversionResult.Success ?: return this
        return ReviewLine(evidence, original, values, revision, LineVerification(revision, now, values, conversion), false)
    }

    fun asParsedItem(): ParsedInvoiceItem = evidence.copy(
        description = values.description, quantity = values.quantity, unit = values.unit,
        unitRatePaise = values.ratePaise, lineAmountPaise = values.amountPaise
    )

    companion object {
        fun from(item: ParsedInvoiceItem): ReviewLine {
            val snapshot = item.copy(
                rawLines = item.rawLines.frozen(),
                qualityFlags = Collections.unmodifiableMap(item.qualityFlags.toMap()),
                suggestedCandidates = item.suggestedCandidates.frozen()
            )
            val values = ReviewValues(item.description, item.quantity, item.unit, item.unitRatePaise, item.lineAmountPaise)
            return ReviewLine(snapshot, values, values, 0, null, false)
        }
    }
}

/** Lives in the scan ViewModel. Verification and approval are deliberately session-scoped. */
internal class InvoiceReviewSession private constructor(
    val originalInvoice: ParsedInvoice,
    val lines: List<ReviewLine>,
    val supplierName: String,
    val invoiceNumber: String,
    private val approvedAtMillis: Long?
) {
    val headerComplete: Boolean get() = supplierName.isNotBlank() && invoiceNumber.isNotBlank()

    fun verifiedCount(activeVariantIds: Set<Long>): Int =
        lines.count { it.status(activeVariantIds) == OwnerReviewStatus.OWNER_VERIFIED }

    fun canApprove(activeVariantIds: Set<Long>): Boolean =
        headerComplete && lines.isNotEmpty() && verifiedCount(activeVariantIds) == lines.size

    fun isApproved(activeVariantIds: Set<Long>): Boolean =
        approvedAtMillis != null && canApprove(activeVariantIds)

    fun updateLine(index: Int, change: (ReviewLine) -> ReviewLine): InvoiceReviewSession {
        val current = lines.getOrNull(index) ?: return this
        val updated = change(current)
        if (updated === current) return this
        return InvoiceReviewSession(originalInvoice, lines.mapIndexed { i, line ->
            if (i == index) updated else line
        }.frozen(), supplierName, invoiceNumber, null)
    }

    fun editHeader(supplier: String, number: String): InvoiceReviewSession =
        if (supplier == supplierName && number == invoiceNumber) this
        else InvoiceReviewSession(originalInvoice, lines, supplier, number, null)

    fun approve(activeVariantIds: Set<Long>, now: Long): InvoiceReviewSession =
        if (canApprove(activeVariantIds)) InvoiceReviewSession(originalInvoice, lines, supplierName, invoiceNumber, now)
        else this

    fun v2StagingIssues(): List<String> = buildList {
        if (lines.any { InvoiceNumbers.quantityDecimal(it.original.quantity) == null }) {
            add("This database version cannot store a missing original quantity alongside its correction.")
        }
        if (lines.any { it.original.unit != it.values.unit }) {
            add("This database version cannot store both the original and corrected unit.")
        }
    }

    fun reviewedInvoice(): ParsedInvoice = originalInvoice.copy(
        supplierName = supplierName.trim().takeIf(String::isNotBlank),
        invoiceNumber = invoiceNumber.trim().takeIf(String::isNotBlank),
        items = lines.map(ReviewLine::asParsedItem)
    )

    companion object {
        fun from(invoice: ParsedInvoice): InvoiceReviewSession {
            val lines = invoice.items.map(ReviewLine::from).frozen()
            return InvoiceReviewSession(
                invoice.copy(items = lines.map { it.evidence }.frozen()), lines,
                invoice.supplierName.orEmpty(), invoice.invoiceNumber.orEmpty(), null
            )
        }
    }
}

private fun <T> List<T>.frozen(): List<T> = Collections.unmodifiableList(toList())
