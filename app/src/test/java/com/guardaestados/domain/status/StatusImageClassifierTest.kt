package com.guardaestados.domain.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusImageClassifierTest {
    private val classifier = StatusImageClassifier()

    @Test
    fun `accepts supported image mime types`() {
        assertTrue(classifier.isAccepted(candidate(name = "photo.jpeg", mimeType = "image/jpeg")))
        assertTrue(classifier.isAccepted(candidate(name = "photo.jpg", mimeType = "image/jpg")))
        assertTrue(classifier.isAccepted(candidate(name = "photo.png", mimeType = "image/png")))
        assertTrue(classifier.isAccepted(candidate(name = "photo.webp", mimeType = "image/webp")))
    }

    @Test
    fun `accepts supported video mime types`() {
        assertTrue(classifier.isAccepted(candidate(name = "status.mp4", mimeType = "video/mp4")))
        assertTrue(classifier.isAccepted(candidate(name = "status.3gp", mimeType = "video/3gpp")))
        assertTrue(classifier.isAccepted(candidate(name = "status.webm", mimeType = "video/webm")))
    }

    @Test
    fun `normalizes jpg alias to jpeg`() {
        assertEquals("image/jpeg", classifier.normalizeMimeType("image/jpg"))
    }

    @Test
    fun `normalizes supported video type`() {
        assertEquals("video/mp4", classifier.normalizeMimeType(" video/mp4 "))
    }

    @Test
    fun `rejects directories and unsupported mime types`() {
        assertFalse(classifier.isAccepted(candidate(name = "folder", mimeType = "image/jpeg", isDirectory = true)))
        assertFalse(classifier.isAccepted(candidate(name = "unknown", mimeType = null)))
        assertFalse(classifier.isAccepted(candidate(name = "archive.zip", mimeType = "application/zip")))
    }

    @Test
    fun `rejects temporary and invalid files`() {
        assertFalse(classifier.isAccepted(candidate(name = ".pending.jpg", mimeType = "image/jpeg")))
        assertFalse(classifier.isAccepted(candidate(name = "photo.tmp", mimeType = "image/png")))
        assertFalse(classifier.isAccepted(candidate(name = "clip.mp4", mimeType = "video/mp4", sizeBytes = 0L)))
        assertFalse(classifier.isAccepted(candidate(name = "", mimeType = "image/jpeg")))
    }

    private fun candidate(
        name: String?,
        mimeType: String?,
        isDirectory: Boolean = false,
        sizeBytes: Long? = 128L
    ): StatusImageCandidate {
        return StatusImageCandidate(
            name = name,
            mimeType = mimeType,
            isDirectory = isDirectory,
            sizeBytes = sizeBytes
        )
    }
}
