package dev.vignesh.cycleshop.ui.invoice

import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants
import java.util.Locale

/**
 * Local deterministic matching engine that matches extracted invoice lines
 * against existing Room catalog products and variants.
 *
 * Matching Strategy:
 * 1. Verified SKU or supplier code mapping (Confidence: 1.0)
 * 2. Normalized brand and product description matching (Confidence: 0.70 - 0.90)
 * 3. Size, color, and technical specification matching (+0.10 - +0.20)
 * 4. Candidate search & ranking for uncertain matches
 *
 * Strict boundary:
 * - Does NOT assume unverified supplier codes uniquely identify a product.
 * - Displays multiple ranked candidates when match is uncertain.
 * - Zero Gemini API / external network calls.
 */
class ProductMatchingEngine {

    /**
     * Matches a parsed line item against catalog items and returns ranked candidates.
     */
    fun matchItem(
        item: ParsedInvoiceItem,
        productsWithVariants: List<ProductWithVariants>,
        brands: List<BrandEntity> = emptyList()
    ): List<ProductMatchCandidate> {
        val brandMap = brands.associateBy { it.id }
        val candidates = mutableListOf<ProductMatchCandidate>()

        val rawDesc = item.description.uppercase(Locale.ROOT)
        val rawItemCode = item.itemCode?.uppercase(Locale.ROOT)

        for (pwv in productsWithVariants) {
            val product = pwv.product
            val brand = brandMap[product.brandId]?.name ?: "Generic"
            val prodNameUpper = product.name.uppercase(Locale.ROOT)
            val brandUpper = brand.uppercase(Locale.ROOT)

            for (variant in pwv.variants) {
                val skuUpper = variant.sku.uppercase(Locale.ROOT)
                val supplierCodeUpper = variant.supplierCode?.uppercase(Locale.ROOT)
                val sizeUpper = variant.size?.uppercase(Locale.ROOT)
                val colorUpper = variant.color?.uppercase(Locale.ROOT)

                var score = 0.0f
                val reasons = mutableListOf<String>()

                // ── Tier 1: Verified SKU or Supplier Code Match ────────────
                if (rawItemCode != null && supplierCodeUpper != null && rawItemCode == supplierCodeUpper) {
                    score = 1.0f
                    reasons.add("Exact Supplier Code: $supplierCodeUpper")
                } else if (rawDesc.contains(skuUpper) || (rawItemCode != null && rawItemCode == skuUpper)) {
                    score = 0.98f
                    reasons.add("Exact SKU: ${variant.sku}")
                } else {
                    // ── Tier 2: Normalized Brand + Product Description ─────
                    val brandInDesc = rawDesc.contains(brandUpper)
                    if (brandInDesc) {
                        score += 0.30f
                        reasons.add("Brand: $brand")
                    }

                    // Model / Name keyword token matching
                    val nameTokens = prodNameUpper.split(Regex("""[\s\-_/]+""")).filter { it.length > 2 }
                    val matchedTokens = nameTokens.filter { rawDesc.contains(it) }
                    if (matchedTokens.isNotEmpty()) {
                        val tokenScore = (matchedTokens.size.toFloat() / nameTokens.size) * 0.30f
                        score += tokenScore
                        reasons.add("Model: ${matchedTokens.joinToString(" ")}")
                    }

                    // ── Tier 3: Size, Color & Spec Attributes ──────────────
                    if (sizeUpper != null && rawDesc.contains(sizeUpper)) {
                        score += 0.15f
                        reasons.add("Size: ${variant.size}")
                    }

                    if (colorUpper != null && rawDesc.contains(colorUpper)) {
                        score += 0.10f
                        reasons.add("Color: ${variant.color}")
                    }

                    // Component keywords (Spokes, Grease, Chain, Brake, Derailleur)
                    val componentKeywords = listOf("SPOKES", "GREASE", "CHAIN", "BRAKE", "DERAILLEUR")
                    val matchedComp = componentKeywords.filter { rawDesc.contains(it) && prodNameUpper.contains(it) }
                    if (matchedComp.isNotEmpty()) {
                        score += 0.05f
                        reasons.add("Component: ${matchedComp.joinToString()}")
                    }
                }

                // Cap score at 1.0
                score = score.coerceIn(0.0f, 1.0f)

                if (score >= 0.35f) {
                    candidates.add(
                        ProductMatchCandidate(
                            variantId = variant.id,
                            productId = product.id,
                            productName = product.name,
                            brandName = brand,
                            variantSku = variant.sku,
                            supplierCode = variant.supplierCode,
                            variantSize = variant.size,
                            variantColor = variant.color,
                            costPricePaise = variant.costPricePaise,
                            sellingPricePaise = variant.sellingPricePaise,
                            confidenceScore = score,
                            matchReason = reasons.joinToString(" • ")
                        )
                    )
                }
            }
        }

        // Return candidates sorted by confidence score descending
        return candidates.sortedByDescending { it.confidenceScore }
    }

    /**
     * Matches all items in a [ParsedInvoice] and returns a new invoice with populated candidates.
     */
    fun matchInvoice(
        invoice: ParsedInvoice,
        productsWithVariants: List<ProductWithVariants>,
        brands: List<BrandEntity> = emptyList()
    ): ParsedInvoice {
        val updatedItems = invoice.items.map { item ->
            val candidates = matchItem(item, productsWithVariants, brands)
            item.copy(suggestedCandidates = candidates)
        }
        return invoice.copy(items = updatedItems)
    }
}
