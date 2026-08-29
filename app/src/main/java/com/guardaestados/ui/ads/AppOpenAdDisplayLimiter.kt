package com.guardaestados.ui.ads

internal const val AppOpenAdMinimumIntervalMillis: Long = 8L * 60L * 60L * 1000L

internal interface AppOpenAdDisplayStore {
    fun hasSeenFirstOpen(): Boolean
    fun markFirstOpenSeen()
    fun lastShownAtMillis(): Long
    fun recordShownAtMillis(timestampMillis: Long)
}

internal class AppOpenAdDisplayLimiter(
    private val store: AppOpenAdDisplayStore,
    private val minimumIntervalMillis: Long = AppOpenAdMinimumIntervalMillis
) {
    fun markColdStartObserved(): Boolean {
        if (store.hasSeenFirstOpen()) {
            return true
        }

        store.markFirstOpenSeen()
        return false
    }

    fun canShow(nowMillis: Long): Boolean {
        val lastShownAt = store.lastShownAtMillis()
        return lastShownAt <= 0L || nowMillis - lastShownAt >= minimumIntervalMillis
    }

    fun recordShown(nowMillis: Long) {
        store.recordShownAtMillis(nowMillis)
    }
}
