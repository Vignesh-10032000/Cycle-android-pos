package dev.vignesh.cycleshop.ui.invoice

import android.graphics.Rect

/**
 * Pure Kotlin representation of an axis-aligned bounding rectangle on the source image.
 *
 * Using a pure Kotlin data class instead of [android.graphics.Rect] allows:
 * 1. 100% JVM unit testability without Android SDK runtime stubs.
 * 2. Deterministic geometry calculations (width, height, contains, row overlap).
 * 3. Sorting by coordinates (top-to-bottom, left-to-right) for invoice table reconstruction.
 */
data class OcrBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = maxOf(0, right - left)
    val height: Int get() = maxOf(0, bottom - top)
}

/**
 * Safe conversion from nullable [android.graphics.Rect] to [OcrBoundingBox].
 */
fun Rect?.toOcrBoundingBox(): OcrBoundingBox? = this?.let {
    OcrBoundingBox(left = it.left, top = it.top, right = it.right, bottom = it.bottom)
}

/**
 * Immutable snapshot of a single ML Kit text recognition block.
 *
 * ML Kit returns a three-level hierarchy:
 *   Text → [TextBlock] → [TextLine] → [TextElement]
 *
 * We capture this hierarchy for future structured invoice parsing (Phase 3+)
 * while keeping the Phase 3.1 surface area limited to raw text display.
 *
 * @param blockText   Full concatenated text of the block.
 * @param lines       Ordered list of lines within the block.
 * @param boundingBox Bounding rectangle on the original image, or null if unbound.
 * @param confidence  Block-level confidence score [0.0, 1.0]. Null for ML Kit v2 Latin.
 */
data class OcrTextBlock(
    val blockText: String,
    val lines: List<OcrTextLine>,
    val boundingBox: OcrBoundingBox?,
    val confidence: Float? = null
)

/**
 * A single line of OCR text, preserving element-level detail.
 *
 * @param lineText    Whitespace-joined text of all elements on this line.
 * @param elements    Individual word/token elements recognized on this line.
 * @param boundingBox Bounding rectangle for the line in original image pixels.
 */
data class OcrTextLine(
    val lineText: String,
    val elements: List<String>,
    val boundingBox: OcrBoundingBox?
)

/**
 * Complete OCR result returned by [InvoiceScanViewModel] after processing
 * one invoice image with ML Kit Text Recognition.
 *
 * Security note: This class does NOT store the raw image bytes. Only the
 * recognized text blocks and performance metadata are captured. Callers
 * must ensure this result is not written to external logs.
 *
 * @param rawText         Full document text as a single string (block separator = "\n\n").
 * @param blocks          Ordered list of [OcrTextBlock] objects preserving spatial layout.
 * @param processingMs    Wall-clock milliseconds from InputImage creation to recognition complete.
 * @param imageWidthPx    Width of the source image in pixels (for bounding-box coordinate context).
 * @param imageHeightPx   Height of the source image in pixels.
 * @param rotationDegrees EXIF/sensor rotation that was applied before recognition (0, 90, 180, 270).
 */
data class OcrResult(
    val rawText: String,
    val blocks: List<OcrTextBlock>,
    val processingMs: Long,
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val rotationDegrees: Int
) {
    /** Number of blocks recognized. */
    val blockCount: Int get() = blocks.size

    /** Number of non-blank text lines across all blocks. */
    val lineCount: Int get() = blocks.sumOf { it.lines.size }

    /** Ordered list of all lines across all blocks. */
    val allLines: List<OcrTextLine> get() = blocks.flatMap { it.lines }

    /** True when at least one character was recognized. */
    val hasContent: Boolean get() = rawText.isNotBlank()
}
