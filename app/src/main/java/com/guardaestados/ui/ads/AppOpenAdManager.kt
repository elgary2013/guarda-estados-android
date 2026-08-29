package com.guardaestados.ui.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.concurrent.atomic.AtomicBoolean

class AppOpenAdManager(
    context: Context,
    private val launchStartedElapsedRealtime: Long = SystemClock.elapsedRealtime(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val displayLimiter = AppOpenAdDisplayLimiter(
        SharedPreferencesAppOpenAdDisplayStore(appContext)
    )
    private val coldStartAttempted = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)
    private var canShowOnThisColdStart = false
    private var loadTimedOut = false
    private var appOpenAd: AppOpenAd? = null

    fun markColdStartObserved() {
        canShowOnThisColdStart = displayLimiter.markColdStartObserved()
    }

    fun loadAndShowOnColdStart(activity: Activity) {
        if (!canShowOnThisColdStart) return
        if (!coldStartAttempted.compareAndSet(false, true)) return
        if (!isWithinColdStartWindow()) return
        if (!displayLimiter.canShow(nowMillis())) return
        if (!activity.isReadyForAppOpenAd()) return
        if (!isLoading.compareAndSet(false, true)) return

        loadTimedOut = false
        mainHandler.postDelayed(
            {
                loadTimedOut = true
                if (!isShowing.get()) {
                    appOpenAd = null
                }
            },
            AppOpenAdLoadTimeoutMillis
        )

        AppOpenAd.load(
            appContext,
            AppOpenAdUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading.set(false)
                    if (loadTimedOut || !isStillEligible(activity)) {
                        appOpenAd = null
                        return
                    }

                    appOpenAd = ad
                    showLoadedAd(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading.set(false)
                    appOpenAd = null
                }
            }
        )
    }

    private fun showLoadedAd(activity: Activity) {
        val ad = appOpenAd ?: return
        if (!isShowing.compareAndSet(false, true)) return
        if (loadTimedOut || !isStillEligible(activity)) {
            isShowing.set(false)
            appOpenAd = null
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                displayLimiter.recordShown(nowMillis())
            }

            override fun onAdDismissedFullScreenContent() {
                releaseAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                releaseAd()
            }
        }
        ad.show(activity)
    }

    private fun releaseAd() {
        appOpenAd = null
        isShowing.set(false)
    }

    private fun isStillEligible(activity: Activity): Boolean {
        return isWithinColdStartWindow() && activity.isReadyForAppOpenAd()
    }

    private fun isWithinColdStartWindow(): Boolean {
        return SystemClock.elapsedRealtime() - launchStartedElapsedRealtime <= AppOpenAdColdStartWindowMillis
    }
}

private class SharedPreferencesAppOpenAdDisplayStore(
    context: Context
) : AppOpenAdDisplayStore {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        AppOpenAdPreferencesName,
        Context.MODE_PRIVATE
    )

    override fun hasSeenFirstOpen(): Boolean {
        return preferences.getBoolean(AppOpenAdFirstOpenSeenKey, false)
    }

    override fun markFirstOpenSeen() {
        preferences.edit().putBoolean(AppOpenAdFirstOpenSeenKey, true).apply()
    }

    override fun lastShownAtMillis(): Long {
        return preferences.getLong(AppOpenAdLastShownAtKey, 0L)
    }

    override fun recordShownAtMillis(timestampMillis: Long) {
        preferences.edit().putLong(AppOpenAdLastShownAtKey, timestampMillis).apply()
    }
}

private fun Activity.isReadyForAppOpenAd(): Boolean {
    val lifecycleOwner = this as? LifecycleOwner ?: return false
    return !isFinishing &&
        !isDestroyed &&
        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
}

private const val AppOpenAdPreferencesName = "app_open_ads"
private const val AppOpenAdFirstOpenSeenKey = "first_open_seen"
private const val AppOpenAdLastShownAtKey = "last_shown_at_millis"
private const val AppOpenAdColdStartWindowMillis = 10_000L
private const val AppOpenAdLoadTimeoutMillis = 4_000L
