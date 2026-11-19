package dev.vignesh.cycleshop.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity
import dev.vignesh.cycleshop.data.local.entity.SupplierProductMappingEntity
import dev.vignesh.cycleshop.data.local.model.LineReviewStatus
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus
import dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceWithLines
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for purchase invoice staging and supplier mappings.
 *
 * Guarantees atomic invoice insertions and strict supplier-aware deduplication queries.
 */
@Dao
interface PurchaseDao {

    // ── Invoice Headers ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: PurchaseInvoiceEntity): Long

    @Query("""
        SELECT * FROM purchase_invoices 
        WHERE invoice_number = :invoiceNumber 
          AND (
            (:supplierGstin IS NOT NULL AND supplier_gstin = :supplierGstin) 
            OR supplier_name = :supplierName
          )
        LIMIT 1
    """)
    suspend fun findDuplicateInvoice(
        invoiceNumber: String,
        supplierGstin: String?,
        supplierName: String
    ): PurchaseInvoiceEntity?

    @Query("SELECT * FROM purchase_invoices WHERE id = :invoiceId")
    suspend fun getInvoiceById(invoiceId: Long): PurchaseInvoiceEntity?

    @Query("UPDATE purchase_invoices SET status = :status, updated_at = :updatedAt WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(
        invoiceId: Long,
        status: PurchaseInvoiceStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM purchase_invoices WHERE id = :invoiceId")
    suspend fun deleteInvoice(invoiceId: Long)

    // ── Invoice Lines ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLines(lines: List<PurchaseLineEntity>): List<Long>

    @Query("SELECT * FROM purchase_lines WHERE invoice_id = :invoiceId ORDER BY line_number ASC")
    fun getLinesForInvoice(invoiceId: Long): Flow<List<PurchaseLineEntity>>

    @Query("SELECT * FROM purchase_lines WHERE id = :lineId")
    suspend fun getLineById(lineId: Long): PurchaseLineEntity?

    @Query("""
        UPDATE purchase_lines 
        SET variant_id = :variantId, 
            review_status = :status,
            corrected_quantity = :correctedQty,
            corrected_unit_price_paise = :correctedPricePaise
        WHERE id = :lineId
    """)
    suspend fun updateLineDetails(
        lineId: Long,
        variantId: Long?,
        status: LineReviewStatus,
        correctedQty: Double?,
        correctedPricePaise: Long?
    )

    // ── Combined 1-to-Many Relationships ────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM purchase_invoices WHERE id = :invoiceId")
    fun getInvoiceWithLines(invoiceId: Long): Flow<PurchaseInvoiceWithLines?>

    @Transaction
    @Query("SELECT * FROM purchase_invoices ORDER BY created_at DESC")
    fun getAllInvoicesWithLines(): Flow<List<PurchaseInvoiceWithLines>>

    @Transaction
    @Query("SELECT * FROM purchase_invoices WHERE status = :status ORDER BY created_at DESC")
    fun getInvoicesByStatus(status: PurchaseInvoiceStatus): Flow<List<PurchaseInvoiceWithLines>>

    // ── Supplier-Product Mappings ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSupplierMapping(mapping: SupplierProductMappingEntity): Long

    @Query("""
        SELECT * FROM supplier_product_mappings 
        WHERE supplier_identifier = :supplierIdentifier 
          AND (supplier_item_code = :itemCode OR :itemCode IS NULL)
        LIMIT 1
    """)
    suspend fun getVerifiedMapping(
        supplierIdentifier: String,
        itemCode: String?
    ): SupplierProductMappingEntity?

    @Query("SELECT * FROM supplier_product_mappings WHERE supplier_identifier = :supplierIdentifier")
    fun getMappingsForSupplier(supplierIdentifier: String): Flow<List<SupplierProductMappingEntity>>

    @Query("SELECT * FROM supplier_product_mappings")
    fun getAllVerifiedMappings(): Flow<List<SupplierProductMappingEntity>>

    // ── Atomic Staging Transaction ──────────────────────────────────────────

    @Transaction
    suspend fun stageInvoiceWithLines(
        invoice: PurchaseInvoiceEntity,
        lines: List<PurchaseLineEntity>,
        verifiedMappings: List<SupplierProductMappingEntity> = emptyList()
    ): Long {
        val invoiceId = insertInvoice(invoice)
        val linkedLines = lines.map { it.copy(invoiceId = invoiceId) }
        insertLines(linkedLines)

        for (mapping in verifiedMappings) {
            insertOrUpdateSupplierMapping(mapping)
        }

        return invoiceId
    }
}
