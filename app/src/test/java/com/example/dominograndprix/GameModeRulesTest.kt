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

    /** Manos exactas para varios asientos a la vez sin añadir fichas muertas. */
    private fun setExactHands(engine: DominoEngine, vararg hands: Pair<Int, List<DominoTile>>) {
        val bySeat = hands.toMap()
        engine.players.indices.forEach { seat ->
            engine.players[seat] = engine.players[seat].copy(
                hand = bySeat[seat] ?: emptyList()
            )
        }
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

        // Derecha [3|6]: el [3|3] sigue abierto a la izquierda (cuenta doble = 6) y la derecha
        // queda en 6 => extremos 6 + 6 = 12 => 4 puntos (12 / 3).
        play(engine, 1, tileOf(engine, 1, 3, 6))
        assertEquals(4, scoreOf(engine, 1))

        // Derecha [6|0]: el [3|3] sigue abierto a la izquierda (6) y la derecha queda en 0
        // => extremos 6 + 0 = 6 => 2 puntos (6 / 3).
        play(engine, 2, tileOf(engine, 2, 6, 0))
        assertEquals(2, scoreOf(engine, 2))

        // Izquierda con [3|0]: conecta por la izquierda.
        assertTrue(playLeft(engine, 3, tileOf(engine, 3, 3, 0)))
        assertEquals(3, scoreOf(engine, 3))
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

    // ==================================================================
    // NUEVAS REGLAS: ALL FIVES CON DOBLES, MATADOR, TRANCA POR EQUIPOS
    // ==================================================================

    @Test
    fun allFives_countsOpenDoublesTwiceReaching15And20() {
        val engine = engine(GameMode.ALL_FIVES)
        setExactHands(
            engine,
            0 to tiles(6 to 6, 0 to 0),
            1 to tiles(6 to 3, 0 to 0),
            2 to tiles(3 to 4, 0 to 0),
            3 to tiles(4 to 4, 0 to 0)
        )

        // 1. Abre con [6|6] => un único doble en mesa cuenta doble = 12 (no múltiplo de 5 => 0 pts).
        engine.currentPlayerIndex = 0
        play(engine, 0, tileOf(engine, 0, 6, 6))
        assertEquals(0, scoreOf(engine, 0))

        // 2. Conecta [6|3]: el [6|6] abierto en el otro extremo cuenta doble (12),
        // más el 3 en el extremo abierto = 15 => ¡múltiplo de 5, da 15 puntos!
        play(engine, 1, tileOf(engine, 1, 6, 3))
        assertEquals(15, scoreOf(engine, 1))

        // 3. Conecta [3|4]: el [6|6] cuenta 12, más el 4 = 16 (no múltiplo de 5 => 0 pts).
        play(engine, 2, tileOf(engine, 2, 3, 4))
        assertEquals(0, scoreOf(engine, 2))

        // 4. Conecta [4|4]: el [6|6] cuenta 12, más el [4|4] abierto cuenta 8 => 12 + 8 = 20 => ¡20 puntos!
        play(engine, 3, tileOf(engine, 3, 4, 4))
        assertEquals(20, scoreOf(engine, 3))
    }

    @Test
    fun matador_connectsProperPipsToSumSeven() {
        val engine = engine(GameMode.MATADOR)
        stageHands(
            engine,
            0 to tiles(5 to 5),
            1 to tiles(6 to 1),
            2 to tiles(5 to 2)
        )

        // Abre con [5|5]: extremos 5 y 5.
        engine.currentPlayerIndex = 0
        play(engine, 0, tileOf(engine, 0, 5, 5))

        // Forzamos extremo derecho a 1.
        engine.rightOpenPip = 1

        // Al jugar [6|1] contra 1: para sumar 7, la cara 6 conecta con 1 y el 1 queda abierto.
        val matador61 = tileOf(engine, 1, 6, 1)
        assertTrue(engine.canConnectRight(matador61))
        assertTrue(engine.playTileRight(1, matador61))
        assertEquals("El extremo derecho debe quedar en 1", 1, engine.rightOpenPip)

        // Forzamos extremo derecho a 2.
        engine.rightOpenPip = 2

        // Al jugar [5|2] contra 2: para sumar 7, la cara 5 conecta con 2 y el 2 queda abierto.
        val matador52 = tileOf(engine, 2, 5, 2)
        assertTrue(engine.canConnectRight(matador52))
        assertTrue(engine.playTileRight(2, matador52))
        assertEquals("El extremo derecho debe quedar en 2", 2, engine.rightOpenPip)
    }

    @Test
    fun teamTranca_winsByCombinedTeamPips() {
        val engine = engine(GameMode.BLOCK)
        // Equipo 0 (asientos 0 y 2): 5 + 30 = 35 puntos.
        // Equipo 1 (asientos 1 y 3): 10 + 10 = 20 puntos.
        // Individualmente el asiento 0 tiene 5 (el menor), pero por equipos gana el Equipo 1 (20 < 35).
        setExactHands(
            engine,
            0 to tiles(5 to 0),
            2 to tiles(6 to 6, 6 to 6, 6 to 6),
            1 to tiles(5 to 5),
            3 to tiles(5 to 5)
        )
        engine.currentPlayerIndex = 0

        // Apertura [1|1] y se tranca la mesa (nadie puede jugar sobre 1|1).
        engine.leftOpenPip = 1
        engine.rightOpenPip = 1

        // 4 pases consecutivos provocan la tranca:
        engine.passTurn(0)
        engine.passTurn(1)
        engine.passTurn(2)
        engine.passTurn(3)

        assertTrue(engine.isRoundOver)
        assertTrue(engine.lastRoundWasBlocked)
        assertEquals("Debe ganar el equipo 1 con menor suma conjunta (20 < 35)", 1, engine.winningTeam)
    }

    @Test
    fun teamScore_triggersMatchOver() {
        val engine = DominoEngine(gameMode = GameMode.DRAW, targetScore = 100)
        // Asiento 0 tiene 55 puntos, su compañero (asiento 2) anota 45 puntos.
        // Ninguno llega a 100 en solitario, pero el equipo suma 100.
        engine.players[0] = engine.players[0].copy(score = 55)

        setExactHands(
            engine,
            0 to tiles(5 to 5),
            1 to tiles(6 to 6, 6 to 6, 6 to 6, 6 to 3),
            2 to tiles(3 to 3),
            3 to tiles(0 to 0)
        )
        engine.currentPlayerIndex = 2
        engine.leftOpenPip = 3
        engine.rightOpenPip = 3

        // El asiento 2 juega y se queda sin fichas => hace dominó.
        // Rival tiene 45 pips en mano:
        assertTrue(engine.playTileLeft(2, tileOf(engine, 2, 3, 3)))
        assertTrue("La ronda termina", engine.isRoundOver)
        assertTrue("La partida debe terminar porque el equipo alcanzó 100 puntos", engine.isMatchOver)
        assertEquals(0, engine.winningTeam)
    }

    @Test
    fun allFives_spinnerAllowsPlayOnFourSidesAndScoresAllEnds() {
        val engine = engine(GameMode.ALL_FIVES)
        stageHands(
            engine,
            0 to tiles(6 to 6, 6 to 1),
            1 to tiles(6 to 3),
            2 to tiles(6 to 5),
            3 to tiles(6 to 1)
        )

        // 1. Asiento 0 juega el primer doble [6|6]: se convierte en spinner
        engine.currentPlayerIndex = 0
        val double6 = tileOf(engine, 0, 6, 6)
        assertTrue(engine.playTileLeft(0, double6))
        assertEquals(double6, engine.spinnerTile)
        // [6|6] abierto en ambos extremos (cuenta doble al estar sola): 6 * 2 = 12 (no múltiplo de 5 => 0 pts)
        assertEquals(0, scoreOf(engine, 0))

        // 2. Asiento 1 juega [6|3] a la derecha:
        // Aún no hay ramas transversales UP/DOWN conectadas. Extremos abiertos: izquierda [6|6] (cuenta 12),
        // derecha [3] => 12 + 3 = 15 => ¡Múltiplo de 5 => 15 puntos para asiento 1!
        engine.currentPlayerIndex = 1
        val tile63 = tileOf(engine, 1, 6, 3)
        assertTrue(engine.playTileRight(1, tile63))
        assertEquals(15, scoreOf(engine, 1))
        assertEquals(6, engine.leftOpenPip)
        assertEquals(3, engine.rightOpenPip)

        // 3. Como [6|6] es el spinner, se pueden conectar fichas por UP y DOWN con pip 6!
        val tile65Up = tileOf(engine, 2, 6, 5)
        assertTrue("Debe poder conectar arriba del spinner [6|6]", engine.canConnectUp(tile65Up))
        val playableSidesSeat2 = engine.getPlayableSides(2, tile65Up)
        assertTrue(playableSidesSeat2.contains(BoardSide.UP))
        assertTrue(playableSidesSeat2.contains(BoardSide.DOWN))

        // Jugamos [6|5] por UP: se activan las 4 ramas del spinner:
        engine.currentPlayerIndex = 2
        assertTrue(engine.playTile(2, tile65Up, BoardSide.UP))
        assertEquals(5, engine.upOpenPip)

        // Ahora los extremos abiertos en cruz son:
        // LEFT: 6 (extremo libre spinner), RIGHT: 3, UP: 5, DOWN: 6 (extremo libre spinner)
        // Suma: 6 + 3 + 5 + 6 = 20 => ¡Múltiplo de 5 => 20 puntos para asiento 2!
        assertEquals(20, scoreOf(engine, 2))

        // 4. Asiento 3 juega [6|1] por DOWN:
        engine.currentPlayerIndex = 3
        val tile61Down = tileOf(engine, 3, 6, 1)
        assertTrue(engine.playTile(3, tile61Down, BoardSide.DOWN))
        assertEquals(1, engine.downOpenPip)

        // Ahora los extremos abiertos son:
        // LEFT: 6, RIGHT: 3, UP: 5, DOWN: 1
        // Suma: 6 + 3 + 5 + 1 = 15 => ¡Múltiplo de 5 => 15 puntos para asiento 3!
        assertEquals(15, scoreOf(engine, 3))

        // 5. Asiento 0 juega [6|1] por LEFT cubriendo el último brazo directo del spinner:
        engine.currentPlayerIndex = 0
        val tile61Left = tileOf(engine, 0, 6, 1)
        assertTrue(engine.playTileLeft(0, tile61Left))
        assertEquals(1, engine.leftOpenPip)

        // Ahora los 4 extremos son: LEFT: 1, RIGHT: 3, UP: 5, DOWN: 1
        // Suma: 1 + 3 + 5 + 1 = 10 => ¡Múltiplo de 5 => 10 puntos para asiento 0!
        assertEquals(10, scoreOf(engine, 0))
    }

    @Test
    fun allFives_lateSpinnerAllowsPlayOnFourSides() {
        val engine = engine(GameMode.ALL_FIVES)
        stageHands(
            engine,
            0 to tiles(5 to 4),
            1 to tiles(4 to 4),
            2 to tiles(4 to 2)
        )

        // 1. Asiento 0 abre con [5|4] (no es doble, no hay spinner todavía)
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, tileOf(engine, 0, 5, 4)))
        assertNull("Una ficha normal inicial no crea spinner", engine.spinnerTile)
        assertNull(engine.upOpenPip)
        assertNull(engine.downOpenPip)

        // 2. Asiento 1 juega [4|4] a la derecha: es el primer doble de la ronda -> ¡se convierte en spinner!
        engine.currentPlayerIndex = 1
        val double4 = tileOf(engine, 1, 4, 4)
        assertTrue(engine.playTileRight(1, double4))
        assertEquals("El primer doble tardío debe ser el spinner", double4, engine.spinnerTile)
        assertEquals(4, engine.upOpenPip)
        assertEquals(4, engine.downOpenPip)

        // 3. Asiento 2 debe poder jugar en las ramas UP y DOWN del nuevo spinner tardío
        val tile42 = tileOf(engine, 2, 4, 2)
        val playableSides = engine.getPlayableSides(2, tile42)
        assertTrue("Debe permitir jugar UP en spinner tardío", playableSides.contains(BoardSide.UP))
        assertTrue("Debe permitir jugar DOWN en spinner tardío", playableSides.contains(BoardSide.DOWN))

        // Jugamos en UP:
        engine.currentPlayerIndex = 2
        assertTrue(engine.playTile(2, tile42, BoardSide.UP))
        assertEquals(2, engine.upOpenPip)
    }

    @Test
    fun allFives_targetScoreReachedDuringRound_finishesRoundAndMatchCleanly() {
        // Reproduce el caso de reporte del usuario: alcanzar 100 puntos en México
        val engine = DominoEngine(gameMode = GameMode.ALL_FIVES, targetScore = 100)
        // Fijamos equipo 0 a 95 puntos
        engine.players[0] = engine.players[0].copy(score = 95)

        stageHands(
            engine,
            0 to tiles(5 to 5),
            1 to tiles(5 to 0)
        )

        // Asiento 0 abre con [5|5]: suma 10 puntos => equipo llega a 105 puntos (>= 100)
        engine.currentPlayerIndex = 0
        val double5 = tileOf(engine, 0, 5, 5)
        assertTrue(engine.playTileLeft(0, double5))

        // Debe finalizar inmediatamente la ronda y la partida sin bloquearse
        assertTrue("La ronda debe estar terminada", engine.isRoundOver)
        assertTrue("La partida debe estar terminada al alcanzar targetScore", engine.isMatchOver)
        assertEquals(0, engine.winningTeam)

        // Comprobamos que la IA no intente jugar y no se quede en bucle
        assertFalse("AI no debe ejecutar jugadas cuando la partida terminó", engine.executeAIMove())
    }

    @Test
    fun allModes_doublesPlacedAreMarkedAsDoubleForCrosswiseRendering() {
        val engine = engine(GameMode.DRAW)
        stageHand(engine, 0, tiles(6 to 6, 6 to 2))
        engine.currentPlayerIndex = 0

        // Colocamos el doble [6|6]
        assertTrue(engine.playTileLeft(0, tileOf(engine, 0, 6, 6)))
        val placedDouble = engine.boardChain.first()
        assertTrue("La ficha colocada debe registrar isDouble = true", placedDouble.isDouble)
        assertTrue("El doble inicial debe colocarse vertical (en cruz)", placedDouble.isVertical)
    }

    @Test
    fun allFives_doubleSixAndDoubleFour_scores20Points() {
        // Regla oficial de Muggins / All Fives explicada por el usuario:
        // [6|6] en un extremo (12) + [4|4] en el otro extremo (8) = 20 puntos!
        val engine = DominoEngine(gameMode = GameMode.ALL_FIVES, targetScore = 500)
        stageHands(
            engine,
            0 to tiles(6 to 6),
            1 to tiles(6 to 4),
            2 to tiles(4 to 4)
        )

        // 1. Asiento 0 juega [6|6]: vale 12 (no múltiplo de 5 => 0)
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, tileOf(engine, 0, 6, 6)))

        // 2. Asiento 1 juega [6|4] por la derecha:
        // Extremos: izquierda [6|6]=12, derecha [4]=4 => suma 16 (0 pts)
        engine.currentPlayerIndex = 1
        assertTrue(engine.playTileRight(1, tileOf(engine, 1, 6, 4)))

        // 3. Asiento 2 juega [4|4] por la derecha:
        // Extremos: izquierda [6|6] = 12 pts, derecha [4|4] = 8 pts!
        // Suma = 12 + 8 = 20 puntos => ¡Múltiplo de 5 => Anota 20 puntos!
        engine.currentPlayerIndex = 2
        assertTrue(engine.playTileRight(2, tileOf(engine, 2, 4, 4)))

        assertEquals(20, engine.calculateOpenEndsSum())
        assertEquals(20, scoreOf(engine, 2))
        val evalText = engine.getOpenEndsEvaluationText()
        assertTrue("El texto del HUD debe indicar el cálculo detallado y los 20 pts: $evalText", evalText.contains("20 pts ⭐"))
    }

    @Test
    fun tilesAlignment_touchingSeamlesslyWithoutGaps() {
        // Verifica que los 4 brazos del spinner toquen físicamente la ficha inicial (horizontal 31x17) sin huecos
        val engine = DominoEngine(gameMode = GameMode.ALL_FIVES, targetScore = 500)
        stageHand(engine, 0, tiles(6 to 6))
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, tileOf(engine, 0, 6, 6)))

        val spinner = engine.boardChain.first()
        assertEquals(105, spinner.virtX)
        assertEquals(119, spinner.virtY)
        assertFalse(spinner.isVertical) // 31 de ancho (105..136), 17 de alto (119..136)

        // Hueco IZQUIERDA: horizontal 31x17, debe tocar el borde izquierdo (105) con solape
        val leftSlot = engine.getNextSlotForSide(BoardSide.LEFT)
        assertEquals(76, leftSlot.virtX)
        assertEquals(119, leftSlot.virtY)
        assertFalse(leftSlot.isVertical)
        // 76 + 31 = 107 >= 105 (solapa 2px con el spinner)
        assertTrue("Izquierda debe tocar el spinner", leftSlot.virtX + 31 >= spinner.virtX)

        // Hueco DERECHA: horizontal 31x17, debe tocar el borde derecho (136) con solape
        val rightSlot = engine.getNextSlotForSide(BoardSide.RIGHT)
        assertEquals(134, rightSlot.virtX)
        assertEquals(119, rightSlot.virtY)
        assertFalse(rightSlot.isVertical)
        // 134 <= 136 (solapa 2px con el borde derecho del spinner)
        assertTrue("Derecha debe tocar el spinner", rightSlot.virtX <= spinner.virtX + 31)

        // Hueco ARRIBA: vertical 17x31, centrado sobre el lomo superior del spinner (119)
        val upSlot = engine.getNextSlotForSide(BoardSide.UP)
        assertEquals(112, upSlot.virtX) // Centrado: 105 + (31 - 17)/2 = 112
        assertEquals(90, upSlot.virtY)
        assertTrue(upSlot.isVertical)
        // 90 + 31 = 121 >= 119 (solapa 2px con el borde superior del spinner)
        assertTrue("Arriba debe tocar el spinner", upSlot.virtY + 31 >= spinner.virtY)

        // Hueco ABAJO: vertical 17x31, centrado bajo el lomo inferior del spinner (136)
        val downSlot = engine.getNextSlotForSide(BoardSide.DOWN)
        assertEquals(112, downSlot.virtX) // Centrado: 105 + (31 - 17)/2 = 112
        assertEquals(134, downSlot.virtY)
        assertTrue(downSlot.isVertical)
        // 134 <= 136 (solapa 2px con el borde inferior del spinner)
        assertTrue("Abajo debe tocar el spinner", downSlot.virtY <= spinner.virtY + 17)
    }

    @Test
    fun traceAllFivesRightBranchPlacement() {
        val engine = DominoEngine(gameMode = GameMode.ALL_FIVES, targetScore = 500)
        stageHands(
            engine,
            0 to tiles(6 to 6),
            1 to tiles(6 to 5),
            2 to tiles(5 to 4)
        )

        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, tileOf(engine, 0, 6, 6)))

        engine.currentPlayerIndex = 1
        assertTrue(engine.playTileRight(1, tileOf(engine, 1, 6, 5)))

        engine.currentPlayerIndex = 2
        assertTrue(engine.playTileRight(2, tileOf(engine, 2, 5, 4)))

        println("=== BOARD CHAIN DUMP ===")
        engine.boardChain.forEach { placed ->
            println("PLACED: ${placed.tile} at virtX=${placed.virtX}, virtY=${placed.virtY}, side=${placed.branchSide}, isVert=${placed.isVertical}, pipL=${placed.pipLeft}, pipR=${placed.pipRight}")
        }
    }
}


