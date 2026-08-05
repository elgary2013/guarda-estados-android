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
    }

    @Test
    fun `size value formats kilobytes and megabytes`() {
        assertEquals("2.0 KB", formatter.sizeValue(2048L))
        assertEquals("1.5 MB", formatter.sizeValue(1572864L))
    }

    @Test
    fun `format value maps common image mime types`() {
        assertEquals("JPG", formatter.formatValue("image/jpeg"))
        assertEquals("PNG", formatter.formatValue("image/png"))
        assertEquals("WebP", formatter.formatValue("image/webp"))
    }

    @Test
    fun `format value keeps unknown nonblank mime type`() {
        assertEquals("image/gif", formatter.formatValue(" image/gif "))
        assertNull(formatter.formatValue(" "))
    }

    @Test
    fun `dimensions value requires positive width and height`() {
        assertEquals("1080 x 1920 px", formatter.dimensionsValue(1080, 1920))
        assertNull(formatter.dimensionsValue(null, 1920))
        assertNull(formatter.dimensionsValue(1080, 0))
    }
}