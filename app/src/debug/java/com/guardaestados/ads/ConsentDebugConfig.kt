package com.guardaestados.ads

import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.guardaestados.BuildConfig

internal object ConsentDebugConfig {
    fun create(context: Context): ConsentDebugSettings? {
        val builder = ConsentDebugSettings.Builder(context)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)

        if (BuildConfig.UMP_TEST_DEVICE_HASHED_ID.isNotBlank()) {
            builder.addTestDeviceHashedId(BuildConfig.UMP_TEST_DEVICE_HASHED_ID)
        }

        return builder.build()
    }
}
