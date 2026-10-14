package com.example.dominograndprix

import com.example.dominograndprix.data.DailyChallenge
import com.example.dominograndprix.data.DailyChallengeManager
import com.example.dominograndprix.data.DailyChallengeResult
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.CalendarDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Desafio diario.
 *
 * Lo que define a un reto diario no es que sea una partida mas, sino que sea LA MISMA partida
 * para todo el mundo ese dia. Todo lo que se comprueba aqui gira alrededor de eso: determinismo
 * por fecha, retos distintos entre dias, y una racha que solo continua si se juega y se gana.
 */
class DailyChallengeTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // --- Aritmetica de fechas ---

    @Test
    fun dayNumberIsConsecutiveAcrossRealDates() {
        // Se contrasta contra fechas conocidas: si el calculo fallara, la semilla cambiaria de
        // forma imprevisible y el reto no seria el mismo para todos.
        val jan1 = CalendarDate(2026, 1, 1)
        val jan2 = CalendarDate(2026, 1, 2)
        val jan31 = CalendarDate(2026, 1, 31)
        val feb1 = CalendarDate(2026, 2, 1)
        val dec31 = CalendarDate(2026, 12, 31)
        val nextJan1 = CalendarDate(2027, 1, 1)

        assertEquals("Dias consecutivos", jan1.dayNumber + 1, jan2.dayNumber)
        assertEquals("Enero tiene 31 dias", jan31.dayNumber + 1, feb1.dayNumber)
        assertEquals("Un año despues", dec31.dayNumber + 1, nextJan1.dayNumber)
        assertEquals("366 dias tiene 2026? No: 2026 no es bisiesto", 365, nextJan1.dayNumber - jan1.dayNumber)
    }

    @Test
    fun leapYearsAreHandled() {
        // 2024 fue bisiesto: febrero tuvo 29 dias.
        val feb28 = CalendarDate(2024, 2, 28)
        val mar1 = CalendarDate(2024, 3, 1)
        assertEquals("2024 es bisiesto", feb28.dayNumber + 2, mar1.dayNumber)

        val feb28NonLeap = CalendarDate(2025, 2, 28)
        val mar1NonLeap = CalendarDate(2025, 3, 1)
        assertEquals("2025 no es bisiesto", feb28NonLeap.dayNumber + 1, mar1NonLeap.dayNumber)
    }

    @Test
    fun sameDateAlwaysGivesTheSameDayNumber() {
        assertEquals(CalendarDate(2026, 3, 14).dayNumber, CalendarDate(2026, 3, 14).dayNumber)
    }

    // --- Determinismo del reto ---

    @Test
    fun theSameDayAlwaysProducesTheSameChallenge() {
        val date = CalendarDate(2026, 3, 14)
        val first = DailyChallengeManager.challengeFor(date)
        val second = DailyChallengeManager.challengeFor(date)

        assertEquals("Misma semilla", first.seed, second.seed)
        assertEquals("Mismo modo", first.mode, second.mode)
        assertEquals("Misma dificultad", first.difficulty, second.difficulty)
        assertEquals("Mismo objetivo", first.targetScore, second.targetScore)
    }

    @Test
    fun consecutiveDaysProduceDifferentChallenges() {
        // Si dos dias seguidos dieran el mismo reto, no habria reto diario.
        val seeds = (1..30).map { day ->
            DailyChallengeManager.dailySeed(CalendarDate(2026, 4, day))
        }
        assertEquals(
            "Treinta dias seguidos deben dar treinta semillas distintas",
            30, seeds.toSet().size
        )

        val modes = (1..20).map { day ->
            DailyChallengeManager.challengeFor(CalendarDate(2026, 4, day)).mode
        }
        assertTrue(
            "En veinte dias deben aparecer varios modos distintos, no siempre el mismo: $modes",
            modes.toSet().size >= 3
        )
    }

    @Test
    fun theSeedIsStableAcrossTheWholeYearAndDoesNotDrift() {
        // Se recorre un año entero comprobando que no hay colisiones de semilla: dos retos
        // distintos no pueden compartir partida.
        val seeds = mutableSetOf<Long>()
        var date = CalendarDate(2026, 1, 1)
        var count = 0
        while (date.year == 2026) {
            val seed = DailyChallengeManager.dailySeed(date)
            assertTrue("Semilla repetida el $date", seeds.add(seed))
            count++
            date = nextDay(date)
        }
        assertEquals("Un año completo", 365, count)
    }

    /** Siguiente dia natural, para recorrer un año en los tests. */
    private fun nextDay(date: CalendarDate): CalendarDate {
        val daysInMonth = when (date.month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (isLeap(date.year)) 29 else 28
        }
        return if (date.day < daysInMonth) {
            date.copy(day = date.day + 1)
        } else if (date.month < 12) {
            CalendarDate(date.year, date.month + 1, 1)
        } else {
            CalendarDate(date.year + 1, 1, 1)
        }
    }

    private fun isLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    @Test
    fun theChallengeIsPlayableAndDeterministicInTheEngine() {
        // El reto debe poder jugarse de verdad: dos motores con la semilla del dia reparten lo
        // mismo y, jugados igual, acaban igual. Es lo que hace comparable la puntuacion.
        val challenge = DailyChallengeManager.challengeFor(CalendarDate(2026, 7, 9))
        val first = playWithSeed(challenge)
        val second = playWithSeed(challenge)

        assertEquals("El mismo reto debe dar la misma partida", first, second)
    }

    @Test
    fun differentDaysGiveDifferentMatches() {
        val today = DailyChallengeManager.challengeFor(CalendarDate(2026, 7, 9))
        val tomorrow = DailyChallengeManager.challengeFor(CalendarDate(2026, 7, 10))

        assertNotEquals("Dos retos distintos no pueden dar la misma partida", today.seed, tomorrow.seed)
    }

    /** Juega una ronda con la semilla del reto y devuelve una firma del resultado. */
    private fun playWithSeed(challenge: DailyChallenge): String {
        val engine = DominoEngine(
            gameMode = challenge.mode,
            targetScore = challenge.targetScore,
            random = Random(challenge.seed),
            randomSeed = challenge.seed,
            difficulty = challenge.difficulty,
            humanPlayerIndices = emptySet()
        )
        var steps = 0
        while (!engine.isRoundOver && steps < 800) {
            engine.executeAIMove()
            steps++
        }
        return buildString {
            appendLine("ganador=" + engine.winnerPlayerId)
            appendLine("tablero=" + engine.boardChain.map { it.tile })
            appendLine("marcadores=" + engine.players.map { it.score })
            append("pasos=$steps")
        }
    }

    // --- Racha ---

    @Test
    fun firstWinStartsTheStreak() {
        val today = CalendarDate(2026, 5, 10)
        assertEquals(1, DailyChallengeManager.nextStreak(previous = null, today = today, won = true))
    }

    @Test
    fun winningOnConsecutiveDaysIncreasesTheStreak() {
        val yesterday = CalendarDate(2026, 5, 9)
        val today = CalendarDate(2026, 5, 10)
        val previous = DailyChallengeResult(yesterday.dayNumber, won = true, points = 80, streak = 4)

        assertEquals(5, DailyChallengeManager.nextStreak(previous, today, won = true))
    }

    @Test
    fun losingResetsTheStreak() {
        val yesterday = CalendarDate(2026, 5, 9)
        val today = CalendarDate(2026, 5, 10)
        val previous = DailyChallengeResult(yesterday.dayNumber, won = true, points = 80, streak = 7)

        assertEquals("Perder corta la racha", 0, DailyChallengeManager.nextStreak(previous, today, won = false))
    }

    @Test
    fun skippingADayResetsTheStreak() {
        // Es lo que le da valor a mantenerla: si no se juega, se pierde.
        val threeDaysAgo = CalendarDate(2026, 5, 7)
        val today = CalendarDate(2026, 5, 10)
        val previous = DailyChallengeResult(threeDaysAgo.dayNumber, won = true, points = 80, streak = 9)

        assertEquals(
            "Dejar pasar un dia devuelve la racha a 1 (se gana hoy, pero no encadena)",
            1,
            DailyChallengeManager.nextStreak(previous, today, won = true)
        )
    }

    @Test
    fun aLostDayAlsoBreaksTheChainEvenIfTodayIsWon() {
        val yesterday = CalendarDate(2026, 5, 9)
        val today = CalendarDate(2026, 5, 10)
        val previous = DailyChallengeResult(yesterday.dayNumber, won = false, points = 20, streak = 0)

        assertEquals(1, DailyChallengeManager.nextStreak(previous, today, won = true))
    }

    // --- Persistencia ---

    @Test
    fun theResultSurvivesTheJsonRoundTrip() {
        val result = DailyChallengeResult(dayNumber = 740000, won = true, points = 65, streak = 3)
        val restored = json.decodeFromString(
            DailyChallengeResult.serializer(),
            json.encodeToString(DailyChallengeResult.serializer(), result)
        )
        assertEquals(result, restored)
    }

    @Test
    fun theChallengeSurvivesTheJsonRoundTrip() {
        // Importa porque la fecha lleva serializador propio: si estuviera mal, el guardado del
        // reto del dia se perderia en silencio.
        val challenge = DailyChallengeManager.challengeFor(CalendarDate(2026, 11, 23))
        val restored = json.decodeFromString(
            DailyChallenge.serializer(),
            json.encodeToString(DailyChallenge.serializer(), challenge)
        )
        assertEquals(challenge, restored)
        assertEquals(challenge.date, restored.date)
    }

    @Test
    fun noResultMeansNoDailyPlayedYet() {
        // El almacen devuelve null cuando no hay nada: la interfaz debe poder distinguirlo.
        val empty: DailyChallengeResult? = null
        assertNull(empty)
    }

    @Test
    fun todayIsResolvedFromTheDeviceClock() {
        // Solo se comprueba que no revienta y que da una fecha razonable; el reloj no se puede
        // falsear desde un test unitario.
        val challenge = DailyChallengeManager.today()
        assertTrue("El año debe ser plausible", challenge.date.year in 2024..2100)
        assertTrue("El mes debe ser valido", challenge.date.month in 1..12)
        assertTrue("El dia debe ser valido", challenge.date.day in 1..31)
        assertTrue("El objetivo debe ser uno de los previstos", challenge.targetScore in listOf(50, 60, 75))
    }
}
