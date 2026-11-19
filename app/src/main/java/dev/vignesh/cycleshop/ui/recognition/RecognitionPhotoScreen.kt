package dev.vignesh.cycleshop.ui.recognition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.vignesh.cycleshop.ui.photo.PhotoCamera
import dev.vignesh.cycleshop.ui.photo.rememberCameraPermission
import dev.vignesh.cycleshop.ui.photo.rememberImagePicker

internal enum class RecognitionPhotoKind(
    val title: String, val instruction: String, val shutterLabel: String
) {
    BICYCLE("Scan Bicycle", "Fit the whole bicycle in the frame and keep any branding readable.", "Capture bicycle"),
    SPARE_PART("Scan Spare Part", "Fill the frame with one part and keep its markings in focus.", "Capture spare part")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecognitionPhotoScreen(
    kind: RecognitionPhotoKind,
    onBack: () -> Unit,
    model: RecognitionPhotoViewModel = viewModel()
) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val openCamera = rememberCameraPermission(model::onCameraPermission)
    val pickPhoto = rememberImagePicker(model::onGallerySelection)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        val latest = model.state.value
        if ((latest == RecognitionPhotoState.PermissionDenied && granted) ||
            (latest == RecognitionPhotoState.Camera && !granted)
        ) model.onCameraPermission(granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kind.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { AnalyzeFooter() }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val photoState = state
            if (photoState == RecognitionPhotoState.Camera) {
                PhotoCamera(
                    instruction = kind.instruction,
                    shutterLabel = kind.shutterLabel,
                    createCaptureFile = model::createCaptureFile,
                    onPhoto = model::onCapture,
                    onError = model::onCameraError,
                    onPickPhoto = pickPhoto,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val previewHeight = (maxHeight * 0.53f).coerceIn(120.dp, 410.dp)
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (photoState) {
                            RecognitionPhotoState.Empty -> {
                                PhotoMessage("Add a photo", kind.instruction)
                                PhotoSourceActions("Open camera", openCamera, pickPhoto)
                            }
                            RecognitionPhotoState.PermissionDenied -> {
                                PhotoMessage("Camera access is off", "Allow camera access to take a photo, or choose one from your gallery.", true)
                                PhotoSourceActions("Allow camera", openCamera, pickPhoto)
                                TextButton(onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    )
                                }) { Text("Open app settings") }
                            }
                            RecognitionPhotoState.Preparing -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator()
                                    Text("Preparing photo…", style = MaterialTheme.typography.titleMedium)
                                }
                                PhotoSourceActions("Retake", openCamera, pickPhoto)
                                TextButton(onClick = model::remove) { Text("Cancel") }
                            }
                            is RecognitionPhotoState.Ready -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    AsyncImage(
                                        model = photoState.photo.file,
                                        contentDescription = "Selected photo preview",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().height(previewHeight).testTag("recognition-preview")
                                    )
                                }
                                PhotoSourceActions("Retake", openCamera, pickPhoto)
                                TextButton(onClick = model::remove) { Text("Remove photo") }
                            }
                            is RecognitionPhotoState.Error -> {
                                PhotoMessage("Photo unavailable", photoState.message, true)
                                PhotoSourceActions("Retry", if (photoState.canRetryPhoto) model::retryPhoto else openCamera, pickPhoto)
                                if (photoState.canRetryPhoto) {
                                    TextButton(onClick = openCamera) { Text("Retake") }
                                    TextButton(onClick = model::remove) { Text("Remove photo") }
                                }
                            }
                            RecognitionPhotoState.Camera -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSourceActions(
    cameraLabel: String, onCamera: () -> Unit, onGallery: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(onClick = onCamera, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Text(cameraLabel, Modifier.padding(start = 6.dp))
        }
        OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Text("Gallery", Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun PhotoMessage(title: String, message: String, error: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (error) colors.errorContainer else colors.surfaceContainer,
            contentColor = if (error) colors.onErrorContainer else colors.onSurface
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AnalyzeFooter() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Analyze") }
            Text("AI analysis will be available in a later update.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
