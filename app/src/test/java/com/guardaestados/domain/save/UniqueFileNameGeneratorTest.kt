package com.guardaestados.domain.save

import org.junit.Assert.assertEquals
import org.junit.Test

class UniqueFileNameGeneratorTest {
    private val generator = UniqueFileNameGenerator()

    @Test
    fun `keeps preferred name when it is available`() {
        val result = generator.generate("estado.jpg") { false }

        assertEquals("estado.jpg", result)
    }

    @Test
    fun `adds numeric suffix before extension when name exists`() {
        val existingNames = setOf("estado.jpg", "estado_1.jpg")

        val result = generator.generate("estado.jpg") { candidate -> candidate in existingNames }

        assertEquals("estado_2.jpg", result)
    }

    @Test
    fun `adds numeric suffix to extensionless names`() {
        val result = generator.generate("estado") { candidate -> candidate == "estado" }

        assertEquals("estado_1", result)
    }

    @Test
    fun `uses fallback name for blank preferred name`() {
        val result = generator.generate(" ") { false }

        assertEquals("archivo", result)
    }
}