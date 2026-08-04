package com.guardaestados.domain.save

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeImageFileNameGeneratorTest {
    private val generator = SafeImageFileNameGenerator()

    @Test
    fun `generates safe jpg name with timestamp`() {
        assertEquals(
            "estado_lindo_1234.jpg",
            generator.generate("estado lindo.jpeg", "image/jpeg", 1234L)
        )
    }

    @Test
    fun `uses mime type extension`() {
        assertEquals("foto_7.png", generator.generate("foto.webp", "image/png", 7L))
        assertEquals("foto_8.webp", generator.generate("foto.jpg", "image/webp", 8L))
    }

    @Test
    fun `falls back when original name has no safe characters`() {
        assertEquals(
            "estado_9.jpg",
            generator.generate("***.jpg", "image/jpeg", 9L)
        )
    }

    @Test
    fun `limits long base names`() {
        val generated = generator.generate("a".repeat(80) + ".png", "image/png", 10L)
        assertTrue(generated.startsWith("a".repeat(48)))
        assertTrue(generated.endsWith("_10.png"))
    }
}
