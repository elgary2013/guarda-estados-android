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
    fun keepsVideoMimeTypeWhenAvailable() {
        assertEquals("video/mp4", resolver.resolve("video/mp4"))
    }

    @Test
    fun fallsBackWhenMimeTypeIsBlank() {
        assertEquals("image/*", resolver.resolve(""))
    }

    @Test
    fun fallsBackWhenMimeTypeIsNotSupported() {
        assertEquals("image/*", resolver.resolve("application/octet-stream"))
    }
}
