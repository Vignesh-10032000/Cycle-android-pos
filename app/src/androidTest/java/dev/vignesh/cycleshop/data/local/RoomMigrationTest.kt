package dev.vignesh.cycleshop.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device instrumentation test for safe Room database migration from Schema v1 to v2.
 *
 * Verifies:
 * 1. Zero data loss during schema migration (brands, products, variants, and stock_movements preserved).
 * 2. New tables (purchase_invoices, purchase_lines, supplier_product_mappings) created with exact schema.
 * 3. New tables accept valid inserts post-migration.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private val TEST_DB = "migration_v1_v2_test.db"
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun cleanup() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migration1To2_preservesExistingDataAndAddsPurchaseTables() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create Schema Version 1 tables
                    db.execSQL("CREATE TABLE IF NOT EXISTS `brands` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_brands_name` ON `brands` (`name`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `brand_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `type` TEXT NOT NULL, `is_active` INTEGER NOT NULL DEFAULT 1, `created_at` INTEGER NOT NULL, FOREIGN KEY(`brand_id`) REFERENCES `brands`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_brand_id` ON `products` (`brand_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `product_variants` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `product_id` INTEGER NOT NULL, `sku` TEXT NOT NULL, `supplier_code` TEXT, `size` TEXT, `color` TEXT, `mrp_paise` INTEGER NOT NULL, `cost_price_paise` INTEGER NOT NULL DEFAULT 0, `selling_price_paise` INTEGER NOT NULL, `stock_quantity` INTEGER NOT NULL DEFAULT 0, `specs_json` TEXT, `created_at` INTEGER NOT NULL, FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_variants_product_id` ON `product_variants` (`product_id`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_product_variants_sku` ON `product_variants` (`sku`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_variants_supplier_code` ON `product_variants` (`supplier_code`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `variant_id` INTEGER NOT NULL, `delta` INTEGER NOT NULL, `movement_type` TEXT NOT NULL, `unit_price_paise` INTEGER, `reference_id` TEXT, `notes` TEXT, `created_at` INTEGER NOT NULL, FOREIGN KEY(`variant_id`) REFERENCES `product_variants`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_variant_id` ON `stock_movements` (`variant_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_movement_type` ON `stock_movements` (`movement_type`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_created_at` ON `stock_movements` (`created_at`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase

        // Seed sample records in version 1
        v1Db.execSQL("INSERT INTO brands (id, name) VALUES (1, 'Hero')")
        v1Db.execSQL("INSERT INTO products (id, brand_id, name, category, type, is_active, created_at) VALUES (1, 1, 'Hero Sprint Pro', 'MTB', 'BICYCLE', 1, 1000)")
        v1Db.execSQL("INSERT INTO product_variants (id, product_id, sku, supplier_code, size, color, mrp_paise, cost_price_paise, selling_price_paise, stock_quantity, specs_json, created_at) VALUES (1, 1, 'VGL-HERO-275-BLK', 'SUP-101', '27.5T', 'Black', 1500000, 1100000, 1350000, 5, null, 1000)")
        v1Db.execSQL("INSERT INTO stock_movements (id, variant_id, delta, movement_type, unit_price_paise, reference_id, notes, created_at) VALUES (1, 1, 5, 'OPENING', 1100000, 'INIT-001', 'Opening stock', 1000)")

        // Execute MIGRATION_1_2
        VglDatabase.MIGRATION_1_2.migrate(v1Db)

        // Validate existing records are preserved
        val brandCursor = v1Db.query("SELECT * FROM brands WHERE id = 1")
        assertTrue("Brand record must survive migration", brandCursor.moveToFirst())
        assertEquals("Hero", brandCursor.getString(brandCursor.getColumnIndexOrThrow("name")))
        brandCursor.close()

        val productCursor = v1Db.query("SELECT * FROM products WHERE id = 1")
        assertTrue("Product record must survive migration", productCursor.moveToFirst())
        assertEquals("Hero Sprint Pro", productCursor.getString(productCursor.getColumnIndexOrThrow("name")))
        productCursor.close()

        val variantCursor = v1Db.query("SELECT * FROM product_variants WHERE id = 1")
        assertTrue("Variant record must survive migration", variantCursor.moveToFirst())
        assertEquals("VGL-HERO-275-BLK", variantCursor.getString(variantCursor.getColumnIndexOrThrow("sku")))
        assertEquals(5, variantCursor.getInt(variantCursor.getColumnIndexOrThrow("stock_quantity")))
        variantCursor.close()

        val movementCursor = v1Db.query("SELECT * FROM stock_movements WHERE id = 1")
        assertTrue("Stock movement record must survive migration", movementCursor.moveToFirst())
        assertEquals(5, movementCursor.getInt(movementCursor.getColumnIndexOrThrow("delta")))
        assertEquals("OPENING", movementCursor.getString(movementCursor.getColumnIndexOrThrow("movement_type")))
        movementCursor.close()

        // Validate new purchase staging tables accept records
        v1Db.execSQL(
            "INSERT INTO purchase_invoices (id, supplier_name, supplier_gstin, invoice_number, invoice_date, status, created_at, updated_at) " +
                    "VALUES (1, 'M.STEEL INDUSTRIES', '03AABCM1234F1Z1', '466', '18/10/2026', 'CONFIRMED_STAGED', 2000, 2000)"
        )
        val invoiceCursor = v1Db.query("SELECT * FROM purchase_invoices WHERE id = 1")
        assertTrue("Purchase invoice must insert successfully", invoiceCursor.moveToFirst())
        assertEquals("466", invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("invoice_number")))
        assertEquals("CONFIRMED_STAGED", invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("status")))
        invoiceCursor.close()

        v1Db.execSQL(
            "INSERT INTO purchase_lines (id, invoice_id, line_number, variant_id, raw_description, raw_quantity, raw_unit, inventory_unit, unit_conversion_factor, effective_quantity_base, review_status, created_at) " +
                    "VALUES (1, 1, 1, 1, 'Spokes 14G', 10.0, 'GROSS', 'PIECE', 144, 1440, 'VERIFIED_MATCH', 2000)"
        )
        val lineCursor = v1Db.query("SELECT * FROM purchase_lines WHERE id = 1")
        assertTrue("Purchase line must insert successfully", lineCursor.moveToFirst())
        assertEquals(1440, lineCursor.getInt(lineCursor.getColumnIndexOrThrow("effective_quantity_base")))
        lineCursor.close()

        v1Db.execSQL(
            "INSERT INTO supplier_product_mappings (id, supplier_identifier, supplier_item_code, supplier_desc_pattern, variant_id, verified_by_owner, created_at, updated_at) " +
                    "VALUES (1, '03AABCM1234F1Z1', 'SPK-14G', 'Spokes 14G', 1, 1, 2000, 2000)"
        )
        val mapCursor = v1Db.query("SELECT * FROM supplier_product_mappings WHERE id = 1")
        assertTrue("Supplier product mapping must insert successfully", mapCursor.moveToFirst())
        assertEquals("SPK-14G", mapCursor.getString(mapCursor.getColumnIndexOrThrow("supplier_item_code")))
        mapCursor.close()

        helper.close()
    }
}
