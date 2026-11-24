package com.example.dominograndprix

import com.example.dominograndprix.data.CareerFlow
import com.example.dominograndprix.data.CareerStats
import com.example.dominograndprix.data.JourneyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Transiciones de la carrera: ganar un reto y repetir la gira.
 *
 * Cubre el defecto reportado: al ganar el ultimo reto de la ultima sede, el progreso se
 * quedaba en "Reto 5 de 5" para siempre. El menu seguia diciendo "continuar torneo" y volvia a
 * jugar la misma final, sin manera de empezar la gira otra vez sin borrar la coleccion.
 */
class CareerFlowTest {

    private fun progress(
        venue: Int = 0,
        challenge: Int = 0,
        completed: Set<String> = emptySet(),
        unlocked: Set<String> = emptySet(),
        finished: Boolean = false,
        stats: CareerStats = CareerStats()
    ) = JourneyProgress(
        completedVenueIds = completed,
        unlockedVenueIds = unlocked,
        currentVenueIndex = venue,
        currentChallengeIndex = challenge,
        tournamentFinished = finished,
        stats = stats
    )

    @Test
    fun winningAChallengeAdvancesToTheNextOne() {
        val next = CareerFlow.afterWin(progress(venue = 2, challenge = 1), 2, 1, "puerto_rico")

        assertEquals(2, next.currentVenueIndex)
        assertEquals(2, next.currentChallengeIndex)
        assertFalse("La gira sigue en marcha", next.tournamentFinished)
    }

    @Test
    fun winningTheLastChallengeOfAVenueUnlocksTheNextVenue() {
        val next = CareerFlow.afterWin(progress(venue = 0, challenge = 4), 0, 4, "mexico")

        assertEquals(1, next.currentVenueIndex)
        assertEquals(0, next.currentChallengeIndex)
        assertTrue(next.completedVenueIds.contains("mexico"))
        assertTrue("La siguiente sede queda desbloqueada", next.unlockedVenueIds.contains("cuba"))
        assertFalse(next.tournamentFinished)
    }

    @Test
    fun winningTheLastChallengeOfTheLastVenueFinishesTheTour() {
        val before = progress(venue = 6, challenge = 4, completed = setOf("mexico", "cuba"))
        val next = CareerFlow.afterWin(before, 6, 4, "tokyo")

        assertTrue("La gira queda terminada", next.tournamentFinished)
        assertEquals("No se sale de la ultima sede", 6, next.currentVenueIndex)
        assertEquals("Ni del ultimo reto", 4, next.currentChallengeIndex)
        assertTrue(next.completedVenueIds.contains("tokyo"))
        assertEquals(
            "No puede completarse menos de lo que habia",
            before.completedVenueIds.size + 1,
            next.completedVenueIds.size
        )
    }

    @Test
    fun replayingTheTourStartsFromTheFirstVenueWithoutLosingTheCollection() {
        val finished = progress(
            venue = 6,
            challenge = 4,
            completed = setOf("mexico", "cuba", "tokyo"),
            unlocked = setOf("mexico", "cuba"),
            finished = true,
            stats = CareerStats(matchesPlayed = 12, matchesWon = 9)
        )

        val replay = CareerFlow.replayTour(finished)

        assertEquals("Vuelve al primer reto", 0, replay.currentVenueIndex)
        assertEquals(0, replay.currentChallengeIndex)
        assertFalse("Ya no estamos en el estado 'torneo completado'", replay.tournamentFinished)
        assertEquals("Los trofeos se conservan", finished.completedVenueIds, replay.completedVenueIds)
        assertEquals("Los desbloqueos se conservan", finished.unlockedVenueIds, replay.unlockedVenueIds)
        assertEquals("Las estadisticas se conservan", 12, replay.stats.matchesPlayed)
    }

    @Test
    fun theTourIsNotConsideredFinishedBeforeTheLastChallenge() {
        // Reto 5 de una sede intermedia: la gira continua, aunque la sede quede completada.
        val almost = CareerFlow.afterWin(progress(venue = 5, challenge = 4), 5, 4, "las_vegas")
        assertFalse(CareerFlow.isTourFinished(almost))
        assertEquals(6, almost.currentVenueIndex)
        assertEquals(0, almost.currentChallengeIndex)

        // Y solo se marca terminada al ganar el ultimo reto de la ultima sede.
        val done = CareerFlow.afterWin(almost, 6, 4, "tokyo")
        assertTrue(CareerFlow.isTourFinished(done))
    }

    @Test
    fun theTotalChallengeCountMatchesTheCampaign() {
        assertEquals(35, CareerFlow.TOTAL_CHALLENGES)
        assertEquals(7, CareerFlow.VENUES)
        assertEquals(5, CareerFlow.CHALLENGES_PER_VENUE)
    }

    @Test
    fun winningChallengesOneToFourDoesNotCompleteTheVenue() {
        var current = progress(venue = 0, challenge = 0)
        for (challengeIdx in 0..3) {
            current = CareerFlow.afterWin(current, venueIndex = 0, challengeIndex = challengeIdx, venueId = "mexico")
            assertFalse(
                "Ganar el reto ${challengeIdx + 1}/5 no debe marcar la sede como completada",
                current.completedVenueIds.contains("mexico")
            )
        }

        // Al ganar el reto 5/5 (indice 4) si queda completada la sede.
        val afterFifth = CareerFlow.afterWin(current, venueIndex = 0, challengeIndex = 4, venueId = "mexico")
        assertTrue(
            "Ganar el reto 5/5 debe marcar la sede como completada",
            afterFifth.completedVenueIds.contains("mexico")
        )

        // Repetir el reto 5/5 no duplica entradas.
        val repeated = CareerFlow.afterWin(afterFifth, venueIndex = 0, challengeIndex = 4, venueId = "mexico")
        assertEquals(1, repeated.completedVenueIds.size)
    }
}
