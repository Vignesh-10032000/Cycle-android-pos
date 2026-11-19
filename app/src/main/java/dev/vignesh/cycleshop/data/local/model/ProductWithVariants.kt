package dev.vignesh.cycleshop.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity

/**
 * 1-to-N relation representing a product and all of its concrete variants.
 */
data class ProductWithVariants(
    @Embedded
    val product: ProductEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "product_id"
    )
    val variants: List<ProductVariantEntity> = emptyList()
)
