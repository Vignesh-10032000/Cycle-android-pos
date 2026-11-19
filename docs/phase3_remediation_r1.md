# Phase 3 remediation R1

On 19 November 2026, the OnePlus 9R running Android 14 held Room schema v2, with 67 stock units and nine matching ledger entries. This report covers the installation present at the start of R1. The original installation from before the Step 3A audit was removed during that audit.

## Git and backup

The project had no Git metadata. Local commit `69be01a` on `main` records the existing project before the test configuration changes. A Git bundle of that checkpoint was cloned into a separate directory and checked with `git fsck --full`. Both schema exports and the source for `MIGRATION_1_2` are in that checkpoint. Device records, real invoice photos, SDK settings and the local Google Services configuration are stored outside Git. The 19 private workspace inputs were zipped and verified against the original files.

The backup folder is `%LOCALAPPDATA%\VGL-CycleShop-Backups\R1-20261119-161355`. It contains the captured database and WAL in `device-snapshot.tar`, the standalone recovery file `vgl_cycleshop.verified.db`, `verified-inventory.json`, the source APK, test logs, the private workspace archive and the Git bundle. Keep the folder private because these files can contain business data. The original invoice fixture photos required by the OCR tests are in `private-workspace-inputs.zip` at their project-relative paths.

The VGL process was stopped before ADB read the database files. Neither app-private invoice directory existed, and no staged rows referenced an invoice image. Files selected through the Android photo picker can live outside the app sandbox and need their own backup if an invoice depends on them. There were no such references in this snapshot.

`scripts/verify_room_backup.py` recreated a standalone database from a temporary copy of the archive by using SQLite's backup API. It reopened the result and compared its logical contents to the source, checked the Room schema version and identity hash against v2, and ran SQLite integrity and foreign-key checks. The script requires a fresh output filename.

```powershell
$backup = Join-Path $env:LOCALAPPDATA 'VGL-CycleShop-Backups/R1-20261119-161355'
python scripts/verify_room_backup.py "$backup/device-snapshot.tar" "$backup/restore-drill.db" --schema app/schemas/dev.vignesh.cycleshop.data.local.VglDatabase/2.json
```

## Device inventory

The v2 identity hash is `2ab82dea9d1e8183d3a7af401445c015`. SQLite column, index, and foreign-key metadata matched the exported schema for all seven tables. The v1 schema describes the four catalog and ledger tables. The v1 → v2 migration adds purchases and supplier mappings and preserves the inventory tables. All nine ledger entries on the phone are `OPENING` movements.

| Room table | Rows |
| --- | ---: |
| `brands` | 6 |
| `products` | 8 |
| `product_variants` | 9 |
| `stock_movements` | 9 |
| `purchase_invoices` | 0 |
| `purchase_lines` | 0 |
| `supplier_product_mappings` | 0 |

| SKU | Stock | Sum of movement deltas |
| --- | ---: | ---: |
| VGL-BSA-LADYBIRD-PNK | 5 | 5 |
| VGL-FF-TGT29-ORG | 4 | 4 |
| VGL-HERC-ROADEO-YEL | 4 | 4 |
| VGL-HERO-SP275-BLK | 5 | 5 |
| VGL-HERO-SP275-BLU | 3 | 3 |
| VGL-KMC-Z7-116 | 12 | 12 |
| VGL-MON-HELI-M | 6 | 6 |
| VGL-SHIM-RDTX800 | 8 | 8 |
| VGL-SHIM-VB-PADS | 20 | 20 |
| **Total** | **67** | **67** |

## Incident and Gradle diagnosis

Device logs from the earlier audit show an update of `dev.vignesh.cycleshop` followed by its uninstall for all users at 16:00:01 and the instrumentation APK's uninstall at 16:00:02. Its reinstall appeared at 16:01:29. The isolated reproduction recorded `android-test.uninstall-after-tests=true` and Gradle explicitly logging removal of both isolated APKs. The business package was the target of that same cleanup workflow during the original audit. The earlier host files labelled as a pre-test database and WAL are 47-byte ADB error responses. The current database can be recovered from the R1 backup. The older state remains unrecovered. Android lists a backup set for this phone, but its package contents and pre-incident date are unverified.

Running the isolated Gradle task with the explicit `IP:port` transport reproduced exit code 1 while its XML recorded 23 tests and zero failures or errors. The installed Android Gradle Plugin 9.4.1 worker stores per-device test status in a map. Its listener cuts the device ID out of JUnit's serialized unique ID, where JUnit escapes a colon as `%3A`. The worker then looks up the raw ADB serial containing a colon and treats the missing key as failure. Running JUnit's actual `UniqueId` class confirmed the mismatch. Use the colon-free mDNS serial or a USB serial with the isolated Gradle task. The guarded direct ADB script also accepts an explicit IP endpoint.

| Check executed | Result |
| --- | --- |
| Gradle `:app:testIsolatedUnitTest` | 71 passed, zero failures or skips |
| Gradle `:app:connectedIsolatedAndroidTest` using IP and port | 23 passed in the XML, Gradle exit 1 |
| Gradle `:app:connectedIsolatedAndroidTest` using mDNS on the same phone | 23 passed, Gradle exit 0 |
| Direct `run_instrumentation.ps1` using IP and port | `OK (23 tests)`, exit 0 |
| Isolated restore on the OnePlus | App opened the restored database. Full logical digest matched the backup after launch. |
| Business app comparison after all test runs | Installed APK and newly captured DB archive matched their pre-test copies byte for byte. |

## Test and restore workflow

Instrumentation is built only for `dev.vignesh.cycleshop.isolated`. Android gives that package its own app data, and its test runner checks the target package before it creates the application. The PowerShell runner also verifies both APK identities and the instrumentation manifest before it installs anything. Most database tests use in-memory Room. Persistence and migration tests use their own named files inside the isolated app sandbox.

```powershell
.\gradlew.bat :app:testIsolatedUnitTest
adb devices -l
$env:ANDROID_SERIAL = '<USB or mDNS serial from adb devices>'
.\gradlew.bat :app:connectedIsolatedAndroidTest
.\run_instrumentation.ps1 -Device '<ADB serial from adb devices>'
```

To make another backup of this debuggable install, select the device explicitly, stop the app, and check that its process has exited. Use `run-as dev.vignesh.cycleshop` to check for the database, any `-wal` or `-journal` sidecar, `files/invoices`, and `cache/invoices`. Pass the existing paths together to `adb exec-out run-as dev.vignesh.cycleshop tar -cf - <paths>` and save stdout as binary in a new host folder. Use Python binary file output or PowerShell 7.4+ native redirection. Verify the resulting tar with the script before relying on it. Copying only the main database can miss committed WAL records.

Practice restoration using a fresh isolated app sandbox or a separate disposable device. Stop the restore target before copying data. Put the standalone database at `databases/vgl_cycleshop.db` in that sandbox and restore archived images to their corresponding directories. Verify the file hash on the device **before** launching. The first drill's raw Windows ADB input produced a malformed file and the isolated app seeded new demo rows. The successful drill transferred Base64 text, decoded it inside the isolated sandbox, and verified the file SHA-256 before startup. After startup, the full database digest matched, including historical timestamps. In this case row counts alone would have missed the failed transfer. An archive restore point represents a particular moment. For an actual production recovery, first archive and compare the destination's current data and decide how to handle newer records. A manual debug restore relies on `run-as`, which is generally unavailable for a signed non-debuggable release.

## How the pieces fit

- **Android storage.** The business app's data directory on this device is `/data/user/0/dev.vignesh.cycleshop`. The APK is installed separately. Room uses SQLite files under `databases/` and CameraX capture writes into private app storage. Android removes app-specific files on uninstall. [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific)
- **Database backups.** A snapshot preserves rows and schema at a point in time. SQLite may keep committed changes in the WAL until checkpointing. The standalone copy merges the snapshot into a database that can be reopened without the original WAL. Images require their own files even when Room stores the URI. [SQLite backup API](https://www.sqlite.org/backup.html)
- **Instrumentation.** Tests run against the package selected by the instrumentation manifest. They can invoke real application lifecycle code and use its files. Even a suite with in-memory databases can lose the target app's data when the runner uninstalls the package. An isolated application ID protects the business package during setup, test execution, and cleanup. [Android instrumentation manifest](https://developer.android.com/guide/topics/manifest/instrumentation-element)
- **Git.** A commit or bundle recovers tracked source, versioned schema exports, and migration code. It cannot recover SQLite rows, untracked invoice photos, or the phone's runtime state. Keep both a source checkpoint and a verified data backup.

## Review sequence after R1

| Step | Reviewable change |
| --- | --- |
| R2 | Replace canned results with OCR-backed values. Reject non-finite and out-of-range numbers and remove silent truncation. |
| R3 | Require explicit owner verification per line and keep the original OCR evidence separate from corrections. |
| R4 | Scope supplier mappings and invoice identities correctly, define approved variant and package conversion rules, and design duplicate enforcement. |
| R5 | Retain invoice images durably and guard historical data from edits or deletion. |
| R6 | Introduce the versioned schema changes on approved rules. Verify existing rows through v2 → v3 and v1 → v3. Land changes that depend on those columns together with their migration. |
| R7 | Run full regression, migration, and OnePlus isolation and preservation checks. |

Phase 3 Step 3B atomic purchase posting follows review and validation of those changes.
