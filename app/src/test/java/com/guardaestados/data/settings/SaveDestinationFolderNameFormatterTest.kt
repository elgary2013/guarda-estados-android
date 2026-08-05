package com.guardaestados.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SaveDestinationFolderNameFormatterTest {
    private val formatter = SaveDestinationFolderNameFormatter()

    @Test
    fun `formats tree uri document id as folder name`() {
        val result = formatter.format("content://provider/tree/primary%3ADownload%2FSaves")

        assertEquals("Saves", result)
    }

    @Test
    fun `formats raw tree id after colon as folder name`() {
        val result = formatter.format("primary:Pictures")

        assertEquals("Pictures", result)
    }

    @Test
    fun `falls back to original value when no name can be derived`() {
        val result = formatter.format("")

        assertEquals("", result)
    }
}