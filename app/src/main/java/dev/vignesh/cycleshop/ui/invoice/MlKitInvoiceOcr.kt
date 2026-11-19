package dev.vignesh.cycleshop.ui.invoice

import com.google.mlkit.vision.text.Text

internal fun Text.toInvoiceOcrResult(
    width: Int,
    height: Int,
    rotationDegrees: Int
): OcrResult = OcrResult(
    rawText = text,
    blocks = textBlocks.map { block ->
        OcrTextBlock(block.text, block.lines.map { line ->
            OcrTextLine(line.text, line.elements.map { it.text }, line.boundingBox.toOcrBoundingBox(),
                words = line.elements.mapNotNull { element ->
                    val box = element.boundingBox.toOcrBoundingBox() ?: return@mapNotNull null
                    val edge = element.cornerPoints
                    val slope = if (edge != null && edge.size >= 2 && edge[1].x > edge[0].x) {
                        (edge[1].y - edge[0].y).toDouble() / (edge[1].x - edge[0].x)
                    } else null
                    OcrWord(element.text, box, slope)
                }
            )
        }, block.boundingBox.toOcrBoundingBox())
    },
    processingMs = 0L,
    imageWidthPx = width,
    imageHeightPx = height,
    rotationDegrees = rotationDegrees
)
