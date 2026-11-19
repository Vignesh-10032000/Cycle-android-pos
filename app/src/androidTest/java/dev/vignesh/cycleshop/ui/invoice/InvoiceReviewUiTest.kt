package dev.vignesh.cycleshop.ui.invoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vignesh.cycleshop.ui.theme.VglTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InvoiceReviewUiTest {
    @get:Rule val compose = createComposeRule()
    private val catalog = listOf(ReviewVariant(4L, "Tube", "SKU-4"), ReviewVariant(5L, "Valve", "SKU-5"))
    private val active = catalog.map { it.id }.toSet()

    @Test
    fun choosingThenVerifyingAndEditingUpdatesProgressAndApproval() {
        val exact = ProductMatchCandidate(4, 1, "Tube", "Test", "SKU-4", null, null, null, 100L, 200L, 1f, "Exact")
        val invoice = ParsedInvoice("Sample Supplier", "SAMPLE-1", null, null, null, null,
            listOf(ParsedInvoiceItem(1, description = "OCR tube", quantity = 2.0, unit = "PC",
                unitRatePaise = 100L, lineAmountPaise = 200L, suggestedCandidates = listOf(exact))),
            2.0, 200L, InvoiceLayoutType.GENERIC_GST, "OCR tube 2")
        show(InvoiceReviewSession.from(invoice))

        compose.onNodeWithTag("approve-invoice").assertIsNotEnabled()
        compose.onNodeWithTag("invoice-review-list").performScrollToIndex(2)
        compose.onNodeWithTag("line-0-status").assertTextContains("Suggested match", substring = true)
        compose.onNodeWithTag("line-0-verify").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("line-0-select").performScrollTo().performClick()
        compose.onNodeWithTag("variant-4").performClick()
        compose.onNodeWithTag("line-0-status").performScrollTo().assertTextContains("Owner selected", substring = true)
        compose.onNodeWithTag("approve-invoice").assertIsNotEnabled()
        compose.onNodeWithTag("line-0-verify").performScrollTo().performClick()
        compose.onNodeWithText("1 / 1 lines verified").assertExists()
        compose.onNodeWithTag("approve-invoice").assertIsEnabled().performClick()
        compose.onNodeWithText("Confirm approval").performClick()
        compose.onNodeWithText("Approved in this session.", substring = true).assertExists()

        compose.onNodeWithTag("line-0-edit").performScrollTo().performClick()
        compose.onNodeWithTag("edit-quantity").performScrollTo().performTextReplacement("3")
        compose.onNodeWithText("Apply changes").performClick()
        compose.onNodeWithText("0 / 1 lines verified").assertExists()
        compose.onNodeWithTag("line-0-current").performScrollTo().assertTextContains("Qty 3 PC", substring = true)
        compose.onNodeWithTag("line-0-original").assertTextContains("Qty 2 PC", substring = true)
        compose.onNodeWithTag("approve-invoice").assertIsNotEnabled()
        compose.onNodeWithTag("line-0-reject").performScrollTo().performClick()
        compose.onNodeWithTag("line-0-status").performScrollTo().assertTextContains("Rejected", substring = true)
        compose.onNodeWithTag("line-0-verify").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun realStryderScanLeavesAllSevenMissingQuantitiesUnverified() {
        val invoice = runBlocking { parseInvoicePhoto("B") }
        assertEquals(62.0, invoice.totalQuantityExtracted!!, 0.0)
        val review = InvoiceReviewSession.from(invoice)
        val missing = review.lines.indices.filter { review.lines[it].original.quantity == null }
        assertEquals(7, missing.size)
        for (index in missing) {
            // Resolve all other mandatory fields to isolate the effect of the missing quantity.
            val line = review.lines[index]
            val readyExceptQuantity = line.edit(line.values.copy(description = "Test line", unit = "PC",
                ratePaise = 100L, amountPaise = 100L, variantId = 4L))
            assertNull(readyExceptQuantity.verify(active, 100).verification)
        }
        assertFalse(review.canApprove(active))

        show(review)
        compose.onNodeWithText("0 / 29 lines verified").assertExists()
        compose.onNodeWithTag("approve-invoice").assertIsNotEnabled()
        for (index in missing) {
            compose.onNodeWithTag("invoice-review-list").performScrollToIndex(index + 2)
            compose.onNodeWithTag("line-$index-status").assertTextContains("Needs review", substring = true)
            compose.onNodeWithTag("line-$index-original").performScrollTo().assertTextContains("Qty Missing", substring = true)
            compose.onNodeWithTag("line-$index-verify").performScrollTo().assertIsNotEnabled()
        }
    }

    private fun show(initial: InvoiceReviewSession) {
        compose.setContent {
            var review by remember { mutableStateOf(initial) }
            VglTheme {
                InvoiceReviewScreen(review, catalog, imageUri = null, saving = false, stagedInvoiceId = null,
                    onNavigateBack = {},
                    onEditHeader = { supplier, invoice -> review = review.editHeader(supplier, invoice) },
                    onEditLine = { index, change -> review = review.updateLine(index, change) },
                    onVerifyLine = { index -> review = review.updateLine(index) { it.verify(active, 100) } },
                    onApprove = { review = review.approve(active, 200) },
                    onStage = {},
                    onDeleteImageAndExit = {}
                )
            }
        }
    }
}
