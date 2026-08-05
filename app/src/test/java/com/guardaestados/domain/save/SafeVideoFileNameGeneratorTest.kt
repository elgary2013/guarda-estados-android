package com.guardaestados.domain.save

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeVideoFileNameGeneratorTest {
    private val generator = SafeVideoFileNameGenerator()

    @Test
    fun `generates deterministic safe mp4 name`() {
        assertEquals("mi_video.mp4", generator.generate("mi video!!.mp4", "video/mp4"))
    }

    @Test
    fun `uses mime type extension when available`() {
        assertEquals("estado.3gp", generator.generate("estado.mov", "video/3gpp"))
        assertEquals("estado.webm", generator.generate("estado.mov", "video/webm"))
    }

    @Test
    fun `falls back for blank names`() {
        assertEquals("video.mp4", generator.generate(" ", "video/mp4"))
    }
}
