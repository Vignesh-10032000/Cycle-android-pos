package dev.vignesh.cycleshop.ui.photo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.Surface as AndroidSurface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.io.IOException

@Composable
internal fun rememberImagePicker(onImage: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        it?.let(onImage)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
internal fun rememberCameraPermission(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}

/** The caller chooses where each capture goes and owns files delivered to [onPhoto]. */
@Composable
internal fun PhotoCamera(
    instruction: String,
    shutterLabel: String,
    createCaptureFile: () -> File,
    onPhoto: (File) -> Unit,
    onError: (String) -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val currentError by rememberUpdatedState(onError)
    val currentPhoto by rememberUpdatedState(onPhoto)
    val previewView = remember(context) {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    val session = remember(previewView, lifecycle) { CameraSession() }
    var ready by remember(session) { mutableStateOf(false) }
    var capturing by remember(session) { mutableStateOf(false) }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    DisposableEffect(session) {
        val preview = Preview.Builder().build()
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        previewView.previewStreamState.observe(lifecycle) {
            ready = it == PreviewView.StreamState.STREAMING
        }
        providerFuture.addListener({
            if (!session.disposed) {
                try {
                    val provider = providerFuture.get()
                    session.provider = provider
                    capture.targetRotation = previewView.display?.rotation ?: AndroidSurface.ROTATION_0
                    preview.surfaceProvider = previewView.surfaceProvider
                    provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    session.capture = capture
                } catch (_: Exception) {
                    currentError("Could not open the camera.")
                }
            }
        }, mainExecutor)
        onDispose {
            session.disposed = true
            previewView.previewStreamState.removeObservers(lifecycle)
            session.provider?.unbind(preview, capture)
        }
    }

    fun takePhoto() {
        val capture = session.capture ?: return
        if (!ready || capturing || session.disposed) return
        val file = try {
            createCaptureFile()
        } catch (_: IOException) {
            currentError("Could not create a temporary photo file.")
            return
        } catch (_: SecurityException) {
            currentError("Could not access photo storage.")
            return
        }
        capturing = true
        capture.targetRotation = previewView.display?.rotation ?: AndroidSurface.ROTATION_0
        try {
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(), mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        if (session.disposed) {
                            file.delete()
                        } else {
                            capturing = false
                            currentPhoto(file)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        file.delete()
                        if (!session.disposed) {
                            capturing = false
                            currentError("Could not take the photo. Try again.")
                        }
                    }
                }
            )
        } catch (_: RuntimeException) {
            file.delete()
            capturing = false
            currentError("Could not take the photo. Try again.")
        }
    }

    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (!ready || capturing) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(if (capturing) "Taking photo…" else "Starting camera…")
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(instruction, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = onPickPhoto, enabled = !capturing) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Text("Gallery", Modifier.padding(start = 8.dp))
                    }
                    FilledIconButton(
                        onClick = ::takePhoto,
                        enabled = ready && !capturing,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = shutterLabel)
                    }
                }
            }
        }
    }
}

private class CameraSession {
    var disposed = false
    var provider: ProcessCameraProvider? = null
    var capture: ImageCapture? = null
}
