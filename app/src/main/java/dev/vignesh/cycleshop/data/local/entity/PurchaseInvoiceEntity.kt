package dev.vignesh.cycleshop.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus

/**
 * Persisted supplier purchase invoice document header.
 *
 * Represents an audited supplier invoice staged for owner approval.
 * Deduplication is enforced via composite supplier and invoice number indices.
 */
@Entity(
    tableName = "purchase_invoices",
    indices = [
        Index(value = ["supplier_name", "invoice_number"]),
        Index(value = ["supplier_gstin", "invoice_number"]),
        Index(value = ["status"]),
        Index(value = ["created_at"])
    ]
)
data class PurchaseInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "supplier_name")
    val supplierName: String,

    @ColumnInfo(name = "supplier_gstin")
    val supplierGstin: String? = null,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,

    @ColumnInfo(name = "invoice_date")
    val invoiceDate: String? = null,

    @ColumnInfo(name = "consignee_name")
    val consigneeName: String? = null,

    @ColumnInfo(name = "consignee_gstin")
    val consigneeGstin: String? = null,

    @ColumnInfo(name = "total_amount_paise")
    val totalAmountPaise: Long? = null,

    @ColumnInfo(name = "total_quantity")
    val totalQuantity: Double? = null,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    @ColumnInfo(name = "raw_ocr_text")
    val rawOcrText: String? = null,

    @ColumnInfo(name = "status", defaultValue = "'CONFIRMED_STAGED'")
    val status: PurchaseInvoiceStatus = PurchaseInvoiceStatus.CONFIRMED_STAGED,

    @ColumnInfo(name = "supporting_doc_ref")
    val supportingDocRef: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
