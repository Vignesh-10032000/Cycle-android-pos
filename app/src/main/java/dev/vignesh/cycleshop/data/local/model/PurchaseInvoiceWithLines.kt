package dev.vignesh.cycleshop.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity

/**
 * 1-to-many relationship combining a [PurchaseInvoiceEntity] header with its [PurchaseLineEntity] lines.
 */
data class PurchaseInvoiceWithLines(
    @Embedded
    val invoice: PurchaseInvoiceEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "invoice_id"
    )
    val lines: List<PurchaseLineEntity>
)
