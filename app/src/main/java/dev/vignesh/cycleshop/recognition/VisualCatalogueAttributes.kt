package dev.vignesh.cycleshop.recognition

import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import java.text.Normalizer
import java.util.Locale

private val word = Regex("""[\p{L}\p{N}]+""")
private val combiningMark = Regex("""\p{M}+""")
private val code = Regex("""[\p{L}\p{N}]+(?:[-_][\p{L}\p{N}]+)*""")

internal fun catalogueWords(text: String): List<String> = word.findAll(foldCatalogueText(text))
    .map { it.value }.toList()

private fun foldCatalogueText(text: String) = Normalizer.normalize(text, Normalizer.Form.NFKD)
    .replace(combiningMark, "").lowercase(Locale.ROOT)

internal fun List<String>.hasPhrase(phrase: List<String>) =
    phrase.isNotEmpty() && windowed(phrase.size).any { it == phrase }

internal fun observationCodes(text: String): Set<String> = code.findAll(
    foldCatalogueText(text).replace(Regex("[‐‑–—]"), "-")
).map { it.value.filter(Char::isLetterOrDigit) }
    .filter { it.any(Char::isDigit) && it.any(Char::isLetter) }.toSet()

internal fun manufacturerCodes(name: String, variant: ProductVariantEntity, specs: Map<String, String>): Set<String> {
    val trusted = listOf("manufacturer_code", "manufacturer_part_number", "mpn", "model_code")
        .mapNotNull(specs::get) + Regex("""\(([^)]+)\)""").findAll(name).map { it.groupValues[1] }
    return trusted.flatMap(::observationCodes).toSet() -
        observationCodes(variant.sku) - observationCodes(variant.supplierCode.orEmpty())
}

internal fun normalizedCategory(value: String): List<String> {
    val words = catalogueWords(value)
    return when (words.joinToString(" ")) {
        "mountain bike", "mountain bicycle", "mtb" -> listOf("mtb")
        "hybrid bike", "hybrid bicycle" -> listOf("hybrid")
        "city bike", "city bicycle" -> listOf("city")
        else -> words
    }
}

private val colourAliases = mapOf(
    "gray" to "grey", "charcoal" to "grey", "silver" to "silver",
    "cobalt" to "blue", "navy" to "blue", "maroon" to "red", "beige" to "beige"
)
private val baseColours = setOf(
    "black", "white", "grey", "red", "orange", "yellow", "green", "blue", "purple", "pink", "brown", "gold"
)
internal fun normalizedColours(value: String): Set<String> = value.split(Regex("""[/+,;&]|\band\b""", RegexOption.IGNORE_CASE))
    .flatMap { group ->
        val words = catalogueWords(group)
        words.mapNotNull { colourAliases[it] ?: it.takeIf(baseColours::contains) }
            .ifEmpty { listOf(words.joinToString(" ")).filter(String::isNotEmpty) }
    }.toSet()

internal enum class VisualSizeRelation { MATCH, CONFLICT, INCOMPARABLE }
private enum class SizeScale { WIDTH_MM, FRAME_LABEL, WHEEL_NOMINAL_MM, WHEEL_C }
private data class SizeValue(val scale: SizeScale, val units: String)
private const val NUMBER = """[0-9]+(?:\.[0-9]+)?"""
private val dimension = Regex("""(?<![\p{L}\p{N}/.])($NUMBER)\s*(mm|cm|inches|inch|in|["″])(?![\p{L}\p{N}])""")
private val labelledFrame = Regex("""(?<![\p{L}\p{N}.])($NUMBER)\s+frame\b|\bframe\s+($NUMBER)(?![\p{L}\p{N}.])""")
private val frameLabel = Regex("""(?<![\p{L}\p{N}])(xxs|xs|s|m|l|xl|xxl|small|medium|large)(?![\p{L}\p{N}])""")
private val wheelInches = Regex("""(?<![\p{L}\p{N}.])($NUMBER)\s*t\b""")
private val wheelC = Regex("""(?<![\p{L}\p{N}.])([0-9]{3})\s*c\b""")
private val wheelWidth = Regex("""^\s*($NUMBER)\s*[x×]\s*$NUMBER\s*(c\b)?""")

internal fun compareVisualSize(
    kind: VisualSizeKind, observed: String, variantSize: String?, specs: Map<String, String>
): VisualSizeRelation {
    val printed = sizeValues(kind, observed, typed = true)
    // A labelled variant size takes precedence over descriptive specs shared by multiple variants.
    val variant = sizeValues(
        kind, variantSize.orEmpty(),
        typed = kind == VisualSizeKind.PART_DIMENSION || kind == VisualSizeKind.ACCESSORY_DIMENSION
    )
    val fallback = when (kind) {
        VisualSizeKind.FRAME -> listOfNotNull(specs["frame_size"], specs["frame"])
        VisualSizeKind.WHEEL -> listOfNotNull(specs["wheel_size"], specs["wheel"])
        else -> listOfNotNull(specs["length"], specs["dimensions"], specs["size"])
    }
    val stored = variant.ifEmpty { fallback.flatMap { sizeValues(kind, it, typed = true) }.toSet() }
    val comparable = stored.filter { value -> printed.any { it.scale == value.scale } }
    return when {
        comparable.isEmpty() -> VisualSizeRelation.INCOMPARABLE
        comparable.all { it in printed } -> VisualSizeRelation.MATCH
        else -> VisualSizeRelation.CONFLICT
    }
}

private fun sizeValues(kind: VisualSizeKind, text: String, typed: Boolean): Set<SizeValue> {
    val value = foldCatalogueText(text)
    return when (kind) {
        VisualSizeKind.FRAME -> value.split('/').filterNot { wheelInches.containsMatchIn(it) || wheelC.containsMatchIn(it) }
            .flatMap { segment ->
                val explicitlyFrame = Regex("""\bframe\b""").containsMatchIn(segment)
                val labels = frameLabel.findAll(segment).map { SizeValue(SizeScale.FRAME_LABEL, frameLetter(it.value)) }
                val labelled = labelledFrame.findAll(segment).filterNot { hit ->
                    // If a unit is present, the dimension parser owns the value.
                    dimension.containsMatchIn(hit.value) ||
                        dimension.findAll(segment).any { it.range.first in hit.range }
                }.map { mm(it.groupValues[1].ifEmpty { it.groupValues[2] }, "inch", SizeScale.WIDTH_MM) }
                (labels + labelled + if (typed || explicitlyFrame || labels.any()) {
                    dimensions(segment, SizeScale.WIDTH_MM)
                } else emptySequence()).toList()
            }.toSet()
        VisualSizeKind.WHEEL -> buildSet {
            wheelInches.findAll(value).forEach { add(mm(it.groupValues[1], "inch", SizeScale.WHEEL_NOMINAL_MM)) }
            wheelC.findAll(value).forEach { add(SizeValue(SizeScale.WHEEL_C, it.groupValues[1])) }
            if (typed) {
                addAll(dimensions(value, SizeScale.WHEEL_NOMINAL_MM))
                wheelWidth.find(value)?.let {
                    val nominal = it.groupValues[1]
                    if (it.groupValues[2].isNotEmpty()) add(SizeValue(SizeScale.WHEEL_C, nominal))
                    else add(mm(nominal, "inch", SizeScale.WHEEL_NOMINAL_MM))
                }
            }
        }
        else -> dimensions(value, SizeScale.WIDTH_MM).toSet()
    }
}

private fun dimensions(value: String, scale: SizeScale) = dimension.findAll(value)
    .map { mm(it.groupValues[1], it.groupValues[2], scale) }

private fun mm(amount: String, unit: String, scale: SizeScale): SizeValue {
    val factor = when (unit) { "mm" -> "1"; "cm" -> "10"; else -> "25.4" }
    return SizeValue(scale, (amount.toBigDecimal() * factor.toBigDecimal()).stripTrailingZeros().toPlainString())
}

private fun frameLetter(label: String) = when (label) {
    "small" -> "s"
    "medium" -> "m"
    "large" -> "l"
    else -> label
}

internal fun storedSpec(kind: VisualSpecificationKind, specs: Map<String, String>): String? = when (kind) {
    VisualSpecificationKind.FRAME_MATERIAL -> specs["frame_material"] ?: specs["frame"]
    VisualSpecificationKind.BRAKE_TYPE -> specs["brake_type"] ?: specs["brakes"]
    VisualSpecificationKind.GEAR_COUNT -> specs["gear_count"] ?: specs["gears"]
    VisualSpecificationKind.MOUNT_TYPE -> specs["mount_type"]
    VisualSpecificationKind.LINK_COUNT -> specs["link_count"] ?: specs["links"]
    VisualSpecificationKind.COMPATIBILITY -> specs["compatibility"]
}
