package dev.vignesh.cycleshop.recognition

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import dev.vignesh.cycleshop.recognition.VisualValidationIssue.Code
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualResponseValidatorTest {
    private val fixtures = checkNotNull(javaClass.getResourceAsStream("/recognition/labelled_responses.json"))
        .bufferedReader().use { JsonParser.parseReader(it).asJsonArray }
        .associate { it.asJsonObject.get("id").asString to it.asJsonObject }
    private val testRequest = VisualAnalysisRequest(byteArrayOf(1), VisualImageFormat.JPEG)

    @Test
    fun labelledResponsesAllPassThroughTheInjectedFake() = runBlocking {
        for ((id, fixture) in fixtures) {
            val service: VisualAnalysisService = FakeVisualAnalysisService.returningJson(fixture.get("response").toString())
            val result = service.analyze(testRequest)
            assertTrue("$id failed: $result", result is VisualAnalysisResult.Success)
        }
    }

    @Test
    fun missingBrandStaysUnknownAndWheelLabelDoesNotFillFrameSize() {
        val fixture = fixtures.getValue("hero_unbranded_ambiguous")
        assertTrue(fixture.get("expectedSku").isJsonNull)
        assertEquals(2, fixture.getAsJsonArray("plausibleSkus").size())
        val observation = observation("hero_unbranded_ambiguous")
        assertEquals(VisualAttribute.Unknown("No legible brand."), observation.brand)
        val model = observation.likelyModel as VisualAttribute.Uncertain
        assertNull(model.possibleValue)
        assertEquals(
            VisualAttribute.Unknown("Frame size sticker is out of view."),
            observation.sizes.single { it.kind == VisualSizeKind.FRAME }.attribute
        )
        assertEquals(
            VisualAttribute.Observed("27.5T", "WHEEL 27.5T"),
            observation.sizes.single { it.kind == VisualSizeKind.WHEEL }.attribute
        )
    }

    @Test
    fun observedBrandOutsideCatalogueRemainsAnObservation() {
        val fixture = fixtures.getValue("unlisted_bicycle_brand")
        assertTrue(fixture.get("expectedProductName").isJsonNull)
        assertEquals(
            VisualAttribute.Observed("Testwheel", "TESTWHEEL"),
            observation("unlisted_bicycle_brand").brand
        )
    }

    @Test
    fun unidentifiableTypeRemainsUnknown() {
        val json = response("brake_shoe_unmarked")
        val type = json.getAsJsonObject("product_type")
        type.addProperty("status", "UNKNOWN")
        type.add("value", null)
        type.addProperty("basis", "Subject is too blurred.")
        val result = VisualResponseValidator.validate(json.toString()) as VisualAnalysisResult.Success
        assertEquals(VisualAttribute.Unknown("Subject is too blurred."), result.observation.productType)
    }

    @Test
    fun rejectsMalformedAndLenientJson() {
        val valid = response().toString()
        listOf(
            "", "{", """{'schema_version':1}""", """{"schema_version":1,}""",
            "/* comment */$valid", "$valid{}", valid.dropLast(1)
        ).forEach { assertIssue(Code.MALFORMED_JSON, it) }
    }

    @Test
    fun rejectsUnsupportedVersionAndWrongFieldShapes() {
        assertIssue(Code.UNSUPPORTED_VERSION, response().apply { addProperty("schema_version", 2) })
        assertIssue(Code.INVALID_SHAPE, response().apply { addProperty("schema_version", "1") })
        assertIssue(Code.INVALID_SHAPE, response().apply { remove("brand") })
        assertIssue(Code.INVALID_SHAPE, response().apply { getAsJsonObject("colour").addProperty("value", 123) })
    }

    @Test
    fun rejectsUnsupportedAttributesAndCatalogueFactsInModelPayload() {
        assertIssue(Code.UNSUPPORTED_ATTRIBUTE, response().apply { addProperty("stock_quantity", 6) })
        assertIssue(Code.UNSUPPORTED_ATTRIBUTE, response().apply {
            getAsJsonObject("brand").addProperty("probability", 0.9)
        })
        assertIssue(Code.UNSUPPORTED_ATTRIBUTE, response().apply {
            getAsJsonArray("specifications")[0].asJsonObject.addProperty("kind", "INTERNAL_SKU")
        })
        assertIssue(Code.UNSUPPORTED_ATTRIBUTE, response().apply {
            getAsJsonObject("product_type").addProperty("value", "MOTORCYCLE")
        })
    }

    @Test
    fun rejectsContradictoryFieldsAndDuplicateKeys() {
        assertIssue(Code.CONFLICTING_ATTRIBUTES, response().apply {
            getAsJsonObject("brand").addProperty("status", "UNKNOWN")
        })
        assertIssue(Code.CONFLICTING_ATTRIBUTES, response().apply {
            getAsJsonObject("product_type").addProperty("value", "SPARE_PART")
        })
        assertIssue(Code.CONFLICTING_ATTRIBUTES, response().apply {
            val sizes = getAsJsonArray("sizes")
            sizes.add(sizes[0].deepCopy())
        })
        assertIssue(Code.CONFLICTING_ATTRIBUTES, """{"schema_version":1,"schema_version":2}""")
    }

    @Test
    fun rejectsBrandAndSpecificationWithoutMatchingTranscriptions() {
        assertIssue(Code.UNSUPPORTED_EVIDENCE, response().apply {
            getAsJsonObject("brand").addProperty("value", "Firefox")
        })
        assertIssue(Code.UNSUPPORTED_EVIDENCE, response().apply {
            getAsJsonArray("specifications")[0].asJsonObject.addProperty("value", "Carbon")
        })
    }

    @Test
    fun rejectsGuessedSizesMisclassifiedMarkingsAndPartialNumbers() {
        assertIssue(Code.UNSUPPORTED_EVIDENCE, response().apply {
            sizeAttribute(0).addProperty("basis", "Looks about 17 inches.")
        })
        assertIssue(Code.UNSUPPORTED_EVIDENCE, response().apply {
            sizeAttribute(0).addProperty("status", "UNCERTAIN")
        })
        assertIssue(Code.UNSUPPORTED_EVIDENCE, response().apply {
            sizeAttribute(0).addProperty("basis", "WHEEL 27.5T")
            sizeAttribute(0).addProperty("value", "27.5T")
        })
        assertIssue(Code.UNSUPPORTED_EVIDENCE, response().apply {
            sizeAttribute(0).addProperty("value", "7 in")
        })
        assertIssue(Code.UNSUPPORTED_EVIDENCE, response().apply {
            getAsJsonArray("markings").set(2, JsonPrimitive("SPRINT PRO 27.5T"))
            sizeAttribute(1).addProperty("basis", "SPRINT PRO 27.5T")
        })
    }

    @Test
    fun rejectsResponsesOutsideSizeLimits() {
        assertIssue(Code.RESPONSE_LIMIT, " ".repeat(20_000))
        assertIssue(Code.RESPONSE_LIMIT, "[".repeat(12) + "0" + "]".repeat(12))
        assertIssue(Code.RESPONSE_LIMIT, response().apply {
            getAsJsonObject("colour").addProperty("basis", "a".repeat(201))
        })
    }

    @Test
    fun responseSchemaExposesRequiredNullableFieldsAndSupportedEnums() {
        val schema = JsonParser.parseString(VisualResponseSchema.toJson()).asJsonObject
        val properties = schema.getAsJsonObject("properties")
        assertEquals("application/json", VisualResponseSchema.MIME_TYPE)
        assertEquals(properties.keySet(), schema.getAsJsonArray("required").map { it.asString }.toSet())
        val typeField = properties.getAsJsonObject("product_type").getAsJsonObject("properties")
        assertTrue(typeField.getAsJsonObject("value").get("nullable").asBoolean)
        assertEquals(
            listOf("BICYCLE", "SPARE_PART", "ACCESSORY"),
            typeField.getAsJsonObject("value").getAsJsonArray("enum").map { it.asString }
        )
    }

    @Test
    fun fakeCanReturnUnavailableWithoutAnyApiOrCredentials() = runBlocking {
        val offline = VisualAnalysisResult.Unavailable(VisualAnalysisResult.Unavailable.Reason.OFFLINE)
        val service: VisualAnalysisService = FakeVisualAnalysisService { request ->
            assertEquals(VisualImageFormat.JPEG, request.format)
            offline
        }
        assertEquals(offline, service.analyze(testRequest))
    }

    private fun observation(id: String) =
        (VisualResponseValidator.validate(response(id).toString()) as VisualAnalysisResult.Success).observation

    private fun response(id: String = "hero_black_branded") =
        fixtures.getValue(id).getAsJsonObject("response").deepCopy()

    private fun JsonObject.sizeAttribute(index: Int) =
        getAsJsonArray("sizes")[index].asJsonObject.getAsJsonObject("attribute")

    private fun assertIssue(code: Code, json: JsonObject) = assertIssue(code, json.toString())

    private fun assertIssue(code: Code, json: String) {
        val result = VisualResponseValidator.validate(json)
        assertTrue("Expected $code, got $result", result is VisualAnalysisResult.InvalidResponse)
        assertEquals(code, (result as VisualAnalysisResult.InvalidResponse).issue.code)
    }
}
