package com.example.dominograndprix.ads

import com.example.dominograndprix.BuildConfig

/**
 * Punto único de configuración de los IDs de Google AdMob para Dominó Grand Prix.
 *
 * Los valores provienen de `app/build.gradle.kts` a través de [BuildConfig],
 * incluido el APPLICATION_ID inyectado en el manifiesto mediante manifestPlaceholders.
 *
 * Durante la fase de desarrollo y prueba cerrada (Closed Testing) se utilizan los IDs
 * oficiales de PRUEBA de Google (prefijo 3940256099942544) para prevenir penalizaciones
 * por tráfico inválido.
 */
object AdConfig {
    const val TEST_PUBLISHER_PREFIX = "ca-app-pub-3940256099942544"

    /** App ID de AdMob (el mismo declarado en el manifiesto). */
    val appId: String get() = BuildConfig.ADMOB_APP_ID

    /** ID del bloque de anuncio para el banner inferior. */
    val bannerUnitId: String get() = BuildConfig.ADMOB_BANNER_ID

    /** ID del bloque de anuncio para el intersticial entre partidas. */
    val interstitialUnitId: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID

    /** `true` mientras se estén usando los identificadores de prueba de Google. */
    val usingTestIds: Boolean get() = appId.startsWith(TEST_PUBLISHER_PREFIX)
}
