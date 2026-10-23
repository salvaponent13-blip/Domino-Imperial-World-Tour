package com.example.dominograndprix

import com.example.dominograndprix.data.CareerReporter
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Acumuladores de partida del motor.
 *
 * Existen por un defecto real: los indicadores de ronda del motor (`lastRoundWasBlocked`,
 * `winnerPlayerId`) solo describen la ULTIMA ronda. Construir el informe de carrera con ellos
 * hacia que una partida de cinco rondas se contara como una sola, y las estadisticas del
 * jugador habrian sido silenciosamente falsas: no un error visible, sino un numero equivocado
 * que nadie habria notado.
 */
class MatchAccumulatorsTest {

    private fun engine(
        mode: GameMode = GameMode.BLOCK,
        targetScore: Int = 300,
        seed: Long = 31L,
        humanSeats: Set<Int> = setOf(0)
    ) = DominoEngine(
        gameMode = mode,
        targetScore = targetScore,
        random = Random(seed),
        randomSeed = seed,
        difficulty = Difficulty.STANDARD,
        humanPlayerIndices = humanSeats
    )

    @Test
    fun countersStartAtZero() {
        val engine = engine()
        assertEquals(0, engine.matchRoundsWon)
        assertEquals(0, engine.matchBlockedRounds)
        assertEquals(0, engine.matchDominos)
        assertEquals(0, engine.matchTrancasWon)
        assertEquals(0, engine.matchWinsWithSingleTile)
        assertEquals(0, engine.matchPointsScored)
    }

    /**
     * Juega [rounds] rondas con la mesa llena de IA y devuelve cuantas cerro de verdad.
     *
     * Lleva guarda de pasos a proposito: un bucle sobre el motor que no converge cuelga la
     * suite entera en vez de fallar, y eso ya ha pasado en este proyecto.
     */
    private fun playRounds(engine: DominoEngine, rounds: Int): Int {
        var closed = 0
        var guard = 0
        while (closed < rounds && guard < 20_000) {
            if (engine.isMatchOver) break
            if (engine.isRoundOver) {
                engine.startNewRound()
                continue
            }
            engine.executeAIMove()
            guard++
            if (engine.isRoundOver) closed++
        }
        return closed
    }

    @Test
    fun countersAccumulateAcrossRounds() {
        // El sentido de todo esto: varias rondas deben sumar, no sobreescribirse.
        val engine = engine(targetScore = 100_000, humanSeats = emptySet())
        val closed = playRounds(engine, rounds = 5)

        assertEquals("Deben cerrarse las cinco rondas pedidas", 5, closed)
        assertEquals(
            "Cada ronda cerrada debe contarse exactamente una vez",
            5,
            engine.matchRoundsPlayed
        )
        assertTrue(
            "Las victorias del bando y las trancas no pueden superar las rondas jugadas",
            engine.matchRoundsWon <= engine.matchRoundsPlayed &&
                engine.matchBlockedRounds <= engine.matchRoundsPlayed
        )
    }

    @Test
    fun roundsWonPlusBlockedRoundsDoesNotAddUpToRoundsPlayed() {
        // Documenta por que `matchRoundsPlayed` existe como contador propio: una ronda que gana
        // el bando rival no es "ganada" ni "trancada por nosotros", asi que aquella suma se
        // queda corta. Construir el informe con esa suma contaba de menos.
        val engine = engine(targetScore = 100_000, humanSeats = emptySet())
        val closed = playRounds(engine, rounds = 4)

        assertEquals(4, closed)
        assertEquals(4, engine.matchRoundsPlayed)
        assertTrue(
            "La suma de ganadas y trancadas no puede pasar del total jugado",
            engine.matchRoundsWon + engine.matchBlockedRounds <= engine.matchRoundsPlayed
        )
    }

    @Test
    fun dominosAndTrancasAreCountedSeparatelyAndInTotal() {
        val engine = engine(targetScore = 100_000, humanSeats = emptySet())
        val closed = playRounds(engine, rounds = 6)
        assertEquals(6, closed)

        // Cada ronda es o domino (mano vaciada) o tranca (mesa bloqueada), nunca las dos.
        assertEquals(
            "Las rondas ganadas deben repartirse entre dominos y trancas",
            engine.matchRoundsWon,
            engine.matchDominos + engine.matchTrancasWon
        )
    }

    @Test
    fun aNewMatchResetsTheCountersButANewRoundDoesNot() {
        val engine = engine(targetScore = 100_000, humanSeats = emptySet())
        val closed = playRounds(engine, rounds = 1)
        assertEquals("La primera ronda debe cerrarse", 1, closed)
        assertEquals("Y contarse", 1, engine.matchRoundsPlayed)

        engine.startNewRound()
        assertEquals(
            "Empezar una ronda NO puede borrar las estadisticas de la partida",
            1,
            engine.matchRoundsPlayed
        )

        engine.setupGame()
        assertEquals("Empezar una partida nueva si las reinicia", 0, engine.matchRoundsPlayed)
        assertEquals(0, engine.matchRoundsWon)
        assertEquals(0, engine.matchBlockedRounds)
    }

    @Test
    fun theReportCountsEveryRoundOfTheMatchNotJustTheLastOne() {
        // La comprobacion que habria cazado el defecto original.
        val engine = engine(targetScore = 100_000, humanSeats = setOf(0))
        val closed = playRoundsWithHuman(engine, rounds = 4)
        assertEquals("Deben cerrarse las cuatro rondas", 4, closed)

        val report = CareerReporter.reportFor(
            engine = engine,
            venueId = "cuba",
            won = true,
            difficulty = Difficulty.STANDARD,
            isDailyChallenge = false
        )

        assertEquals(
            "El informe debe reflejar las cuatro rondas, no una",
            4,
            report.roundsPlayed
        )
        assertEquals(
            "Las rondas ganadas del informe son las del acumulador",
            engine.matchRoundsWon,
            report.roundsWon
        )
        assertEquals("Los dominos del informe son los del acumulador", engine.matchDominos, report.dominos)
        assertEquals("Las trancas del informe son las del acumulador", engine.matchTrancasWon, report.trancasWon)
    }

    /** Juega una ronda resolviendo a mano los turnos del asiento humano. */
    private fun playRoundWithHuman(engine: DominoEngine) {
        var guard = 0
        while (!engine.isRoundOver && !engine.isMatchOver && guard < 5_000) {
            if (engine.isCurrentPlayerHuman) {
                val seat = engine.currentPlayerIndex
                val view = engine.viewFor(seat)
                val move = view.playableMoves.firstOrNull()
                // OJO: hay que respetar el LADO que devuelve la vista. Colocar siempre por la
                // izquierda ignorando el lado hace que la jugada se rechace y el turno no
                // avance, que es el error que tuvo este test en su primera version.
                val played = when {
                    move != null && move.side == BoardSide.LEFT ->
                        engine.playTileLeft(seat, move.tile)
                    move != null ->
                        engine.playTileRight(seat, move.tile)
                    engine.gameMode.allowsDraw && engine.boneyard.isNotEmpty() ->
                        engine.drawTile(seat, 0) != null
                    else -> {
                        engine.passTurn(seat)
                        true
                    }
                }
                if (!played && move == null) {
                    // Si robar tampoco es posible y pasar no cierra, se corta para no girar en
                    // vacio: un bucle infinito cuelga la suite en vez de fallar.
                    break
                }
            } else {
                engine.executeAIMove()
            }
            guard++
        }
    }

    /** Igual que [playRounds] pero resolviendo los turnos humanos. */
    private fun playRoundsWithHuman(engine: DominoEngine, rounds: Int): Int {
        var closed = 0
        var guard = 0
        while (closed < rounds && guard < 6_000) {
            if (engine.isMatchOver) break
            if (engine.isRoundOver) {
                engine.startNewRound()
                continue
            }
            playRoundWithHuman(engine)
            closed++
        }
        return closed
    }

    @Test
    fun aLostMatchStillRecordsWhatHappenedInIt() {
        // Aunque se pierda, lo jugado cuenta para las estadisticas: si no, perder seria
        // "no haber jugado" y los logros de constancia no tendrian sentido.
        val engine = engine(targetScore = 100_000, humanSeats = setOf(0))
        playRoundWithHuman(engine)
        assertTrue("La ronda debe cerrarse", engine.isRoundOver)

        val report = CareerReporter.reportFor(
            engine = engine,
            venueId = "cuba",
            won = false,
            difficulty = Difficulty.TOUGH,
            isDailyChallenge = false
        )

        assertEquals(1, report.roundsPlayed)
    }
}
