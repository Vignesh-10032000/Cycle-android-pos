package dev.vignesh.cycleshop.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.vignesh.cycleshop.data.local.model.InventoryUnit
import dev.vignesh.cycleshop.data.local.model.LineReviewStatus

/**
 * Line item on a supplier purchase invoice document.
 *
 * Foreign Keys:
 * - [invoiceId] -> [PurchaseInvoiceEntity.id] (CASCADE delete when invoice is deleted)
 * - [variantId] -> [ProductVariantEntity.id] (SET NULL if variant is deleted; required before inventory update)
 *
 * Strict rule:
 * [variantId] can be null during initial OCR and staging. In Step 3B, a non-null [variantId]
 * and [LineReviewStatus.VERIFIED_MATCH] are strictly required before any stock balance movement can be generated.
 */
@Entity(
    tableName = "purchase_lines",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoice_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["invoice_id"]),
        Index(value = ["variant_id"]),
        Index(value = ["review_status"])
    ]
)
data class PurchaseLineEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "invoice_id")
    val invoiceId: Long,

    @ColumnInfo(name = "line_number")
    val lineNumber: Int,

    @ColumnInfo(name = "variant_id")
    val variantId: Long? = null,

    @ColumnInfo(name = "supplier_item_code")
    val supplierItemCode: String? = null,

    @ColumnInfo(name = "raw_description")
    val rawDescription: String,

    @ColumnInfo(name = "corrected_description")
    val correctedDescription: String? = null,

    @ColumnInfo(name = "hsn_code")
    val hsnCode: String? = null,

    @ColumnInfo(name = "raw_quantity")
    val rawQuantity: Double,

    @ColumnInfo(name = "corrected_quantity")
    val correctedQuantity: Double? = null,

    @ColumnInfo(name = "raw_unit")
    val rawUnit: String,

    @ColumnInfo(name = "inventory_unit", defaultValue = "'PIECE'")
    val inventoryUnit: InventoryUnit = InventoryUnit.PIECE,

    @ColumnInfo(name = "unit_conversion_factor", defaultValue = "1")
    val unitConversionFactor: Int = 1,

    @ColumnInfo(name = "effective_quantity_base")
    val effectiveQuantityBase: Int,

    @ColumnInfo(name = "raw_unit_price_paise")
    val rawUnitPricePaise: Long? = null,

    @ColumnInfo(name = "corrected_unit_price_paise")
    val correctedUnitPricePaise: Long? = null,

    @ColumnInfo(name = "raw_line_amount_paise")
    val rawLineAmountPaise: Long? = null,

    @ColumnInfo(name = "corrected_line_amount_paise")
    val correctedLineAmountPaise: Long? = null,

    @ColumnInfo(name = "gst_rate_percent")
    val gstRatePercent: Double? = null,

    @ColumnInfo(name = "review_status", defaultValue = "'PENDING_MATCH'")
    val reviewStatus: LineReviewStatus = LineReviewStatus.PENDING_MATCH,

    @ColumnInfo(name = "rejection_reason")
    val rejectionReason: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
