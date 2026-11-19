package dev.vignesh.cycleshop.ui.invoice

import androidx.room.withTransaction
import dev.vignesh.cycleshop.data.local.VglDatabase
import dev.vignesh.cycleshop.data.local.dao.ProductDao
import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity
import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus
import dev.vignesh.cycleshop.data.repository.PurchaseRepository

internal data class InvoiceStagingV2(
    val invoice: PurchaseInvoiceEntity,
    val lines: List<PurchaseLineEntity>
) {
    companion object {
        fun from(review: InvoiceReviewSession, activeVariantIds: Set<Long>, imageUri: String): InvoiceStagingV2 {
            check(review.isApproved(activeVariantIds)) { "Approve the current review of every line before staging." }
            val storageIssues = review.v2StagingIssues()
            check(storageIssues.isEmpty()) { storageIssues.joinToString("\n") }
            val ocr = review.originalInvoice
            val header = PurchaseInvoiceEntity(
                supplierName = review.supplierName.trim(),
                supplierGstin = ocr.supplierGstin,
                invoiceNumber = review.invoiceNumber.trim(),
                invoiceDate = ocr.invoiceDate,
                consigneeName = ocr.consigneeName,
                consigneeGstin = ocr.consigneeGstin,
                totalAmountPaise = ocr.totalAmountExtractedPaise,
                totalQuantity = ocr.totalQuantityExtracted,
                imageUri = imageUri,
                rawOcrText = ocr.rawText,
                status = PurchaseInvoiceStatus.NEEDS_REVIEW
            )
            val lines = review.lines.map { line ->
                val receipt = checkNotNull(line.verification)
                val original = line.original
                val current = receipt.values
                PurchaseLineEntity(
                    invoiceId = 0,
                    lineNumber = line.evidence.lineNumber,
                    variantId = current.variantId,
                    supplierItemCode = line.evidence.itemCode,
                    rawDescription = original.description,
                    correctedDescription = current.description.takeIf { it != original.description },
                    hsnCode = line.evidence.hsnCode,
                    rawQuantity = checkNotNull(original.quantity),
                    correctedQuantity = current.quantity.takeIf { it != original.quantity },
                    rawUnit = original.unit.orEmpty(),
                    inventoryUnit = receipt.conversion.targetUnit,
                    unitConversionFactor = receipt.conversion.conversionFactor,
                    effectiveQuantityBase = receipt.conversion.effectiveBaseQuantity,
                    rawUnitPricePaise = original.ratePaise,
                    correctedUnitPricePaise = current.ratePaise.takeIf { it != original.ratePaise },
                    rawLineAmountPaise = original.amountPaise,
                    correctedLineAmountPaise = current.amountPaise.takeIf { it != original.amountPaise },
                    gstRatePercent = line.evidence.gstRatePercent,
                    reviewStatus = LineReviewStatus.PENDING_MATCH
                )
            }
            return InvoiceStagingV2(header, lines)
        }
    }
}

internal suspend fun stageReviewedInvoiceV2(
    db: VglDatabase,
    repository: PurchaseRepository,
    review: InvoiceReviewSession,
    imageUri: String
): Long = db.withTransaction {
    val active = db.productDao().activeReviewVariantIds(review.selectedVariants())
    val draft = InvoiceStagingV2.from(review, active, imageUri)
    val duplicate = repository.checkDuplicate(
        draft.invoice.supplierName, draft.invoice.supplierGstin, draft.invoice.invoiceNumber
    )
    if (duplicate is DeduplicationResult.ExactDuplicate) error(duplicate.message)
    // Stage review data only. Suggestions never create supplier mappings.
    repository.stageInvoice(draft.invoice, draft.lines).getOrThrow()
}

internal suspend fun ProductDao.activeReviewVariantIds(ids: Set<Long>): Set<Long> = ids.filterTo(mutableSetOf()) { id ->
    val variant = getVariantById(id)
    variant != null && getProductById(variant.productId)?.isActive == true
}

internal fun InvoiceReviewSession.selectedVariants(): Set<Long> =
    lines.mapNotNull { it.values.variantId }.toSet()
