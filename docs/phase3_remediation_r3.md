# Phase 3 remediation R3

R3 requires explicit line verification before whole-invoice approval. It keeps the OCR record separate from the owner’s changes. Approval lives in the current scan session. No stock posting or migration was implemented.

## Actual behavior

| State | When it applies |
| --- | --- |
| OCR extracted | OCR produced usable values and no candidate is available. |
| Suggested match | The matcher found a candidate and the owner has yet to choose a variant. |
| Owner selected | The owner selected an active catalog variant and the line is ready for verification. |
| Needs review | A required field or conversion is unresolved, a selected variant is unavailable, or an edit invalidated a verification. |
| Owner verified | The owner clicked **Verify line**, the required fields passed validation, and the receipt matches the current revision and values. |
| Rejected | The owner rejected the line. It remains part of the invoice and blocks approval until reopened and verified. |

The invoice review displays the original description, quantity, unit, rate and amount alongside the current values. It also exposes the OCR text, bounding box and quality warnings. The variant picker prioritizes suggestions and searches the existing active catalog by name or SKU. Its action only selects the variant. The app records a local confirmation action, revision, timestamp, values and resolved conversion when **Verify line** succeeds. There is no separate owner sign-in in this app.

The verification check reads the selected variant and its parent product from Room. Room v2 has an active flag on the parent product. Invoice approval rechecks all selections, requires at least one line, a supplier name and invoice number, and a current receipt for every line. Reconciliation differences remain warnings. Unknown or incompatible unit conversions, including a Set with no Piece conversion, block verification. Existing conversions must produce a positive, exact, in-range integer result. Rate and amount remain integer paise.

## Persistence boundary

The scan ViewModel holds immutable OCR text, line and word boxes, field-quality observations, current values and verification receipts. The session survives recomposition and navigation within the scan screen. Session verification is lost when the scan session or process is discarded.

Room v2 can stage the full raw OCR text, original structured line values, the selected variant and separate description, quantity, rate and amount corrections. It cannot store a corrected invoice unit, a nullable original quantity, the spatial evidence, or a verification receipt. R3 therefore blocks v2 staging if the input had a missing original quantity or a changed unit. For compatible invoices the stage action rechecks the approval and active catalog in one Room transaction, saves the values as `NEEDS_REVIEW` and `PENDING_MATCH`, and creates no mapping, stock movement or inventory write. Existing v2 records are untouched. No reader should use a historical v2 `VERIFIED_MATCH` label as evidence of explicit approval.

On the supplied Stryder scan the printed total remains 62 and all seven missing line quantities remain missing in the original snapshot. None of those lines can be verified until the owner corrects its quantity and resolves its other required fields. Even after those corrections, v2 staging cannot represent the original missing quantities.

## Proposed Room v3 changes for R6 review

Use a versioned migration. Keep the v1 → v2 migration and both existing schema exports. The following structures are a proposal and were not added in R3.

| Table | Proposed columns and constraints |
| --- | --- |
| `purchase_lines` | Change `raw_quantity` to nullable `REAL` and `raw_unit` to nullable `TEXT`. Add `corrected_unit TEXT NULL`, `review_revision INTEGER NOT NULL DEFAULT 0 CHECK (review_revision >= 0)`, and `owner_review_state TEXT NOT NULL DEFAULT 'NEEDS_REVIEW'` constrained to the six R3 states. Retain existing IDs, FKs, and raw and corrected paise columns. |
| `purchase_invoice_ocr_evidence` | `invoice_id INTEGER PRIMARY KEY` FK to `purchase_invoices.id`, `format_version INTEGER NOT NULL`, `source_kind TEXT NOT NULL` with `LIVE_OCR` or `LEGACY_PARTIAL`, `raw_text TEXT NULL`, `raw_supplier_name TEXT NULL`, `raw_invoice_number TEXT NULL`, and `ocr_payload_json TEXT NULL` with image dimensions, rotation and the original block, line and word text and boxes. Require the payload and text for `LIVE_OCR`. |
| `purchase_line_ocr_evidence` | `purchase_line_id INTEGER PRIMARY KEY` FK to `purchase_lines.id`, `raw_lines_json TEXT NOT NULL`, nullable `box_left INTEGER`, `box_top INTEGER`, `box_right INTEGER`, `box_bottom INTEGER` as an all-present or all-absent group, and `field_quality_json TEXT NOT NULL`. The original structured numeric and description fields remain in `purchase_lines`. |
| `purchase_line_verifications` | `purchase_line_id INTEGER NOT NULL` FK to `purchase_lines.id`, `review_revision INTEGER NOT NULL`, composite PK of those two fields, `verified_at_epoch_ms INTEGER NOT NULL`, `action TEXT NOT NULL CHECK (action = 'OWNER_CONFIRM')`, `verified_variant_id INTEGER NOT NULL` FK to `product_variants.id` with delete restricted, `verified_description TEXT NOT NULL`, `verified_quantity_decimal TEXT NOT NULL`, `verified_invoice_unit TEXT NOT NULL`, `verified_rate_paise INTEGER NOT NULL CHECK (verified_rate_paise > 0)`, `verified_amount_paise INTEGER NOT NULL CHECK (verified_amount_paise >= 0)`, `verified_stock_unit TEXT NOT NULL`, `verified_conversion_factor INTEGER NOT NULL CHECK (verified_conversion_factor > 0)`, and `verified_stock_quantity INTEGER NOT NULL CHECK (verified_stock_quantity > 0)`. |
| `purchase_invoices` | Add `review_revision INTEGER NOT NULL DEFAULT 0`, `approved_review_revision INTEGER NULL` and `approved_at_epoch_ms INTEGER NULL`. Treat approval as current only when both approval fields are present and the revisions match. |

Insert original evidence once and give correction updates no access to its fields. Coordinate evidence and receipt deletion rules with R5. Add the new columns through the R6 migration, rebuilding `purchase_lines` where SQLite requires a rebuild for nullable columns. Preserve legacy source values and IDs as-is. Where legacy data lacks separate evidence, record `LEGACY_PARTIAL`. Keep historical status as reference without generating verification events or guessing missing text, geometry or quantities. In v3, each material edit advances the line and invoice revisions. Write a new receipt and status in one transaction on verification. Test v2 → v3 and v1 → v3, including legacy rows with no trustworthy receipt.

## Modified files and verification

- Review model and staging boundary in `InvoiceReviewSession.kt`, `InvoiceStagingV2.kt`, and `PurchaseRepository.kt`. The new model replaces the old `PurchaseValidationEngine.kt`, which inferred verification from numeric validity and a variant ID.
- Scan and UI changes in `InvoiceScanViewModel.kt`, `InvoiceScanScreen.kt`, `InvoiceReviewScreen.kt`, `InvoiceDataModels.kt`, `OcrResult.kt`, and `ProductMatchingEngine.kt`.
- Regression coverage in `InvoiceReviewSessionTest.kt`, `InvoiceReviewUiTest.kt`, and `PurchaseStagingDeviceTest.kt`. The UI test reuses the real-photo reader in `InvoiceRealOcrDeviceTest.kt`.

```powershell
$env:ANDROID_SERIAL='adb-7816fa26-S4GFEO._adb-tls-connect._tcp'
.\gradlew.bat :app:testIsolatedUnitTest :app:assembleIsolated :app:connectedIsolatedAndroidTest --console=plain
git diff --check
git diff --exit-code 9439cdc -- app/schemas app/src/main/java/dev/vignesh/cycleshop/data/local
```

The Gradle command exited 0. All 80 JVM tests and 26 Android instrumentation tests passed, with zero failures, errors or skips. The isolated APK built. Compose tests exercised the actual review screen on the OnePlus 9R with Android 14. They confirmed candidate selection, verification and approval as separate actions, invalidation after an edit, rejection, and the visible blocked state of all seven missing Stryder quantities. The Room integration test verified inactive variant rejection and raw versus corrected persistence in a disposable database with unchanged stock and ledger. Read-only checks of the business app’s package path and SHA-256 hashes of its database and WAL matched before and after testing. Room v1/v2 exports and migration code did not change.

## Developer notes

Compose observes the current draft, which can change. A change creates a fresh value object and increments the line revision, while the OCR object and its defensively copied collections remain unchanged. The state machine explains what the owner can do next. Selection supplies a variant ID, while verification records a deliberate confirmation of the selected variant and every material field together. Comparing the proof revision and values to the current draft prevents an old approval from reappearing when an edit is reversed. The source OCR record lets an auditor compare the original reading with both the correction and the values the owner confirmed.
