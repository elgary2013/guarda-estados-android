package com.guardaestados.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdsPrivacyState(
    val canRequestAds: Boolean,
    val privacyOptionsAvailable: Boolean
)

class ConsentManager(context: Context) {
    private val appContext = context.applicationContext
    private val consentInformation = UserMessagingPlatform.getConsentInformation(appContext)
    private val mobileAdsInitializationStarted = AtomicBoolean(false)
    private val mobileAdsReady = AtomicBoolean(false)
    private val _privacyState = MutableStateFlow(currentPrivacyState())

    val privacyState: StateFlow<AdsPrivacyState> = _privacyState.asStateFlow()

    fun updateConsent(activity: Activity) {
        val params = consentRequestParameters(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                refreshState()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    refreshState()
                    initializeMobileAdsIfAllowed()
                }
            },
            {
                refreshState()
                initializeMobileAdsIfAllowed()
            }
        )
    }

    private fun consentRequestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        ConsentDebugConfig.create(activity)?.let(builder::setConsentDebugSettings)
        return builder.build()
    }

    fun showPrivacyOptions(
        activity: Activity,
        onUnavailable: () -> Unit,
        onError: () -> Unit
    ) {
        if (!currentPrivacyState().privacyOptionsAvailable) {
            onUnavailable()
            return
        }
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            refreshState()
            initializeMobileAdsIfAllowed()
            if (formError != null) {
                onError()
            }
        }
    }

    private fun initializeMobileAdsIfAllowed() {
        if (!consentInformation.canRequestAds()) {
            refreshState()
            return
        }
        if (mobileAdsInitializationStarted.compareAndSet(false, true)) {
            MobileAds.initialize(appContext) {
                mobileAdsReady.set(true)
                refreshState()
            }
        } else {
            refreshState()
        }
    }

    private fun refreshState() {
        _privacyState.value = currentPrivacyState()
    }

    private fun currentPrivacyState(): AdsPrivacyState {
        return AdsPrivacyState(
            canRequestAds = consentInformation.canRequestAds() && mobileAdsReady.get(),
            privacyOptionsAvailable = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        )
    }
}
