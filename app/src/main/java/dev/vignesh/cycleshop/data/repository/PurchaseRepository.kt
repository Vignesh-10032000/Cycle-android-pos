package dev.vignesh.cycleshop.data.repository

import dev.vignesh.cycleshop.data.local.dao.PurchaseDao
import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity
import dev.vignesh.cycleshop.data.local.entity.SupplierProductMappingEntity
import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceWithLines
import dev.vignesh.cycleshop.ui.invoice.DeduplicationResult
import dev.vignesh.cycleshop.ui.invoice.InvoiceDeduplicationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository coordinating purchase invoice staging, deduplication, and supplier mappings.
 *
 * Strict boundary:
 * Does NOT generate stock movements or touch inventory ledger balances.
 * Invoices and line items are staged purely in `purchase_invoices` and `purchase_lines`.
 */
class PurchaseRepository(
    private val purchaseDao: PurchaseDao
) {

    val allStagedInvoices: Flow<List<PurchaseInvoiceWithLines>> =
        purchaseDao.getAllInvoicesWithLines()

    val pendingReviewInvoices: Flow<List<PurchaseInvoiceWithLines>> =
        purchaseDao.getInvoicesByStatus(PurchaseInvoiceStatus.CONFIRMED_STAGED)

    fun getInvoiceWithLines(invoiceId: Long): Flow<PurchaseInvoiceWithLines?> =
        purchaseDao.getInvoiceWithLines(invoiceId)

    /**
     * Checks if an incoming invoice duplicates an existing record in the database.
     */
    suspend fun checkDuplicate(
        supplierName: String,
        supplierGstin: String?,
        invoiceNumber: String,
        supportingDocRef: String? = null
    ): DeduplicationResult {
        val existingInvoices = purchaseDao.getAllInvoicesWithLines().first().map { it.invoice }
        return InvoiceDeduplicationEngine.checkDuplicate(
            incomingSupplierName = supplierName,
            incomingSupplierGstin = supplierGstin,
            incomingInvoiceNumber = invoiceNumber,
            incomingDocRef = supportingDocRef,
            existingInvoices = existingInvoices
        )
    }

    /**
     * Stages an audited invoice and its line items inside an atomic SQLite transaction.
     */
    suspend fun stageInvoice(
        invoice: PurchaseInvoiceEntity,
        lines: List<PurchaseLineEntity>,
        verifiedMappings: List<SupplierProductMappingEntity> = emptyList()
    ): Result<Long> {
        return try {
            // Check for strict duplicate prior to insert
            val existing = purchaseDao.findDuplicateInvoice(
                invoiceNumber = invoice.invoiceNumber,
                supplierGstin = invoice.supplierGstin,
                supplierName = invoice.supplierName
            )
            if (existing != null) {
                return Result.failure(
                    IllegalStateException("Invoice #${invoice.invoiceNumber} from '${invoice.supplierName}' already exists in database (ID: ${existing.id}).")
                )
            }

            val stagedId = purchaseDao.stageInvoiceWithLines(
                invoice = invoice.copy(status = PurchaseInvoiceStatus.CONFIRMED_STAGED),
                lines = lines,
                verifiedMappings = verifiedMappings
            )
            Result.success(stagedId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLineDetails(
        lineId: Long,
        variantId: Long?,
        status: LineReviewStatus,
        correctedQty: Double? = null,
        correctedPricePaise: Long? = null
    ) {
        purchaseDao.updateLineDetails(lineId, variantId, status, correctedQty, correctedPricePaise)
    }

    suspend fun deleteStagedInvoice(invoiceId: Long) {
        purchaseDao.deleteInvoice(invoiceId)
    }

    suspend fun getVerifiedMapping(
        supplierIdentifier: String,
        itemCode: String?
    ): SupplierProductMappingEntity? {
        return purchaseDao.getVerifiedMapping(supplierIdentifier, itemCode)
    }

    suspend fun saveVerifiedMapping(mapping: SupplierProductMappingEntity): Long {
        return purchaseDao.insertOrUpdateSupplierMapping(mapping)
    }
}
