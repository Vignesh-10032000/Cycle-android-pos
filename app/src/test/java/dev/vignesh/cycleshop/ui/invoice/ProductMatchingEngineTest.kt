package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM Unit tests for [ProductMatchingEngine].
 *
 * Verifies the 4-tier local matching engine:
 * 1. Tier 1: Verified SKU and supplier code mapping (Confidence: 1.0 or 0.98)
 * 2. Tier 2: Normalized Brand + Product Description keyword matching
 * 3. Tier 3: Size, Color, and Technical Spec matching bonuses
 * 4. Tier 4: Candidate ranking & uncertain match fallback
 */
class ProductMatchingEngineTest {

    private val engine = ProductMatchingEngine()

    private val heroBrand = BrandEntity(id = 1L, name = "Hero")
    private val mbwBrand = BrandEntity(id = 2L, name = "MBW")
    private val firefoxBrand = BrandEntity(id = 3L, name = "Firefox")

    private val catalog = listOf(
        // Product 1: Hero Sprint Pro Bicycle
        ProductWithVariants(
            product = ProductEntity(
                id = 101L,
                brandId = 1L,
                name = "Sprint Pro 29T",
                category = "Bicycle",
                type = ProductType.BICYCLE
            ),
            variants = listOf(
                ProductVariantEntity(
                    id = 1001L,
                    productId = 101L,
                    sku = "HERO-SPR-29-BLK",
                    supplierCode = "SUP-H29-BLK",
                    size = "29T",
                    color = "Black",
                    mrpPaise = 1800000L,
                    costPricePaise = 1400000L,
                    sellingPricePaise = 1650000L,
                    stockQuantity = 5
                ),
                ProductVariantEntity(
                    id = 1002L,
                    productId = 101L,
                    sku = "HERO-SPR-29-RED",
                    supplierCode = "SUP-H29-RED",
                    size = "29T",
                    color = "Red",
                    mrpPaise = 1800000L,
                    costPricePaise = 1400000L,
                    sellingPricePaise = 1650000L,
                    stockQuantity = 3
                )
            )
        ),
        // Product 2: MBW Spokes
        ProductWithVariants(
            product = ProductEntity(
                id = 201L,
                brandId = 2L,
                name = "Cycle Spokes Steel",
                category = "Spares",
                type = ProductType.SPARE_PART
            ),
            variants = listOf(
                ProductVariantEntity(
                    id = 2001L,
                    productId = 201L,
                    sku = "MBW-SPK-14-12",
                    supplierCode = null, // M.STEEL has no manufacturer supplier code on invoice
                    size = "12-1-75",
                    color = "Steel",
                    mrpPaise = 18000L,
                    costPricePaise = 14000L,
                    sellingPricePaise = 16000L,
                    stockQuantity = 20
                ),
                ProductVariantEntity(
                    id = 2002L,
                    productId = 201L,
                    sku = "MBW-SPK-14-24-BLK",
                    supplierCode = null, // M.STEEL has no manufacturer supplier code on invoice
                    size = "24-1-95",
                    color = "Black",
                    mrpPaise = 25000L,
                    costPricePaise = 20000L,
                    sellingPricePaise = 23000L,
                    stockQuantity = 15
                )
            )
        ),
        // Product 3: MBW Red Gel Grease
        ProductWithVariants(
            product = ProductEntity(
                id = 301L,
                brandId = 2L,
                name = "Red Gel Grease",
                category = "Lubricants",
                type = ProductType.ACCESSORY
            ),
            variants = listOf(
                ProductVariantEntity(
                    id = 3001L,
                    productId = 301L,
                    sku = "MBW-GRS-250G",
                    supplierCode = null, // M.STEEL has no manufacturer supplier code on invoice
                    size = "250gm",
                    color = "Red",
                    mrpPaise = 8000L,
                    costPricePaise = 5500L,
                    sellingPricePaise = 7000L,
                    stockQuantity = 50
                )
            )
        ),
        // Product 4: Stryder Elvis Pro Bicycle (Real manufacturer supplier code)
        ProductWithVariants(
            product = ProductEntity(
                id = 401L,
                brandId = 4L,
                name = "Elvis Pro 20T",
                category = "Bicycle",
                type = ProductType.BICYCLE
            ),
            variants = listOf(
                ProductVariantEntity(
                    id = 4001L,
                    productId = 401L,
                    sku = "STRY-ELVIS-20-BLU",
                    supplierCode = "6005008229", // Real manufacturer supplier code printed on Stryder invoice
                    size = "20T",
                    color = "MelBlue/Orange",
                    mrpPaise = 750000L,
                    costPricePaise = 580000L,
                    sellingPricePaise = 690000L,
                    stockQuantity = 2
                )
            )
        )
    )

    private val stryderBrand = BrandEntity(id = 4L, name = "Stryder")
    private val brands = listOf(heroBrand, mbwBrand, firefoxBrand, stryderBrand)

    // ── Tier 1: Exact SKU & Supplier Code Matching ─────────────────────────

    @Test
    fun `Tier 1 - exact real manufacturer supplier code match yields 1_0 confidence`() {
        val invoiceItem = ParsedInvoiceItem(
            lineNumber = 1,
            itemCode = "6005008229", // Real manufacturer supplier code from Stryder invoice
            description = "BIC 20 ELVIS PRO DD FS MELBLUEORG SKD",
            hsnCode = "87120010",
            quantity = 1.0,
            unit = "PC",
            unitRatePaise = 580000L
        )

        val candidates = engine.matchItem(invoiceItem, catalog, brands)

        assertFalse("Expected candidates to be found", candidates.isEmpty())
        val topCandidate = candidates.first()
        assertEquals("STRY-ELVIS-20-BLU", topCandidate.variantSku)
        assertEquals(1.0f, topCandidate.confidenceScore, 0.001f)
        assertEquals(MatchConfidence.EXACT_SKU, topCandidate.confidence)
        assertTrue(topCandidate.matchReason.contains("Exact Supplier Code: 6005008229"))
    }

    @Test
    fun `MSteel invoice without supplier code matches via Tier 2 and 3 description heuristics rather than synthetic supplier code`() {
        val invoiceItem = ParsedInvoiceItem(
            lineNumber = 1,
            itemCode = null, // M.STEEL invoice has no supplier code column
            description = "CYCLE SPOKES WITH NIPPLES 14-STEEL - 12-1-75 MBW",
            hsnCode = "87149990",
            quantity = 5.0,
            unit = "Gross",
            unitRatePaise = 14000L
        )

        val candidates = engine.matchItem(invoiceItem, catalog, brands)

        assertFalse("Expected candidates to be found", candidates.isEmpty())
        val topCandidate = candidates.first()
        assertEquals("MBW-SPK-14-12", topCandidate.variantSku)
        // Must match via Brand + Model description rather than synthetic supplier code
        assertFalse("Must NOT claim exact supplier code match", topCandidate.matchReason.contains("Exact Supplier Code"))
        assertTrue("Must match via Brand", topCandidate.matchReason.contains("Brand: MBW"))
    }

    @Test
    fun `Tier 1 - exact SKU in description yields 0_98 confidence`() {
        val invoiceItem = ParsedInvoiceItem(
            lineNumber = 1,
            description = "Bicycle HERO-SPR-29-BLK Matt Black 29T",
            quantity = 2.0
        )

        val candidates = engine.matchItem(invoiceItem, catalog, brands)

        assertFalse("Expected candidates to be found", candidates.isEmpty())
        val topCandidate = candidates.first()
        assertEquals("HERO-SPR-29-BLK", topCandidate.variantSku)
        assertEquals(0.98f, topCandidate.confidenceScore, 0.001f)
        assertEquals(MatchConfidence.EXACT_SKU, topCandidate.confidence)
    }

    // ── Tier 2 & 3: Brand, Description, Size & Color Matching ───────────────

    @Test
    fun `Tier 2 and 3 - normalized brand, description, size and color match accurately`() {
        val invoiceItem = ParsedInvoiceItem(
            lineNumber = 6,
            description = "CYCLE SPOKES WITH NIPPLES 14-STEEL - 24-1-95 BLACK MBW",
            hsnCode = "87149990",
            quantity = 5.0
        )

        val candidates = engine.matchItem(invoiceItem, catalog, brands)

        assertFalse(candidates.isEmpty())
        val topMatch = candidates.first()
        assertEquals("MBW-SPK-14-24-BLK", topMatch.variantSku)
        assertTrue("Expected score >= 0.70", topMatch.confidenceScore >= 0.70f)
        assertTrue("Expected brand match reason", topMatch.matchReason.contains("Brand: MBW"))
        assertTrue("Expected color match reason", topMatch.matchReason.contains("Color: Black"))
        assertTrue("Expected size match reason", topMatch.matchReason.contains("Size: 24-1-95"))
    }

    @Test
    fun `Tier 2 - grease description matches Red Gel Grease variant`() {
        val invoiceItem = ParsedInvoiceItem(
            lineNumber = 9,
            description = "RED GEL GREASE 250GM MBW",
            hsnCode = "271019",
            quantity = 40.0
        )

        val candidates = engine.matchItem(invoiceItem, catalog, brands)

        assertFalse(candidates.isEmpty())
        val top = candidates.first()
        assertEquals("MBW-GRS-250G", top.variantSku)
        assertTrue("Expected high confidence for grease", top.confidenceScore >= 0.75f)
    }

    // ── Candidate Ranking and Fallbacks ─────────────────────────────────────

    @Test
    fun `candidates are ranked by confidence score in descending order`() {
        // Line description matches Hero Sprint Pro 29T, with color Red
        val invoiceItem = ParsedInvoiceItem(
            lineNumber = 1,
            description = "Hero Sprint Pro 29T Red",
            quantity = 1.0
        )

        val candidates = engine.matchItem(invoiceItem, catalog, brands)

        assertTrue(candidates.size >= 2)
        // Red variant should rank higher than Black variant
        assertEquals("HERO-SPR-29-RED", candidates[0].variantSku)
        assertTrue(candidates[0].confidenceScore > candidates[1].confidenceScore)
    }

    @Test
    fun `unrelated product description returns empty candidates list`() {
        val invoiceItem = ParsedInvoiceItem(
            lineNumber = 1,
            description = "COMPLETELY UNRELATED ELECTRONIC GADGET 100V",
            quantity = 1.0
        )

        val candidates = engine.matchItem(invoiceItem, catalog, brands)
        assertTrue("Expected no false positive candidates", candidates.isEmpty())
    }
}
