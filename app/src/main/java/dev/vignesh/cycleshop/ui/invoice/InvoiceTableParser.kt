package dev.vignesh.cycleshop.ui.invoice

import java.util.Locale
import kotlin.math.abs

/** Reads invoice cells from observed text and geometry. Totals are independent of the body. */
class InvoiceTableParser {
    fun parse(ocrResult: OcrResult): ParsedInvoice {
        val words = ocrResult.allLines.flatMap { it.words }
        val geometry = Geometry(words)
        val table = findColumns(geometry)
        val lines = ocrResult.allLines.filter { line ->
            table == null || (line.boundingBox?.top ?: Int.MAX_VALUE) < table.headerTop
        }.sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
        val headerText = lines.joinToString("\n") { it.lineText }.ifBlank {
            ocrResult.rawText.takeIf { table == null }.orEmpty()
        }
        val upper = headerText.uppercase(Locale.ROOT)
        val layout = when {
            "M.STEEL" in upper || Regex("""\bMBW\b""").containsMatchIn(upper) -> InvoiceLayoutType.MSTEEL_MBW
            "STRYDER" in upper || Regex("""\bBICYCLE SUPPLIER\b""").containsMatchIn(upper) ->
                InvoiceLayoutType.BICYCLE_SUPPLIER
            else -> InvoiceLayoutType.GENERIC_GST
        }
        val grid = table?.let { readTable(geometry, it, layout) }
        val gst = GSTIN.findAll(upper).map { it.value }.distinct().take(2).toList()
        val invoiceNo = INVOICE_NO.findAll(headerText).map { it.groupValues[1] }
            .firstOrNull { it.any(Char::isDigit) }
            ?: inlineNumber(geometry, table?.headerTop)
        val supplier = when (layout) {
            InvoiceLayoutType.MSTEEL_MBW -> headerText.lineSequence().firstOrNull {
                it.contains("M.STEEL", true) || it.trim().equals("MBW", true)
            }
            InvoiceLayoutType.BICYCLE_SUPPLIER -> headerText.lineSequence().firstOrNull {
                it.trim().startsWith("STRYDER", true) || it.trim().startsWith("BICYCLE SUPPLIER", true)
            }
            InvoiceLayoutType.GENERIC_GST -> headerText.lineSequence().firstOrNull {
                it.isNotBlank() && !it.contains("TAX INVOICE", true) && it.any(Char::isLetter) &&
                    !it.contains("Invoice No", true)
            }
        }?.trim()
        val explicitQuantity = labeledTotal(ocrResult.rawText, QUANTITY_TOTAL)?.let(InvoiceNumbers::quantity)
        val explicitTaxable = labeledTotal(ocrResult.rawText, TAXABLE_TOTAL)
            ?.let { InvoiceNumbers.paise(it, allowZero = true) }
        return ParsedInvoice(
            supplierName = supplier,
            invoiceNumber = invoiceNo,
            invoiceDate = DATE.find(headerText)?.groupValues?.get(1),
            supplierGstin = gst.getOrNull(0),
            consigneeName = CONSIGNEE.find(headerText)?.groupValues?.get(1)?.trim(),
            consigneeGstin = gst.getOrNull(1),
            items = grid?.items.orEmpty(),
            totalQuantityExtracted = explicitQuantity ?: grid?.quantityTotal,
            totalAmountExtractedPaise = explicitTaxable ?: grid?.taxableTotal,
            layoutType = layout,
            rawText = ocrResult.rawText,
            processingTimeMs = ocrResult.processingMs
        )
    }

    private enum class Column { SERIAL, DESCRIPTION, HSN, QUANTITY, RATE, UNIT, TAX_TYPE, TAX_RATE, AMOUNT }

    private class Geometry(val words: List<OcrWord>) {
        val font = words.map { it.boundingBox.height }.filter { it > 0 }.median()?.toDouble() ?: 12.0
        // Perspective can change the line angle down the page. Estimate it locally from long word edges.
        private val slopes = words.filter {
            it.boundingBox.width >= font * 2 && it.topEdgeSlope?.let { slope -> abs(slope) <= .25 } == true
        }.groupBy { band(it) }
        private val angles = slopes.mapValues { (band, _) ->
            (band - 1..band + 1).flatMap { slopes[it].orEmpty() }.mapNotNull { it.topEdgeSlope }.median() ?: 0.0
        }
        private val descriptionAngles = mutableMapOf<Pair<Int, Int>, Double?>()
        private fun band(word: OcrWord) = (word.y / (font * 4)).toInt()
        fun y(word: OcrWord, referenceX: Double): Double =
            word.y + (referenceX - word.x) * (angles[band(word)] ?: 0.0)

        fun descriptionY(word: OcrWord, referenceX: Double): Double {
            val xBand = (word.x / (font * 6)).toInt()
            val yBand = band(word)
            val angle = descriptionAngles.getOrPut(xBand to yBand) {
                val x = (xBand + .5) * font * 6
                (yBand - 1..yBand + 1).flatMap { slopes[it].orEmpty() }
                    .filter { abs(it.x - x) < font * 9 }
                    .mapNotNull(OcrWord::topEdgeSlope).median()
            }
            return if (angle == null) y(word, referenceX) else word.y + (referenceX - word.x) * angle
        }
    }

    private data class Columns(
        val centers: Map<Column, Double>,
        val headerTop: Int,
        val bodyStart: Double,
        val itemCodeInDescription: Boolean,
        val rowOffsets: Map<Column, Double>,
        val descriptionRight: Double?
    ) {
        private val ordered = centers.entries.sortedBy { it.value }
        val referenceX = centers[Column.HSN] ?: centers.getValue(Column.DESCRIPTION)
        fun at(x: Double): Column {
            for ((current, next) in ordered.zipWithNext()) {
                // The description heading can sit well to the left of the column's right edge.
                val boundary = if (current.key == Column.DESCRIPTION && next.key == Column.HSN) {
                    descriptionRight ?: (current.value + next.value) / 2
                } else (current.value + next.value) / 2
                if (x < boundary) return current.key
            }
            return ordered.last().key
        }
    }

    private fun findColumns(geometry: Geometry): Columns? {
        val words = geometry.words.sortedBy { it.y }
        for (description in words.filter { it.label in setOf("DESCRIPTION", "PARTICULARS", "ITEM") }) {
            val nearby = words.filter { abs(geometry.y(it, description.x) - description.y) <= geometry.font * 2.4 }
            val quantity = uniqueHeading(nearby.filter {
                it.x > description.x && it.label in setOf("QTY", "QUANTITY", "QUANTITYUNIT")
            }, geometry.font) ?: continue
            val amount = uniqueHeading(nearby.filter {
                it.x > quantity.x && it.label in setOf("AMOUNT", "AMOUND", "VALUE")
            }, geometry.font) ?: continue
            val hsn = nearby.firstOrNull { it.label == "HSN" && it.x in description.x..quantity.x }
            val qtyX = if (quantity.label == "QUANTITYUNIT") {
                quantity.boundingBox.left + quantity.boundingBox.width * .33
            } else quantity.x
            val unit = nearby.firstOrNull {
                it.label in setOf("UNIT", "UNI", "UOM", "PER") && it.x > (hsn?.x ?: description.x)
            }
            val rate = nearby.filter { it.x in qtyX..amount.x && it.label in setOf("RATE", "PRICE") }
                .minByOrNull { it.x }
            val tax = nearby.filter {
                it.label in setOf("TAX", "GST", "RATE%") && it.x > (hsn?.x ?: description.x) &&
                    abs(it.x - (rate?.x ?: Double.NEGATIVE_INFINITY)) > geometry.font
            }.sortedBy { it.x }.fold(mutableListOf<OcrWord>()) { anchors, word ->
                if (anchors.lastOrNull()?.let { word.x - it.x > geometry.font * 2 } != false) anchors += word
                anchors
            }
            val serial = nearby.firstOrNull { it.label in setOf("SR", "SNO", "SL") && it.x < description.x }
            val codeLayout = nearby.any { it.label.contains("BATCH") } && quantity.x > (unit?.x ?: quantity.x)
            val centers = buildMap {
                put(Column.DESCRIPTION, description.x)
                put(Column.QUANTITY, qtyX)
                put(Column.AMOUNT, amount.x)
                hsn?.let { put(Column.HSN, it.x) }
                rate?.let { put(Column.RATE, it.x) }
                unit?.let { put(Column.UNIT, it.x) }
                serial?.let { put(Column.SERIAL, it.x) }
                if (tax.size >= 2) put(Column.TAX_TYPE, tax.first().x)
                tax.lastOrNull()?.let { put(Column.TAX_RATE, it.x) }
            }
            val headerTop = listOfNotNull(description, quantity, amount, rate, hsn, unit)
                .minOf { it.boundingBox.top }
            val hsnCodeLabel = nearby.firstOrNull { it.label == "CODE" && hsn != null && abs(it.x - hsn.x) < 2 * geometry.font }
            val qtyUnitLabel = nearby.firstOrNull { it.label == "NOS" && abs(it.x - qtyX) < 3 * geometry.font }
            val currencyLabel = nearby.firstOrNull { it.x > qtyX && "INR" in it.label }
            val rowOffsets = buildMap {
                if (codeLayout && hsnCodeLabel != null) {
                    qtyUnitLabel?.let { put(Column.QUANTITY, hsnCodeLabel.y - it.y) }
                    currencyLabel?.let {
                        put(Column.RATE, hsnCodeLabel.y - it.y)
                        put(Column.AMOUNT, hsnCodeLabel.y - it.y)
                    }
                }
            }
            val descriptionRight = hsn?.let { it.x - geometry.font * if (codeLayout) 2.6 else 2.3 }
            return Columns(centers, headerTop, description.y + geometry.font * .7, codeLayout, rowOffsets, descriptionRight)
        }
        return null
    }

    private fun uniqueHeading(candidates: List<OcrWord>, font: Double): OcrWord? {
        val first = candidates.firstOrNull() ?: return null
        return first.takeUnless { candidates.any { abs(it.x - first.x) > font } }
    }

    private data class TableResult(
        val items: List<ParsedInvoiceItem>,
        val quantityTotal: Double?,
        val taxableTotal: Long?
    )
    private data class Cell(val word: OcrWord, val column: Column, val y: Double, val text: String = word.text)

    private fun readTable(geometry: Geometry, table: Columns, layout: InvoiceLayoutType): TableResult {
        val font = geometry.font
        val ref = table.referenceX
        val footer = geometry.words.filter {
            it.label == "TOTAL" && it.x < table.centers.getValue(Column.QUANTITY) - font &&
                geometry.y(it, ref) > table.bodyStart + 2 * font
        }.minByOrNull { geometry.y(it, ref) }
        val footerY = footer?.let { geometry.y(it, ref) } ?: Double.POSITIVE_INFINITY
        val body = geometry.words.filter { geometry.y(it, ref) in table.bodyStart..(footerY - font * .8) }
        // Track lateral perspective drift using the right edges of repeated HSN cells.
        val hsnWords = body.filter {
            Column.HSN in table.centers && HSN.matches(cleanCell(it.text)) &&
                abs(it.x - ref) <= font * 3.5
        }.sortedBy { geometry.y(it, ref) }
        val first = hsnWords.take(3)
        val last = hsnWords.takeLast(3)
        val firstRight = first.map { it.boundingBox.right }.median()
        val height = if (last.isNotEmpty()) last.map { it.y }.average() - first.map { it.y }.average() else 0.0
        val drift = if (firstRight != null && height > font * 5) {
            ((last.map { it.boundingBox.right }.median()!! - firstRight) / height).coerceIn(-.12, .12)
        } else 0.0
        val firstY = first.firstOrNull()?.y ?: table.bodyStart
        val hsnSet = hsnWords.toHashSet()
        fun column(word: OcrWord): Column = if (word in hsnSet) Column.HSN else
            table.at(word.x - (word.y - firstY).coerceAtLeast(0.0) * drift)
        val cells = body.flatMap { word ->
            val column = column(word)
            // In layouts with adjacent Rate and Per columns, ML Kit sometimes joins these words.
            val pair = RATE_AND_UNIT.matchEntire(cleanCell(word.text))
            if (layout == InvoiceLayoutType.MSTEEL_MBW && pair != null && column in setOf(Column.RATE, Column.UNIT)) {
                listOf(Cell(word, Column.RATE, geometry.y(word, ref), pair.groupValues[1]),
                    Cell(word, Column.UNIT, geometry.y(word, ref), pair.groupValues[2]))
            } else {
                val rowY = when {
                    column in table.rowOffsets -> word.y + table.rowOffsets.getValue(column)
                    table.itemCodeInDescription && column in setOf(Column.DESCRIPTION, Column.SERIAL) ->
                        geometry.descriptionY(word, ref)
                    else -> geometry.y(word, ref)
                }
                listOf(Cell(word, column, rowY))
            }
        }
        val hsnAnchors = cells.filter { it.column == Column.HSN && HSN.matches(cleanCell(it.word.text)) }
            .map { it.y }
        val otherAnchors = cells.filter { cell ->
            val text = cleanCell(cell.word.text)
            when {
                table.itemCodeInDescription -> cell.column in setOf(Column.DESCRIPTION, Column.SERIAL) &&
                    NUMERIC_CODE.matches(text.trimEnd('-')) && cell.word.x < table.centers.getValue(Column.DESCRIPTION)
                cell.column == Column.SERIAL -> (SERIAL.matches(text) || SERIAL_PREFIX.containsMatchIn(text)) &&
                    cells.any { it.column == Column.DESCRIPTION && abs(it.y - cell.y) <= font * .8 }
                Column.HSN !in table.centers && cell.column == Column.DESCRIPTION ->
                    text.any(Char::isLetter) && cells.any { it.column == Column.QUANTITY && abs(it.y - cell.y) <= font * .6 }
                else -> false
            }
        }.map { it.y }.filter { candidate ->
            hsnAnchors.none { abs(it - candidate) < font * 1.2 }
        }
        val anchors = (hsnAnchors + otherAnchors).sorted().fold(mutableListOf<Double>()) { rows, row ->
            if (rows.lastOrNull()?.let { row - it >= font * .75 } != false) rows += row
            rows
        }
        val rows = List(anchors.size) { mutableListOf<Cell>() }
        for (cell in cells) {
            val nearest = anchors.indices.minByOrNull { abs(anchors[it] - cell.y) } ?: continue
            val distance = abs(anchors[nearest] - cell.y)
            val description = cell.column in setOf(Column.DESCRIPTION, Column.SERIAL)
            val previous = anchors.indexOfLast { it <= cell.y }
            val following = anchors.getOrNull(nearest + if (cell.y > anchors[nearest]) 1 else -1)
            when {
                distance <= font * (if (description) 1.05 else .82) &&
                    (description || following == null || abs(following - cell.y) - distance > font * .12) ->
                    rows[nearest] += cell
                description && cell.text.any(Char::isLetter) && previous >= 0 &&
                    cell.y - anchors[previous] <= font * 2.4 ->
                    rows[previous] += cell
            }
        }
        val items = rows.mapIndexed { index, row -> makeItem(index + 1, row, layout, table.itemCodeInDescription, font) }
        val totals = geometry.words.filter { footer != null && abs(geometry.y(it, ref) - footerY) <= font }
        val qtyTotal = totals.filter { column(it) == Column.QUANTITY }.singleOrNull()
            ?.let { InvoiceNumbers.quantity(cleanCell(it.text)) }
        // The bicycle table's footer is under its pre-tax line-amount column.
        val amountTotal = if (table.itemCodeInDescription) totals.filter { column(it) == Column.AMOUNT }
            .singleOrNull()?.let { InvoiceNumbers.paise(cleanCell(it.text), allowZero = true) } else null
        return TableResult(items, qtyTotal, amountTotal)
    }

    private fun makeItem(
        number: Int, cells: List<Cell>, layout: InvoiceLayoutType, hasCode: Boolean, font: Double
    ): ParsedInvoiceItem {
        val flags = mutableMapOf<String, FieldQuality>()
        fun words(column: Column) = cells.filter { it.column == column }.sortedBy { it.word.x }
        fun <T> field(name: String, column: Column, parse: (String) -> T?): T? {
            val source = words(column).map { cleanCell(it.text) }.filter(String::isNotBlank)
            val value = source.takeIf { it.isNotEmpty() }?.joinToString(" ")?.let(parse)
            flags[name] = when {
                value != null -> FieldQuality.EXACT
                source.isEmpty() -> FieldQuality.MISSING
                else -> FieldQuality.PARTIAL
            }
            return value
        }
        val descriptionCells = cells.filter { cell ->
            cell.column == Column.DESCRIPTION || (cell.column == Column.SERIAL && cell.word.text.any(Char::isLetter)) ||
                (hasCode && cell.column == Column.SERIAL && NUMERIC_CODE.matches(cleanCell(cell.word.text).trimEnd('-')))
        }.sortedBy { it.y }
        val descriptionLines = mutableListOf<MutableList<Cell>>()
        for (cell in descriptionCells) {
            val line = descriptionLines.lastOrNull()?.takeIf { cell.y - it.first().y < font * .9 }
                ?: mutableListOf<Cell>().also(descriptionLines::add)
            line += cell
        }
        val desc = descriptionLines.flatMap { it.sortedBy { cell -> cell.word.x } }
            .map { cleanCell(it.word.text) }.filter(String::isNotBlank).toMutableList()
        var itemCode: String? = null
        if (hasCode) {
            val codeIndex = desc.take(2).indexOfFirst { NUMERIC_CODE.matches(it.trimEnd('-')) }
            val first = desc.getOrNull(codeIndex)?.trimEnd('-').orEmpty()
            if (codeIndex >= 0) {
                repeat(codeIndex + 1) { desc.removeAt(0) }
                if (desc.firstOrNull() == "-") desc.removeAt(0)
                // This layout prints a ten-digit material code. Short or joined text stays unassigned.
                itemCode = first.takeIf { it.length == 10 }
            }
            flags["itemCode"] = when {
                itemCode != null -> FieldQuality.EXACT
                first.isEmpty() -> FieldQuality.MISSING
                else -> FieldQuality.PARTIAL
            }
        } else if (layout == InvoiceLayoutType.MSTEEL_MBW && desc.isNotEmpty()) {
            desc[0] = desc[0].replaceFirst(SERIAL_PREFIX, "")
        }
        val description = desc.joinToString(" ").trim()
        flags["description"] = if (description.isBlank()) FieldQuality.MISSING else FieldQuality.EXACT
        val hsn = field("hsnCode", Column.HSN) { it.takeIf(HSN::matches) }
        val qty = field("quantity", Column.QUANTITY) {
            // A comma followed by three digits is ambiguous in a column printed with three
            // decimal places. It could be a misread dot. Keep that cell for review.
            if (hasCode && ',' in it && '.' !in it) null else InvoiceNumbers.quantity(it)
        }
        val price = field("unitRate", Column.RATE, InvoiceNumbers::paise)
        val amount = field("lineAmount", Column.AMOUNT) { InvoiceNumbers.paise(it, allowZero = true) }
        val taxRate = field("taxRate", Column.TAX_RATE, InvoiceNumbers::percentage)
        val unit = field("unit", Column.UNIT) { cell ->
            cell.takeIf { Regex("""[A-Za-z]+""").matches(it) }
        }
        return ParsedInvoiceItem(
            lineNumber = number,
            itemCode = itemCode,
            description = description,
            hsnCode = hsn,
            quantity = qty,
            unit = unit,
            unitRatePaise = price,
            gstRatePercent = taxRate,
            lineAmountPaise = amount,
            rawLines = cells.sortedWith(compareBy({ it.column.ordinal }, { it.word.x }))
                .distinctBy { it.word }.map { it.word.text },
            boundingBox = cells.map { it.word.boundingBox }.takeIf { it.isNotEmpty() }?.let { boxes ->
                OcrBoundingBox(boxes.minOf { it.left }, boxes.minOf { it.top },
                    boxes.maxOf { it.right }, boxes.maxOf { it.bottom })
            },
            qualityFlags = flags
        )
    }

    private fun inlineNumber(geometry: Geometry, before: Int?): String? {
        val words = geometry.words.filter { before == null || it.y < before }
        val candidates = words.filter { it.label == "INVOICE" }.flatMap { label ->
            val row = words.filter {
                it.x > label.x && abs(geometry.y(it, label.x) - label.y) <= geometry.font * .55
            }.sortedBy { it.x }
            if (row.firstOrNull()?.label != "NO") emptyList() else listOfNotNull(
                row.getOrNull(1)?.text?.trim(':', '|')?.takeIf {
                    Regex("""[A-Za-z0-9]+(?:[-/][A-Za-z0-9]+)*""").matches(it) && it.any(Char::isDigit)
                }
            )
        }.distinct()
        return candidates.singleOrNull()
    }

    private fun labeledTotal(text: String, pattern: Regex): String? =
        pattern.findAll(text).map { it.groupValues[1].trim() }.distinct().toList().singleOrNull()

    companion object {
        private val GSTIN = Regex("""\b[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]\b""")
        private val HSN = Regex("""[0-9]{4}|[0-9]{6}|[0-9]{8}""")
        private val INVOICE_NO = Regex(
            """(?im)^\s*(?:Invoice|Inv\.?|Bill)[ \t]+No\.?[ \t:]+([A-Za-z0-9]+(?:[-/][A-Za-z0-9]+)*)(?=[ \t]|$)"""
        )
        private val DATE = Regex("""(?im)^\s*(?:Date|Dt\.?)[ \t:]+(\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4})\s*$""")
        private val CONSIGNEE = Regex("""(?im)^\s*(?:Consignee|Ship to)[ \t]*:[ \t]*(\S[^\r\n]*)$""")
        private val QUANTITY_TOTAL = Regex("""(?im)^[ \t]*Total[ \t]+(?:Quantity|Qty)[ \t:]+([^\r\n]+)$""")
        private val TAXABLE_TOTAL = Regex("""(?im)^[ \t]*(?:Total[ \t]+)?Taxable[ \t]+(?:Value|Amount)[ \t:]+([^\r\n]+)$""")
        private val RATE_AND_UNIT = Regex(
            """([0-9][0-9.,]*)(?:[|\]/ ])?(Gross|Grs|Pcs?|Doz|Pair|Sets?)""", RegexOption.IGNORE_CASE
        )
        private val SERIAL = Regex("""[0-9]{1,3}\.?""")
        private val SERIAL_PREFIX = Regex("""^[0-9]{1,3}(?=[A-Z])""", RegexOption.IGNORE_CASE)
        private val NUMERIC_CODE = Regex("""[0-9]{5,13}""")
    }
}

private val OcrWord.x: Double get() = (boundingBox.left + boundingBox.right) / 2.0
private val OcrWord.y: Double get() = (boundingBox.top + boundingBox.bottom) / 2.0
private val OcrWord.label: String get() = text.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() || it == '%' }
private fun cleanCell(text: String): String = text.trim().trim('|', '[', ']').trim()
private fun <T : Comparable<T>> List<T>.median(): T? = sorted().getOrNull(size / 2)
