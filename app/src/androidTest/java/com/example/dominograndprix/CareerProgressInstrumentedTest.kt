package com.example.dominograndprix

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.dominograndprix.data.CareerProgressStore
import com.example.dominograndprix.data.JourneyProgress
import com.example.dominograndprix.data.WorldTourRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verificacion en dispositivo de la persistencia de la carrera.
 *
 * Este test existe por un motivo concreto: la persistencia usa kotlinx.serialization, que
 * genera un serializador que R8 puede eliminar al ofuscar. Los tests unitarios corren
 * sobre el codigo sin ofuscar, asi que no prueban NADA sobre el APK de release. Este test
 * se ejecuta contra el APK instalado (minificado en release), de modo que si el
 * serializador no sobrevive a R8, aqui falla.
 */
@RunWith(AndroidJUnit4::class)
class CareerProgressInstrumentedTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun clearSavedProgress() {
        context.getSharedPreferences("domino_imperial_career", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun freshInstall_startsFromCatalogDefaults() {
        clearSavedProgress()
        assertFalse("Sin guardado previo no debe haber progreso", CareerProgressStore.hasSavedProgress(context))
        val loaded = CareerProgressStore.load(context)
        assertEquals(JourneyProgress.DEFAULT_CASH, loaded.cash)
        assertEquals(JourneyProgress.DEFAULT_WRP, loaded.wrp)
        assertTrue(loaded.completedVenueIds.isEmpty())
    }

    @Test
    fun savedProgressSurvivesTheRoundTripThroughDisk() {
        clearSavedProgress()

        val progress = JourneyProgress(
            cash = 7654,
            wrp = 4321,
            completedVenueIds = setOf("cuba", "mexico"),
            unlockedVenueIds = setOf("cuba", "mexico", "puerto_rico"),
            soldTrophyIds = setOf("cigars", "medal")
        )
        CareerProgressStore.save(context, progress)

        assertTrue("Debe quedar constancia del guardado", CareerProgressStore.hasSavedProgress(context))
        val loaded = CareerProgressStore.load(context)

        assertEquals("La caja debe sobrevivir al guardado", 7654, loaded.cash)
        assertEquals("Los puntos WRP deben sobrevivir", 4321, loaded.wrp)
        assertEquals(setOf("cuba", "mexico"), loaded.completedVenueIds)
        assertEquals(setOf("cuba", "mexico", "puerto_rico"), loaded.unlockedVenueIds)
        assertEquals(setOf("cigars", "medal"), loaded.soldTrophyIds)

        // Y debe reconstruir el estado del catalogo sin perder el orden
        val venues = CareerProgressStore.applyToVenues(WorldTourRepository.getInitialVenues(), loaded)
        assertEquals("La Habana", venues.first().name)
        assertTrue("Cuba debe seguir completada", venues[0].isCompleted)
        assertTrue("Mexico debe seguir desbloqueada", venues[1].isUnlocked)
        assertFalse("San Juan no debe desbloquearse sola", venues[2].isCompleted)

        val trophies = CareerProgressStore.applyToTrophies(WorldTourRepository.getLuxuryPrizes(), loaded)
        assertTrue("Los habanos deben seguir vendidos", trophies.first { it.id == "cigars" }.isSold)
        assertFalse("El anillo no se ha vendido", trophies.first { it.id == "ring" }.isSold)
    }

    @Test
    fun savingAfterReloadKeepsAccumulatingWithoutLosingData() {
        clearSavedProgress()

        CareerProgressStore.save(context, JourneyProgress(cash = 1000, wrp = 200))
        val first = CareerProgressStore.load(context)
        assertTrue(CareerProgressStore.hasSavedProgress(context))

        // Segunda escritura: simula ganar una partida y guardar de nuevo
        CareerProgressStore.save(
            context,
            first.copy(cash = first.cash + 500, completedVenueIds = setOf("cuba"))
        )
        val second = CareerProgressStore.load(context)

        assertEquals("El acumulado debe persistir", 1500, second.cash)
        assertEquals(200, second.wrp)
        assertEquals(setOf("cuba"), second.completedVenueIds)
    }

    @Test
    fun corruptSavedDataFallsBackToDefaultsInsteadOfCrashing() {
        context.getSharedPreferences("domino_imperial_career", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("journey_progress", "{esto no es json valido")
            .putBoolean("journey_saved", true)
            .commit()

        // Lo importante: no debe lanzar excepcion al arrancar la app
        val loaded = CareerProgressStore.load(context)
        assertEquals(JourneyProgress.DEFAULT_CASH, loaded.cash)
        assertEquals(JourneyProgress.DEFAULT_WRP, loaded.wrp)
    }
}
