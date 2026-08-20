package com.guardaestados.domain.saved

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SavedMultiActionSummaryCalculatorTest {
    private val calculator = SavedMultiActionSummaryCalculator()

    @Test
    fun `summarizes all successful actions`() {
        val summary = calculator.summarize(successCount = 3, totalCount = 3)

        assertEquals(SavedMultiActionSummary(successCount = 3, failedCount = 0), summary)
    }

    @Test
    fun `summarizes partial actions`() {
        val summary = calculator.summarize(successCount = 2, totalCount = 3)

        assertEquals(SavedMultiActionSummary(successCount = 2, failedCount = 1), summary)
    }

    @Test
    fun `summarizes multi delete successes omissions and failures`() {
        val summary = calculator.summarize(successCount = 2, totalCount = 5)

        assertEquals(SavedMultiActionSummary(successCount = 2, failedCount = 3), summary)
    }

    @Test
    fun `summarizes all failed actions`() {
        val summary = calculator.summarize(successCount = 0, totalCount = 2)

        assertEquals(SavedMultiActionSummary(successCount = 0, failedCount = 2), summary)
    }

    @Test
    fun `rejects impossible counts`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.summarize(successCount = 4, totalCount = 3)
        }
    }
}
