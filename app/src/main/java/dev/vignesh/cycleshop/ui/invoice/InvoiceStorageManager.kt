package dev.vignesh.cycleshop.ui.invoice

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Manages app-private invoice image storage and safe lifecycle cleanup.
 *
 * Requirements:
 * 1. Storage is strictly app-private (Context.filesDir/invoices/ and Context.cacheDir/invoices/).
 * 2. File names use anonymous timestamps (no supplier PII in filenames).
 * 3. Safe cleanup routines delete temporary capture files upon retake, user dismissal,
 *    or when files exceed a maximum retention age (24 hours).
 */
object InvoiceStorageManager {

    private const val INVOICES_DIR = "invoices"
    private const val MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours

    /**
     * Allocates a new empty JPEG file in app-private storage.
     */
    fun allocateInvoiceFile(context: Context): File {
        val dir = File(context.filesDir, INVOICES_DIR).also { it.mkdirs() }
        val filename = "invoice_${System.currentTimeMillis()}.jpg"
        return File(dir, filename).also { it.createNewFile() }
    }

    /**
     * Safely deletes a specific invoice image file identified by its [Uri].
     * Only deletes files located inside this app's private invoice directory.
     *
     * @return true if the file was deleted or did not exist, false if deletion failed.
     */
    fun deleteInvoiceImage(context: Context, uri: Uri?): Boolean {
        if (uri == null) return true
        return try {
            val path = uri.path ?: return false
            val file = if (path.startsWith(context.filesDir.absolutePath)) {
                File(path)
            } else {
                // FileProvider content URI handling
                val name = uri.lastPathSegment ?: return false
                File(File(context.filesDir, INVOICES_DIR), name)
            }
            if (file.exists() && file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                file.delete()
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Cleans up orphaned invoice images older than [maxAgeMs] (default 24 hours).
     * Called on app startup or invoice scan completion to prevent disk leaks.
     *
     * @return count of deleted stale files.
     */
    fun cleanupStaleInvoices(context: Context, maxAgeMs: Long = MAX_AGE_MS): Int {
        var count = 0
        try {
            val dir = File(context.filesDir, INVOICES_DIR)
            if (!dir.exists()) return 0
            val cutoff = System.currentTimeMillis() - maxAgeMs
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoff) {
                    if (file.delete()) count++
                }
            }
        } catch (_: Exception) {
            // Fail safely without crashing
        }
        return count
    }
}
