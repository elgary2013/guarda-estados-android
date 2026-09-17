package com.guardaestados.ui.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdDisplayLimiterTest {
    @Test
    fun markColdStartObserved_skipsFirstHistoricalOpen() {
        val store = FakeAppOpenAdDisplayStore()
        val limiter = AppOpenAdDisplayLimiter(store)

        assertFalse(limiter.markColdStartObserved())
        assertTrue(store.hasSeenFirstOpen())
        assertTrue(limiter.markColdStartObserved())
    }

    @Test
    fun canShow_respectsFourHourInterval() {
        val store = FakeAppOpenAdDisplayStore(lastShownAtMillis = 1_000L)
        val limiter = AppOpenAdDisplayLimiter(store)
        val fourHoursMillis = 4L * 60L * 60L * 1000L

        assertFalse(limiter.canShow(1_000L + fourHoursMillis - 1L))
        assertTrue(limiter.canShow(1_000L + fourHoursMillis))
    }

    @Test
    fun recordShown_persistsTimestamp() {
        val store = FakeAppOpenAdDisplayStore()
        val limiter = AppOpenAdDisplayLimiter(store)

        limiter.recordShown(12_345L)

        assertFalse(limiter.canShow(12_345L + AppOpenAdMinimumIntervalMillis - 1L))
        assertTrue(limiter.canShow(12_345L + AppOpenAdMinimumIntervalMillis))
    }
}

private class FakeAppOpenAdDisplayStore(
    private var hasSeenFirstOpen: Boolean = false,
    private var lastShownAtMillis: Long = 0L
) : AppOpenAdDisplayStore {
    override fun hasSeenFirstOpen(): Boolean = hasSeenFirstOpen

    override fun markFirstOpenSeen() {
        hasSeenFirstOpen = true
    }

    override fun lastShownAtMillis(): Long = lastShownAtMillis

    override fun recordShownAtMillis(timestampMillis: Long) {
        lastShownAtMillis = timestampMillis
    }
}
