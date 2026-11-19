package dev.vignesh.cycleshop.recognition

import java.io.File
import java.io.IOException

/** Only manages generated files in the private recognition cache. */
internal class RecognitionPhotoStore(private val cacheDir: File) {
    private val directory = File(cacheDir, "recognition_photos")

    fun createCapture(): File = create("capture", ".jpg")
    fun createImport(): File = create("import", ".img")
    fun createPrepared(): File = create("prepared", ".jpg")

    fun delete(file: File) {
        // Compare canonical paths to keep cleanup inside this directory, even for a symlink.
        runCatching {
            if (directory.canonicalFile == File(cacheDir.canonicalFile, directory.name) &&
                file.canonicalFile.parentFile == directory.canonicalFile &&
                file.canonicalFile.name == file.name && isGeneratedFile(file)
            ) {
                file.delete()
            }
        }
    }

    private fun create(prefix: String, extension: String): File {
        val root = directory.canonicalFile
        if (root != File(cacheDir.canonicalFile, directory.name)) {
            throw IOException("Recognition cache is unavailable")
        }
        root.mkdirs()
        if (!root.isDirectory) throw IOException("Recognition cache is unavailable")
        synchronized(initialized) {
            if (initialized.add(root)) {
                // On first use in this app process, any generated file is from an earlier process.
                root.listFiles()?.forEach { if (it.isFile) delete(it) }
            }
        }
        return File.createTempFile("$prefix-", extension, root)
    }

    private fun isGeneratedFile(file: File) = generatedName.matches(file.name)

    private companion object {
        val initialized = mutableSetOf<File>()
        val generatedName = Regex("(?:capture|prepared)-[0-9]+\\.jpg|import-[0-9]+\\.img")
    }
}
