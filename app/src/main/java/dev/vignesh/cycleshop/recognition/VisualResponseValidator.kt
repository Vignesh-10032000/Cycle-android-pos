package dev.vignesh.cycleshop.recognition

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.IOException
import java.io.StringReader

data class VisualValidationIssue(val code: Code, val path: String) {
    enum class Code {
        MALFORMED_JSON, INVALID_SHAPE, UNSUPPORTED_VERSION, UNSUPPORTED_ATTRIBUTE,
        CONFLICTING_ATTRIBUTES, UNSUPPORTED_EVIDENCE, RESPONSE_LIMIT
    }
}

object VisualResponseValidator {
    private const val MAX_RESPONSE_CHARS = 16_384
    private const val MAX_TEXT_CHARS = 200
    private const val MAX_JSON_DEPTH = 8

    fun validate(json: String): VisualAnalysisResult {
        return try {
            checkResponse(json.length <= MAX_RESPONSE_CHARS, Code.RESPONSE_LIMIT, "$")
            JsonReader(StringReader(json)).use { reader ->
                reader.strictness = Strictness.STRICT
                val root = readJson(reader)
                checkResponse(reader.peek() == JsonToken.END_DOCUMENT, Code.MALFORMED_JSON, "$")
                VisualAnalysisResult.Success(decode(root))
            }
        } catch (e: RejectedResponse) {
            VisualAnalysisResult.InvalidResponse(e.issue)
        } catch (_: IOException) {
            VisualAnalysisResult.InvalidResponse(VisualValidationIssue(Code.MALFORMED_JSON, "$"))
        }
    }

    private fun decode(json: JsonElement): VisualObservation {
        val root = json.fields(
            "$", "schema_version", "product_type", "brand", "category",
            "likely_model", "colour", "sizes", "markings", "specifications"
        )
        val version = root["schema_version"].takeIf {
            it.isJsonPrimitive && it.asJsonPrimitive.isNumber
        }?.asString?.toIntOrNull() ?: reject(Code.INVALID_SHAPE, "$.schema_version")
        checkResponse(version == VisualResponseSchema.VERSION, Code.UNSUPPORTED_VERSION, "$.schema_version")

        val productType = root["product_type"].attribute("$.product_type") { value, path ->
            enumValue<VisualProductType>(value, path)
        }
        val reportedType = when (productType) {
            is VisualAttribute.Observed -> productType.value
            is VisualAttribute.Uncertain -> productType.possibleValue
            is VisualAttribute.Unknown -> null
        }
        val markings = root["markings"].items("$.markings", VisualResponseSchema.MAX_MARKINGS) { item, path ->
            item.text(path)
        }
        val brand = root["brand"].textAttribute("$.brand")
        val model = root["likely_model"].textAttribute("$.likely_model")
        requireVisibleText(brand, markings, "$.brand")
        if (model is VisualAttribute.Observed) requireVisibleText(model, markings, "$.likely_model")

        val sizes = root["sizes"].items("$.sizes", VisualSizeKind.entries.size) { item, path ->
            val size = item.fields(path, "kind", "attribute")
            val kind = enumValue<VisualSizeKind>(size["kind"].text("$path.kind"), "$path.kind")
            val attribute = size["attribute"].textAttribute("$path.attribute")
            checkResponse(
                reportedType == null || kind.productType == reportedType,
                Code.CONFLICTING_ATTRIBUTES, "$path.kind"
            )
            validateSize(kind, attribute, markings, "$path.attribute")
            if (attribute is VisualAttribute.Observed) {
                checkResponse(productType is VisualAttribute.Observed, Code.UNSUPPORTED_EVIDENCE, path)
            }
            VisualSize(kind, attribute)
        }
        requireDistinct(sizes.map { it.kind }, "$.sizes")

        val specifications = root["specifications"].items(
            "$.specifications", VisualSpecificationKind.entries.size
        ) { item, path ->
            val spec = item.fields(path, "kind", "value", "marking")
            val kind = enumValue<VisualSpecificationKind>(spec["kind"].text("$path.kind"), "$path.kind")
            val value = spec["value"].text("$path.value")
            val marking = spec["marking"].text("$path.marking")
            if (kind == VisualSpecificationKind.FRAME_MATERIAL && productType is VisualAttribute.Observed) {
                checkResponse(productType.value == VisualProductType.BICYCLE, Code.CONFLICTING_ATTRIBUTES, path)
            }
            requireMarking(value, marking, markings, path)
            VisualSpecification(kind, value, marking)
        }
        requireDistinct(specifications.map { it.kind }, "$.specifications")

        return VisualObservation(
            schemaVersion = version,
            productType = productType,
            brand = brand,
            category = root["category"].textAttribute("$.category"),
            likelyModel = model,
            colour = root["colour"].textAttribute("$.colour"),
            sizes = sizes,
            markings = markings,
            specifications = specifications
        )
    }

    private fun validateSize(
        kind: VisualSizeKind,
        size: VisualAttribute<String>,
        markings: List<String>,
        path: String
    ) {
        if (size is VisualAttribute.Uncertain) {
            checkResponse(size.possibleValue == null, Code.UNSUPPORTED_EVIDENCE, "$path.value")
        }
        if (size !is VisualAttribute.Observed) return
        requireMarking(size.value, size.evidence, markings, path)

        val frameLabel = frameContext.containsMatchIn(size.evidence)
        val wheelLabel = wheelContext.containsMatchIn(size.evidence)
        val supported = when (kind) {
            VisualSizeKind.FRAME ->
                frameLabel && !wheelLabel && (frameLetter.matches(size.value) || dimension.matches(size.value))
            VisualSizeKind.WHEEL ->
                wheelLabel && !frameLabel && (wheelSize.matches(size.value) || dimension.matches(size.value))
            VisualSizeKind.PART_DIMENSION, VisualSizeKind.ACCESSORY_DIMENSION ->
                !frameLabel && !wheelLabel && dimension.matches(size.value)
        }
        checkResponse(supported, Code.UNSUPPORTED_EVIDENCE, path)
    }

    private fun requireVisibleText(attribute: VisualAttribute<String>, markings: List<String>, path: String) {
        when (attribute) {
            is VisualAttribute.Observed -> requireMarking(attribute.value, attribute.evidence, markings, path)
            is VisualAttribute.Uncertain -> {
                attribute.possibleValue?.let { requireMarking(it, attribute.reason, markings, path) }
            }
            is VisualAttribute.Unknown -> Unit
        }
    }

    private fun requireMarking(value: String, basis: String, markings: List<String>, path: String) {
        val token = Regex(
            """(?<![\p{L}\p{N}.])""" + Regex.escape(value) + """(?![\p{L}\p{N}]|\.\d)""",
            RegexOption.IGNORE_CASE
        )
        checkResponse(
            markings.any { it.equals(basis, ignoreCase = true) } && token.containsMatchIn(basis),
            Code.UNSUPPORTED_EVIDENCE, path
        )
    }

    private fun JsonElement.textAttribute(path: String) = attribute(path) { value, _ -> value }

    private fun <T> JsonElement.attribute(path: String, convert: (String, String) -> T): VisualAttribute<T> {
        val fields = fields(path, "status", "value", "basis")
        val status = enumValue<AttributeStatus>(fields["status"].text("$path.status"), "$path.status")
        val value = fields["value"].takeUnless { it.isJsonNull }?.text("$path.value")
        val basis = fields["basis"].text("$path.basis")
        return when (status) {
            AttributeStatus.OBSERVED -> {
                if (value == null) reject(Code.CONFLICTING_ATTRIBUTES, "$path.value")
                VisualAttribute.Observed(convert(value, "$path.value"), basis)
            }
            AttributeStatus.UNCERTAIN -> VisualAttribute.Uncertain(value?.let { convert(it, "$path.value") }, basis)
            AttributeStatus.UNKNOWN -> {
                checkResponse(value == null, Code.CONFLICTING_ATTRIBUTES, "$path.value")
                VisualAttribute.Unknown(basis)
            }
        }
    }

    private fun JsonElement.fields(path: String, vararg names: String): Fields {
        checkResponse(isJsonObject, Code.INVALID_SHAPE, path)
        val obj = asJsonObject
        checkResponse(obj.keySet().all { it in names }, Code.UNSUPPORTED_ATTRIBUTE, path)
        names.forEach { checkResponse(obj.has(it), Code.INVALID_SHAPE, "$path.$it") }
        return Fields(obj)
    }

    private class Fields(private val obj: JsonObject) {
        operator fun get(key: String): JsonElement = obj.get(key)
    }

    private fun JsonElement.text(path: String): String {
        checkResponse(isJsonPrimitive && asJsonPrimitive.isString, Code.INVALID_SHAPE, path)
        return asString.trim().also {
            checkResponse(it.isNotEmpty() && it.none(Char::isISOControl), Code.INVALID_SHAPE, path)
            checkResponse(it.length <= MAX_TEXT_CHARS, Code.RESPONSE_LIMIT, path)
        }
    }

    private fun <T> JsonElement.items(path: String, max: Int, convert: (JsonElement, String) -> T): List<T> {
        checkResponse(isJsonArray, Code.INVALID_SHAPE, path)
        checkResponse(asJsonArray.size() <= max, Code.RESPONSE_LIMIT, path)
        return asJsonArray.mapIndexed { index, item -> convert(item, "$path[$index]") }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, path: String): T =
        enumValues<T>().firstOrNull { it.name == value } ?: reject(Code.UNSUPPORTED_ATTRIBUTE, path)

    private fun <T> requireDistinct(values: List<T>, path: String) =
        checkResponse(values.distinct().size == values.size, Code.CONFLICTING_ATTRIBUTES, path)

    /** Read strict JSON with bounded nesting and reject duplicate keys before building a tree. */
    private fun readJson(reader: JsonReader, depth: Int = 0): JsonElement {
        checkResponse(depth <= MAX_JSON_DEPTH, Code.RESPONSE_LIMIT, "$")
        return when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> JsonObject().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    checkResponse(!has(name), Code.CONFLICTING_ATTRIBUTES, "$")
                    add(name, readJson(reader, depth + 1))
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> JsonArray().apply {
                reader.beginArray()
                while (reader.hasNext()) add(readJson(reader, depth + 1))
                reader.endArray()
            }
            JsonToken.STRING -> JsonPrimitive(reader.nextString())
            JsonToken.NUMBER -> {
                val number = reader.nextString().toBigDecimalOrNull() ?: reject(Code.INVALID_SHAPE, "$")
                JsonPrimitive(number)
            }
            JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
            JsonToken.NULL -> JsonNull.INSTANCE.also { reader.nextNull() }
            else -> reject(Code.MALFORMED_JSON, "$")
        }
    }

    private fun checkResponse(ok: Boolean, code: Code, path: String) {
        if (!ok) reject(code, path)
    }

    private fun reject(code: Code, path: String): Nothing = throw RejectedResponse(VisualValidationIssue(code, path))
    private class RejectedResponse(val issue: VisualValidationIssue) : RuntimeException()

    private val frameContext = Regex("""\bframe\b""", RegexOption.IGNORE_CASE)
    private val wheelContext = Regex("""\b(wheel|tyre|tire)\b""", RegexOption.IGNORE_CASE)
    private val frameLetter = Regex("""(?:XXS|XS|S|M|L|XL|XXL)""", RegexOption.IGNORE_CASE)
    private val dimension = Regex("""[1-9]\d*(?:\.\d+)?\s*(?:mm|cm|in|inch(?:es)?|["″])""", RegexOption.IGNORE_CASE)
    private val wheelSize = Regex("""(?:[1-9]\d?(?:\.\d+)?T|[1-9]\d{2}C)""", RegexOption.IGNORE_CASE)
}

private typealias Code = VisualValidationIssue.Code
