package com.guardaestados.domain.saved

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedImageDeleteTargetValidatorTest {
    private val validator = SavedImageDeleteTargetValidator()

    @Test
    fun `accepts images in saved relative path`() {
        assertTrue(
            validator.isValid(
                relativePath = "Pictures/EstadoGo/Im\u00E1genes/",
                mimeType = "image/jpeg"
            )
        )
    }

    @Test
    fun `accepts saved relative path without trailing slash`() {
        assertTrue(
            validator.isValid(
                relativePath = "Pictures/EstadoGo/Im\u00E1genes",
                mimeType = "image/png"
            )
        )
    }

    @Test
    fun `accepts saved status videos in app relative path`() {
        assertTrue(
            validator.isValid(
                relativePath = "Movies/EstadoGo/Videos/",
                mimeType = "video/mp4"
            )
        )
    }

    @Test
    fun `accepts generated video parts in app relative path`() {
        assertTrue(
            validator.isValid(
                relativePath = "Movies/EstadoGo/Videos/Partes/",
                mimeType = "video/mp4"
            )
        )
    }

    @Test
    fun `rejects media outside saved relative paths`() {
        assertFalse(
            validator.isValid(
                relativePath = "Pictures/WhatsApp/",
                mimeType = "image/jpeg"
            )
        )
        assertFalse(
            validator.isValid(
                relativePath = "Movies/Other/",
                mimeType = "video/mp4"
            )
        )
        assertFalse(
            validator.isValid(
                relativePath = "Pictures/GuardaEstados/",
                mimeType = "image/jpeg"
            )
        )
        assertFalse(
            validator.isValid(
                relativePath = "Pictures/EstadoGo/",
                mimeType = "image/jpeg"
            )
        )
        assertFalse(
            validator.isValid(
                relativePath = "Movies/EstadoGo/",
                mimeType = "video/mp4"
            )
        )
        assertFalse(
            validator.isValid(
                relativePath = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses/",
                mimeType = "image/jpeg"
            )
        )
    }

    @Test
    fun `rejects videos in image path and images in video paths`() {
        assertFalse(
            validator.isValid(
                relativePath = "Pictures/EstadoGo/Im\u00E1genes/",
                mimeType = "video/mp4"
            )
        )
        assertFalse(
            validator.isValid(
                relativePath = "Movies/EstadoGo/Videos/",
                mimeType = "image/jpeg"
            )
        )
        assertFalse(
            validator.isValid(
                relativePath = "Movies/EstadoGo/Videos/Partes/",
                mimeType = "image/jpeg"
            )
        )
    }
}
