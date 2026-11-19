package dev.vignesh.cycleshop.data.local.model

/**
 * Review and verification status of an individual purchase invoice line item.
 *
 * Rules:
 * - A line must have [VERIFIED_MATCH] with a non-null variantId before it can update inventory in Step 3B.
 * - Missing or partial OCR fields default to [PENDING_MATCH] or require [MANUAL_CORRECTED].
 */
enum class LineReviewStatus(val dbValue: String) {
    PENDING_MATCH("PENDING_MATCH"),
    VERIFIED_MATCH("VERIFIED_MATCH"),
    MANUAL_CORRECTED("MANUAL_CORRECTED"),
    UNMATCHED_REJECTED("UNMATCHED_REJECTED");

    companion object {
        fun fromDb(value: String): LineReviewStatus {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: PENDING_MATCH
        }
    }
}
