package com.guardaestados.domain.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSplitPlannerTest {
    private val planner = VideoSplitPlanner()

    @Test
    fun `creates ordered segments using selected duration`() {
        val parts = planner.plan(durationMs = 65_000L, partDurationSeconds = 30)

        assertEquals(3, parts.size)
        assertEquals(VideoSegment(index = 1, startMs = 0L, endMs = 30_000L), parts[0])
        assertEquals(VideoSegment(index = 2, startMs = 30_000L, endMs = 60_000L), parts[1])
        assertEquals(VideoSegment(index = 3, startMs = 60_000L, endMs = 65_000L), parts[2])
    }

    @Test
    fun `returns no segments for unknown empty or invalid duration`() {
        assertTrue(planner.plan(null, 30).isEmpty())
        assertTrue(planner.plan(0L, 30).isEmpty())
        assertTrue(planner.plan(10_000L, 0).isEmpty())
    }

    @Test
    fun `estimates part count`() {
        assertEquals(5, planner.estimatedPartCount(60_001L, 15))
        assertEquals(2, planner.estimatedPartCount(60_000L, 30))
    }
}

class SafeVideoPartNameGeneratorTest {
    private val generator = SafeVideoPartNameGenerator()

    @Test
    fun `generates safe ordered mp4 names`() {
        assertEquals(
            "mi_video_parte_03_1234.mp4",
            generator.generate("mi video!!.mov", partIndex = 3, timestampMillis = 1234L)
        )
    }

    @Test
    fun `falls back for blank names and clamps invalid part index`() {
        assertEquals(
            "video_parte_01_99.mp4",
            generator.generate(" ", partIndex = -4, timestampMillis = 99L)
        )
    }
}

class ReadableVideoDurationFormatterTest {
    private val formatter = ReadableVideoDurationFormatter()

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("1:05", formatter.format(65_000L))
    }

    @Test
    fun `formats hours minutes and seconds`() {
        assertEquals("1:01:01", formatter.format(3_661_000L))
    }

    @Test
    fun `formats unknown duration`() {
        assertEquals("No disponible", formatter.format(null))
        assertEquals("No disponible", formatter.format(0L))
    }
}
