package dev.vignesh.cycleshop.ui.invoice

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// ── Entry point ───────────────────────────────────────────────────────────────

/**
 * Full-screen invoice scanning experience.
 *
 * Integrates:
 * - Android permission rationale for CAMERA
 * - CameraX live preview with single-tap capture
 * - Android Photo Picker for existing invoice photographs
 * - In-app JPEG preview using Coil
 * - Offline ML Kit Text Recognition v2
 * - OCR result display: raw text, block count, line count, processing time
 * - Copy-to-clipboard action
 * - Retake / Retry navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScanScreen(
    onBack: () -> Unit,
    viewModel: InvoiceScanViewModel = viewModel(factory = InvoiceScanViewModelFactory())
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // ── Camera permission launcher ────────────────────────────────────────────
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = viewModel.allocateCaptureFile(context)
            viewModel.onCameraReady(file.absolutePath)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // ── Photo Picker launcher ─────────────────────────────────────────────────
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onPhotoPickerResult(context, it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Scan Invoice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "invoice-scan-state"
            ) { currentState ->
                when (currentState) {
                    is InvoiceScanState.Idle -> IdlePanel(
                        onOpenCamera = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    is InvoiceScanState.PermissionDenied -> PermissionDeniedPanel(
                        onRetry = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    is InvoiceScanState.Previewing -> CameraPreviewPanel(
                        captureFilePath = currentState.captureFilePath,
                        onCaptureDone = { uri -> viewModel.onCaptureSuccess(context, uri) },
                        onCaptureFailed = { reason -> viewModel.onCaptureFailed(reason) },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    is InvoiceScanState.CapturePending -> ProcessingPanel(imageUri = null)

                    is InvoiceScanState.Processing -> ProcessingPanel(imageUri = currentState.imageUri)

                    is InvoiceScanState.Success -> {
                        var showReviewScreen by remember { mutableStateOf(currentState.parsedInvoice != null) }
                        if (showReviewScreen && currentState.parsedInvoice != null) {
                            InvoiceReviewScreen(
                                invoice = currentState.parsedInvoice,
                                imageUri = currentState.imageUri,
                                onNavigateBack = { showReviewScreen = false },
                                onSaveReviewed = { updated ->
                                    viewModel.updateParsedInvoice(updated)
                                    viewModel.stageInvoiceToDatabase(
                                        context = context,
                                        invoice = updated,
                                        imageUri = currentState.imageUri
                                    ) { result ->
                                        scope.launch {
                                            result.fold(
                                                onSuccess = { invoiceId ->
                                                    snackbarHostState.showSnackbar(
                                                        "Invoice staged successfully (ID: #$invoiceId, Status: CONFIRMED_STAGED. Zero stock modified)."
                                                    )
                                                },
                                                onFailure = { err ->
                                                    snackbarHostState.showSnackbar(
                                                        "Staging failed: ${err.message ?: "Unknown error"}"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                },
                                onDeleteImageAndExit = {
                                    viewModel.deleteCurrentImage(context)
                                    onBack()
                                }
                            )
                        } else {
                            OcrResultPanel(
                                imageUri = currentState.imageUri,
                                result = currentState.result,
                                parsedInvoice = currentState.parsedInvoice,
                                onOpenReview = { showReviewScreen = true },
                                onCopy = { text ->
                                    clipboard.setText(AnnotatedString(text))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Text copied to clipboard")
                                    }
                                },
                                onRetake = { viewModel.reset() }
                            )
                        }
                    }

                    is InvoiceScanState.Error -> ErrorPanel(
                        message = currentState.message,
                        imageUri = currentState.imageUri,
                        onRetry = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

// ── Idle panel ────────────────────────────────────────────────────────────────

@Composable
private fun IdlePanel(
    onOpenCamera: () -> Unit,
    onPickPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Supplier Invoice Scanner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Capture or import a supplier invoice photograph.\nML Kit reads text offline — no internet required.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onOpenCamera,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(Icons.Default.Camera, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Open Camera", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onPickPhoto,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Import from Gallery", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Permission denied panel ────────────────────────────────────────────────────

@Composable
private fun PermissionDeniedPanel(
    onRetry: () -> Unit,
    onPickPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "VGL Cycle Shop needs camera access to photograph supplier invoices for offline OCR processing. Grant permission in Settings or use the gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Grant Camera Permission")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Use Gallery Instead")
        }
    }
}

// ── Camera preview panel ───────────────────────────────────────────────────────

/**
 * Live CameraX viewfinder with shutter and gallery shortcut.
 *
 * ### CameraX Lifecycle integration
 * [ProcessCameraProvider.getInstance] returns a ListenableFuture; we obtain
 * a callback via [addListener] on the main executor. Inside the callback we
 * call [ProcessCameraProvider.bindToLifecycle], passing:
 * - The [LocalLifecycleOwner] from the Compose tree (the host Activity)
 * - [CameraSelector.DEFAULT_BACK_CAMERA]
 * - A [Preview] use-case wired to [PreviewView]
 * - An [ImageCapture] use-case for still capture
 *
 * CameraX automatically:
 * - Starts the preview when the owner reaches STARTED
 * - Pauses when owner goes to STOPPED
 * - Releases the sensor when owner goes to DESTROYED
 * No manual lifecycle callbacks are needed.
 *
 * ### Image Capture
 * [ImageCapture.takePicture] with [ImageCapture.OutputFileOptions.Builder(File)]
 * writes directly to the pre-allocated file. The [ImageCaptureCaptureCallback]
 * provides the saved URI. We then post this URI to the ViewModel which
 * begins OCR.
 */
@Composable
private fun CameraPreviewPanel(
    captureFilePath: String,
    onCaptureDone: (Uri) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onPickPhoto: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── CameraX PreviewView ────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            // Target rotation matches the display rotation at bind time.
                            // CameraX will embed correct EXIF orientation in the JPEG.
                            .build()
                        imageCapture = capture

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            onCaptureFailed(e.javaClass.simpleName)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Bottom controls overlay ────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Position the invoice within view and tap Capture",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery shortcut
                FilledTonalButton(onClick = onPickPhoto) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Gallery")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Gallery")
                }

                // Shutter button
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        val capture = imageCapture ?: return@Surface
                        val captureFile = java.io.File(captureFilePath)
                        val outputOptions = ImageCapture.OutputFileOptions
                            .Builder(captureFile)
                            .build()

                        capture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    // Build a FileProvider URI — never expose raw file paths
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        captureFile
                                    )
                                    onCaptureDone(uri)
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    onCaptureFailed(exc.javaClass.simpleName)
                                }
                            }
                        )
                    }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Capture invoice",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ── Processing panel ───────────────────────────────────────────────────────────

@Composable
private fun ProcessingPanel(imageUri: Uri?) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Invoice being processed",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            Spacer(Modifier.height(24.dp))
        }

        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            strokeWidth = 5.dp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Reading invoice text…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "ML Kit OCR running offline. This takes 1–4 seconds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── OCR result panel ───────────────────────────────────────────────────────────

@Composable
private fun OcrResultPanel(
    imageUri: Uri,
    result: OcrResult,
    parsedInvoice: ParsedInvoice? = null,
    onOpenReview: () -> Unit = {},
    onCopy: (String) -> Unit,
    onRetake: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Structured Table Banner if parsed ─────────────────────────────
        if (parsedInvoice != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✨ Structured Invoice Parsed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${parsedInvoice.items.size} rows • ${parsedInvoice.supplierName ?: "Supplier"} (${parsedInvoice.layoutType.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = onOpenReview,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Review")
                    }
                }
            }
        }

        // ── Invoice image preview ──────────────────────────────────────────
        AsyncImage(
            model = imageUri,
            contentDescription = "Scanned invoice",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // ── Stats row ─────────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(label = "Blocks", value = result.blockCount.toString())
                StatChip(label = "Lines", value = result.lineCount.toString())
                StatChip(label = "OCR Time", value = "${result.processingMs} ms")
                StatChip(label = "Rotation", value = "${result.rotationDegrees}°")
            }
        }

        // ── Empty-result notice ────────────────────────────────────────────
        if (!result.hasContent) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "No text was recognized. Ensure the invoice is in focus, well-lit, and not blurred. Try retaking with the camera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // ── Recognized text card ───────────────────────────────────────────
        if (result.hasContent) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recognized Text",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { onCopy(result.rawText) }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy recognized text",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = result.rawText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Block-level detail ─────────────────────────────────────────
            Text(
                text = "Block Detail  (preserved for invoice parsing)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            result.blocks.forEachIndexed { index, block ->
                BlockCard(index = index + 1, block = block)
            }
        }

        // ── Action buttons ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Retake")
            }

            if (result.hasContent) {
                Button(
                    onClick = { onCopy(result.rawText) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Copy All")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BlockCard(index: Int, block: OcrTextBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$index",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Text(
                    text = block.blockText.take(80) + if (block.blockText.length > 80) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            block.boundingBox?.let { bbox ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Bounds: (${bbox.left}, ${bbox.top}) – (${bbox.right}, ${bbox.bottom})  " +
                            "${block.lines.size} line(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Error panel ────────────────────────────────────────────────────────────────

@Composable
private fun ErrorPanel(
    message: String,
    imageUri: Uri?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(12.dp)
                    )
            )
            Spacer(Modifier.height(16.dp))
        }

        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "OCR Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Try Again")
        }
    }
}
