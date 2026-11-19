package dev.vignesh.cycleshop.recognition

/** Deterministic analyzer for tests. The JSON goes through the production validator. */
internal class FakeVisualAnalysisService(
    private val respond: (VisualAnalysisRequest) -> VisualAnalysisResult
) : VisualAnalysisService {
    override suspend fun analyze(request: VisualAnalysisRequest): VisualAnalysisResult = respond(request)

    companion object {
        fun returningJson(json: String): VisualAnalysisService =
            FakeVisualAnalysisService { VisualResponseValidator.validate(json) }
    }
}
