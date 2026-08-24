package com.guardaestados.domain.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTrimPlannerTest {
    private val planner = VideoTrimPlanner()

    @Test
    fun `calculates trim duration in seconds`() {
        val range = VideoTrimRange(startSeconds = 165, endSeconds = 360)

        assertEquals(195, planner.durationSeconds(range))
    }

    @Test
    fun `maps selected range to absolute original milliseconds`() {
        val range = VideoTrimRange(startSeconds = 3, endSeconds = 9)

        assertEquals(3_000L, range.startMs)
        assertEquals(9_000L, range.endMs)
        assertEquals(6_000L, range.durationMs)
    }

    @Test
    fun `validates range inside video duration`() {
        val range = VideoTrimRange(startSeconds = 165, endSeconds = 360)

        assertEquals(VideoTrimRangeValidation.Valid, planner.validate(range, durationMs = 420_000L))
    }

    @Test
    fun `formats seconds as minutes and seconds`() {
        assertEquals("0:00", planner.formatSeconds(0))
        assertEquals("2:45", planner.formatSeconds(165))
        assertEquals("6:00", planner.formatSeconds(360))
        assertEquals("3:15", planner.formatSeconds(195))
    }

    @Test
    fun `rejects start equal to or after end`() {
        assertEquals(
            VideoTrimRangeValidation.StartNotBeforeEnd,
            planner.validate(VideoTrimRange(startSeconds = 60, endSeconds = 60), durationMs = 420_000L)
        )
        assertEquals(
            VideoTrimRangeValidation.StartNotBeforeEnd,
            planner.validate(VideoTrimRange(startSeconds = 120, endSeconds = 60), durationMs = 420_000L)
        )
    }

    @Test
    fun `rejects ranges outside video duration`() {
        assertEquals(
            VideoTrimRangeValidation.OutsideDuration,
            planner.validate(VideoTrimRange(startSeconds = -1, endSeconds = 60), durationMs = 420_000L)
        )
        assertEquals(
            VideoTrimRangeValidation.OutsideDuration,
            planner.validate(VideoTrimRange(startSeconds = 60, endSeconds = 421), durationMs = 420_000L)
        )
    }

    @Test
    fun `coerces range to keep at least one second`() {
        val previousRange = VideoTrimRange(startSeconds = 3, endSeconds = 9)

        assertEquals(
            VideoTrimRange(startSeconds = 8, endSeconds = 9),
            planner.coerceRange(
                startSeconds = 9,
                endSeconds = 9,
                durationMs = 10_000L,
                previousRange = previousRange
            )
        )
        assertEquals(
            VideoTrimRange(startSeconds = 3, endSeconds = 4),
            planner.coerceRange(
                startSeconds = 3,
                endSeconds = 3,
                durationMs = 10_000L,
                previousRange = previousRange
            )
        )
    }

    @Test
    fun `coerces range inside video duration`() {
        assertEquals(
            VideoTrimRange(startSeconds = 0, endSeconds = 10),
            planner.coerceRange(startSeconds = -5, endSeconds = 20, durationMs = 10_000L)
        )
    }

    @Test
    fun `adjusts start by five seconds without crossing end`() {
        val range = VideoTrimRange(startSeconds = 3, endSeconds = 9)

        assertEquals(
            VideoTrimRange(startSeconds = 0, endSeconds = 9),
            planner.adjustStart(range, deltaSeconds = -5, durationMs = 10_000L)
        )
        assertEquals(
            VideoTrimRange(startSeconds = 8, endSeconds = 9),
            planner.adjustStart(range, deltaSeconds = 5, durationMs = 10_000L)
        )
    }

    @Test
    fun `adjusts end by five seconds without crossing start or duration`() {
        val range = VideoTrimRange(startSeconds = 3, endSeconds = 9)

        assertEquals(
            VideoTrimRange(startSeconds = 3, endSeconds = 4),
            planner.adjustEnd(range, deltaSeconds = -5, durationMs = 10_000L)
        )
        assertEquals(
            VideoTrimRange(startSeconds = 3, endSeconds = 10),
            planner.adjustEnd(range, deltaSeconds = 5, durationMs = 10_000L)
        )
    }
}
