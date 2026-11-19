package dev.vignesh.cycleshop.recognition

interface VisualAnalysisService {
    suspend fun analyze(request: VisualAnalysisRequest): VisualAnalysisResult
}

enum class VisualImageFormat(val mimeType: String) {
    JPEG("image/jpeg"), PNG("image/png"), WEBP("image/webp")
}

/** Image after local preparation. The scan entry point is only a hint to the analyzer. */
class VisualAnalysisRequest(
    imageBytes: ByteArray,
    val format: VisualImageFormat,
    val typeHint: VisualProductType? = null
) {
    private val image = imageBytes.copyOf()

    init {
        require(image.isNotEmpty()) { "An image is required." }
    }

    fun copyImageBytes(): ByteArray = image.copyOf()
}

sealed interface VisualAnalysisResult {
    data class Success(val observation: VisualObservation) : VisualAnalysisResult
    data class InvalidResponse(val issue: VisualValidationIssue) : VisualAnalysisResult
    data class Unavailable(val reason: Reason) : VisualAnalysisResult {
        enum class Reason { OFFLINE, TIMEOUT, QUOTA, ACCESS_DENIED, SERVICE_ERROR }
    }
}
