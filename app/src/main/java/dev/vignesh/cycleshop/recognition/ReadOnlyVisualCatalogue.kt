package dev.vignesh.cycleshop.recognition

import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.model.ProductWithVariants

interface ReadOnlyVisualCatalogue {
    suspend fun activeSnapshot(): VisualCatalogueSnapshot
}

/** Existing catalogue values for matching. Stock comes from the stored variant records. */
data class VisualCatalogueSnapshot(
    val brands: List<BrandEntity>,
    val products: List<ProductWithVariants>
)
