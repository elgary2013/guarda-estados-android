package com.guardaestados.ui.ads

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.guardaestados.R
import com.guardaestados.ui.theme.LocalGuardaEstadosColors
import kotlin.math.roundToInt

private const val AndroidTestBannerAdUnitId = "ca-app-pub-3940256099942544/9214589741"

@Composable
fun AdaptiveBannerAd(
    adUnitId: String,
    canRequestAds: Boolean,
    modifier: Modifier = Modifier
) {
    if (!canRequestAds) {
        return
    }

    val context = LocalContext.current
    val resolvedAdUnitId = remember(context, adUnitId) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            AndroidTestBannerAdUnitId
        } else {
            adUnitId
        }
    }
    var loadFailed by remember(resolvedAdUnitId) { mutableStateOf(false) }

    if (loadFailed) {
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val adWidth = maxWidth.value.roundToInt()
        if (adWidth <= 0) {
            return@BoxWithConstraints
        }
        val adSize = remember(context, adWidth) {
            @Suppress("DEPRECATION")
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        }
        val adView = remember(context, resolvedAdUnitId, adWidth) {
            AdView(context).apply {
                setAdUnitId(resolvedAdUnitId)
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadFailed = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(adView) {
            onDispose {
                adView.destroy()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.ad_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = LocalGuardaEstadosColors.current.body
            )
            key(resolvedAdUnitId, adWidth) {
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
