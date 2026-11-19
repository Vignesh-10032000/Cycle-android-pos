package dev.vignesh.cycleshop.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RecognitionPhotoStorageTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun cleanupIsRestrictedToGeneratedRecognitionFiles() {
        val cache = folder.newFolder("cache")
        val invoice = File(folder.newFolder("invoices"), "capture-123.jpg").apply { writeText("invoice evidence") }
        val recognition = File(cache, "recognition_photos").apply { mkdir() }
        val stale = File(recognition, "prepared-123.jpg").apply { writeText("orphan") }
        val foreign = File(recognition, "keep.jpg").apply { writeText("unmanaged") }
        val store = RecognitionPhotoStore(cache)

        val active = store.createCapture()
        assertFalse(stale.exists())
        assertTrue(active.exists())
        val replacement = RecognitionPhotoStore(cache).createPrepared()
        assertTrue(active.exists())
        store.delete(active)
        store.delete(replacement)
        store.delete(invoice)
        store.delete(foreign)

        assertFalse(active.exists())
        assertFalse(replacement.exists())
        assertEquals("invoice evidence", invoice.readText())
        assertEquals("unmanaged", foreign.readText())
    }

    @Test
    fun samplesBeforeDecodingInsteadOfAllocatingFullResolution() {
        assertEquals(1, recognitionDecodeSample(2048))
        assertEquals(2, recognitionDecodeSample(2049))
        assertEquals(4, recognitionDecodeSample(6000))
        assertEquals(16, recognitionDecodeSample(20_000))
    }
}
