package com.guardaestados.domain.save

import org.junit.Assert.assertEquals
import org.junit.Test

class MultiSaveStatusSummaryCalculatorTest {
    private val calculator = MultiSaveStatusSummaryCalculator()

    @Test
    fun `counts all successful saves`() {
        val summary = calculator.summarize(
            listOf(
                SaveStatusImageResult.Success("one.jpg"),
                SaveStatusImageResult.Success("two.mp4")
            )
        )

        assertEquals(MultiSaveStatusSummary(savedCount = 2, failedCount = 0), summary)
    }

    @Test
    fun `counts non success results as not saved`() {
        val summary = calculator.summarize(
            listOf(
                SaveStatusImageResult.Success("one.jpg"),
                SaveStatusImageResult.Duplicate,
                SaveStatusImageResult.Error
            )
        )

        assertEquals(MultiSaveStatusSummary(savedCount = 1, failedCount = 2), summary)
    }

    @Test
    fun `counts all failed saves`() {
        val summary = calculator.summarize(
            listOf(
                SaveStatusImageResult.DestinationPermissionLost,
                SaveStatusImageResult.DestinationUnavailable
            )
        )

        assertEquals(MultiSaveStatusSummary(savedCount = 0, failedCount = 2), summary)
    }

    @Test
    fun `empty results have zero counts`() {
        val summary = calculator.summarize(emptyList())

        assertEquals(MultiSaveStatusSummary(savedCount = 0, failedCount = 0), summary)
    }
}
