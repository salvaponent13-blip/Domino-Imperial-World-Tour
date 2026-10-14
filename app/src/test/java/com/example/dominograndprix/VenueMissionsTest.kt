package com.example.dominograndprix

import com.example.dominograndprix.data.AchievementCatalog
import com.example.dominograndprix.data.JourneyProgress
import com.example.dominograndprix.data.MatchReport
import com.example.dominograndprix.data.VenueMissionCatalog
import com.example.dominograndprix.data.WorldTourRepository
import com.example.dominograndprix.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Misiones de sede.
 *
 * Son el motivo concreto para volver a una ciudad ya completada, asi que lo que se comprueba
 * aqui es que cada mision se cumpla EXACTAMENTE cuando debe: ni antes (se regalaria) ni despues
 * (el jugador cumpliria la condicion y no se enteraria).
 */
class VenueMissionsTest {

    private fun report(
        won: Boolean = true,
        dominos: Int = 0,
        trancasWon: Int = 0,
        roundsWon: Int = 1,
        bestRoundPoints: Int = 0,
        drewFromBoneyard: Boolean = false,
        singleTileWin: Boolean = false
    ) = MatchReport(
        venueId = "cuba",
        won = won,
        cashDelta = 0,
        wrpDelta = 0,
        difficulty = Difficulty.STANDARD,
        roundsPlayed = roundsWon,
        roundsWon = roundsWon,
        blockedRounds = trancasWon,
        dominos = dominos,
        trancasWon = trancasWon,
        pointsScored = bestRoundPoints,
        bestRoundPoints = bestRoundPoints,
        drewFromBoneyard = drewFromBoneyard,
        wonWithSingleTileLeft = singleTileWin
    )

    @Test
    fun everyVenueInTheTourHasMissions() {
        // Si una sede no tiene misiones, su tarjeta saldria vacia y el jugador no tendria motivo
        // para volver a ella.
        for (venue in WorldTourRepository.getInitialVenues()) {
            assertTrue(
                "La sede ${venue.id} debe tener al menos una mision",
                VenueMissionCatalog.missionsFor(venue.id).isNotEmpty()
            )
        }
    }

    @Test
    fun missionIdentifiersAreUniqueAndStable() {
        val all = VenueMissionCatalog.allMissions()
        assertEquals(
            "Los identificadores de mision deben ser unicos",
            all.size,
            all.map { it.id }.toSet().size
        )
        assertTrue("El catalogo no puede estar vacio", all.size >= 14)
    }

    @Test
    fun everyMissionBelongsToTheVenueItIsFiledUnder() {
        // Control de un error facil de cometer copiando y pegando: una mision de Tokio guardada
        // bajo la clave de Cuba nunca se mostraria en su sitio.
        for (venue in WorldTourRepository.getInitialVenues()) {
            for (mission in VenueMissionCatalog.missionsFor(venue.id)) {
                assertEquals(
                    "La mision ${mission.id} debe pertenecer a ${venue.id}",
                    venue.id,
                    mission.venueId
                )
            }
        }
    }

    @Test
    fun losingFulfillsNothingAtAll() {
        // Barrido defensivo: ninguna mision del catalogo puede cumplirse perdiendo. Seria un
        // regalo y vaciaria de sentido el sistema.
        val lost = report(won = false, dominos = 5, trancasWon = 5, roundsWon = 5, bestRoundPoints = 90)
        for (mission in VenueMissionCatalog.allMissions()) {
            assertFalse(
                "La mision ${mission.id} no puede cumplirse perdiendo",
                mission.isFulfilled(lost)
            )
        }
    }

    @Test
    fun theGoodRoundMissionNeedsEnoughPointsInOneRound() {
        val mission = VenueMissionCatalog.missionsFor("cuba").first { it.id == "cuba_good_round" }
        assertTrue(mission.isFulfilled(report(bestRoundPoints = 25)))
        assertFalse(mission.isFulfilled(report(bestRoundPoints = 20)))
    }

    @Test
    fun noMissionIsTriviallyFulfilledWhereItsConditionCannotFail() {
        // Control de un error de diseno facil de cometer: una mision del tipo "gana sin robar
        // del pozo" NO sirve en Puerto Rico (Block), Montecarlo (Block Hungarian) ni Las Vegas
        // (Matador), porque esas modalidades no tienen pozo y la mision se cumpliria sola.
        //
        // Aqui se comprueba que toda mision de esas sedes sigue cumpliendose aunque el jugador
        // hubiera robado, lo que demuestra que no depende de esa condicion imposible.
        for (venueId in listOf("puerto_rico", "monte_carlo", "las_vegas")) {
            val drewAnyway = report(drewFromBoneyard = true, dominos = 2, trancasWon = 2, roundsWon = 3, bestRoundPoints = 45)
            val mustStillBeReachable = VenueMissionCatalog.missionsFor(venueId).all { it.isFulfilled(drewAnyway) }
            assertTrue(
                "En $venueId, sin pozo, alguna mision no es alcanzable: no puede depender de no robar",
                mustStillBeReachable
            )
        }
    }

    @Test
    fun theBigRoundMissionNeedsASingleGoodRoundNotAnAccumulatedTotal() {
        // Es la diferencia entre "una ronda buena" y "arrastrar puntos de muchas rondas".
        val mission = VenueMissionCatalog.missionsFor("mexico").first { it.id == "mexico_big_round" }

        assertTrue("Una ronda de 30 puntos la cumple", mission.isFulfilled(report(bestRoundPoints = 30)))
        assertFalse("Una ronda de 25 no", mission.isFulfilled(report(bestRoundPoints = 25)))
    }

    @Test
    fun theTrancaMissionNeedsAWonBlockedRound() {
        val mission = VenueMissionCatalog.missionsFor("puerto_rico").first { it.id == "pr_tranca" }

        assertTrue(mission.isFulfilled(report(trancasWon = 1)))
        assertFalse("Ganar por domino no es ganar por tranca", mission.isFulfilled(report(trancasWon = 0)))
    }

    @Test
    fun theCloseCallMissionNeedsWinningWithOneTileLeft() {
        val mission = VenueMissionCatalog.missionsFor("new_orleans").first { it.id == "no_close_call" }

        assertTrue(mission.isFulfilled(report(singleTileWin = true)))
        assertFalse(mission.isFulfilled(report(singleTileWin = false)))
    }

    @Test
    fun theVegasMissionNeedsSustainedDominance() {
        val mission = VenueMissionCatalog.missionsFor("las_vegas").first { it.id == "vegas_shutout" }

        assertTrue("Dos dominos y una tranca son tres rondas de dominio", mission.isFulfilled(report(dominos = 2, trancasWon = 1)))
        assertFalse("Dos rondas no bastan", mission.isFulfilled(report(dominos = 1, trancasWon = 1)))
    }

    @Test
    fun onlyNewlyFulfilledMissionsAreReported() {
        // Si ya estaba completada, no puede volver a anunciarse.
        val freshly = VenueMissionCatalog.newlyFulfilled(
            venueId = "cuba",
            report = report(dominos = 1, bestRoundPoints = 25),
            completed = emptySet()
        )
        assertEquals(setOf("cuba_domino", "cuba_good_round"), freshly)

        val again = VenueMissionCatalog.newlyFulfilled(
            venueId = "cuba",
            report = report(dominos = 1, bestRoundPoints = 25),
            completed = setOf("cuba_domino")
        )
        assertEquals("La ya completada no se repite", setOf("cuba_good_round"), again)
    }

    @Test
    fun aVenueWithoutMissionsReportsNothingInsteadOfCrashing() {
        assertTrue(VenueMissionCatalog.missionsFor("sede_inexistente").isEmpty())
        assertTrue(
            VenueMissionCatalog.newlyFulfilled("sede_inexistente", report(), emptySet()).isEmpty()
        )
        assertEquals(0, VenueMissionCatalog.countFor("sede_inexistente"))
    }

    @Test
    fun completingEverythingIsReportedByTheApplyReportPath() {
        // El camino real: aplicar un informe debe dejar las misiones dentro del progreso, en el
        // mismo conjunto que los logros.
        val (updated, _) = AchievementCatalog.applyReport(
            JourneyProgress(),
            report(dominos = 1, bestRoundPoints = 25)
        )
        assertTrue(
            "Las misiones completadas deben quedar guardadas en el progreso",
            updated.unlockedAchievements.contains("cuba_good_round")
        )
        assertTrue(updated.unlockedAchievements.contains("cuba_domino"))
    }

    @Test
    fun theCompletedCountIsAccuratePerVenue() {
        val completed = setOf("cuba_domino", "tokyo_flawless")
        assertEquals(1, VenueMissionCatalog.completedCountFor("cuba", completed))
        assertEquals(1, VenueMissionCatalog.completedCountFor("tokyo", completed))
        assertEquals(0, VenueMissionCatalog.completedCountFor("las_vegas", completed))
        assertEquals(2, VenueMissionCatalog.countFor("cuba"))
    }
}
