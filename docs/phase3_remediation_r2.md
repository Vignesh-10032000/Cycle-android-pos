# Phase 3 remediation R2

R2 replaces fixed invoice answers with spatial OCR extraction and validates numbers at the parser, review screen, and staging boundary. It retains the existing v2 data model. The scope ends at purchase staging.

## What changed

| Files | Change |
| --- | --- |
| `InvoiceTableParser.kt`, `OcrResult.kt`, `MlKitInvoiceOcr.kt` | Preserve ML Kit word boxes and edge angles. Find columns from recognized headings and rows from observed body cells. Allow layout rules for printed formats, including joined Rate and Per cells. Return missing or ambiguous data for review. |
| `InvoiceNumbers.kt`, `InvoiceDataModels.kt` | Parse complete decimal fields, convert money to exact paise, and compare totals independently. Keep quantities grouped by their invoice units. |
| `UnitConversionEngine.kt`, `PurchaseValidationEngine.kt` | Reject non-finite, non-positive, overflowing, or non-integral stock quantities. Require review for unknown units and incompatible conversions. A Set can count as one Set. Conversion to Pieces requires an approved rule. |
| `InvoiceScanViewModel.kt`, `InvoiceReviewScreen.kt` | Use the shared OCR adapter and decimal input validation. Show warnings for unreadable cells, unresolved conversions, and reconciliation differences. Edits to quantity or rate keep the independently read line amount for comparison. |
| Parser, numeric, conversion and staging validator JVM tests, plus `InvoiceRealOcrDeviceTest.kt` and `r2-quantity-*.txt` | Regressions for header-only scans, missing rows, changed prices, shuffled OCR blocks, unknown layouts, malformed and ambiguous cells, and numeric limits. Photo ground truth stays in test assets. |

The old parser supplied invoice-number shortcuts, fixed 11-row and 29-row tables, a generic quantity of one, and calculated totals in place of printed totals. The ML Kit pipeline passed line-level geometry only. Catalog matching runs after parsing and adds suggestions while carrying through invoice fields. Its verification behavior belongs to R3.

## Before and after on OnePlus 9R

| Input | Before R2 | After R2 |
| --- | --- | --- |
| Supplier header only | Produced 11 M.STEEL or 29 Stryder rows and could self-reconcile | Zero rows, and no totals unless separately observed |
| Invoice A, M.STEEL / MBW | Returned 11 predefined rows and a combined quantity of 126 | Detected 11 actual rows. All 11 read quantities matched ground truth, yielding **50 Gross + 76 Pcs**. The printed 126 remains visible as a mixed-unit document total. |
| Invoice B, Stryder | Returned 29 predefined rows, 62 bicycles and ₹3,12,770 from fixed values | Detected 29 actual rows. All 22 readable line quantities matched their ground-truth rows, summing to 55. Seven line quantities remain unresolved. The printed **62 bicycles** and **₹3,12,770 taxable total** were read independently. |

The original implementation's apparent full coverage on these photos does not establish OCR accuracy because its answers were in the production code. In R2 the first photo verifies quantities on every row and both photos verify selected amounts and rates. This does not establish accuracy for every OCR field. A syntactically valid OCR misread can still pass parsing. The app surfaces unresolved fields and discrepancies for review.

Invoice B's full invoice number is obscured in the supplied photo. The parser leaves it unresolved instead of treating a sales order number as the invoice number. The current staging flow blocks invoices missing mandatory header or line values. Resized OCR probes did not reliably improve Invoice B. The production pipeline retains the original-image pass. Unknown layouts and tables with competing quantity headings can require a clearer scan or a new layout rule.

## Validation

```powershell
$env:ANDROID_SERIAL='adb-7816fa26-S4GFEO._adb-tls-connect._tcp'
.\gradlew.bat :app:testIsolatedUnitTest :app:assembleIsolated :app:connectedIsolatedAndroidTest --console=plain
git diff --check
git diff --exit-code -- app/schemas app/src/main/java/dev/vignesh/cycleshop/data/local
```

- Gradle exited 0. **81 JVM tests and 23 instrumentation tests passed**, with zero failures, errors, or skips. The physical suite included both real invoice photos and the existing stock-write isolation test.
- The built APK uses `dev.vignesh.cycleshop.isolated`. The instrumentation APK uses `dev.vignesh.cycleshop.isolated.test`.
- The business Room database and WAL each matched their R1 archive bytes on two read-only checks. There was no schema or migration source diff.

## How the parser works

ML Kit can return OCR blocks by column, join words across cells, and report a tilted row at different heights across the page. Word boxes, their estimated angles, and printed headings let the parser project tokens onto comparable row positions and assign columns. A row needs body evidence. A recognizable supplier name only helps identify a layout.

Decimal strings go through `BigDecimal` before an exact conversion to integer paise. Binary `Double` cannot represent many currency decimals exactly. Room v2 quantities still use `Double`, so parsing checks that the supported decimal value round-trips without losing digits. Any derived stock count must also fit an integer exactly. An unresolved conversion stays flagged for review in staging, using zero for the required v2 factor and stock-count fields.

Reconciliation compares a sum of observed rows with a separately read printed total. It cannot prove row accuracy. Two swapped values can give the same sum, and different purchase units cannot be combined into one stock count. A difference adds a warning while the extracted fields stay as read.
