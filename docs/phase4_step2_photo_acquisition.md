# Phase 4, Increment 2 — Photo acquisition

**Scan Bicycle** and **Scan Part** open a native photo session with a viewfinder, gallery picker, prepared preview, and retake, remove, and retry actions. Analyze stays disabled and explains that AI analysis will arrive in a later update.

- `ui/photo/PhotoAcquisition.kt` contains the shared CameraX viewfinder, permission launcher, and image-only Photo Picker. The invoice screen still sends the original FileProvider or picker URI to its existing OCR workflow.
- `ui/recognition/RecognitionPhotoScreen.kt` and `RecognitionPhotoViewModel.kt` manage each Compose destination and its photo session. The destination chooses its scan instructions; the ViewModel owns the source, current job, and prepared file.
- `recognition/RecognitionPhotoPreparer.kt` reads, normalizes, and encodes the image locally. `RecognitionPhotoStore.kt` owns only generated files in `cacheDir/recognition_photos`. Session exit, replacement, and removal release the session files. A new app process removes abandoned generated files on first recognition cache use.
- `VglNavGraph.kt` connects the two dashboard destinations to the new screen. The `VisualAnalysisService` and catalogue interfaces from Increment 1 remain available for later work.

CameraX binds the preview and still capture to the screen's lifecycle. The shutter becomes available when frames stream and sets the current display rotation before capture. Leaving the composition unbinds those instances. A callback arriving after disposal cleans up its unfinished capture file.

Android gives the Photo Picker caller a scoped `content://` URI. Recognition reads it with `ContentResolver` and copies at most 32 MiB into a temporary file before decoding. It does not need a filesystem path or a persistent grant. A cloud-backed picker provider can take longer to return bytes.

EXIF orientation describes how to display the encoded pixels. The preparer handles all eight rotations and reflections, then encodes a fresh JPEG with the source metadata removed. It inspects dimensions before allocating the bitmap, caps the decoded edge at 2,048 pixels, and produces at most 1,600 pixels on the long edge and 1.5 MiB encoded. Sampling uses powers of two, so some large inputs come out below 1,600 pixels.

Compose observes loading, error, and ready states while disk reads and native bitmap work run off the main thread. The ViewModel cancels a superseded request and checks its revision before displaying a result. Native decoding and individual provider reads may finish before they notice cancellation. Decodes run one at a time and canceled work cleans its own output.

## Verification

On 19 November 2026 the isolated APK built offline and all 95 JVM tests and 31 instrumentation tests passed. The Android suite ran on the OnePlus 9R with Android 14 and included the existing invoice OCR and review tests. The new tests verified all eight EXIF orientations and source metadata removal, large input and oversized-file rejection, replacement, retry, and session cleanup.

The OnePlus rear camera produced a 1,200 × 1,600 portrait JPEG and a 1,600 × 1,200 landscape JPEG in the bicycle screen. Camera service diagnostics confirmed release on Home and navigation back, and reacquisition on resume. Android's permission dialog was denied, the recovery message and Gallery action appeared, and granting permission through app settings reopened the viewfinder. The spare-part screen captured a photo and imported a 4,500 × 3,000 EXIF-rotated gallery fixture as an upright 750 × 1,125 JPEG. The user's invoice photo opened through the shared picker and produced OCR text blocks in the invoice flow.

The business package path and the SHA-256 hashes of its database and WAL matched the before-test baseline. The isolated APK contained no generated Firebase project or API-key resources. The camera and picker checks cover this physical device and OS version. Recognition results and catalogue matching are outside this increment.
