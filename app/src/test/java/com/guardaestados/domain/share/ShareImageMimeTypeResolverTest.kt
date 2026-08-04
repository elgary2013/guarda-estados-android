package com.guardaestados.domain.share

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareImageMimeTypeResolverTest {
    private val resolver = ShareImageMimeTypeResolver()

    @Test
    fun keepsImageMimeTypeWhenAvailable() {
        assertEquals("image/jpeg", resolver.resolve("image/jpeg"))
    }

    @Test
    fun fallsBackWhenMimeTypeIsBlank() {
        assertEquals("image/*", resolver.resolve(""))
    }

    @Test
    fun fallsBackWhenMimeTypeIsNotImage() {
        assertEquals("image/*", resolver.resolve("application/octet-stream"))
    }
}
