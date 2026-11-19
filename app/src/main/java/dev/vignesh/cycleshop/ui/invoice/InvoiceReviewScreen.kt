package dev.vignesh.cycleshop.ui.invoice

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InvoiceReviewScreen(
    review: InvoiceReviewSession,
    catalog: List<ReviewVariant>,
    imageUri: Uri?,
    saving: Boolean,
    stagedInvoiceId: Long?,
    onNavigateBack: () -> Unit,
    onEditHeader: (String, String) -> Unit,
    onEditLine: (Int, (ReviewLine) -> ReviewLine) -> Unit,
    onVerifyLine: (Int) -> Unit,
    onApprove: () -> Unit,
    onStage: () -> Unit,
    onDeleteImageAndExit: () -> Unit
) {
    val byId = remember(catalog) { catalog.associateBy { it.id } }
    val activeIds = byId.keys
    val verifiedCount = review.verifiedCount(activeIds)
    val approved = review.isApproved(activeIds)
    val editable = !saving && stagedInvoiceId == null
    val storageIssues = remember(review) { review.v2StagingIssues() }
    val displayedInvoice = remember(review) { review.reviewedInvoice() }
    var showImage by remember { mutableStateOf(false) }
    var showApprove by remember { mutableStateOf(false) }
    var showHeaderEditor by remember { mutableStateOf(false) }

    if (showApprove) {
        AlertDialog(
            onDismissRequest = { showApprove = false },
            title = { Text("Approve invoice review?") },
            text = { Text("All $verifiedCount lines have been verified in this session. Approval here leaves stock unchanged.") },
            confirmButton = {
                Button(enabled = editable && review.canApprove(activeIds), onClick = {
                    showApprove = false
                    onApprove()
                }) { Text("Confirm approval") }
            },
            dismissButton = { TextButton(onClick = { showApprove = false }) { Text("Cancel") } }
        )
    }
    if (showHeaderEditor) {
        HeaderEditor(review, onDismiss = { showHeaderEditor = false }) { supplier, number ->
            onEditHeader(supplier, number)
            showHeaderEditor = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review invoice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to scan")
                    }
                },
                actions = {
                    if (imageUri != null) {
                        IconButton(onClick = { showImage = !showImage }) {
                            Icon(Icons.Default.Image, contentDescription = "Toggle invoice photo")
                        }
                    }
                    IconButton(enabled = editable, onClick = onDeleteImageAndExit) {
                        Icon(Icons.Default.Delete, contentDescription = "Discard scan")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$verifiedCount / ${review.lines.size} lines verified", fontWeight = FontWeight.SemiBold)
                    if (stagedInvoiceId != null) {
                        Text("Invoice staged for review. Stock is unchanged.")
                    } else if (approved) {
                        Text("Approved in this session. Verification must be repeated after reopening.")
                        if (storageIssues.isNotEmpty()) {
                            Text("This review cannot be saved with the current database format.", color = MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = onStage, enabled = !saving && storageIssues.isEmpty(), modifier = Modifier.fillMaxWidth()) {
                            Text(if (saving) "Saving…" else "Save staged invoice")
                        }
                    } else {
                        if (!review.headerComplete) Text("Enter the supplier and invoice number.", style = MaterialTheme.typography.bodySmall)
                        Button(
                            enabled = review.canApprove(activeIds) && editable,
                            onClick = { showApprove = true },
                            modifier = Modifier.fillMaxWidth().testTag("approve-invoice")
                        ) { Text("Approve review") }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("invoice-review-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showImage && imageUri != null) {
                item { AsyncImage(imageUri, "Original invoice photo", Modifier.fillMaxWidth().height(280.dp)) }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(review.supplierName.ifBlank { "Supplier missing" }, style = MaterialTheme.typography.titleMedium)
                        Text("Invoice number: ${review.invoiceNumber.ifBlank { "Missing" }}")
                        if (review.supplierName != review.originalInvoice.supplierName.orEmpty() ||
                            review.invoiceNumber != review.originalInvoice.invoiceNumber.orEmpty()) {
                            Text("OCR header: ${review.originalInvoice.supplierName ?: "Missing"} / ${review.originalInvoice.invoiceNumber ?: "Missing"}")
                        }
                        review.originalInvoice.supplierGstin?.let { Text("GSTIN: $it") }
                        review.originalInvoice.invoiceDate?.let { Text("Date: $it") }
                        TextButton(enabled = editable, onClick = { showHeaderEditor = true }) { Text("Edit invoice details") }
                    }
                }
            }
            item {
                val difference = displayedInvoice.quantityReconciliation == Reconciliation.DIFFERENCE ||
                    displayedInvoice.taxableReconciliation == Reconciliation.DIFFERENCE
                val units = displayedInvoice.quantitiesByUnit.entries.joinToString(" + ") { (unit, qty) ->
                    "${qty.stripTrailingZeros().toPlainString()} ${unit.ifBlank { "(unit missing)" }}"
                }.ifBlank { "Unavailable" }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Line quantities: $units")
                    Text("Printed quantity: ${displayedInvoice.totalQuantityExtracted ?: "Unavailable"}")
                    Text("Printed taxable total: ${displayedInvoice.totalAmountExtractedPaise?.let(::formatPaise) ?: "Unavailable"}")
                    if (difference) Text("Line values differ from the printed totals.", color = MaterialTheme.colorScheme.error)
                    if (approved) storageIssues.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            itemsIndexed(review.lines, key = { index, _ -> index }) { index, line ->
                ReviewLineCard(index, line, byId, editable,
                    onEdit = { change -> onEditLine(index, change) },
                    onVerify = { onVerifyLine(index) })
            }
        }
    }
}

@Composable
private fun ReviewLineCard(
    index: Int,
    line: ReviewLine,
    catalog: Map<Long, ReviewVariant>,
    editable: Boolean,
    onEdit: ((ReviewLine) -> ReviewLine) -> Unit,
    onVerify: () -> Unit
) {
    val lineTag = "line-$index"
    val status = line.status(catalog.keys)
    val selected = catalog[line.values.variantId]
    val issues = line.issues(catalog.keys)
    val suggestion = line.evidence.suggestedCandidates.firstOrNull()
    var editing by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var showEvidence by remember { mutableStateOf(false) }

    if (editing) {
        LineEditor(line.values, onDismiss = { editing = false }) { values ->
            onEdit { it.edit(values.copy(variantId = it.values.variantId)) }
            editing = false
        }
    }
    if (choosing) {
        VariantPicker(catalog, line.evidence.suggestedCandidates, onDismiss = { choosing = false }) { variant ->
            onEdit { it.edit(it.values.copy(variantId = variant.id)) }
            choosing = false
        }
    }

    Card(Modifier.fillMaxWidth().testTag(lineTag)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Line ${line.evidence.lineNumber} · ${status.label}", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("$lineTag-status"))
            Text("${line.values.description.ifBlank { "Description missing" }}", fontWeight = FontWeight.SemiBold)
            Text("Current: ${numericValues(line.values)}", modifier = Modifier.testTag("$lineTag-current"))
            Text("OCR: ${line.original.description.ifBlank { "Description missing" }}")
            Text(numericValues(line.original), modifier = Modifier.testTag("$lineTag-original"))
            if (line.evidence.qualityFlags.any { it.value != FieldQuality.EXACT }) {
                val observedIssues = line.evidence.qualityFlags.filterValues { it != FieldQuality.EXACT }.keys
                    .joinToString { fieldLabels[it] ?: it }
                Text("Uncertain OCR: $observedIssues", style = MaterialTheme.typography.bodySmall)
            }
            if (line.asParsedItem().amountReconciliation == Reconciliation.DIFFERENCE) {
                Text("Quantity × rate differs from the line amount.", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = { showEvidence = !showEvidence }) {
                Text(if (showEvidence) "Hide OCR evidence" else "Show OCR evidence")
            }
            if (showEvidence) {
                Text(line.evidence.rawLines.joinToString(" ").ifBlank { "No OCR text for this line." })
                line.evidence.boundingBox?.let { Text("Image region: ${it.left}, ${it.top} to ${it.right}, ${it.bottom}") }
            }
            HorizontalDivider()
            suggestion?.let { Text("Suggested: ${it.productName} · ${it.variantSku}", style = MaterialTheme.typography.bodySmall) }
            Text(selected?.let { "Selected: ${it.name} · ${it.sku}" }
                ?: if (line.values.variantId != null) "Selected variant unavailable" else "No variant selected")
            issues.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = editable, onClick = { editing = true }, modifier = Modifier.testTag("$lineTag-edit")) {
                    Text("Edit values")
                }
                OutlinedButton(enabled = editable && catalog.isNotEmpty(), onClick = { choosing = true },
                    modifier = Modifier.testTag("$lineTag-select")) {
                    Text("Choose variant")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = editable && !line.rejected && issues.isEmpty() && status != OwnerReviewStatus.OWNER_VERIFIED,
                    onClick = onVerify, modifier = Modifier.testTag("$lineTag-verify")) { Text("Verify line") }
                TextButton(enabled = editable, onClick = { onEdit { it.setRejected(!it.rejected) } },
                    modifier = Modifier.testTag("$lineTag-reject")) {
                    Text(if (line.rejected) "Reopen line" else "Reject line")
                }
            }
        }
    }
}

@Composable
private fun LineEditor(original: ReviewValues, onDismiss: () -> Unit, onApply: (ReviewValues) -> Unit) {
    var description by remember { mutableStateOf(original.description) }
    var quantity by remember { mutableStateOf(quantityText(original.quantity).orEmpty()) }
    var unit by remember { mutableStateOf(original.unit.orEmpty()) }
    var rate by remember { mutableStateOf(decimalText(original.ratePaise)) }
    var amount by remember { mutableStateOf(decimalText(original.amountPaise)) }
    val parsedQuantity = InvoiceNumbers.quantity(quantity)
    val parsedRate = InvoiceNumbers.paise(rate)
    val parsedAmount = InvoiceNumbers.paise(amount, allowZero = true)
    val invalidQuantity = quantity.isNotBlank() && parsedQuantity == null
    val invalidRate = rate.isNotBlank() && parsedRate == null
    val invalidAmount = amount.isNotBlank() && parsedAmount == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Correct line values") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(description, { description = it }, label = { Text("Description") })
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, isError = invalidQuantity,
                    modifier = Modifier.testTag("edit-quantity"))
                OutlinedTextField(unit, { unit = it }, label = { Text("Invoice unit") },
                    modifier = Modifier.testTag("edit-unit"))
                OutlinedTextField(rate, { rate = it }, label = { Text("Rate (₹)") }, isError = invalidRate,
                    modifier = Modifier.testTag("edit-rate"))
                OutlinedTextField(amount, { amount = it }, label = { Text("Line amount (₹)") }, isError = invalidAmount,
                    modifier = Modifier.testTag("edit-amount"))
            }
        },
        confirmButton = {
            Button(enabled = !invalidQuantity && !invalidRate && !invalidAmount, onClick = {
                onApply(ReviewValues(description.trim(), parsedQuantity, unit.trim().takeIf(String::isNotBlank),
                    parsedRate, parsedAmount, original.variantId))
            }) { Text("Apply changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun HeaderEditor(review: InvoiceReviewSession, onDismiss: () -> Unit, onApply: (String, String) -> Unit) {
    var supplier by remember { mutableStateOf(review.supplierName) }
    var number by remember { mutableStateOf(review.invoiceNumber) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Invoice details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(supplier, { supplier = it }, label = { Text("Supplier") }, singleLine = true)
                OutlinedTextField(number, { number = it }, label = { Text("Invoice number") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onApply(supplier.trim(), number.trim()) }) { Text("Apply details") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun VariantPicker(
    catalog: Map<Long, ReviewVariant>,
    suggestions: List<ProductMatchCandidate>,
    onDismiss: () -> Unit,
    onChoose: (ReviewVariant) -> Unit
) {
    val rank = remember(suggestions) { suggestions.mapIndexed { index, candidate -> candidate.variantId to index }.toMap() }
    var query by remember { mutableStateOf("") }
    val results = remember(catalog, query, rank) {
        val search = query.trim()
        catalog.values.filter { it.name.contains(search, ignoreCase = true) || it.sku.contains(search, ignoreCase = true) }
            .sortedWith(compareBy({ rank[it.id] ?: Int.MAX_VALUE }, { it.name }, { it.sku }))
    }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Choose a catalog variant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Search by product or SKU") }, singleLine = true)
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(results, key = { it.id }) { variant ->
                        TextButton(onClick = { onChoose(variant) }, modifier = Modifier.fillMaxWidth().testTag("variant-${variant.id}")) {
                            Text("${variant.name} · ${variant.sku}${if (variant.id in rank) " (suggested)" else ""}")
                        }
                    }
                }
                if (results.isEmpty()) Text("No matching active variants.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val fieldLabels = mapOf("itemCode" to "item code", "hsnCode" to "HSN", "unitRate" to "rate",
    "lineAmount" to "line amount", "taxRate" to "tax rate")

private fun numericValues(values: ReviewValues): String =
    "Qty ${quantityText(values.quantity) ?: "Missing"} ${values.unit ?: "(unit missing)"} · " +
        "Rate ${values.ratePaise?.let(::formatPaise) ?: "Missing"} · " +
        "Amount ${values.amountPaise?.let(::formatPaise) ?: "Missing"}"

private fun quantityText(quantity: Double?): String? =
    InvoiceNumbers.quantityDecimal(quantity)?.stripTrailingZeros()?.toPlainString()

private fun decimalText(paise: Long?): String =
    paise?.let { InvoiceNumbers.decimalRupees(it).toPlainString() }.orEmpty()

private fun formatPaise(paise: Long): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(InvoiceNumbers.decimalRupees(paise))
