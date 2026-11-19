package dev.vignesh.cycleshop.recognition

import com.google.gson.JsonArray
import com.google.gson.JsonObject

internal enum class AttributeStatus { OBSERVED, UNCERTAIN, UNKNOWN }

/** Uses the OpenAPI schema subset accepted by Gemini responseSchema. Rules about
 * relationships between fields are enforced by [VisualResponseValidator]. */
object VisualResponseSchema {
    const val VERSION = 1
    const val MIME_TYPE = "application/json"
    internal const val MAX_MARKINGS = 12

    fun toJson(): String = objectSchema(
        "schema_version" to type("integer"),
        "product_type" to attribute(strings(VisualProductType.entries.map { it.name })),
        "brand" to attribute(type("string")),
        "category" to attribute(type("string")),
        "likely_model" to attribute(type("string")),
        "colour" to attribute(type("string")),
        "sizes" to array(
            objectSchema(
                "kind" to strings(VisualSizeKind.entries.map { it.name }),
                "attribute" to attribute(type("string"))
            ),
            VisualSizeKind.entries.size
        ),
        "markings" to array(type("string"), MAX_MARKINGS),
        "specifications" to array(
            objectSchema(
                "kind" to strings(VisualSpecificationKind.entries.map { it.name }),
                "value" to type("string"),
                "marking" to type("string")
            ),
            VisualSpecificationKind.entries.size
        )
    ).toString()

    private fun attribute(value: JsonObject) = objectSchema(
        "status" to strings(AttributeStatus.entries.map { it.name }),
        "value" to value.apply { addProperty("nullable", true) },
        "basis" to type("string")
    )

    private fun type(name: String) = JsonObject().apply { addProperty("type", name) }

    private fun strings(values: List<String>) = type("string").apply {
        add("enum", JsonArray().apply { values.forEach { add(it) } })
    }

    private fun array(item: JsonObject, max: Int) = type("array").apply {
        add("items", item)
        addProperty("maxItems", max)
    }

    private fun objectSchema(vararg fields: Pair<String, JsonObject>) = type("object").apply {
        add("properties", JsonObject().apply { fields.forEach { (name, schema) -> add(name, schema) } })
        add("required", JsonArray().apply { fields.forEach { (name, _) -> add(name) } })
    }
}
