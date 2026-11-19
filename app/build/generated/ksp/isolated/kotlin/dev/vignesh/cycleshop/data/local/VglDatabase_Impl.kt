package dev.vignesh.cycleshop.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.vignesh.cycleshop.`data`.local.dao.InventoryDao
import dev.vignesh.cycleshop.`data`.local.dao.InventoryDao_Impl
import dev.vignesh.cycleshop.`data`.local.dao.ProductDao
import dev.vignesh.cycleshop.`data`.local.dao.ProductDao_Impl
import dev.vignesh.cycleshop.`data`.local.dao.PurchaseDao
import dev.vignesh.cycleshop.`data`.local.dao.PurchaseDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class VglDatabase_Impl : VglDatabase() {
  private val _productDao: Lazy<ProductDao> = lazy {
    ProductDao_Impl(this)
  }

  private val _inventoryDao: Lazy<InventoryDao> = lazy {
    InventoryDao_Impl(this)
  }

  private val _purchaseDao: Lazy<PurchaseDao> = lazy {
    PurchaseDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "2ab82dea9d1e8183d3a7af401445c015", "bf8f06369fb1b14a2a08dafc03a7db1b") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `brands` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_brands_name` ON `brands` (`name`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `brand_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `type` TEXT NOT NULL, `is_active` INTEGER NOT NULL DEFAULT 1, `created_at` INTEGER NOT NULL, FOREIGN KEY(`brand_id`) REFERENCES `brands`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_products_brand_id` ON `products` (`brand_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `product_variants` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `product_id` INTEGER NOT NULL, `sku` TEXT NOT NULL, `supplier_code` TEXT, `size` TEXT, `color` TEXT, `mrp_paise` INTEGER NOT NULL, `cost_price_paise` INTEGER NOT NULL DEFAULT 0, `selling_price_paise` INTEGER NOT NULL, `stock_quantity` INTEGER NOT NULL DEFAULT 0, `specs_json` TEXT, `created_at` INTEGER NOT NULL, FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_product_variants_product_id` ON `product_variants` (`product_id`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_product_variants_sku` ON `product_variants` (`sku`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_product_variants_supplier_code` ON `product_variants` (`supplier_code`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `variant_id` INTEGER NOT NULL, `delta` INTEGER NOT NULL, `movement_type` TEXT NOT NULL, `unit_price_paise` INTEGER, `reference_id` TEXT, `notes` TEXT, `created_at` INTEGER NOT NULL, FOREIGN KEY(`variant_id`) REFERENCES `product_variants`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_variant_id` ON `stock_movements` (`variant_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_movement_type` ON `stock_movements` (`movement_type`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_created_at` ON `stock_movements` (`created_at`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `purchase_invoices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `supplier_name` TEXT NOT NULL, `supplier_gstin` TEXT, `invoice_number` TEXT NOT NULL, `invoice_date` TEXT, `consignee_name` TEXT, `consignee_gstin` TEXT, `total_amount_paise` INTEGER, `total_quantity` REAL, `image_uri` TEXT, `raw_ocr_text` TEXT, `status` TEXT NOT NULL DEFAULT 'CONFIRMED_STAGED', `supporting_doc_ref` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_supplier_name_invoice_number` ON `purchase_invoices` (`supplier_name`, `invoice_number`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_supplier_gstin_invoice_number` ON `purchase_invoices` (`supplier_gstin`, `invoice_number`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_status` ON `purchase_invoices` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_invoices_created_at` ON `purchase_invoices` (`created_at`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `purchase_lines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoice_id` INTEGER NOT NULL, `line_number` INTEGER NOT NULL, `variant_id` INTEGER, `supplier_item_code` TEXT, `raw_description` TEXT NOT NULL, `corrected_description` TEXT, `hsn_code` TEXT, `raw_quantity` REAL NOT NULL, `corrected_quantity` REAL, `raw_unit` TEXT NOT NULL, `inventory_unit` TEXT NOT NULL DEFAULT 'PIECE', `unit_conversion_factor` INTEGER NOT NULL DEFAULT 1, `effective_quantity_base` INTEGER NOT NULL, `raw_unit_price_paise` INTEGER, `corrected_unit_price_paise` INTEGER, `raw_line_amount_paise` INTEGER, `corrected_line_amount_paise` INTEGER, `gst_rate_percent` REAL, `review_status` TEXT NOT NULL DEFAULT 'PENDING_MATCH', `rejection_reason` TEXT, `created_at` INTEGER NOT NULL, FOREIGN KEY(`invoice_id`) REFERENCES `purchase_invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`variant_id`) REFERENCES `product_variants`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_invoice_id` ON `purchase_lines` (`invoice_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_variant_id` ON `purchase_lines` (`variant_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_review_status` ON `purchase_lines` (`review_status`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `supplier_product_mappings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `supplier_identifier` TEXT NOT NULL, `supplier_item_code` TEXT, `supplier_desc_pattern` TEXT NOT NULL, `variant_id` INTEGER NOT NULL, `verified_by_owner` INTEGER NOT NULL DEFAULT 1, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, FOREIGN KEY(`variant_id`) REFERENCES `product_variants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_product_mappings_supplier_identifier_supplier_item_code` ON `supplier_product_mappings` (`supplier_identifier`, `supplier_item_code`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_product_mappings_variant_id` ON `supplier_product_mappings` (`variant_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2ab82dea9d1e8183d3a7af401445c015')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `brands`")
        connection.execSQL("DROP TABLE IF EXISTS `products`")
        connection.execSQL("DROP TABLE IF EXISTS `product_variants`")
        connection.execSQL("DROP TABLE IF EXISTS `stock_movements`")
        connection.execSQL("DROP TABLE IF EXISTS `purchase_invoices`")
        connection.execSQL("DROP TABLE IF EXISTS `purchase_lines`")
        connection.execSQL("DROP TABLE IF EXISTS `supplier_product_mappings`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsBrands: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBrands.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrands.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBrands: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBrands: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesBrands.add(TableInfo.Index("index_brands_name", true, listOf("name"), listOf("ASC")))
        val _infoBrands: TableInfo = TableInfo("brands", _columnsBrands, _foreignKeysBrands, _indicesBrands)
        val _existingBrands: TableInfo = read(connection, "brands")
        if (!_infoBrands.equals(_existingBrands)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |brands(dev.vignesh.cycleshop.data.local.entity.BrandEntity).
              | Expected:
              |""".trimMargin() + _infoBrands + """
              |
              | Found:
              |""".trimMargin() + _existingBrands)
        }
        val _columnsProducts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsProducts.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProducts.put("brand_id", TableInfo.Column("brand_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProducts.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProducts.put("category", TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProducts.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProducts.put("is_active", TableInfo.Column("is_active", "INTEGER", true, 0, "1", TableInfo.CREATED_FROM_ENTITY))
        _columnsProducts.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProducts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysProducts.add(TableInfo.ForeignKey("brands", "RESTRICT", "NO ACTION", listOf("brand_id"), listOf("id")))
        val _indicesProducts: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesProducts.add(TableInfo.Index("index_products_brand_id", false, listOf("brand_id"), listOf("ASC")))
        _indicesProducts.add(TableInfo.Index("index_products_name", false, listOf("name"), listOf("ASC")))
        val _infoProducts: TableInfo = TableInfo("products", _columnsProducts, _foreignKeysProducts, _indicesProducts)
        val _existingProducts: TableInfo = read(connection, "products")
        if (!_infoProducts.equals(_existingProducts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |products(dev.vignesh.cycleshop.data.local.entity.ProductEntity).
              | Expected:
              |""".trimMargin() + _infoProducts + """
              |
              | Found:
              |""".trimMargin() + _existingProducts)
        }
        val _columnsProductVariants: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsProductVariants.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("product_id", TableInfo.Column("product_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("sku", TableInfo.Column("sku", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("supplier_code", TableInfo.Column("supplier_code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("size", TableInfo.Column("size", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("color", TableInfo.Column("color", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("mrp_paise", TableInfo.Column("mrp_paise", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("cost_price_paise", TableInfo.Column("cost_price_paise", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("selling_price_paise", TableInfo.Column("selling_price_paise", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("stock_quantity", TableInfo.Column("stock_quantity", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("specs_json", TableInfo.Column("specs_json", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProductVariants.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProductVariants: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysProductVariants.add(TableInfo.ForeignKey("products", "RESTRICT", "NO ACTION", listOf("product_id"), listOf("id")))
        val _indicesProductVariants: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesProductVariants.add(TableInfo.Index("index_product_variants_product_id", false, listOf("product_id"), listOf("ASC")))
        _indicesProductVariants.add(TableInfo.Index("index_product_variants_sku", true, listOf("sku"), listOf("ASC")))
        _indicesProductVariants.add(TableInfo.Index("index_product_variants_supplier_code", false, listOf("supplier_code"), listOf("ASC")))
        val _infoProductVariants: TableInfo = TableInfo("product_variants", _columnsProductVariants, _foreignKeysProductVariants, _indicesProductVariants)
        val _existingProductVariants: TableInfo = read(connection, "product_variants")
        if (!_infoProductVariants.equals(_existingProductVariants)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |product_variants(dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity).
              | Expected:
              |""".trimMargin() + _infoProductVariants + """
              |
              | Found:
              |""".trimMargin() + _existingProductVariants)
        }
        val _columnsStockMovements: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStockMovements.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockMovements.put("variant_id", TableInfo.Column("variant_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockMovements.put("delta", TableInfo.Column("delta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockMovements.put("movement_type", TableInfo.Column("movement_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockMovements.put("unit_price_paise", TableInfo.Column("unit_price_paise", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockMovements.put("reference_id", TableInfo.Column("reference_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockMovements.put("notes", TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStockMovements.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStockMovements: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysStockMovements.add(TableInfo.ForeignKey("product_variants", "RESTRICT", "NO ACTION", listOf("variant_id"), listOf("id")))
        val _indicesStockMovements: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesStockMovements.add(TableInfo.Index("index_stock_movements_variant_id", false, listOf("variant_id"), listOf("ASC")))
        _indicesStockMovements.add(TableInfo.Index("index_stock_movements_movement_type", false, listOf("movement_type"), listOf("ASC")))
        _indicesStockMovements.add(TableInfo.Index("index_stock_movements_created_at", false, listOf("created_at"), listOf("ASC")))
        val _infoStockMovements: TableInfo = TableInfo("stock_movements", _columnsStockMovements, _foreignKeysStockMovements, _indicesStockMovements)
        val _existingStockMovements: TableInfo = read(connection, "stock_movements")
        if (!_infoStockMovements.equals(_existingStockMovements)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |stock_movements(dev.vignesh.cycleshop.data.local.entity.StockMovementEntity).
              | Expected:
              |""".trimMargin() + _infoStockMovements + """
              |
              | Found:
              |""".trimMargin() + _existingStockMovements)
        }
        val _columnsPurchaseInvoices: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPurchaseInvoices.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("supplier_name", TableInfo.Column("supplier_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("supplier_gstin", TableInfo.Column("supplier_gstin", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("invoice_number", TableInfo.Column("invoice_number", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("invoice_date", TableInfo.Column("invoice_date", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("consignee_name", TableInfo.Column("consignee_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("consignee_gstin", TableInfo.Column("consignee_gstin", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("total_amount_paise", TableInfo.Column("total_amount_paise", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("total_quantity", TableInfo.Column("total_quantity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("image_uri", TableInfo.Column("image_uri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("raw_ocr_text", TableInfo.Column("raw_ocr_text", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("status", TableInfo.Column("status", "TEXT", true, 0, "'CONFIRMED_STAGED'", TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("supporting_doc_ref", TableInfo.Column("supporting_doc_ref", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseInvoices.put("updated_at", TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPurchaseInvoices: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPurchaseInvoices: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesPurchaseInvoices.add(TableInfo.Index("index_purchase_invoices_supplier_name_invoice_number", false, listOf("supplier_name", "invoice_number"), listOf("ASC", "ASC")))
        _indicesPurchaseInvoices.add(TableInfo.Index("index_purchase_invoices_supplier_gstin_invoice_number", false, listOf("supplier_gstin", "invoice_number"), listOf("ASC", "ASC")))
        _indicesPurchaseInvoices.add(TableInfo.Index("index_purchase_invoices_status", false, listOf("status"), listOf("ASC")))
        _indicesPurchaseInvoices.add(TableInfo.Index("index_purchase_invoices_created_at", false, listOf("created_at"), listOf("ASC")))
        val _infoPurchaseInvoices: TableInfo = TableInfo("purchase_invoices", _columnsPurchaseInvoices, _foreignKeysPurchaseInvoices, _indicesPurchaseInvoices)
        val _existingPurchaseInvoices: TableInfo = read(connection, "purchase_invoices")
        if (!_infoPurchaseInvoices.equals(_existingPurchaseInvoices)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |purchase_invoices(dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity).
              | Expected:
              |""".trimMargin() + _infoPurchaseInvoices + """
              |
              | Found:
              |""".trimMargin() + _existingPurchaseInvoices)
        }
        val _columnsPurchaseLines: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPurchaseLines.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("invoice_id", TableInfo.Column("invoice_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("line_number", TableInfo.Column("line_number", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("variant_id", TableInfo.Column("variant_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("supplier_item_code", TableInfo.Column("supplier_item_code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("raw_description", TableInfo.Column("raw_description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("corrected_description", TableInfo.Column("corrected_description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("hsn_code", TableInfo.Column("hsn_code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("raw_quantity", TableInfo.Column("raw_quantity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("corrected_quantity", TableInfo.Column("corrected_quantity", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("raw_unit", TableInfo.Column("raw_unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("inventory_unit", TableInfo.Column("inventory_unit", "TEXT", true, 0, "'PIECE'", TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("unit_conversion_factor", TableInfo.Column("unit_conversion_factor", "INTEGER", true, 0, "1", TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("effective_quantity_base", TableInfo.Column("effective_quantity_base", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("raw_unit_price_paise", TableInfo.Column("raw_unit_price_paise", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("corrected_unit_price_paise", TableInfo.Column("corrected_unit_price_paise", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("raw_line_amount_paise", TableInfo.Column("raw_line_amount_paise", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("corrected_line_amount_paise", TableInfo.Column("corrected_line_amount_paise", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("gst_rate_percent", TableInfo.Column("gst_rate_percent", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("review_status", TableInfo.Column("review_status", "TEXT", true, 0, "'PENDING_MATCH'", TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("rejection_reason", TableInfo.Column("rejection_reason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPurchaseLines.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPurchaseLines: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysPurchaseLines.add(TableInfo.ForeignKey("purchase_invoices", "CASCADE", "NO ACTION", listOf("invoice_id"), listOf("id")))
        _foreignKeysPurchaseLines.add(TableInfo.ForeignKey("product_variants", "SET NULL", "NO ACTION", listOf("variant_id"), listOf("id")))
        val _indicesPurchaseLines: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesPurchaseLines.add(TableInfo.Index("index_purchase_lines_invoice_id", false, listOf("invoice_id"), listOf("ASC")))
        _indicesPurchaseLines.add(TableInfo.Index("index_purchase_lines_variant_id", false, listOf("variant_id"), listOf("ASC")))
        _indicesPurchaseLines.add(TableInfo.Index("index_purchase_lines_review_status", false, listOf("review_status"), listOf("ASC")))
        val _infoPurchaseLines: TableInfo = TableInfo("purchase_lines", _columnsPurchaseLines, _foreignKeysPurchaseLines, _indicesPurchaseLines)
        val _existingPurchaseLines: TableInfo = read(connection, "purchase_lines")
        if (!_infoPurchaseLines.equals(_existingPurchaseLines)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |purchase_lines(dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity).
              | Expected:
              |""".trimMargin() + _infoPurchaseLines + """
              |
              | Found:
              |""".trimMargin() + _existingPurchaseLines)
        }
        val _columnsSupplierProductMappings: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSupplierProductMappings.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSupplierProductMappings.put("supplier_identifier", TableInfo.Column("supplier_identifier", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSupplierProductMappings.put("supplier_item_code", TableInfo.Column("supplier_item_code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSupplierProductMappings.put("supplier_desc_pattern", TableInfo.Column("supplier_desc_pattern", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSupplierProductMappings.put("variant_id", TableInfo.Column("variant_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSupplierProductMappings.put("verified_by_owner", TableInfo.Column("verified_by_owner", "INTEGER", true, 0, "1", TableInfo.CREATED_FROM_ENTITY))
        _columnsSupplierProductMappings.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSupplierProductMappings.put("updated_at", TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSupplierProductMappings: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSupplierProductMappings.add(TableInfo.ForeignKey("product_variants", "CASCADE", "NO ACTION", listOf("variant_id"), listOf("id")))
        val _indicesSupplierProductMappings: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesSupplierProductMappings.add(TableInfo.Index("index_supplier_product_mappings_supplier_identifier_supplier_item_code", false, listOf("supplier_identifier", "supplier_item_code"), listOf("ASC", "ASC")))
        _indicesSupplierProductMappings.add(TableInfo.Index("index_supplier_product_mappings_variant_id", false, listOf("variant_id"), listOf("ASC")))
        val _infoSupplierProductMappings: TableInfo = TableInfo("supplier_product_mappings", _columnsSupplierProductMappings, _foreignKeysSupplierProductMappings, _indicesSupplierProductMappings)
        val _existingSupplierProductMappings: TableInfo = read(connection, "supplier_product_mappings")
        if (!_infoSupplierProductMappings.equals(_existingSupplierProductMappings)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |supplier_product_mappings(dev.vignesh.cycleshop.data.local.entity.SupplierProductMappingEntity).
              | Expected:
              |""".trimMargin() + _infoSupplierProductMappings + """
              |
              | Found:
              |""".trimMargin() + _existingSupplierProductMappings)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "brands", "products", "product_variants", "stock_movements", "purchase_invoices", "purchase_lines", "supplier_product_mappings")
  }

  public override fun clearAllTables() {
    super.performClear(true, "stock_movements", "product_variants", "products", "brands", "purchase_invoices", "purchase_lines", "supplier_product_mappings")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ProductDao::class, ProductDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(InventoryDao::class, InventoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PurchaseDao::class, PurchaseDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun productDao(): ProductDao = _productDao.value

  public override fun inventoryDao(): InventoryDao = _inventoryDao.value

  public override fun purchaseDao(): PurchaseDao = _purchaseDao.value
}
