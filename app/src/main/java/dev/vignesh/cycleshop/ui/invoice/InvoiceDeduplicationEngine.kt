package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import java.util.Locale

/**
 * Result of checking an invoice for duplicates.
 */
sealed class DeduplicationResult {
    data object Unique : DeduplicationResult()

    data class ExactDuplicate(
        val existingInvoiceId: Long,
        val invoiceNumber: String,
        val supplierName: String,
        val invoiceDate: String?,
        val message: String
    ) : DeduplicationResult()

    data class PotentialDuplicate(
        val existingInvoiceId: Long,
        val matchedField: String,
        val matchedValue: String,
        val message: String
    ) : DeduplicationResult()
}

/**
 * Supplier-aware invoice deduplication engine.
 *
 * Prevents re-scanning and double-staging identical invoices while allowing
 * different suppliers to legitimately share identical invoice serial numbers (e.g. #466).
 */
object InvoiceDeduplicationEngine {

    fun checkDuplicate(
        incomingSupplierName: String,
        incomingSupplierGstin: String?,
        incomingInvoiceNumber: String,
        incomingDocRef: String?,
        existingInvoices: List<PurchaseInvoiceEntity>
    ): DeduplicationResult {
        val cleanInvoiceNo = incomingInvoiceNumber.trim().uppercase(Locale.ROOT)
        val cleanSupplierName = normalizeSupplierName(incomingSupplierName)
        val cleanGstin = incomingSupplierGstin?.trim()?.uppercase(Locale.ROOT)
        val cleanDocRef = incomingDocRef?.trim()?.uppercase(Locale.ROOT)

        for (existing in existingInvoices) {
            val existInvoiceNo = existing.invoiceNumber.trim().uppercase(Locale.ROOT)
            val existSupplierName = normalizeSupplierName(existing.supplierName)
            val existGstin = existing.supplierGstin?.trim()?.uppercase(Locale.ROOT)
            val existDocRef = existing.supportingDocRef?.trim()?.uppercase(Locale.ROOT)

            // Rule 1: Exact Supplier GSTIN + Invoice Number match
            if (!cleanGstin.isNullOrBlank() && !existGstin.isNullOrBlank()) {
                if (cleanGstin == existGstin && cleanInvoiceNo == existInvoiceNo) {
                    return DeduplicationResult.ExactDuplicate(
                        existingInvoiceId = existing.id,
                        invoiceNumber = existing.invoiceNumber,
                        supplierName = existing.supplierName,
                        invoiceDate = existing.invoiceDate,
                        message = "Invoice #$cleanInvoiceNo from supplier GSTIN $cleanGstin has already been staged (ID: ${existing.id})."
                    )
                }
            }

            // Rule 2: Normalized Supplier Name + Invoice Number match
            val isSupplierSame = cleanSupplierName == existSupplierName ||
                    cleanSupplierName.contains(existSupplierName) ||
                    existSupplierName.contains(cleanSupplierName)

            if (isSupplierSame && cleanInvoiceNo == existInvoiceNo) {
                return DeduplicationResult.ExactDuplicate(
                    existingInvoiceId = existing.id,
                    invoiceNumber = existing.invoiceNumber,
                    supplierName = existing.supplierName,
                    invoiceDate = existing.invoiceDate,
                    message = "Invoice #$cleanInvoiceNo from '$cleanSupplierName' has already been staged (ID: ${existing.id})."
                )
            }

            // Rule 3: Supporting document cross-match for same supplier (e.g. Sales order or Challan #)
            if (isSupplierSame) {
                if (!cleanDocRef.isNullOrBlank() && cleanDocRef == existDocRef) {
                    return DeduplicationResult.PotentialDuplicate(
                        existingInvoiceId = existing.id,
                        matchedField = "Supporting Document Reference",
                        matchedValue = cleanDocRef,
                        message = "Existing invoice ${existing.id} shares matching order/challan ref '$cleanDocRef'."
                    )
                }
                if (!cleanDocRef.isNullOrBlank() && cleanDocRef == existInvoiceNo) {
                    return DeduplicationResult.PotentialDuplicate(
                        existingInvoiceId = existing.id,
                        matchedField = "Order Reference vs Invoice No",
                        matchedValue = cleanDocRef,
                        message = "Incoming document ref '$cleanDocRef' matches existing invoice number."
                    )
                }
            }
        }

        return DeduplicationResult.Unique
    }

    private fun normalizeSupplierName(name: String): String {
        return name.uppercase(Locale.ROOT)
            .replace(Regex("""[^A-Z0-9]"""), "")
            .replace("PRIVATE", "")
            .replace("LIMITED", "")
            .replace("LTD", "")
            .replace("PVT", "")
            .trim()
    }
}
