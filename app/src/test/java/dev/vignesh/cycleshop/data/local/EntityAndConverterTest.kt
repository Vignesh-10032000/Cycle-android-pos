package dev.vignesh.cycleshop.data.local

import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.entity.StockMovementEntity
import dev.vignesh.cycleshop.data.local.model.MovementType
import dev.vignesh.cycleshop.data.local.model.ProductSpecs
import dev.vignesh.cycleshop.data.local.model.ProductType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EntityAndConverterTest {

    private val converters = Converters()

    @Test
    fun brandEntity_validName_succeeds() {
        val brand = BrandEntity(name = "Hero")
        assertEquals("Hero", brand.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun brandEntity_blankName_throwsException() {
        BrandEntity(name = "   ")
    }

    @Test
    fun productVariantEntity_validFields_succeeds() {
        val variant = ProductVariantEntity(
            productId = 1L,
            sku = "VGL-HERO-275-BLK",
            supplierCode = "SUP-101",
            size = "27.5T",
            color = "Matte Black",
            mrpPaise = 1500000L, // Rs. 15,000.00
            costPricePaise = 1100000L, // Rs. 11,000.00
            sellingPricePaise = 1350000L, // Rs. 13,500.00
            stockQuantity = 5
        )
        assertEquals("VGL-HERO-275-BLK", variant.sku)
        assertEquals(1350000L, variant.sellingPricePaise)
        assertEquals(5, variant.stockQuantity)
    }

    @Test(expected = IllegalArgumentException::class)
    fun productVariantEntity_blankSku_throwsException() {
        ProductVariantEntity(
            productId = 1L,
            sku = "",
            mrpPaise = 100000L,
            sellingPricePaise = 90000L,
            stockQuantity = 1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun productVariantEntity_negativeStock_throwsException() {
        ProductVariantEntity(
            productId = 1L,
            sku = "VGL-001",
            mrpPaise = 100000L,
            sellingPricePaise = 90000L,
            stockQuantity = -1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun productVariantEntity_negativePrice_throwsException() {
        ProductVariantEntity(
            productId = 1L,
            sku = "VGL-001",
            mrpPaise = -100L,
            sellingPricePaise = 90000L,
            stockQuantity = 0
        )
    }

    @Test
    fun stockMovementEntity_validMovements_succeed() {
        val purchase = StockMovementEntity(
            variantId = 1L,
            delta = 10,
            movementType = MovementType.PURCHASE,
            unitPricePaise = 1100000L,
            referenceId = "INV-2026-001"
        )
        assertEquals(10, purchase.delta)
        assertEquals(MovementType.PURCHASE, purchase.movementType)

        val sale = StockMovementEntity(
            variantId = 1L,
            delta = -2,
            movementType = MovementType.SALE,
            unitPricePaise = 1350000L,
            referenceId = "BILL-1004"
        )
        assertEquals(-2, sale.delta)
        assertEquals(MovementType.SALE, sale.movementType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stockMovementEntity_zeroDelta_throwsException() {
        StockMovementEntity(
            variantId = 1L,
            delta = 0,
            movementType = MovementType.ADJUSTMENT
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun stockMovementEntity_negativePurchaseDelta_throwsException() {
        StockMovementEntity(
            variantId = 1L,
            delta = -5,
            movementType = MovementType.PURCHASE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun stockMovementEntity_positiveSaleDelta_throwsException() {
        StockMovementEntity(
            variantId = 1L,
            delta = 3,
            movementType = MovementType.SALE
        )
    }

    @Test
    fun productSpecs_serializationRoundTrip_succeeds() {
        val specs = ProductSpecs(
            schemaVersion = 1,
            attributes = mapOf(
                "frame_size" to "17 inch",
                "wheel_size" to "27.5T",
                "gears" to "21 Speed Shimano",
                "brake_type" to "Dual Disc"
            )
        )
        val json = specs.toJson()
        val parsed = ProductSpecs.fromJson(json)

        assertEquals(1, parsed.schemaVersion)
        assertEquals("17 inch", parsed.attributes["frame_size"])
        assertEquals("21 Speed Shimano", parsed.attributes["gears"])
    }

    @Test
    fun productSpecs_nullOrEmptyJson_returnsDefault() {
        val emptySpecs = ProductSpecs.fromJson(null)
        assertEquals(1, emptySpecs.schemaVersion)
        assertTrue(emptySpecs.attributes.isEmpty())
    }

    @Test
    fun typeConverters_roundTrip_succeeds() {
        // ProductType converter
        val typeDb = converters.fromProductType(ProductType.SPARE_PART)
        assertEquals("SPARE_PART", typeDb)
        assertEquals(ProductType.SPARE_PART, converters.toProductType(typeDb))

        // MovementType converter
        val movementDb = converters.fromMovementType(MovementType.PURCHASE)
        assertEquals("PURCHASE", movementDb)
        assertEquals(MovementType.PURCHASE, converters.toMovementType(movementDb))

        // ProductSpecs converter
        val specs = ProductSpecs(schemaVersion = 1, attributes = mapOf("color" to "Teal"))
        val specsJson = converters.fromProductSpecs(specs)
        assertNotNull(specsJson)
        val deserializedSpecs = converters.toProductSpecs(specsJson)
        assertEquals("Teal", deserializedSpecs?.attributes?.get("color"))

        // Phase 3 Step 3A converters
        val statusDb = converters.fromPurchaseInvoiceStatus(dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus.CONFIRMED_STAGED)
        assertEquals("CONFIRMED_STAGED", statusDb)
        assertEquals(dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus.CONFIRMED_STAGED, converters.toPurchaseInvoiceStatus(statusDb))

        val lineStatusDb = converters.fromLineReviewStatus(dev.vignesh.cycleshop.data.local.model.LineReviewStatus.VERIFIED_MATCH)
        assertEquals("VERIFIED_MATCH", lineStatusDb)
        assertEquals(dev.vignesh.cycleshop.data.local.model.LineReviewStatus.VERIFIED_MATCH, converters.toLineReviewStatus(lineStatusDb))

        val unitDb = converters.fromInventoryUnit(dev.vignesh.cycleshop.data.local.model.InventoryUnit.GROSS)
        assertEquals("GROSS", unitDb)
        assertEquals(dev.vignesh.cycleshop.data.local.model.InventoryUnit.GROSS, converters.toInventoryUnit(unitDb))
    }

    @Test
    fun purchaseInvoiceEntity_validFields_succeeds() {
        val invoice = dev.vignesh.cycleshop.data.local.entity.PurchaseInvoiceEntity(
            supplierName = "MBW Industries",
            supplierGstin = "03AABCM1234F1Z1",
            invoiceNumber = "466",
            invoiceDate = "18/10/2026",
            totalAmountPaise = 7717400L,
            totalQuantity = 126.0,
            status = dev.vignesh.cycleshop.data.local.model.PurchaseInvoiceStatus.CONFIRMED_STAGED
        )
        assertEquals("MBW Industries", invoice.supplierName)
        assertEquals("466", invoice.invoiceNumber)
        assertEquals(126.0, invoice.totalQuantity ?: 0.0, 0.001)
    }

    @Test
    fun purchaseLineEntity_validFields_succeeds() {
        val line = dev.vignesh.cycleshop.data.local.entity.PurchaseLineEntity(
            invoiceId = 1L,
            lineNumber = 1,
            variantId = 10L,
            supplierItemCode = "BIC-20-ELVIS",
            rawDescription = "BIC 20 ELVIS PRO DD FS MELBLUEORG SKD",
            hsnCode = "87120010",
            rawQuantity = 1.0,
            rawUnit = "PC",
            inventoryUnit = dev.vignesh.cycleshop.data.local.model.InventoryUnit.PIECE,
            unitConversionFactor = 1,
            effectiveQuantityBase = 1,
            rawUnitPricePaise = 580000L,
            rawLineAmountPaise = 580000L,
            reviewStatus = dev.vignesh.cycleshop.data.local.model.LineReviewStatus.VERIFIED_MATCH
        )
        assertEquals(1, line.effectiveQuantityBase)
        assertEquals(dev.vignesh.cycleshop.data.local.model.LineReviewStatus.VERIFIED_MATCH, line.reviewStatus)
    }

    @Test
    fun supplierProductMappingEntity_validFields_succeeds() {
        val mapping = dev.vignesh.cycleshop.data.local.entity.SupplierProductMappingEntity(
            supplierIdentifier = "03AABCS9876E1Z5",
            supplierItemCode = "BIC-20-ELVIS",
            supplierDescPattern = "BIC 20 ELVIS PRO",
            variantId = 10L,
            verifiedByOwner = true
        )
        assertEquals("03AABCS9876E1Z5", mapping.supplierIdentifier)
        assertEquals(10L, mapping.variantId)
        assertTrue(mapping.verifiedByOwner)
    }

    @Test(expected = IllegalArgumentException::class)
    fun productSpecs_malformedJson_throwsIllegalArgumentException() {
        val brokenJson = "{broken: json, not valid at all"
        ProductSpecs.fromJson(brokenJson)
    }

    @Test(expected = IllegalArgumentException::class)
    fun productSpecs_unsupportedSchemaVersion_throwsIllegalArgumentException() {
        val futureJson = """{"schema_version": 999, "attributes": {"wheel": "29T"}}"""
        ProductSpecs.fromJson(futureJson)
    }

    @Test(expected = IllegalArgumentException::class)
    fun productSpecs_missingSchemaVersion_throwsIllegalArgumentException() {
        val invalidJson = """{"attributes": {"wheel": "29T"}}"""
        ProductSpecs.fromJson(invalidJson)
    }

    @Test
    fun productVariantEntity_specsAccessor_returnsParsedSpecs() {
        val specs = ProductSpecs(schemaVersion = 1, attributes = mapOf("brake" to "Disc"))
        val variant = ProductVariantEntity(
            productId = 1L,
            sku = "SKU-DISC-01",
            mrpPaise = 2000000L,
            sellingPricePaise = 1800000L,
            stockQuantity = 2,
            specsJson = specs.toJson()
        )
        assertEquals(1, variant.specs.schemaVersion)
        assertEquals("Disc", variant.specs.attributes["brake"])
    }
}

