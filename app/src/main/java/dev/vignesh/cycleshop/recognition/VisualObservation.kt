package dev.vignesh.cycleshop.recognition

enum class VisualProductType { BICYCLE, SPARE_PART, ACCESSORY }

enum class VisualSizeKind(val productType: VisualProductType) {
    FRAME(VisualProductType.BICYCLE),
    WHEEL(VisualProductType.BICYCLE),
    PART_DIMENSION(VisualProductType.SPARE_PART),
    ACCESSORY_DIMENSION(VisualProductType.ACCESSORY)
}

enum class VisualSpecificationKind {
    FRAME_MATERIAL, BRAKE_TYPE, GEAR_COUNT, MOUNT_TYPE, LINK_COUNT, COMPATIBILITY
}

sealed interface VisualAttribute<out T> {
    /** The analyzer reports direct evidence. This is a claim about the image. */
    data class Observed<T>(val value: T, val evidence: String) : VisualAttribute<T>
    data class Uncertain<T>(val possibleValue: T?, val reason: String) : VisualAttribute<T>
    data class Unknown(val reason: String) : VisualAttribute<Nothing>
}

data class VisualSize(val kind: VisualSizeKind, val attribute: VisualAttribute<String>)

data class VisualSpecification(
    val kind: VisualSpecificationKind,
    val value: String,
    val marking: String
)

data class VisualObservation(
    val schemaVersion: Int,
    val productType: VisualAttribute<VisualProductType>,
    val brand: VisualAttribute<String>,
    val category: VisualAttribute<String>,
    val likelyModel: VisualAttribute<String>,
    val colour: VisualAttribute<String>,
    val sizes: List<VisualSize>,
    val markings: List<String>,
    val specifications: List<VisualSpecification>
)
