package com.guardaestados.domain.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class MediaDetailsFormatterTest {
    private val formatter = MediaDetailsFormatter()

    @Test
    fun `size formats kilobytes and megabytes`() {
        assertEquals("2.0 KB", formatter.formatSize(2048L))
        assertEquals("1.5 MB", formatter.formatSize(1572864L))
    }

    @Test
    fun `size returns null for unavailable values`() {
        assertNull(formatter.formatSize(null))
        assertNull(formatter.formatSize(0L))
    }

    @Test
    fun `duration formats minutes and hours`() {
        assertEquals("0:15", formatter.formatDuration(15_000L))
        assertEquals("1:02", formatter.formatDuration(62_000L))
        assertEquals("1:01:05", formatter.formatDuration(3_665_000L))
    }

    @Test
    fun `duration returns null for unavailable values`() {
        assertNull(formatter.formatDuration(null))
        assertNull(formatter.formatDuration(0L))
    }

    @Test
    fun `format maps common image and video mime types`() {
        assertEquals("JPG", formatter.formatMimeType("image/jpeg"))
        assertEquals("PNG", formatter.formatMimeType("image/png"))
        assertEquals("WebP", formatter.formatMimeType("image/webp"))
        assertEquals("MP4", formatter.formatMimeType("video/mp4"))
        assertEquals("WebM", formatter.formatMimeType("video/webm"))
        assertEquals("3GP", formatter.formatMimeType("video/3gpp"))
    }

    @Test
    fun `format returns null for unavailable mime type`() {
        assertNull(formatter.formatMimeType(null))
        assertNull(formatter.formatMimeType(" "))
    }

    @Test
    fun `date time uses supplied locale and time zone`() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals(
            "1/1/26, 12:00 AM",
            formatter.formatDateTime(
                millis = 1_767_225_600_000L,
                locale = Locale.US,
                timeZone = utc
            )?.replace('\u202F', ' ')
        )
    }

    @Test
    fun `date time returns null for unavailable values`() {
        assertNull(formatter.formatDateTime(null))
        assertNull(formatter.formatDateTime(0L))
    }
}
