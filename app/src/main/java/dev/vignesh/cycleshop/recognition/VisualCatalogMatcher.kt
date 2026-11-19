package dev.vignesh.cycleshop.recognition

import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.model.ProductType

data class VisualCatalogueVariant(
    val productId: Long,
    val productName: String,
    val brand: String?,
    val category: String,
    val type: ProductType,
    val variant: ProductVariantEntity
)

data class VisualMatchCandidate(
    val item: VisualCatalogueVariant,
    val score: Int,
    val reasons: List<String>,
    val conflicts: List<String>,
    val missingAttributes: List<String>
)

data class VisualCatalogueIssue(val item: VisualCatalogueVariant, val observedBrand: String)

enum class VisualMatchStatus { EXACT, POSSIBLE, NO_MATCH, INSUFFICIENT_EVIDENCE, CATALOGUE_INCONSISTENCY }

data class VisualMatchResult(
    val status: VisualMatchStatus,
    val candidates: List<VisualMatchCandidate> = emptyList(),
    val catalogueIssues: List<VisualCatalogueIssue> = emptyList()
)

/** Ranks validated observations against local catalogue values. Scores are heuristic points. */
class VisualCatalogMatcher(private val catalogue: ReadOnlyVisualCatalogue) {
    suspend fun match(observation: VisualObservation): VisualMatchResult =
        rank(observation, catalogue.activeSnapshot())

    fun rank(observation: VisualObservation, snapshot: VisualCatalogueSnapshot): VisualMatchResult {
        val type = observation.productType.observed()?.let { ProductType.valueOf(it.name) }
            ?: return VisualMatchResult(VisualMatchStatus.INSUFFICIENT_EVIDENCE)
        val query = MatchQuery(observation)
        if (!query.hasEvidence) return VisualMatchResult(VisualMatchStatus.INSUFFICIENT_EVIDENCE)

        val issues = mutableListOf<VisualCatalogueIssue>()
        val ranked = snapshot.activeVariants().filter { it.type == type }
            .mapNotNull { score(it, query, issues) }
            .sortedWith(compareByDescending<Ranked> { it.candidate.score }.thenBy { it.candidate.item.variant.sku }
                .thenBy { it.candidate.item.variant.id }).toList()
        val catalogueIssues = issues.sortedBy { it.item.variant.sku }.take(MAX_RESULTS)
        val best = ranked.firstOrNull() ?: return VisualMatchResult(
            if (catalogueIssues.isEmpty()) VisualMatchStatus.NO_MATCH else VisualMatchStatus.CATALOGUE_INCONSISTENCY,
            catalogueIssues = catalogueIssues
        )
        val next = ranked.getOrNull(1)?.candidate
        val exact = best.exactEvidence &&
            ranked.count { it.candidate.item.productId == best.candidate.item.productId } == 1 &&
            (next == null || best.candidate.score - next.score >= EXACT_MARGIN) && catalogueIssues.isEmpty()
        return VisualMatchResult(
            if (exact) VisualMatchStatus.EXACT else VisualMatchStatus.POSSIBLE,
            ranked.take(MAX_RESULTS).map { it.candidate },
            catalogueIssues
        )
    }

    private fun score(
        item: VisualCatalogueVariant, query: MatchQuery, issues: MutableList<VisualCatalogueIssue>
    ): Ranked? {
        val name = catalogueWords(item.productName)
        val storedBrand = catalogueWords(item.brand.orEmpty())
        val brandMatches = query.brand != null && query.brandWords == storedBrand
        val specs = runCatching { item.variant.specs.attributes }.getOrDefault(emptyMap())
        val matchedCodes = manufacturerCodes(item.productName, item.variant, specs).intersect(query.markings)
        val modelMatches = name.hasPhrase(query.model) && query.model != storedBrand &&
            query.model != query.category && query.model != query.brandWords

        if (query.brand != null && !brandMatches) {
            // A corroborated model plus the visible brand at the start of the Room product
            // name flags a likely catalogue association issue for owner review.
            if (query.brandWords.isNotEmpty() && name.take(query.brandWords.size) == query.brandWords &&
                ((query.observedModel != null && modelMatches) || matchedCodes.isNotEmpty())
            ) issues += VisualCatalogueIssue(item, query.brand)
            return null
        }
        val manufacturerModel = query.observedModel?.let(::observationCodes)?.any(matchedCodes::contains) == true
        if (query.observedModel != null && !modelMatches && !manufacturerModel) return null

        val evidence = Evidence()
        evidence.reasons += when (item.type) {
            ProductType.BICYCLE -> "Bicycle type observed"
            ProductType.SPARE_PART -> "Spare part type observed"
            ProductType.ACCESSORY -> "Accessory type observed"
        }
        if (brandMatches) evidence.match(14, "Visible brand: ${query.brand}")
        else evidence.missing += "Brand not confirmed by the photo"

        val identity = (query.observedModel != null && modelMatches) || matchedCodes.isNotEmpty()
        if (query.observedModel != null && modelMatches) {
            evidence.match(if (matchedCodes.isEmpty()) 46 else 0, "Visible model: ${query.observedModel}")
        } else if (query.observedModel == null && modelMatches) {
            evidence.match(if (matchedCodes.isEmpty()) 10 else 0, "Possible model is consistent with the catalogue")
        }
        if (query.observedModel == null && matchedCodes.isEmpty()) evidence.missing +=
            "Model or manufacturer marking not confirmed"
        if (matchedCodes.isNotEmpty()) evidence.match(60, "Manufacturer marking matches catalogue code")

        val categoryMatches = query.category.isNotEmpty() &&
            (query.category == normalizedCategory(item.category) || name.hasPhrase(query.category))
        if (categoryMatches) {
            val certain = query.observation.category is VisualAttribute.Observed
            evidence.match(if (certain) 20 else 14, if (certain) "Category matches" else "Possible category is consistent")
        } else if (query.observation.category is VisualAttribute.Observed) {
            evidence.conflict(12, "Photo category differs from the catalogue")
        } else {
            evidence.missing += "Category not confirmed"
        }

        for ((kind, value) in query.sizes) {
            when (compareVisualSize(kind, value, item.variant.size, specs)) {
                VisualSizeRelation.MATCH -> evidence.physical(
                    if (kind == VisualSizeKind.WHEEL) 14 else 22, "${kind.label()}: $value"
                )
                VisualSizeRelation.CONFLICT -> return null
                VisualSizeRelation.INCOMPARABLE -> {
                    evidence.hasUnverifiedObservedSize = true
                    evidence.missing += "No comparable ${kind.label().lowercase()} in the catalogue"
                }
            }
        }
        VisualSizeKind.entries.filter { it.productType.name == item.type.name && query.sizes.none { (kind, _) -> it == kind } }
            .forEach { evidence.missing += "${it.label()} not visible" }

        compareColour(query.colour, item.variant.color, evidence)
        for (spec in query.observation.specifications) {
            val stored = storedSpec(spec.kind, specs)?.takeIf(String::isNotBlank)
            val specName = spec.kind.name.lowercase().replace('_', ' ')
            when {
                stored == null -> evidence.missing += "Catalogue $specName unavailable"
                catalogueWords(stored).hasPhrase(catalogueWords(spec.value)) ->
                    evidence.physical(9, "Visible $specName: ${spec.value}")
                else -> evidence.conflict(12, "Visible $specName differs from the catalogue")
            }
        }

        if (evidence.score < MIN_SCORE ||
            !(identity || (categoryMatches && evidence.hasPhysicalMatch) ||
                (modelMatches && evidence.hasPhysicalMatch && brandMatches))
        ) return null

        val candidate = VisualMatchCandidate(item, evidence.score, evidence.reasons, evidence.conflicts, evidence.missing)
        return Ranked(candidate, identity && (brandMatches || matchedCodes.isNotEmpty()) &&
            evidence.conflicts.isEmpty() && !evidence.hasUnverifiedObservedSize)
    }

    private fun compareColour(observed: Set<String>, stored: String?, evidence: Evidence) {
        val actual = normalizedColours(stored.orEmpty())
        when {
            observed.isEmpty() -> evidence.missing += "Colour not confirmed by the photo"
            actual.isEmpty() -> evidence.missing += "Catalogue colour unavailable"
            observed == actual -> evidence.physical(10, "Colour matches: $stored")
            observed.intersect(actual).isEmpty() -> evidence.conflict(20, "Visible colour differs from $stored")
            else -> {
                evidence.physical(5, "Some colours match: $stored")
                if (observed.all(actual::contains)) evidence.missing += "Full colour combination not visible"
                else evidence.conflict(8, "Some visible colours differ from the catalogue")
            }
        }
    }

    private class MatchQuery(val observation: VisualObservation) {
        val brand = observation.brand.observed()
        val brandWords = catalogueWords(brand.orEmpty())
        val observedModel = observation.likelyModel.observed()
        val model = observation.likelyModel.possible()?.let(::catalogueWords).orEmpty()
        val category = observation.category.possible()?.let(::normalizedCategory).orEmpty()
        val markings = observation.markings.flatMap(::observationCodes).toSet()
        val sizes = observation.sizes.mapNotNull { size -> size.attribute.observed()?.let { size.kind to it } }
        val colour = observation.colour.observed()?.let(::normalizedColours).orEmpty()
        private val hasPhysicalDetail = sizes.isNotEmpty() || colour.isNotEmpty() || observation.specifications.isNotEmpty()
        val hasEvidence = model.isNotEmpty() || markings.isNotEmpty() || (category.isNotEmpty() && hasPhysicalDetail)
    }

    private data class Ranked(val candidate: VisualMatchCandidate, val exactEvidence: Boolean)

    private class Evidence {
        var score = 0
        var hasPhysicalMatch = false
        var hasUnverifiedObservedSize = false
        val reasons = mutableListOf<String>()
        val conflicts = mutableListOf<String>()
        val missing = mutableListOf<String>()

        fun match(points: Int, message: String) {
            score += points
            reasons += message
        }

        fun physical(points: Int, message: String) {
            match(points, message)
            hasPhysicalMatch = true
        }

        fun conflict(points: Int, message: String) {
            score -= points
            conflicts += message
        }
    }

    private companion object {
        const val MIN_SCORE = 24
        const val EXACT_MARGIN = 12
        const val MAX_RESULTS = 5
    }
}

private fun <T> VisualAttribute<T>.observed(): T? = (this as? VisualAttribute.Observed)?.value
private fun <T> VisualAttribute<T>.possible(): T? = when (this) {
    is VisualAttribute.Observed -> value
    is VisualAttribute.Uncertain -> possibleValue
    is VisualAttribute.Unknown -> null
}

private fun VisualSizeKind.label() = when (this) {
    VisualSizeKind.FRAME -> "Frame size"
    VisualSizeKind.WHEEL -> "Wheel size"
    VisualSizeKind.PART_DIMENSION -> "Part dimension"
    VisualSizeKind.ACCESSORY_DIMENSION -> "Accessory dimension"
}
