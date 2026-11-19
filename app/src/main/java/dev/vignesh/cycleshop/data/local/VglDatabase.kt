package dev.vignesh.cycleshop.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.vignesh.cycleshop.data.local.dao.InventoryDao
import dev.vignesh.cycleshop.data.local.dao.ProductDao
import dev.vignesh.cycleshop.data.local.dao.PurchaseDao
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity
import dev.vignesh.cycleshop.data.local.entity.StockMovementEntity
import dev.vignesh.cycleshop.data.local.entity.SupplierProductMappingEntity

/**
 * Main Room database for VGL Cycle Shop.
 *
 * Persists catalog products, variants, brands, append-only stock movement ledger,
 * and staged supplier purchase invoices with verified product mappings.
 *
 * Version History:
 * - v1: Core catalog (brands, products, product_variants, stock_movements)
 * - v2: Purchase staging architecture (purchase_invoices, purchase_lines, supplier_product_mappings)
 */
@Database(
    entities = [
        BrandEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        StockMovementEntity::class,
        PurchaseInvoiceEntity::class,
        PurchaseLineEntity::class,
        SupplierProductMappingEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VglDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        const val DATABASE_NAME = "vgl_cycleshop.db"

        /**
         * Safe migration from Schema Version 1 to Version 2.
         *
         * Creates:
         * 1. `purchase_invoices` table and supporting indices
         * 2. `purchase_lines` table with foreign keys (CASCADE to invoice, SET NULL to variant)
         * 3. `supplier_product_mappings` table for persistent verified mappings
         *
         * Preserves:
         * All existing brands, products, variants, and stock movement ledger records.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create purchase_invoices table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `purchase_invoices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `supplier_name` TEXT NOT NULL,
                        `supplier_gstin` TEXT,
                        `invoice_number` TEXT NOT NULL,
                        `invoice_date` TEXT,
                        `consignee_name` TEXT,
                        `consignee_gstin` TEXT,
                        `total_amount_paise` INTEGER,
                        `total_quantity` REAL,
                        `image_uri` TEXT,
                        `raw_ocr_text` TEXT,
                        `status` TEXT NOT NULL DEFAULT 'CONFIRMED_STAGED',
                        `supporting_doc_ref` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_supplier_name_invoice_number` ON `purchase_invoices` (`supplier_name`, `invoice_number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_supplier_gstin_invoice_number` ON `purchase_invoices` (`supplier_gstin`, `invoice_number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_status` ON `purchase_invoices` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_created_at` ON `purchase_invoices` (`created_at`)")

                // 2. Create purchase_lines table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `purchase_lines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoice_id` INTEGER NOT NULL,
                        `line_number` INTEGER NOT NULL,
                        `variant_id` INTEGER,
                        `supplier_item_code` TEXT,
                        `raw_description` TEXT NOT NULL,
                        `corrected_description` TEXT,
                        `hsn_code` TEXT,
                        `raw_quantity` REAL NOT NULL,
                        `corrected_quantity` REAL,
                        `raw_unit` TEXT NOT NULL,
                        `inventory_unit` TEXT NOT NULL DEFAULT 'PIECE',
                        `unit_conversion_factor` INTEGER NOT NULL DEFAULT 1,
                        `effective_quantity_base` INTEGER NOT NULL,
                        `raw_unit_price_paise` INTEGER,
                        `corrected_unit_price_paise` INTEGER,
                        `raw_line_amount_paise` INTEGER,
                        `corrected_line_amount_paise` INTEGER,
                        `gst_rate_percent` REAL,
                        `review_status` TEXT NOT NULL DEFAULT 'PENDING_MATCH',
                        `rejection_reason` TEXT,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`invoice_id`) REFERENCES `purchase_invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`variant_id`) REFERENCES `product_variants`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_invoice_id` ON `purchase_lines` (`invoice_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_variant_id` ON `purchase_lines` (`variant_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_review_status` ON `purchase_lines` (`review_status`)")

                // 3. Create supplier_product_mappings table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `supplier_product_mappings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `supplier_identifier` TEXT NOT NULL,
                        `supplier_item_code` TEXT,
                        `supplier_desc_pattern` TEXT NOT NULL,
                        `variant_id` INTEGER NOT NULL,
                        `verified_by_owner` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`variant_id`) REFERENCES `product_variants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_product_mappings_supplier_identifier_supplier_item_code` ON `supplier_product_mappings` (`supplier_identifier`, `supplier_item_code`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_product_mappings_variant_id` ON `supplier_product_mappings` (`variant_id`)")
            }
        }

        @Volatile
        private var INSTANCE: VglDatabase? = null

        fun getInstance(context: Context): VglDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VglDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
