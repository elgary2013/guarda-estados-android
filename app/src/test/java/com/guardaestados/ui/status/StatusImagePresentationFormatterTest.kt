package com.guardaestados.ui.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusImagePresentationFormatterTest {
    private val formatter = StatusImagePresentationFormatter()

    @Test
    fun `title prefers trimmed file name`() {
        assertEquals("foto.png", formatter.title(" foto.png ", "01/01/26"))
    }

    @Test
    fun `title falls back to date when name is blank`() {
        assertEquals("01/01/26", formatter.title("  ", "01/01/26"))
    }

    @Test
    fun `size value ignores missing or invalid values`() {
        assertNull(formatter.sizeValue(null))
        assertNull(formatter.sizeValue(0L))
        assertEquals("2048", formatter.sizeValue(2048L))
    }
}
