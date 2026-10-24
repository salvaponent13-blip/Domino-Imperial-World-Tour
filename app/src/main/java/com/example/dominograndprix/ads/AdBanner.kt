package com.example.dominograndprix.ads

import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Banner adaptativo anclado de Google AdMob para Jetpack Compose.
 *
 * Características:
 * - Se adapta dinámicamente al ancho real del dispositivo en lugar de forzar 320x50 fijo.
 * - Destruye la instancia de [AdView] en `onRelease` al salir de composición para prevenir fugas.
 * - Si no hay anuncios o el dispositivo no tiene red, se oculta (`View.GONE`) para no dejar
 *   huecos vacíos antiestéticos en el diseño.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerUnitId
) {
    if (AdManager.adsDisabled) return

    val context = LocalContext.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)

        key(widthDp) {
            val adSize = remember(widthDp) {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            }

            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        this.adUnitId = adUnitId
                        setAdSize(adSize)
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                visibility = View.GONE
                            }

                            override fun onAdLoaded() {
                                visibility = View.VISIBLE
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { adView ->
                    adView.destroy()
                }
            )
        }
    }
}
