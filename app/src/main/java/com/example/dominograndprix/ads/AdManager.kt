package com.example.dominograndprix.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Gestor del anuncio intersticial de AdMob para Dominó Grand Prix.
 *
 * Características de seguridad y experiencia:
 * - Precarga en segundo plano utilizando el [Context.getApplicationContext] para no fugar
 *   referencias a Activities destruidas.
 * - Cooldown mínimo entre intersticiales (60 segundos) para no saturar al usuario.
 * - [show] garantiza la ejecución de [onDismissed] bajo cualquier escenario (sin conexión,
 *   fallo de carga o tras cerrar el anuncio), impidiendo bloqueos de pantalla.
 * - Soporte para desactivación total ([adsDisabled]), útil para futuras compras integradas.
 */
object AdManager {
    private const val TAG = "AdManager"
    private const val COOLDOWN_MS = 60_000L

    var adsDisabled: Boolean = false

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownTime = 0L

    /**
     * Inicializa el SDK de MobileAds y lanza la precarga del primer intersticial.
     */
    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context.applicationContext) { status ->
                Log.d(TAG, "MobileAds inicializado: $status")
            }
            preload(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Error al inicializar MobileAds: ${e.message}")
        }
    }

    /**
     * Precarga un anuncio intersticial si no hay uno ya listo ni en curso de carga.
     */
    fun preload(context: Context, adUnitId: String = AdConfig.interstitialUnitId) {
        if (adsDisabled) return
        if (interstitialAd != null || isLoading) return
        isLoading = true

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial de AdMob cargado y listo.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "Fallo al cargar interstitial: ${error.message} (code ${error.code})")
                }
            }
        )
    }

    /** Hay un intersticial cargado y disponible para mostrarse. */
    val isReady: Boolean get() = interstitialAd != null

    /**
     * Muestra el intersticial si está listo y se cumple el tiempo de cooldown.
     * [onDismissed] se invoca siempre para permitir continuar el flujo del juego.
     */
    fun show(activity: Activity, force: Boolean = false, onDismissed: (() -> Unit)? = null) {
        if (adsDisabled) {
            onDismissed?.invoke()
            return
        }

        val now = System.currentTimeMillis()
        if (!force && (now - lastShownTime < COOLDOWN_MS)) {
            Log.d(TAG, "Intersticial pospuesto por cooldown (${(now - lastShownTime) / 1000}s transcurridos)")
            onDismissed?.invoke()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            preload(activity)
            onDismissed?.invoke()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownTime = System.currentTimeMillis()
                preload(activity)
                onDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preload(activity)
                onDismissed?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Intersticial mostrado en pantalla.")
            }
        }

        ad.show(activity)
    }

    /** Descarta el anuncio en memoria. */
    fun clear() {
        interstitialAd = null
        isLoading = false
    }
}
