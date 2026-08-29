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
    fun canShow_respectsEightHourInterval() {
        val store = FakeAppOpenAdDisplayStore(lastShownAtMillis = 1_000L)
        val limiter = AppOpenAdDisplayLimiter(store)

        assertFalse(limiter.canShow(1_000L + AppOpenAdMinimumIntervalMillis - 1L))
        assertTrue(limiter.canShow(1_000L + AppOpenAdMinimumIntervalMillis))
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
