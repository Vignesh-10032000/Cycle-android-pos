package dev.vignesh.cycleshop.ui.recognition

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vignesh.cycleshop.recognition.PhotoPreparationException
import dev.vignesh.cycleshop.recognition.PreparedRecognitionPhoto
import dev.vignesh.cycleshop.recognition.RecognitionPhotoPreparer
import dev.vignesh.cycleshop.recognition.RecognitionPhotoSource
import dev.vignesh.cycleshop.recognition.RecognitionPhotoStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

internal sealed interface RecognitionPhotoState {
    data object Empty : RecognitionPhotoState
    data object Camera : RecognitionPhotoState
    data object PermissionDenied : RecognitionPhotoState
    data object Preparing : RecognitionPhotoState
    data class Ready(val photo: PreparedRecognitionPhoto) : RecognitionPhotoState
    data class Error(val message: String, val canRetryPhoto: Boolean) : RecognitionPhotoState
}

internal class RecognitionPhotoViewModel(application: Application) : AndroidViewModel(application) {
    private val store = RecognitionPhotoStore(application.cacheDir)
    private val preparer = RecognitionPhotoPreparer(application.contentResolver, store)
    private val mutableState = MutableStateFlow<RecognitionPhotoState>(RecognitionPhotoState.Empty)
    val state = mutableState.asStateFlow()

    private var source: RecognitionPhotoSource? = null
    private var prepared: PreparedRecognitionPhoto? = null
    private var operation: Job? = null
    private var revision = 0L

    fun onCameraPermission(granted: Boolean) {
        resetPhoto()
        mutableState.value = if (granted) RecognitionPhotoState.Camera else RecognitionPhotoState.PermissionDenied
    }

    fun createCaptureFile(): File = store.createCapture()

    fun onCapture(file: File) {
        if (state.value == RecognitionPhotoState.Camera) {
            replacePhoto(RecognitionPhotoSource.Capture(file))
        } else {
            store.delete(file)
        }
    }

    fun onGallerySelection(uri: Uri) = replacePhoto(RecognitionPhotoSource.Gallery(uri))

    fun onCameraError(message: String) {
        if (state.value == RecognitionPhotoState.Camera) {
            mutableState.value = RecognitionPhotoState.Error(message, canRetryPhoto = false)
        }
    }

    fun retryPhoto() {
        val currentSource = source ?: return
        if (state.value is RecognitionPhotoState.Error) prepare(currentSource)
    }

    fun remove() {
        resetPhoto()
        mutableState.value = RecognitionPhotoState.Empty
    }

    private fun replacePhoto(newSource: RecognitionPhotoSource) {
        resetPhoto()
        source = newSource
        prepare(newSource)
    }

    private fun prepare(request: RecognitionPhotoSource) {
        val requestRevision = ++revision
        mutableState.value = RecognitionPhotoState.Preparing
        operation = viewModelScope.launch {
            try {
                val photo = preparer.prepare(request)
                if (requestRevision == revision) {
                    prepared = photo
                    deleteCapture()
                    source = null
                    mutableState.value = RecognitionPhotoState.Ready(photo)
                } else {
                    store.delete(photo.file)
                }
            } catch (e: PhotoPreparationException) {
                onPreparationError(requestRevision, e.message ?: "Could not prepare the photo.")
            } catch (_: SecurityException) {
                onPreparationError(requestRevision, "Access to the selected photo was lost. Choose it again.")
            } catch (_: IOException) {
                onPreparationError(requestRevision, "Could not read or save the photo. Try again.")
            }
        }
    }

    private fun onPreparationError(requestRevision: Long, message: String) {
        if (requestRevision == revision) {
            mutableState.value = RecognitionPhotoState.Error(message, canRetryPhoto = true)
        }
    }

    private fun deleteCapture() {
        (source as? RecognitionPhotoSource.Capture)?.file?.let(store::delete)
    }

    private fun resetPhoto() {
        revision++
        operation?.cancel()
        operation = null
        deleteCapture()
        source = null
        prepared?.file?.let(store::delete)
        prepared = null
    }

    override fun onCleared() = resetPhoto()
}
