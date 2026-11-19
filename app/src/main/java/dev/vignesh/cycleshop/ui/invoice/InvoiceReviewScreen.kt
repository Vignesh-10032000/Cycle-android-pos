package dev.vignesh.cycleshop.ui.invoice

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.NumberFormat
import java.util.Locale

/**
 * Premium editable review screen for structured invoice parsing & local product matching.
 *
 * Displays:
 * - Original invoice photo thumbnail / preview
 * - Extracted metadata (Supplier, GSTIN, Inv No, Date)
 * - Reconciliation status banner (Quantities & Amounts)
 * - Extracted product table rows with editable fields
 * - Suggested Room inventory match candidates
 * - Discrepancy flags (missing HSN, math discrepancy, unlinked SKU)
 * - Safe image cleanup controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceReviewScreen(
    invoice: ParsedInvoice,
    imageUri: Uri?,
    onNavigateBack: () -> Unit,
    onSaveReviewed: (ParsedInvoice) -> Unit,
    onDeleteImageAndExit: () -> Unit
) {
    var editableInvoice by remember { mutableStateOf(invoice) }
    var showImagePreview by remember { mutableStateOf(false) }
    var showStageConfirmDialog by remember { mutableStateOf(false) }

    if (showStageConfirmDialog) {
        val verifiedCount = editableInvoice.items.count { (it.selectedVariantId ?: it.bestCandidate?.variantId) != null }
        val pendingCount = editableInvoice.items.size - verifiedCount
        AlertDialog(
            onDismissRequest = { showStageConfirmDialog = false },
            title = {
                Text(
                    text = "Stage Purchase Invoice",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stage this supplier invoice into the purchase database for audit and review?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Supplier: ${editableInvoice.supplierName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                            Text("• Invoice #: ${editableInvoice.invoiceNumber ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                            Text("• Total Items: ${editableInvoice.items.size}", style = MaterialTheme.typography.bodySmall)
                            Text("• Verified Matches: $verifiedCount", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            Text("• Pending Review: $pendingCount", style = MaterialTheme.typography.bodySmall, color = if (pendingCount > 0) Color(0xFFE65100) else Color.Unspecified)
                        }
                    }
                    Text(
                        text = "🔒 Safety Rule (Phase 3 Step 3A): Staging saves this invoice in the database with status CONFIRMED_STAGED. ZERO stock movements will be created and inventory ledger balances will NOT be changed until Step 3B owner confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStageConfirmDialog = false
                        onSaveReviewed(editableInvoice)
                    }
                ) {
                    Text("Confirm Staging")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStageConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Invoice Review & Matching",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${editableInvoice.supplierName ?: "Supplier"} • ${editableInvoice.invoiceNumber?.let { "#$it" } ?: "No Inv #"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (imageUri != null) {
                        IconButton(onClick = { showImagePreview = !showImagePreview }) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Toggle Photo Preview",
                                tint = if (showImagePreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeleteImageAndExit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Discard & Delete")
                    }

                    Button(
                        onClick = { showStageConfirmDialog = true },
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stage Invoice (No Stock)")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Optional expanded image preview
            if (showImagePreview && imageUri != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Invoice Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Document Header Summary Card
            item {
                InvoiceHeaderCard(editableInvoice)
            }

            // Totals & Reconciliation Status Banner
            item {
                ReconciliationBanner(editableInvoice)
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracted Line Items (${editableInvoice.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Match Rate: ${editableInvoice.matchRatePercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (editableInvoice.matchRatePercent > 50) Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Editable Line Items
            itemsIndexed(editableInvoice.items) { index, item ->
                InvoiceItemCard(
                    item = item,
                    onItemUpdated = { updatedItem ->
                        val updatedList = editableInvoice.items.toMutableList()
                        updatedList[index] = updatedItem
                        editableInvoice = editableInvoice.copy(items = updatedList)
                    }
                )
            }
        }
    }
}

@Composable
private fun InvoiceHeaderCard(invoice: ParsedInvoice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.supplierName ?: "Unknown Supplier",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = invoice.layoutType.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            invoice.supplierGstin?.let { gstin ->
                Text(
                    text = "GSTIN: $gstin",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Invoice: ${invoice.invoiceNumber ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Date: ${invoice.invoiceDate ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            invoice.consigneeName?.let { consignee ->
                Text(
                    text = "Consignee: $consignee",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ReconciliationBanner(invoice: ParsedInvoice) {
    val differences = invoice.quantityReconciliation == Reconciliation.DIFFERENCE ||
        invoice.taxableReconciliation == Reconciliation.DIFFERENCE ||
        invoice.items.any { it.amountReconciliation == Reconciliation.DIFFERENCE }
    val isReconciled = !differences && invoice.quantityReconciliation == Reconciliation.MATCH &&
        invoice.taxableReconciliation == Reconciliation.MATCH
    val units = invoice.quantitiesByUnit.entries.joinToString(" + ") { (unit, qty) ->
        "${qty.stripTrailingZeros().toPlainString()} ${unit.ifBlank { "(unit missing)" }}"
    }.ifBlank { "Unavailable" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReconciled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isReconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isReconciled) Color(0xFF2E7D32) else Color(0xFFE65100),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        differences -> "Invoice totals need review"
                        isReconciled -> "Invoice totals match"
                        else -> "Reconciliation incomplete"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isReconciled) Color(0xFF1B5E20) else Color(0xFFBF360C)
                )
                Text(
                    text = "Read quantities: $units\nPrinted quantity total: ${invoice.totalQuantityExtracted ?: "Unavailable"}\nPrinted taxable total: ${invoice.totalAmountExtractedPaise?.let(::formatPaise) ?: "Unavailable"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isReconciled) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InvoiceItemCard(
    item: ParsedInvoiceItem,
    onItemUpdated: (ParsedInvoiceItem) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedDescription by remember { mutableStateOf(item.description) }
    var editedQty by remember { mutableStateOf(InvoiceNumbers.quantityDecimal(item.quantity)?.toPlainString().orEmpty()) }
    var editedRate by remember { mutableStateOf(item.unitRatePaise?.let { InvoiceNumbers.decimalRupees(it).toPlainString() }.orEmpty()) }
    var showAllCandidates by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Line # and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "#${item.lineNumber.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.isUserEdited) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEDE7F6)
                        ) {
                            Text(
                                text = "Edited",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF512DA8),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Item", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Description: View mode vs Edit mode
            if (isEditing) {
                val quantity = InvoiceNumbers.quantity(editedQty)
                val rate = InvoiceNumbers.paise(editedRate)
                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Product Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedQty,
                        onValueChange = { editedQty = it },
                        label = { Text("Qty") },
                        isError = editedQty.isNotBlank() && quantity == null,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editedRate,
                        onValueChange = { editedRate = it },
                        label = { Text("Rate (₹)") },
                        isError = editedRate.isNotBlank() && rate == null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = (editedQty.isBlank() || quantity != null) &&
                            (editedRate.isBlank() || rate != null),
                        onClick = {
                            onItemUpdated(
                                item.copy(
                                    description = editedDescription,
                                    quantity = quantity,
                                    unitRatePaise = rate,
                                    isUserEdited = true
                                )
                            )
                            isEditing = false
                        }
                    ) {
                        Text("Apply")
                    }
                }
            } else {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Column Metrics Row (Qty, Rate, HSN, Tax, Amount)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetricChip(label = "Qty", value = "${InvoiceNumbers.quantityDecimal(item.quantity)?.stripTrailingZeros()?.toPlainString() ?: "N/A"} ${item.unit ?: ""}")
                item.unitRatePaise?.let { rate ->
                    MetricChip(label = "Rate", value = formatPaise(rate))
                }
                item.hsnCode?.let { hsn ->
                    MetricChip(label = "HSN", value = hsn)
                }
                item.gstRatePercent?.let { gst ->
                    MetricChip(label = "GST", value = "${gst}%")
                }
                item.lineAmountPaise?.let { amount ->
                    MetricChip(label = "Total", value = formatPaise(amount), isPrimary = true)
                }
            }

            if (item.amountReconciliation == Reconciliation.DIFFERENCE) {
                Text("Quantity × rate differs from the printed line amount.", color = MaterialTheme.colorScheme.error)
            }
            val unreadable = listOf("quantity" to "quantity", "unitRate" to "rate", "lineAmount" to "line amount")
                .filter { (field, _) -> item.qualityFlags[field]?.let { it != FieldQuality.EXACT } == true }
                .joinToString { it.second }
            if (unreadable.isNotEmpty()) {
                Text("Check OCR reading for $unreadable.", color = MaterialTheme.colorScheme.error)
            }
            if (item.quantity != null) {
                val unitIssue = when (val conversion = UnitConversionEngine.convert(item.unit, item.quantity)) {
                    is UnitConversionResult.RequiresReview -> conversion.warningMessage
                    is UnitConversionResult.Invalid -> conversion.errorMessage
                    is UnitConversionResult.Success -> null
                }
                unitIssue?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Room Product Match Section
            val bestCandidate = item.bestCandidate
            if (bestCandidate != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (item.matchConfidence) {
                        MatchConfidence.EXACT_SKU -> Color(0xFFE8F5E9)
                        MatchConfidence.HIGH_SIMILARITY -> Color(0xFFE3F2FD)
                        else -> Color(0xFFFFF8E1)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = if (item.matchConfidence == MatchConfidence.EXACT_SKU) Color(0xFF2E7D32) else Color(0xFF1565C0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Catalog Match",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${(bestCandidate.confidenceScore * 100).toInt()}% Match",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (bestCandidate.confidenceScore >= 0.85f) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }

                        Text(
                            text = "${bestCandidate.brandName} ${bestCandidate.productName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "SKU: ${bestCandidate.variantSku} • ${bestCandidate.variantSize ?: "Free Size"} • ${bestCandidate.variantColor ?: "Standard"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Unlinked: No existing SKU in local database",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFBF360C)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, isPrimary: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatPaise(paise: Long): String {
    val rupees = InvoiceNumbers.decimalRupees(paise)
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    return formatter.format(rupees).replace("₹", "₹ ")
}
