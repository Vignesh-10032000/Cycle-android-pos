package dev.vignesh.cycleshop.data.local.model

import org.json.JSONObject

/**
 * Validated, versioned representation of dynamic technical specifications stored as JSON.
 * Ensures schema versioning for backward and forward compatibility as new specs are captured.
 */
data class ProductSpecs(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val attributes: Map<String, String> = emptyMap()
) {
    init {
        require(schemaVersion in 1..CURRENT_SCHEMA_VERSION) {
            "Unsupported specs schema version: $schemaVersion. Maximum supported version is $CURRENT_SCHEMA_VERSION."
        }
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put(KEY_SCHEMA_VERSION, schemaVersion)
        val attrsObj = JSONObject()
        attributes.forEach { (key, value) ->
            attrsObj.put(key, value)
        }
        root.put(KEY_ATTRIBUTES, attrsObj)
        return root.toString()
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val KEY_ATTRIBUTES = "attributes"

        /**
         * Parses JSON string into validated [ProductSpecs].
         *
         * @param json Raw JSON string, or null/blank for uninitialized specs.
         * @return Validated [ProductSpecs]
         * @throws IllegalArgumentException if the JSON is malformed, corrupted, or specifies an unsupported version.
         */
        fun fromJson(json: String?): ProductSpecs {
            if (json.isNullOrBlank()) return ProductSpecs()
            return try {
                val root = JSONObject(json)
                val version = if (root.has(KEY_SCHEMA_VERSION)) {
                    root.getInt(KEY_SCHEMA_VERSION)
                } else {
                    throw IllegalArgumentException("Missing required '$KEY_SCHEMA_VERSION' in specs JSON.")
                }

                require(version in 1..CURRENT_SCHEMA_VERSION) {
                    "Unsupported specs schema version: $version. Maximum supported version is $CURRENT_SCHEMA_VERSION."
                }

                val attrsObj = root.optJSONObject(KEY_ATTRIBUTES)
                    ?: if (root.has(KEY_ATTRIBUTES)) {
                        throw IllegalArgumentException("Expected '$KEY_ATTRIBUTES' to be a JSON object.")
                    } else {
                        JSONObject()
                    }

                val map = mutableMapOf<String, String>()
                val keys = attrsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = attrsObj.optString(key, "")
                }
                ProductSpecs(schemaVersion = version, attributes = map)
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                throw IllegalArgumentException("Malformed or corrupted product specs JSON: ${e.message}", e)
            }
        }
    }
}
