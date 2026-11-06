package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El resumen de ronda tiene que ensenar exactamente los puntos que se sumaron al marcador.
 *
 * Defecto real que cubre esta clase: `lastRoundPointsAwarded` guardaba solo lo que repartia el
 * CIERRE y el dialogo lo usaba como total de la ronda. En All Fives el cierre no reparte pips,
 * asi que el resumen decia "+0 pts" aunque el jugador hubiera anotado 30 en la mesa: el jugador
 * veia sus puntos subir durante la mano y desaparecer en el resumen.
 */
class RoundSummaryPointsTest {

    private fun tile(high: Int, low: Int) = DominoTile(high, low)

    private fun engine(mode: GameMode) =
        DominoEngine.allAi(gameMode = mode, targetScore = 1000, seed = 11L)

    /** Fija la mano de cada asiento indicado; el resto se queda sin fichas (no juega). */
    private fun stage(engine: DominoEngine, vararg hands: Pair<Int, List<DominoTile>>) {
        val bySeat = hands.toMap()
        engine.players.indices.forEach { seat ->
            engine.players[seat] = engine.players[seat].copy(hand = bySeat[seat] ?: emptyList())
        }
    }

    private fun play(engine: DominoEngine, seat: Int, tile: DominoTile): Boolean {
        engine.currentPlayerIndex = seat
        val sides = engine.getPlayableSides(seat, tile)
        assertTrue("La ficha $tile deberia ser jugable por el asiento $seat", sides.isNotEmpty())
        val side = if (sides.contains(BoardSide.LEFT)) BoardSide.LEFT else BoardSide.RIGHT
        return engine.playTile(seat, tile, side)
    }

    @Test
    fun careerPointsCountEvenWhenTheRivalWinsTheHand() {
        // El jugador anota 10 en la mesa y luego el rival cierra la mano. Esos 10 existen: el
        // contador de carrera promete "puntos anotados en toda la partida" y antes se perdian
        // porque solo se sumaban en las manos ganadas.
        val engine = engine(GameMode.ALL_FIVES)
        stage(
            engine,
            0 to listOf(tile(5, 5), tile(1, 1)),   // equipo 0
            1 to listOf(tile(5, 0), tile(2, 2)),   // equipo 1
            2 to listOf(tile(3, 3)),               // equipo 0 (compañero)
            3 to listOf(tile(0, 0))                // equipo 1, cierra la mano
        )

        assertTrue(play(engine, 0, tile(5, 5)))
        assertEquals("El bando del jugador anota 10", 10, engine.teamScore(0))

        assertTrue(play(engine, 1, tile(5, 0)))
        assertTrue(play(engine, 3, tile(0, 0)))
        assertTrue("La mano la cierra el rival", engine.isRoundOver)
        assertEquals("Y gana el equipo 1", 1, engine.winningTeam)

        assertEquals(
            "Los 10 puntos del jugador siguen contando para la carrera",
            10,
            engine.matchPointsScored
        )
        assertEquals(10, engine.matchBestRoundPoints)
        assertEquals("Pero la ronda no se le apunta como ganada", 0, engine.matchRoundsWon)
    }

    @Test
    fun allFivesSummaryAddsTheTablePointsOfTheRound() {
        val engine = engine(GameMode.ALL_FIVES)
        // [5|5] de salida paga 10 en la mesa. Los rivales (equipo 1: asientos 1 y 3) se quedan
        // con 12 + 10 = 22 pips, que la regla de All Fives redondea al múltiplo de 5 más cercano.
        stage(
            engine,
            0 to listOf(tile(5, 5)),
            1 to listOf(tile(6, 6)),
            2 to listOf(tile(6, 5)),
            3 to listOf(tile(6, 4))
        )
        val before = engine.teamScore(0)

        assertTrue(play(engine, 0, tile(5, 5)))
        assertTrue("La mano se vacia y la ronda se cierra", engine.isRoundOver)

        assertEquals("10 de mesa + 20 de cierre redondeado", 30, engine.teamScore(0) - before)
        assertEquals("Puntos de mesa de la ronda", 10, engine.lastRoundTablePoints)
        assertEquals(
            "All Fives redondea los pips rivales (22 -> 20)",
            20,
            engine.lastRoundClosingPoints
        )
        assertEquals(
            "El resumen debe ensenar los 30 puntos que se sumaron, no 0",
            30,
            engine.lastRoundPointsAwarded
        )
        assertEquals(
            "Total del resumen = mesa + cierre",
            engine.lastRoundPointsAwarded,
            engine.lastRoundTablePoints + engine.lastRoundClosingPoints
        )
    }

    @Test
    fun fivesAndThreesSummaryAddsTablePointsAndClosingPips() {
        val engine = engine(GameMode.FIVES_AND_THREES)
        // [5|5] de salida: extremos 10 => 2 puntos (10 / 5). Los rivales (asientos 1 y 3)
        // se quedan con 12 pips en mano, que es lo que reparte el cierre.
        stage(
            engine,
            0 to listOf(tile(5, 5)),
            1 to listOf(tile(6, 6)),
            2 to listOf(tile(6, 5)),
            3 to listOf(tile(0, 0))
        )
        val before = engine.teamScore(0)

        assertTrue(play(engine, 0, tile(5, 5)))
        assertTrue(engine.isRoundOver)

        assertEquals("Puntos de mesa: 2 (10 / 5)", 2, engine.lastRoundTablePoints)
        assertEquals("Pips rivales al cerrar: 12 + 0", 12, engine.lastRoundClosingPoints)
        assertEquals(
            "El total de la ronda es mesa + cierre",
            14,
            engine.lastRoundPointsAwarded
        )
        assertEquals(
            "Y es exactamente lo que sube el marcador del bando ganador",
            engine.teamScore(0) - before,
            engine.lastRoundPointsAwarded
        )
    }

    @Test
    fun matcherResumesWithTheSameRoundSummaryNumbers() {
        val engine = engine(GameMode.FIVES_AND_THREES)
        stage(
            engine,
            0 to listOf(tile(5, 5)),
            1 to listOf(tile(6, 6)),
            2 to listOf(tile(6, 5)),
            3 to listOf(tile(0, 0))
        )
        assertTrue(play(engine, 0, tile(5, 5)))
        val table = engine.lastRoundTablePoints
        val closing = engine.lastRoundClosingPoints
        val total = engine.lastRoundPointsAwarded

        val resumed = DominoEngine.resume(engine.toSnapshot(cityName = "Habana", venueId = "cuba"))

        assertEquals(table, resumed.lastRoundTablePoints)
        assertEquals(closing, resumed.lastRoundClosingPoints)
        assertEquals(total, resumed.lastRoundPointsAwarded)
        assertEquals(
            "El resumen al reanudar no puede descuadrar",
            total,
            resumed.lastRoundTablePoints + resumed.lastRoundClosingPoints
        )
    }
}
