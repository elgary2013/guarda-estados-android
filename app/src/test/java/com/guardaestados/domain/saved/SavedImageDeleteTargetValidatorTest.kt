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
                relativePath = "Pictures/GuardaEstados/",
                mimeType = "image/jpeg"
            )
        )
    }

    @Test
    fun `accepts saved relative path without trailing slash`() {
        assertTrue(
            validator.isValid(
                relativePath = "Pictures/GuardaEstados",
                mimeType = "image/png"
            )
        )
    }

    @Test
    fun `rejects images outside saved relative path`() {
        assertFalse(
            validator.isValid(
                relativePath = "Pictures/WhatsApp/",
                mimeType = "image/jpeg"
            )
        )
    }

    @Test
    fun `rejects non image media in saved relative path`() {
        assertFalse(
            validator.isValid(
                relativePath = "Pictures/GuardaEstados/",
                mimeType = "video/mp4"
            )
        )
    }
}
