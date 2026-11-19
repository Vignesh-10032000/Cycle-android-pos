package dev.vignesh.cycleshop.data.local.model

/**
 * Lifecycle status of a supplier purchase invoice document in the staging pipeline.
 *
 * Strict boundary:
 * - In Phase 3 Step 3A, confirmed invoices are saved as [CONFIRMED_STAGED].
 * - Invoices transition to [POSTED_TO_INVENTORY] ONLY in Step 3B upon explicit owner approval.
 */
enum class PurchaseInvoiceStatus(val dbValue: String) {
    DRAFT("DRAFT"),
    NEEDS_REVIEW("NEEDS_REVIEW"),
    CONFIRMED_STAGED("CONFIRMED_STAGED"),
    POSTED_TO_INVENTORY("POSTED_TO_INVENTORY"),
    REJECTED("REJECTED");

    companion object {
        fun fromDb(value: String): PurchaseInvoiceStatus {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: DRAFT
        }
    }
}
