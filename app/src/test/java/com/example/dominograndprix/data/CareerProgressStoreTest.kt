package com.example.dominograndprix.data

import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.model.GameMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Persistencia de la carrera (sin Android).
 *
 * El juego no tiene moneda: lo que se guarda es el progreso de las sedes, los logros y las
 * estadisticas. Dos cosas son criticas y estan cubiertas aqui:
 * 1. Un guardado de una version anterior (con `cash`/`wrp`/`soldTrophyIds`) debe seguir
 *    cargando sin perder el progreso.
 * 2. Guardar NUNCA puede quitar sedes completadas o desbloqueadas.
 */
class CareerProgressStoreTest {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private fun venue(
        id: String,
        unlocked: Boolean = false,
        completed: Boolean = false
    ) = CityVenue(
        id = id,
        name = id,
        region = "region",
        targetScore = 100,
        defaultMode = GameMode.DRAW,
        trophyTitle = "trofeo",
        rivalName = "rival",
        rivalIntro = "intro",
        rivalDefeatLine = "derrota",
        isUnlocked = unlocked,
        isCompleted = completed
    )

    // --- Serialización ---

    @Test
    fun progress_survivesAJsonRoundTrip() {
        val original = JourneyProgress(
            completedVenueIds = setOf("cuba", "mexico"),
            unlockedVenueIds = setOf("cuba", "mexico", "puerto_rico"),
            unlockedAchievements = setOf("first_win"),
            currentVenueIndex = 1,
            currentChallengeIndex = 3
        )

        val restored = json.decodeFromString(
            JourneyProgress.serializer(),
            json.encodeToString(JourneyProgress.serializer(), original)
        )

        assertEquals(original, restored)
    }

    @Test
    fun emptyProgress_hasNoProgressAndStartsAtTheFirstChallenge() {
        val fresh = JourneyProgress()
        assertEquals(0, fresh.currentVenueIndex)
        assertEquals(0, fresh.currentChallengeIndex)
        assertTrue(fresh.completedVenueIds.isEmpty())
        assertTrue(fresh.unlockedVenueIds.isEmpty())
        assertTrue(fresh.unlockedAchievements.isEmpty())
        assertEquals(0, fresh.stats.matchesPlayed)
    }

    @Test
    fun anOldSaveWithCashAndWrpStillLoadsWithoutLosingProgress() {
        // Guardado real de la v2.4.0: incluye claves que ya no existen en el modelo. Con
        // `ignoreUnknownKeys` se ignoran y el progreso se conserva; sin el, este test fallaba
        // y el jugador perdia la carrera entera.
        val oldSave = """
            {"cash":7777,"wrp":3300,
             "completedVenueIds":["cuba","mexico"],
             "unlockedVenueIds":["cuba","mexico","puerto_rico"],
             "soldTrophyIds":["cigars"],
             "unlockedAchievements":["first_win"],
             "currentVenueIndex":2,"currentChallengeIndex":1,
             "bankruptcyRescueClaimed":false}
        """.trimIndent()

        val loaded = json.decodeFromString(JourneyProgress.serializer(), oldSave)

        assertEquals(setOf("cuba", "mexico"), loaded.completedVenueIds)
        assertEquals(setOf("cuba", "mexico", "puerto_rico"), loaded.unlockedVenueIds)
        assertEquals(setOf("first_win"), loaded.unlockedAchievements)
        assertEquals(2, loaded.currentVenueIndex)
        assertEquals(1, loaded.currentChallengeIndex)
    }

    @Test
    fun partialJson_fillsMissingFieldsWithDefaults() {
        val partial = json.decodeFromString(
            JourneyProgress.serializer(),
            """{"cash":999,"wrp":50}"""
        )
        assertTrue(partial.completedVenueIds.isEmpty())
        assertTrue(partial.unlockedAchievements.isEmpty())
        assertEquals(0, partial.currentVenueIndex)
    }

    // --- Captura del estado actual ---

    @Test
    fun snapshot_capturesCompletedAndUnlockedVenues() {
        val saved = CareerProgressStore.snapshot(
            progress = JourneyProgress(),
            venues = listOf(
                venue("cuba", unlocked = true, completed = true),
                venue("mexico", unlocked = true),
                venue("puerto_rico")
            )
        )

        assertEquals(setOf("cuba"), saved.completedVenueIds)
        assertEquals(setOf("cuba", "mexico"), saved.unlockedVenueIds)
    }

    @Test
    fun snapshot_neverLosesProgressThatTheCatalogDoesNotKnow() {
        // El caso real: el catalogo en memoria viene de antes de jugar, asi que no refleja la
        // sede recien completada. Guardar reemplazando la borraba; guardar con union no.
        val progress = JourneyProgress(
            completedVenueIds = setOf("cuba", "las_vegas"),
            unlockedVenueIds = setOf("cuba", "las_vegas", "tokyo"),
            unlockedAchievements = setOf("first_win")
        )

        val saved = CareerProgressStore.snapshot(
            progress = progress,
            venues = listOf(venue("cuba", unlocked = true))
        )

        assertEquals(setOf("cuba", "las_vegas"), saved.completedVenueIds)
        assertEquals(setOf("cuba", "las_vegas", "tokyo"), saved.unlockedVenueIds)
        assertEquals(setOf("first_win"), saved.unlockedAchievements)
    }

    // --- Aplicación sobre el catálogo ---

    @Test
    fun applyToVenues_restoresUnlockedAndCompletedFlags() {
        val catalog = listOf(venue("cuba", unlocked = true), venue("mexico"), venue("puerto_rico"))
        val progress = JourneyProgress(
            completedVenueIds = setOf("cuba"),
            unlockedVenueIds = setOf("cuba", "mexico")
        )

        val restored = CareerProgressStore.applyToVenues(catalog, progress)

        assertEquals(3, restored.size)
        assertTrue(restored[0].isCompleted)
        assertTrue(restored[1].isUnlocked)
        assertFalse(restored[2].isUnlocked)
        assertEquals(listOf("cuba", "mexico", "puerto_rico"), restored.map { it.id })
    }

    @Test
    fun applyToVenues_neverLocksBackAVenue() {
        val catalog = listOf(venue("cuba", unlocked = true))
        val restored = CareerProgressStore.applyToVenues(catalog, JourneyProgress())
        assertTrue(restored[0].isUnlocked)
    }

    @Test
    fun applyToVenues_keepsCatalogDefaultsForUnknownSavedIds() {
        val catalog = listOf(venue("cuba", unlocked = true), venue("mexico"))
        val restored = CareerProgressStore.applyToVenues(
            catalog,
            JourneyProgress(unlockedVenueIds = setOf("ciudad_fantasma"))
        )
        assertTrue(restored[0].isUnlocked)
        assertFalse(restored[1].isUnlocked)
    }

    // --- Ida y vuelta completa ---

    @Test
    fun snapshotThenApply_reproducesTheSameCareerState() {
        val catalog = listOf(venue("cuba", unlocked = true), venue("mexico"), venue("puerto_rico"))

        val saved = CareerProgressStore.snapshot(
            progress = JourneyProgress(currentVenueIndex = 1, currentChallengeIndex = 2),
            venues = listOf(
                venue("cuba", unlocked = true, completed = true),
                venue("mexico", unlocked = true),
                venue("puerto_rico")
            )
        )
        val reloaded = json.decodeFromString(
            JourneyProgress.serializer(),
            json.encodeToString(JourneyProgress.serializer(), saved)
        )

        val venues = CareerProgressStore.applyToVenues(catalog, reloaded)

        assertEquals(1, reloaded.currentVenueIndex)
        assertEquals(2, reloaded.currentChallengeIndex)
        assertTrue(venues[0].isCompleted)
        assertTrue(venues[1].isUnlocked)
        assertFalse(venues[2].isUnlocked)
    }
}
