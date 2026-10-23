package com.example.dominograndprix

import com.example.dominograndprix.data.AchievementCatalog
import com.example.dominograndprix.data.CareerStats
import com.example.dominograndprix.data.JourneyProgress
import com.example.dominograndprix.data.MatchReport
import com.example.dominograndprix.model.Difficulty
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Logros y estadisticas de carrera.
 *
 * Lo mas importante que se comprueba aqui es la COMPATIBILIDAD: los guardados de jugadores que
 * ya tienen progreso no pueden romperse al anadir campos nuevos. Un fallo ahi no da un error
 * visible, da un jugador que abre la app y ha perdido su partida.
 */
class AchievementsTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun progress(
        stats: CareerStats = CareerStats(),
        achievements: Set<String> = emptySet(),
        completed: Set<String> = emptySet()
    ) = JourneyProgress(
        stats = stats,
        unlockedAchievements = achievements,
        completedVenueIds = completed
    )

    private fun report(
        won: Boolean = true,
        venueId: String? = "cuba",
        difficulty: Difficulty = Difficulty.STANDARD,
        roundsWon: Int = 1,
        dominos: Int = 0,
        trancasWon: Int = 0,
        blockedRounds: Int = 0,
        pointsScored: Int = 0,
        singleTileWin: Boolean = false,
        isDaily: Boolean = false
    ) = MatchReport(
        venueId = venueId,
        won = won,
        difficulty = difficulty,
        roundsPlayed = roundsWon + (if (won) 0 else 1),
        roundsWon = roundsWon,
        blockedRounds = blockedRounds,
        dominos = dominos,
        trancasWon = trancasWon,
        pointsScored = pointsScored,
        wonWithSingleTileLeft = singleTileWin,
        isDailyChallenge = isDaily
    )

    // --- Compatibilidad del guardado ---

    @Test
    fun anOldSaveWithoutTheNewFieldsStillLoads() {
        // JSON de una version anterior, sin logros ni estadisticas: es exactamente el guardado
        // que tiene ahora mismo cualquier jugador de la version publicada.
        val oldSave = """
            {"cash":7777,"wrp":3300,"completedVenueIds":["cuba","mexico"],
             "unlockedVenueIds":["cuba","mexico","puerto_rico"],"soldTrophyIds":["cigars"]}
        """.trimIndent()

        val loaded = json.decodeFromString(JourneyProgress.serializer(), oldSave)

        assertEquals(setOf("cuba", "mexico"), loaded.completedVenueIds)
        assertTrue("Sin logros previos", loaded.unlockedAchievements.isEmpty())
        assertEquals("Las estadisticas arrancan a cero", 0, loaded.stats.matchesPlayed)
    }

    @Test
    fun theExtendedProgressSurvivesTheJsonRoundTrip() {
        val original = progress(
            stats = CareerStats(
                matchesPlayed = 12,
                matchesWon = 7,
                dominos = 4,
                winsByDifficulty = mapOf(Difficulty.TOUGH.name to 3)
            ),
            achievements = setOf(AchievementCatalog.FIRST_BLOOD)
        )
        val restored = json.decodeFromString(
            JourneyProgress.serializer(),
            json.encodeToString(JourneyProgress.serializer(), original)
        )
        assertEquals(original, restored)
        assertEquals(3, restored.stats.toughWins)
        assertEquals(58, restored.stats.winRatePercent)
    }

    // --- Estadisticas ---

    @Test
    fun aWonMatchAccumulatesStats() {
        val (updated, _) = AchievementCatalog.applyReport(
            progress(),
            report(won = true, dominos = 1, roundsWon = 2, pointsScored = 15)
        )

        assertEquals(1, updated.stats.matchesPlayed)
        assertEquals(1, updated.stats.matchesWon)
        assertEquals(2, updated.stats.roundsWon)
        assertEquals(1, updated.stats.dominos)
        assertEquals(15, updated.stats.pointsScored)
        assertEquals(1, updated.stats.winsByDifficulty[Difficulty.STANDARD.name])
    }

    @Test
    fun aLostMatchCountsButDoesNotAddWins() {
        val (updated, _) = AchievementCatalog.applyReport(
            progress(),
            report(won = false, dominos = 0)
        )

        assertEquals(1, updated.stats.matchesPlayed)
        assertEquals(0, updated.stats.matchesWon)
        assertTrue("Sin victorias no hay entradas por dificultad", updated.stats.winsByDifficulty.isEmpty())
    }

    @Test
    fun theWinRateIsZeroWhenNothingHasBeenPlayedYet() {
        // Defensivo: dividir por cero al mostrar el perfil seria un fallo tonto y visible.
        assertEquals(0, CareerStats().winRatePercent)
    }

    // --- Logros ---

    @Test
    fun theFirstWinUnlocksItsAchievement() {
        val (updated, newOnes) = AchievementCatalog.applyReport(progress(), report(won = true))

        assertTrue("Debe desbloquearse el primer logro", newOnes.contains(AchievementCatalog.FIRST_BLOOD))
        assertTrue(updated.unlockedAchievements.contains(AchievementCatalog.FIRST_BLOOD))
    }

    @Test
    fun anAlreadyUnlockedAchievementIsNotReportedAgain() {
        val (_, newOnes) = AchievementCatalog.applyReport(
            progress(achievements = setOf(AchievementCatalog.FIRST_BLOOD)),
            report(won = true)
        )
        assertFalse(
            "Un logro ya conseguido no puede volver a anunciarse",
            newOnes.contains(AchievementCatalog.FIRST_BLOOD)
        )
    }

    @Test
    fun achievementsAccumulateAcrossMatches() {
        var current = progress()
        repeat(10) {
            val (updated, _) = AchievementCatalog.applyReport(current, report(won = true, dominos = 1))
            current = updated
        }
        assertTrue(
            "Diez dominos deben desbloquear el logro correspondiente",
            current.unlockedAchievements.contains(AchievementCatalog.DOMINO_MASTER)
        )
        assertEquals(10, current.stats.dominos)
    }

    @Test
    fun venueAchievementsRequireWinningInThatVenue() {
        // Es lo que da motivo para rejugar una sede ya completada.
        val (vegasLoser, _) = AchievementCatalog.applyReport(
            progress(),
            report(won = false, venueId = "las_vegas")
        )
        assertFalse(
            "Perder en Las Vegas no puede coronar a nadie",
            vegasLoser.unlockedAchievements.contains(AchievementCatalog.VEGAS_CHAMPION)
        )

        val (vegasWinner, _) = AchievementCatalog.applyReport(
            progress(),
            report(won = true, venueId = "las_vegas")
        )
        assertTrue(
            "Ganar en Las Vegas si",
            vegasWinner.unlockedAchievements.contains(AchievementCatalog.VEGAS_CHAMPION)
        )
    }

    @Test
    fun theHavanaAchievementNeedsADominoThere() {
        val (withoutDomino, _) = AchievementCatalog.applyReport(
            progress(),
            report(venueId = "cuba", dominos = 0)
        )
        assertFalse(withoutDomino.unlockedAchievements.contains(AchievementCatalog.HAVANA_DOMINO))

        val (withDomino, _) = AchievementCatalog.applyReport(
            progress(),
            report(venueId = "cuba", dominos = 1)
        )
        assertTrue(withDomino.unlockedAchievements.contains(AchievementCatalog.HAVANA_DOMINO))
    }

    @Test
    fun theDailyChallengeHasItsOwnAchievements() {
        val (dailyWin, _) = AchievementCatalog.applyReport(
            progress(),
            report(won = true, isDaily = true)
        )
        assertTrue(dailyWin.unlockedAchievements.contains(AchievementCatalog.DAILY_FIRST))

        val (streak, _) = AchievementCatalog.applyReport(
            progress(),
            report(won = true, isDaily = true),
            dailyStreak = 7
        )
        assertTrue(
            "Una racha de siete dias debe tener su logro",
            streak.unlockedAchievements.contains(AchievementCatalog.DAILY_STREAK_7)
        )

        val (shortStreak, _) = AchievementCatalog.applyReport(
            progress(),
            report(won = true, isDaily = true),
            dailyStreak = 3
        )
        assertFalse(
            "Con tres dias de racha no puede darse el de siete",
            shortStreak.unlockedAchievements.contains(AchievementCatalog.DAILY_STREAK_7)
        )
    }

    @Test
    fun completingTheWholeTourUnlocksTheFinalAchievement() {
        val (updated, _) = AchievementCatalog.applyReport(
            progress(completed = setOf("cuba", "mexico", "puerto_rico", "new_orleans", "tokyo", "monte_carlo")),
            report(won = true, venueId = "las_vegas")
        )
        assertTrue(
            "Con las siete sedes cerradas debe caer el logro de gira completa",
            updated.unlockedAchievements.contains(AchievementCatalog.PERFECT_TOUR)
        )
    }

    @Test
    fun everyCatalogIdentifierIsReachableInPrinciple() {
        // Control de que el catalogo no tiene entradas muertas: una condicion imposible seria
        // codigo que nadie vera jamas.
        //
        // Se comprueba en dos tandas porque los logros de sede dependen de DONDE se gana: una
        // sola partida solo puede ocurrir en una ciudad. Que haga falta mas de una partida para
        // completar el catalogo es el objetivo del sistema, no un defecto.
        val veteranStats = CareerStats(
            matchesPlayed = 100,
            matchesWon = 90,
            roundsWon = 50,
            trancasWon = 20,
            dominos = 20,
            pointsScored = 2000,
            lastTileWins = 10,
            winsByDifficulty = mapOf(Difficulty.TOUGH.name to 10)
        )
        val richCareer = progress(
            completed = setOf("cuba", "mexico", "puerto_rico", "new_orleans", "tokyo", "monte_carlo", "las_vegas"),
            stats = veteranStats
        )

        // Tanda 1: victoria en La Habana con domino, en el desafio diario.
        val havana = AchievementCatalog.evaluate(
            richCareer,
            report(won = true, venueId = "cuba", dominos = 1, trancasWon = 1, singleTileWin = true, isDaily = true),
            dailyStreak = 10
        ) + richCareer.unlockedAchievements

        // Tanda 2: victoria en Tokio con tranca, y en Las Vegas para el titulo.
        val tokyo = AchievementCatalog.evaluate(
            richCareer,
            report(won = true, venueId = "tokyo", trancasWon = 1),
            dailyStreak = 10
        ) + richCareer.unlockedAchievements

        val vegas = AchievementCatalog.evaluate(
            richCareer,
            report(won = true, venueId = "las_vegas", dominos = 1),
            dailyStreak = 10
        ) + richCareer.unlockedAchievements

        val reached = havana + tokyo + vegas
        val missing = AchievementCatalog.ALL.toSet() - reached
        assertTrue(
            "Hay logros inalcanzables incluso con una carrera perfecta: $missing",
            missing.isEmpty()
        )
    }

    @Test
    fun theCatalogHasNoDuplicateIdentifiers() {
        assertEquals(
            "Los identificadores del catalogo deben ser unicos",
            AchievementCatalog.ALL.size,
            AchievementCatalog.ALL.toSet().size
        )
    }
}
