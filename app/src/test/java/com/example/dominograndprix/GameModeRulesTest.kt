package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de reglas de las tres modalidades con lógica propia:
 *  - Fives & Threes: 1 punto por cada 5 y 1 punto por cada 3 (suma 15 => 8 puntos).
 *  - Block Hungarian: encadenar jugadas consecutivas sin ceder el turno.
 *  - Draw Special: cada asiento juega en su rama asignada y puede invadir la de un
 *    rival que acaba de pasar.
 */
class GameModeRulesTest {

    private companion object {
        /** Ficha inerte que nunca conecta en los escenarios controlados de este test. */
        val DEAD_TILE = listOf(DominoTile(6, 6))
    }

    // ------------------------------------------------------------------
    // Utilidades: los asientos 1..3 son IA, así que para probar jugadas
    // concretas se fija el turno a mano con currentPlayerIndex.
    // ------------------------------------------------------------------

    private fun engine(mode: GameMode): DominoEngine =
        DominoEngine.allAi(gameMode = mode, targetScore = 500)

    private fun tileOf(engine: DominoEngine, playerIndex: Int, high: Int, low: Int): DominoTile =
        engine.players[playerIndex].hand.first { it.high == high && it.low == low }

    /**
     * Fija la mano de cada asiento indicado y rellena el resto con una única ficha [6|6].
     * Además se añade una ficha [6|6] de relleno a CADA mano, incluida la del asiento
     * activo, para que ninguna se vacíe al jugar: si se vaciara, el motor cerraría la
     * ronda y [DominoEngine.finishRound] repartiría los puntos de los rivales al ganador,
     * falseando por completo las comprobaciones de puntuación.
     * [6|6] no es jugable en los escenarios controlados de este test.
     */
    private fun stageHands(engine: DominoEngine, vararg hands: Pair<Int, List<DominoTile>>) {
        val bySeat = hands.toMap()
        engine.players.indices.forEach { seat ->
            engine.players[seat] = engine.players[seat].copy(
                hand = (bySeat[seat] ?: emptyList()) + DEAD_TILE
            )
        }
    }

    private fun tiles(vararg pips: Pair<Int, Int>): List<DominoTile> =
        pips.map { (high, low) -> DominoTile(high, low) }

    /**
     * Mano exacta, sin ficha de relleno. Se usa cuando el test necesita precisamente que
     * la mano se vacíe (por ejemplo para comprobar que el encadenamiento húngaro termina
     * cuando ya no quedan fichas jugables).
     */
    private fun stageExactHand(engine: DominoEngine, seat: Int, hand: List<DominoTile>) {
        engine.players.indices.forEach { index ->
            engine.players[index] = engine.players[index].copy(
                hand = if (index == seat) hand else DEAD_TILE
            )
        }
    }

    /** Mano de un solo asiento, conservando el relleno para el resto. */
    private fun stageHand(engine: DominoEngine, seat: Int, hand: List<DominoTile>) {
        stageHands(engine, seat to hand)
    }

    /** Juega la ficha del asiento indicado por el lado legal que corresponda. */
    private fun play(engine: DominoEngine, playerIndex: Int, tile: DominoTile): Boolean {
        val sides = engine.getPlayableSides(playerIndex, tile)
        assertTrue("La ficha $tile debería ser jugable por el asiento $playerIndex", sides.isNotEmpty())
        return if (sides.contains(BoardSide.LEFT)) {
            engine.playTileLeft(playerIndex, tile)
        } else {
            engine.playTileRight(playerIndex, tile)
        }
    }

    /** Juega la ficha por un lado concreto, para probar jugadas ilegales. */
    private fun playLeft(engine: DominoEngine, playerIndex: Int, tile: DominoTile): Boolean {
        engine.currentPlayerIndex = playerIndex
        return engine.playTileLeft(playerIndex, tile)
    }

    private fun scoreOf(engine: DominoEngine, playerIndex: Int): Int =
        engine.players[playerIndex].score

    // ==================================================================
    // FIVES & THREES
    // ==================================================================

    @Test
    fun fivesAndThrees_scoresOnePointPerFiveAndPerThree() {
        val engine = engine(GameMode.FIVES_AND_THREES)
        stageHand(engine, 0, tiles(5 to 5))
        engine.currentPlayerIndex = 0

        play(engine, 0, tileOf(engine, 0, 5, 5))

        // Extremos [5][5] = 10 => 2 puntos (10 / 5), y no es múltiplo de 3.
        assertEquals("10 en los extremos debe dar 2 puntos", 2, scoreOf(engine, 0))
    }

    @Test
    fun fivesAndThrees_tracksScoreAcrossAPlayingSequence() {
        val engine = engine(GameMode.FIVES_AND_THREES)
        stageHands(
            engine,
            0 to tiles(3 to 3),
            1 to tiles(3 to 6),
            2 to tiles(6 to 0),
            3 to tiles(3 to 0)
        )

        // Apertura [3|3]: extremos [3][3] = 6 => 2 puntos (6 / 3).
        engine.currentPlayerIndex = 0
        play(engine, 0, tileOf(engine, 0, 3, 3))
        assertEquals(2, scoreOf(engine, 0))

        // Derecha [3|6]: extremos [3][6] = 9 => 3 puntos (9 / 3).
        play(engine, 1, tileOf(engine, 1, 3, 6))
        assertEquals(3, scoreOf(engine, 1))

        // Derecha [6|0]: extremos [3][0] = 3 => 1 punto (3 / 3).
        play(engine, 2, tileOf(engine, 2, 6, 0))
        assertEquals(1, scoreOf(engine, 2))

        // Izquierda con [3|0]: los extremos quedan en [3][3] = 6 => 2 puntos.
        // (El 0 no es múltiplo, pero el [3|0] conecta dejando un 3 en el extremo,
        // así que la suma resultante es 6.)
        assertTrue(playLeft(engine, 3, tileOf(engine, 3, 3, 0)))
        assertEquals(2, scoreOf(engine, 3))

        assertEquals(3, engine.leftOpenPip)
        assertEquals(3, engine.rightOpenPip)
    }

    @Test
    fun fivesAndThrees_reachableSumsPayOnePointPerFiveAndPerThree() {
        // Tabla de pagos con la regla clásica: 1 punto por cada 5 y 1 por cada 3.
        //
        // Ojo: con un set doble-seis los dos extremos abiertos suman como máximo 12, así
        // que las sumas alcanzables en partida son 0..12. La suma 15 (el ejemplo clásico
        // de "5 y 3") NO puede darse nunca en una partida real de doble-seis; aquí se
        // comprueba igualmente porque era donde la regla anterior pagaba el doble.
        fun points(sum: Int): Int {
            val fives = if (sum > 0 && sum % 5 == 0) sum / 5 else 0
            val threes = if (sum > 0 && sum % 3 == 0) sum / 3 else 0
            return fives + threes
        }

        // Sumas alcanzables con doble-seis.
        assertEquals(0, points(0))
        assertEquals(0, points(1))
        assertEquals(0, points(2))
        assertEquals(1, points(3))   // 3 / 3
        assertEquals(0, points(4))
        assertEquals(1, points(5))   // 5 / 5
        assertEquals(2, points(6))   // 6 / 3
        assertEquals(0, points(7))
        assertEquals(0, points(8))
        assertEquals(3, points(9))   // 9 / 3
        assertEquals(2, points(10))  // 10 / 5
        assertEquals(0, points(11))
        assertEquals(4, points(12))  // 12 / 3

        // Ejemplo de referencia: 15 pagaría 3 (por el 5) + 5 (por el 3) = 8, nunca 16.
        assertEquals(8, points(15))
        assertEquals(24, points(45))  // 45 / 5 + 45 / 3 = 9 + 15
    }


    @Test
    fun allFivesScoresTheSumItselfNotTheFactor() {
        val engine = engine(GameMode.ALL_FIVES)
        stageHand(engine, 0, tiles(5 to 5))
        engine.currentPlayerIndex = 0
        play(engine, 0, tileOf(engine, 0, 5, 5))

        // All Fives paga la propia suma (10), a diferencia de Fives & Threes (2).
        assertEquals(10, scoreOf(engine, 0))
    }

    @Test
    fun allThreesScoresTheSumItselfAndIgnoresNonMultiples() {
        val engine = engine(GameMode.ALL_THREES)
        stageHands(engine, 0 to tiles(3 to 3), 1 to tiles(3 to 4))

        engine.currentPlayerIndex = 0
        play(engine, 0, tileOf(engine, 0, 3, 3))
        assertEquals(6, scoreOf(engine, 0))

        // Derecha [3|4]: extremos [3][4] = 7 => no puntúa.
        play(engine, 1, tileOf(engine, 1, 3, 4))
        assertEquals(0, scoreOf(engine, 1))
    }

    // ==================================================================
    // BLOCK HUNGARIAN
    // ==================================================================

    @Test
    fun hungarian_keepsTurnAfterPlayingWhenMoreTilesArePlayable() {
        val engine = engine(GameMode.BLOCK_HUNGARIAN)
        stageHand(engine, 0, tiles(3 to 3, 3 to 4))
        engine.currentPlayerIndex = 0

        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 3, 3)))

        assertEquals(
            "Tras encadenar debe seguir siendo el turno del jugador 0",
            0,
            engine.currentPlayerIndex
        )
        assertTrue("Debe indicarse que el turno continúa", engine.canContinueHungarianTurn)
        assertEquals(1, engine.boardChain.size)
        assertEquals(2, engine.players[0].hand.size)
    }

    @Test
    fun hungarian_chainsSeveralTilesThenReleasesTheTurn() {
        val engine = engine(GameMode.BLOCK_HUNGARIAN)
        // Mano exacta: la cadena termina cuando se queda sin fichas jugables.
        stageExactHand(engine, 0, tiles(2 to 2, 2 to 5, 5 to 6))
        engine.currentPlayerIndex = 0

        // 1ª jugada: abre la mesa con el doble [2|2].
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 2, 2)))
        assertTrue(engine.canContinueHungarianTurn)
        assertEquals(0, engine.currentPlayerIndex)

        // 2ª jugada consecutiva: [2|5] conecta por el 2.
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 2, 5)))
        assertTrue("Puede seguir encadenando", engine.canContinueHungarianTurn)
        assertEquals(0, engine.currentPlayerIndex)

        // 3ª jugada consecutiva: [5|6] conecta por el 5. Al quedarse sin fichas, la
        // mano se cierra y la ronda termina con victoria por dominó.
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 5, 6)))

        assertTrue("La ronda debe cerrarse al vaciar la mano", engine.isRoundOver)
        assertEquals("El ganador es quien se quedó sin fichas", 0, engine.winnerPlayerId)
        // Y no debe quedar un encadenamiento pendiente tras cerrar la ronda.
        assertFalse(engine.canContinueHungarianTurn)
        assertEquals(3, engine.boardChain.size)
    }

    @Test
    fun hungarian_aiAlsoChainsItsOwnTurn() {
        val engine = engine(GameMode.BLOCK_HUNGARIAN)
        // Mano exacta (sin relleno): al jugar la segunda ficha se queda sin ninguna, así
        // que ya no puede encadenar más y cede el turno.
        stageExactHand(engine, 2, tiles(3 to 3, 3 to 4))
        engine.currentPlayerIndex = 2
        val boardBefore = engine.boardChain.size
        engine.executeAIMove()
        assertTrue("La IA debe seguir encadenando", engine.canContinueHungarianTurn)
        assertEquals("La IA conserva el turno mientras encadena", 2, engine.currentPlayerIndex)
        assertEquals("La IA debe haber puesto una ficha", boardBefore + 1, engine.boardChain.size)
        assertEquals("Le debe quedar una ficha por encadenar", 1, engine.players[2].hand.size)

        // Segunda ficha: la mano queda vacía, así que se cierra la ronda por dominó y
        // no puede quedar un encadenamiento pendiente.
        engine.executeAIMove()
        assertFalse("Sin fichas no puede seguir encadenando", engine.canContinueHungarianTurn)
        assertEquals("La IA juega su segunda ficha", boardBefore + 2, engine.boardChain.size)
        assertTrue("La IA se queda sin fichas", engine.players[2].hand.isEmpty())
        assertTrue("La ronda se cierra al vaciar la mano", engine.isRoundOver)
        assertEquals("Gana la IA que se quedó sin fichas", 2, engine.winnerPlayerId)
    }

    @Test
    fun otherModesStillAdvanceTheTurnAfterASingleTile() {
        val engine = engine(GameMode.DRAW)
        stageHand(engine, 0, tiles(3 to 3, 3 to 4))
        engine.currentPlayerIndex = 0

        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 3, 3)))
        assertFalse(engine.canContinueHungarianTurn)
        assertEquals("En Draw el turno avanza tras una sola ficha", 1, engine.currentPlayerIndex)
    }

    // ==================================================================
    // DRAW SPECIAL
    // ==================================================================

    @Test
    fun drawSpecial_assignsOneBranchPerSeatStartingFromTheOpener() {
        val engine = engine(GameMode.DRAW_SPECIAL)
        stageHand(engine, 0, tiles(6 to 6))
        engine.currentPlayerIndex = 0
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 6, 6)))

        assertEquals(BoardSide.LEFT, engine.branchForPlayer(0))
        assertEquals(BoardSide.RIGHT, engine.branchForPlayer(1))
        assertEquals("Los compañeros de equipo no comparten rama", BoardSide.LEFT, engine.branchForPlayer(2))
        assertEquals(BoardSide.RIGHT, engine.branchForPlayer(3))
    }

    @Test
    fun drawSpecial_onlyTheOwnBranchIsPlayable() {
        val engine = engine(GameMode.DRAW_SPECIAL)
        // El asiento 0 abre con [4|4] (rama izquierda) y el asiento 1 es dueño de la derecha.
        stageHands(engine, 0 to tiles(4 to 4), 1 to tiles(4 to 3, 3 to 5))

        engine.currentPlayerIndex = 0
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 4, 4)))

        // Se fuerza el extremo derecho a 3 para que las dos ramas sean distinguibles:
        // izquierda = 4, derecha = 3.
        engine.rightOpenPip = 3

        // [4|3] encaja físicamente en ambos extremos (tiene un 4 y un 3), pero la rama
        // izquierda pertenece al asiento 0, así que el asiento 1 solo puede jugar a la derecha.
        val tile = tileOf(engine, 1, 4, 3)
        assertTrue("La ficha encaja por puntos en el extremo izquierdo", engine.canConnectLeft(tile))
        assertEquals(
            "Solo debe poder jugar en su rama asignada",
            setOf(BoardSide.RIGHT),
            engine.getPlayableSides(1, tile)
        )
        assertFalse("No puede jugar en la rama del rival", engine.playTileLeft(1, tile))
        assertEquals("La jugada inválida no debe alterar la mesa", 1, engine.boardChain.size)
    }

    @Test
    fun drawSpecial_blocksTheWholeBranchWhenNoTileFits() {
        val engine = engine(GameMode.DRAW_SPECIAL)
        stageHands(engine, 0 to tiles(3 to 3), 1 to tiles(5 to 5))

        engine.currentPlayerIndex = 0
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 3, 3)))

        // El asiento 1 no tiene ningún 3: no tiene jugada en su rama.
        assertTrue(engine.getPlayableTiles(1).isEmpty())
    }

    @Test
    fun drawSpecial_canInvadeTheBranchOfAnOpponentThatPassed() {
        val engine = engine(GameMode.DRAW_SPECIAL)
        stageHands(
            engine,
            0 to tiles(3 to 3, 3 to 4),
            1 to tiles(5 to 5)
        )
        engine.currentPlayerIndex = 0
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 3, 3)))

        // El asiento 1 (dueño de la rama derecha) se queda sin jugada y pasa.
        engine.currentPlayerIndex = 1
        assertTrue("El rival no tiene jugada en su rama", engine.getPlayableTiles(1).isEmpty())
        engine.passTurn(1)

        // Ahora el asiento 0 puede invadir la rama derecha con su [3|4].
        engine.currentPlayerIndex = 0
        val invader = tileOf(engine, 0, 3, 4)
        assertTrue(
            "Con el rival bloqueado la rama derecha debe quedar disponible",
            engine.getPlayableSides(0, invader).contains(BoardSide.RIGHT)
        )
        assertTrue(engine.playTileRight(0, invader))
        assertEquals(2, engine.boardChain.size)
    }

    @Test
    fun drawSpecial_branchStaysClosedWhileItsOwnerHasNotPassed() {
        val engine = engine(GameMode.DRAW_SPECIAL)
        stageHand(engine, 0, tiles(3 to 3, 3 to 4))

        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 3, 3)))

        val tile = tileOf(engine, 0, 3, 4)
        assertFalse(
            "Sin pase previo del rival la rama derecha no se puede invadir",
            engine.getPlayableSides(0, tile).contains(BoardSide.RIGHT)
        )
        assertTrue(engine.isBranchRestrictedFor(0, BoardSide.RIGHT))
    }

    @Test
    fun drawSpecial_restrictedBranchExpiresAfterTheOwnerPlaysAgain() {
        val engine = engine(GameMode.DRAW_SPECIAL)
        stageHands(engine, 0 to tiles(3 to 3, 3 to 4), 1 to tiles(5 to 5))

        engine.currentPlayerIndex = 0
        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 3, 3)))

        // El rival de la rama derecha pasa y la deja abierta...
        engine.currentPlayerIndex = 1
        assertTrue("El rival no tiene jugada en su rama", engine.getPlayableTiles(1).isEmpty())
        engine.passTurn(1)

        engine.currentPlayerIndex = 0
        assertTrue(engine.getPlayableSides(0, tileOf(engine, 0, 3, 4)).contains(BoardSide.RIGHT))

        // ...pero al jugar el asiento 0, la ventana de invasión se cierra.
        assertTrue(engine.playTileRight(0, tileOf(engine, 0, 3, 4)))
        assertTrue(
            "Tras consumirse la ventana, la rama vuelve a estar cerrada",
            engine.isBranchRestrictedFor(1, BoardSide.LEFT)
        )
    }

    @Test
    fun drawSpecial_openEndsRuleDoesNotApplyToOtherModes() {
        val engine = engine(GameMode.DRAW)
        stageHand(engine, 0, tiles(3 to 3))

        assertTrue(playLeft(engine, 0, tileOf(engine, 0, 3, 3)))

        // En Draw normal no hay ramas asignadas: ambas están disponibles.
        assertNull(engine.branchForPlayer(0))
        assertFalse(engine.isBranchRestrictedFor(0, BoardSide.RIGHT))
    }

    // ==================================================================
    // Cierre de ronda en las tres modalidades
    // ==================================================================

    @Test
    fun allThreeModesResolveTheRoundWithoutDeadlocks() {
        for (mode in listOf(GameMode.FIVES_AND_THREES, GameMode.BLOCK_HUNGARIAN, GameMode.DRAW_SPECIAL)) {
            val engine = engine(mode)
            var steps = 0
            while (!engine.isRoundOver && steps < 400) {
                engine.executeAIMove()
                steps++
            }
            assertTrue(
                "$mode debería resolver la ronda sin quedarse bloqueado (pasos: $steps)",
                engine.isRoundOver
            )
            assertTrue("El ganador debe quedar definido en $mode", engine.winnerPlayerId != null)
            assertTrue("Ningún asiento puede acabar con puntos negativos en $mode", engine.players.all { it.score >= 0 })
            assertTrue(engine.boardChain.isNotEmpty())
        }
    }
}
