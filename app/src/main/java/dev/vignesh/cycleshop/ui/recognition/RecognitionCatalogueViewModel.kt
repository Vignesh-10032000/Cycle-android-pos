package dev.vignesh.cycleshop.ui.recognition

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import dev.vignesh.cycleshop.data.local.model.ProductType
import dev.vignesh.cycleshop.recognition.RoomVisualCatalogue
import dev.vignesh.cycleshop.recognition.VisualAnalysisResult
import dev.vignesh.cycleshop.recognition.VisualCatalogMatcher
import dev.vignesh.cycleshop.recognition.VisualCatalogueVariant
import dev.vignesh.cycleshop.recognition.VisualMatchResult
import dev.vignesh.cycleshop.recognition.VisualObservation
import dev.vignesh.cycleshop.recognition.VisualResponseValidator
import dev.vignesh.cycleshop.recognition.manualSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class RecognitionExample(val label: String, val observation: VisualObservation)

internal sealed interface CatalogueContent {
    data object Loading : CatalogueContent
    data object Unavailable : CatalogueContent
    data object Start : CatalogueContent
    data class Manual(val items: List<VisualCatalogueVariant>) : CatalogueContent
    data class Simulated(val example: RecognitionExample, val result: VisualMatchResult) : CatalogueContent
}

internal data class RecognitionCatalogueState(
    val content: CatalogueContent = CatalogueContent.Loading,
    val query: String = "",
    val examples: List<RecognitionExample> = emptyList(),
    val selectedVariantId: Long? = null
)

internal class RecognitionCatalogueViewModel(
    catalogue: RoomVisualCatalogue,
    types: Set<ProductType>,
    loadExamples: suspend () -> List<RecognitionExample>
) : ViewModel() {
    private data class Request(
        val query: String = "",
        val example: RecognitionExample? = null,
        val selectedVariantId: Long? = null
    )

    private val request = MutableStateFlow(Request())
    private val examples = MutableStateFlow<List<RecognitionExample>>(emptyList())
    private val matcher = VisualCatalogMatcher(catalogue)

    val state = combine(catalogue.snapshots, request, examples) { snapshot, request, examples ->
        val content = when {
            request.example != null -> CatalogueContent.Simulated(request.example, matcher.rank(request.example.observation, snapshot))
            request.query.isNotBlank() -> CatalogueContent.Manual(snapshot.manualSearch(request.query, types))
            else -> CatalogueContent.Start
        }
        RecognitionCatalogueState(content, request.query, examples, request.selectedVariantId)
    }.flowOn(Dispatchers.Default)
        .catch { emit(RecognitionCatalogueState(content = CatalogueContent.Unavailable)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecognitionCatalogueState())

    init {
        viewModelScope.launch { examples.value = loadExamples() }
    }

    fun search(query: String) = request.update { Request(query = query.take(100)) }
    fun simulate(example: RecognitionExample) = request.update { Request(example = example) }
    fun select(variantId: Long) = request.update { it.copy(selectedVariantId = variantId) }
    fun clear() = request.update { Request() }
}

internal suspend fun loadIsolatedExamples(app: Application, kind: RecognitionPhotoKind): List<RecognitionExample> =
    withContext(Dispatchers.IO) {
        if (app.packageName != "dev.vignesh.cycleshop.isolated") return@withContext emptyList()
        val labels = when (kind) {
            RecognitionPhotoKind.BICYCLE -> mapOf(
                "hero_black_branded" to "Branded bicycle",
                "hero_unbranded_ambiguous" to "Ambiguous bicycle",
                "unlisted_bicycle_brand" to "Unlisted brand"
            )
            RecognitionPhotoKind.SPARE_PART -> mapOf(
                "shimano_derailleur_marked" to "Marked derailleur",
                "brake_shoe_unmarked" to "Unmarked brake shoe",
                "kmc_chain_brand_defect" to "KMC catalogue issue",
                "unlisted_accessory" to "Unlisted accessory"
            )
        }
        app.assets.open("recognition/labelled_responses.json").bufferedReader().use { reader ->
            JsonParser.parseReader(reader).asJsonArray.mapNotNull { element ->
                val row = element.asJsonObject
                val label = labels[row.get("id").asString] ?: return@mapNotNull null
                val validated = VisualResponseValidator.validate(row.get("response").toString())
                (validated as? VisualAnalysisResult.Success)?.let { RecognitionExample(label, it.observation) }
            }
        }
    }
