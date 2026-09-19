package com.guardaestados.data.status

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusImageRepositoryTest {
    @Test
    fun `defective document does not prevent other documents from loading`() {
        val loaded = mapDocumentsSafely(sequenceOf("first", "broken", "last")) { document ->
            if (document == "broken") error("Unreadable SAF document")
            document.uppercase()
        }

        assertEquals(listOf("FIRST", "LAST"), loaded)
    }
}
