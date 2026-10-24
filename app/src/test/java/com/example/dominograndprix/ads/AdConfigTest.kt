package com.example.dominograndprix.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para validar que los identificadores de AdMob inyectados vía BuildConfig
 * cumplen el formato exigido por Google:
 * - El App ID contiene '~'.
 * - Las unidades de anuncio (banner e interstitial) contienen '/' y son distintas.
 * - Todos los IDs pertenecen al mismo publisher.
 */
class AdConfigTest {

    @Test
    fun appIdLooksLikeAnAppId() {
        assertTrue("appId vacío", AdConfig.appId.isNotBlank())
        assertTrue(
            "El App ID debe llevar '~' (recibido: ${AdConfig.appId})",
            AdConfig.appId.contains("~")
        )
        assertTrue(AdConfig.appId.startsWith("ca-app-pub-"))
    }

    @Test
    fun unitIdsLookLikeUnitIds() {
        for ((name, id) in listOf("banner" to AdConfig.bannerUnitId, "interstitial" to AdConfig.interstitialUnitId)) {
            assertTrue("$name vacío", id.isNotBlank())
            assertTrue("$name debe llevar '/' (recibido: $id)", id.contains("/"))
            assertTrue("$name debe empezar por ca-app-pub- (recibido: $id)", id.startsWith("ca-app-pub-"))
        }
    }

    @Test
    fun bannerAndInterstitialAreDifferentUnits() {
        assertTrue(AdConfig.bannerUnitId != AdConfig.interstitialUnitId)
    }

    @Test
    fun testIdsAreDetectedWhileTheAppIsNotPublished() {
        assertEquals(
            AdConfig.appId.startsWith(AdConfig.TEST_PUBLISHER_PREFIX),
            AdConfig.usingTestIds
        )
    }

    @Test
    fun unitIdsShareThePublisherOfTheAppId() {
        val publisher = AdConfig.appId.substringBefore('~')
        assertTrue(
            "Banner de otro publisher: ${AdConfig.bannerUnitId}",
            AdConfig.bannerUnitId.startsWith(publisher)
        )
        assertTrue(
            "Interstitial de otro publisher: ${AdConfig.interstitialUnitId}",
            AdConfig.interstitialUnitId.startsWith(publisher)
        )
    }
}
