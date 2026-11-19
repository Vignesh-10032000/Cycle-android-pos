# Phase 4, Step 1 — Offline visual recognition contracts

`VisualAnalysisService` accepts a prepared image and an optional product type hint. It returns observations, a response validation error, or a service availability error. Tests inject a fake that sends fixture JSON through the same validator a future Gemini adapter will use.

`VisualResponseSchema.toJson()` emits a Gemini response schema using the supported OpenAPI subset. The payload's `schema_version` must equal 1. Each core field has a status, a nullable value, and a basis. An observed value describes direct visual evidence. An uncertain value can contain a hypothesis or explain why no value is available. An unknown value is null and carries a reason. `markings` contains verbatim transcriptions. Quoted evidence for a brand, observed model, size or specification must appear in that list. Measured sizes require explicit markings and a size kind. Missing entries in `sizes` and `specifications` mean the analysis made no claim about those entries.

The local validator checks strict JSON syntax, duplicate keys, the field names and types, schema version, response limits, and rules involving several fields. For example, a wheel marking cannot establish a frame size. The validator checks the consistency of supplied text. Independent verification of whether that text exists in the photo requires examining the photo.

`ReadOnlyVisualCatalogue` supplies active Room product and variant records and their brands. A future matcher will consume the observation and catalogue snapshot. Variant records supply stock and shop-specific details.

The eight cases in `app/src/test/resources/recognition/labelled_responses.json` are authored model responses with separate catalogue labels. They cover two labelled Hero variants, an ambiguous unbranded bicycle, a marked derailleur, an unmarked brake shoe, the KMC chain, and an unlisted bicycle and accessory. Labels are ground truth for future retrieval tests. A null expected SKU allows the evidence to remain ambiguous, with an optional list of plausible variants. These fixtures exercise parsing and evidence rules. Photo recognition and candidate ranking accuracy require later increments.

**Catalogue defect.** The sample seeder assigns the Shimano brand ID to the product named KMC Z7 7-Speed Bicycle Chain (SKU `VGL-KMC-Z7-116`). A device test confirms the discrepancy against a freshly seeded in-memory Room database. A later retrieval test should report any miss attributable to this metadata conflict.

Run the offline JVM tests and build with `gradlew.bat :app:testIsolatedUnitTest :app:assembleIsolated --offline`. For a connected device, `run_instrumentation.ps1 -Device <adb-serial>` verifies the target package and runs the existing isolated runner.

Verification on 19 November 2026 passed all 93 isolated JVM tests, including 13 new recognition tests, and built the isolated app and test APKs. All 27 instrumentation tests passed on the OnePlus 9R running Android 14, including the new disposable Room test and the existing invoice OCR and review tests. The installed business package path and SHA-256 hashes of its database and write-ahead log matched the pre-test baseline.
