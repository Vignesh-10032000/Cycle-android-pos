package dev.vignesh.cycleshop.recognition

import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.data.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class RoomVisualCatalogue(repository: InventoryRepository) : ReadOnlyVisualCatalogue {
    val snapshots: Flow<VisualCatalogueSnapshot> =
        combine(repository.allBrands, repository.activeProductsWithVariants, ::VisualCatalogueSnapshot)

    override suspend fun activeSnapshot(): VisualCatalogueSnapshot = snapshots.first()
}

internal fun VisualCatalogueSnapshot.activeVariants(): Sequence<VisualCatalogueVariant> {
    val brandNames = brands.associate { it.id to it.name }
    return products.asSequence().filter { it.product.isActive }.flatMap { (product, variants) ->
        variants.asSequence().filter { it.productId == product.id }.map { variant ->
            VisualCatalogueVariant(
                product.id, product.name, brandNames[product.brandId], product.category, product.type, variant
            )
        }
    }
}

/** Manual search accepts shop identifiers and shows only rows that satisfy the typed query. */
internal fun VisualCatalogueSnapshot.manualSearch(
    query: String, types: Set<ProductType>, limit: Int = 30
): List<VisualCatalogueVariant> {
    val words = catalogueWords(query)
    if (words.isEmpty()) return emptyList()
    return activeVariants().filter { it.type in types }.filter { item ->
        val searchable = listOf(
            item.productName, item.brand, item.category, item.variant.sku,
            item.variant.supplierCode, item.variant.size, item.variant.color
        ).filterNotNull().flatMap(::catalogueWords)
        words.all { word -> searchable.any { it.startsWith(word) } }
    }.sortedWith(compareBy<VisualCatalogueVariant> { it.productName }.thenBy { it.variant.sku })
        .take(limit).toList()
}
