package com.example.dominograndprix.data

import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.LuxuryItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la lógica pura de persistencia de la carrera (sin Android): serialización
 * del progreso, captura del estado actual y aplicación del progreso guardado sobre
 * el catálogo de ciudades y trofeos.
 */
class CareerProgressStoreTest {

    private val json = Json { encodeDefaults = true }

    private fun venue(
        id: String,
        unlocked: Boolean = false,
        completed: Boolean = false
    ) = CityVenue(
        id = id,
        name = id,
        region = "region",
        entryBet = 100,
        targetScore = 100,
        defaultMode = GameMode.DRAW,
        wrpReward = 1500,
        trophyTitle = "trophy",
        trophyValue = 600,
        rivalName = "rival",
        rivalIntro = "intro",
        rivalDefeatLine = "defeat",
        isUnlocked = unlocked,
        isCompleted = completed
    )

    // --- Serialización ---

    @Test
    fun progress_survivesAJsonRoundTrip() {
        val original = JourneyProgress(
            cash = 7300,
            wrp = 4200,
            completedVenueIds = setOf("cuba", "mexico"),
            unlockedVenueIds = setOf("cuba", "mexico", "puerto_rico"),
            soldTrophyIds = setOf("cigars")
        )

        val restored = json.decodeFromString(
            JourneyProgress.serializer(),
            json.encodeToString(JourneyProgress.serializer(), original)
        )

        assertEquals(original, restored)
    }

    @Test
    fun emptyProgress_usesStartingCareerValues() {
        val fresh = JourneyProgress()
        assertEquals(2500, fresh.cash)
        assertEquals(1500, fresh.wrp)
        assertTrue(fresh.completedVenueIds.isEmpty())
        assertTrue(fresh.unlockedVenueIds.isEmpty())
        assertTrue(fresh.soldTrophyIds.isEmpty())
    }

    @Test
    fun partialJson_fillsMissingFieldsWithDefaults() {
        // Un guardado antiguo sin las claves de trofeos debe cargar sin fallar.
        val partial = json.decodeFromString(
            JourneyProgress.serializer(),
            """{"cash":999,"wrp":50}"""
        )
        assertEquals(999, partial.cash)
        assertEquals(50, partial.wrp)
        assertTrue(partial.soldTrophyIds.isEmpty())
    }

    // --- Captura del estado actual ---

    @Test
    fun snapshot_onlyCapturesCompletedUnlockedAndSoldIds() {
        val progress = CareerProgressStore.snapshot(
            progress = JourneyProgress(cash = 1200, wrp = 3000),
            venues = listOf(
                venue("cuba", unlocked = true, completed = true),
                venue("mexico", unlocked = true),
                venue("puerto_rico")
            ),
            trophies = listOf(
                LuxuryItem("cigars", "Habanos", 600, "La Habana", isSold = true),
                LuxuryItem("medal", "Medalla", 900, "México", isSold = false)
            )
        )

        assertEquals(1200, progress.cash)
        assertEquals(3000, progress.wrp)
        assertEquals(setOf("cuba"), progress.completedVenueIds)
        assertEquals(setOf("cuba", "mexico"), progress.unlockedVenueIds)
        assertEquals(setOf("cigars"), progress.soldTrophyIds)
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
        // El orden del catálogo se conserva intacto.
        assertEquals(listOf("cuba", "mexico", "puerto_rico"), restored.map { it.id })
    }

    @Test
    fun applyToVenues_neverLocksBackAVenue() {
        // Cuba ya viene desbloqueada por defecto: un guardado sin datos no debe bloquearla.
        val catalog = listOf(venue("cuba", unlocked = true))
        val restored = CareerProgressStore.applyToVenues(catalog, JourneyProgress())
        assertTrue(restored[0].isUnlocked)
    }

    @Test
    fun applyToVenues_keepsCatalogDefaultsForUnknownSavedIds() {
        // Un id que ya no existe en el catálogo se ignora en silencio.
        val catalog = listOf(venue("cuba", unlocked = true), venue("mexico"))
        val restored = CareerProgressStore.applyToVenues(
            catalog,
            JourneyProgress(unlockedVenueIds = setOf("ciudad_fantasma"))
        )
        assertTrue(restored[0].isUnlocked)
        assertFalse(restored[1].isUnlocked)
    }

    @Test
    fun applyToTrophies_marksOnlyPreviouslySoldItems() {
        val catalog = listOf(
            LuxuryItem("cigars", "Habanos", 600, "La Habana"),
            LuxuryItem("medal", "Medalla", 900, "México")
        )

        val restored = CareerProgressStore.applyToTrophies(
            catalog,
            JourneyProgress(soldTrophyIds = setOf("medal"))
        )

        assertFalse(restored[0].isSold)
        assertTrue(restored[1].isSold)
    }

    // --- Ida y vuelta completa ---

    @Test
    fun snapshotThenApply_reproducesTheSameCareerState() {
        val catalog = listOf(venue("cuba", unlocked = true), venue("mexico"), venue("puerto_rico"))
        val trophies = listOf(
            LuxuryItem("cigars", "Habanos", 600, "La Habana", isSold = true),
            LuxuryItem("medal", "Medalla", 900, "México")
        )

        val saved = CareerProgressStore.snapshot(
            progress = JourneyProgress(cash = 4400, wrp = 7500),
            venues = listOf(
                venue("cuba", unlocked = true, completed = true),
                venue("mexico", unlocked = true),
                venue("puerto_rico")
            ),
            trophies = trophies
        )
        // Simula cerrar la app: pasa por disco (JSON) antes de volver a aplicarse.
        val reloaded = json.decodeFromString(
            JourneyProgress.serializer(),
            json.encodeToString(JourneyProgress.serializer(), saved)
        )

        val venues = CareerProgressStore.applyToVenues(catalog, reloaded)
        val items = CareerProgressStore.applyToTrophies(trophies, reloaded)

        assertEquals(4400, reloaded.cash)
        assertEquals(7500, reloaded.wrp)
        assertTrue(venues[0].isCompleted)
        assertTrue(venues[1].isUnlocked)
        assertFalse(venues[2].isUnlocked)
        assertTrue(items[0].isSold)
        assertFalse(items[1].isSold)
    }
}
