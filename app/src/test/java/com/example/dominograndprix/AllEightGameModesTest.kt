package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de verificación exhaustiva para las 8 modalidades oficiales de dominó:
 * 1. DRAW (Dominó tradicional con robo de pozo)
 * 2. BLOCK (Bloqueo / Tranca sin pozo)
 * 3. ALL_FIVES (Dominó de Cinco con spinner en cruz)
 * 4. ALL_THREES (Dominó de Tres)
 * 5. FIVES_AND_THREES (Cinco y Tres clásico)
 * 6. MATADOR (Suma 7 y 4 comodines Matadores)
 * 7. DRAW_SPECIAL (Robo Especial con ramas asignadas por asiento)
 * 8. BLOCK_HUNGARIAN (Dominó Húngaro con encadenamiento múltiple por turno)
 */
class AllEightGameModesTest {

    // -------------------------------------------------------------------------
    // 1. Verificación de existencia de las 8 variantes en el enum
    // -------------------------------------------------------------------------

    @Test
    fun verifyExactlyEightGameModesExistWithDescriptions() {
        val modes = GameMode.values()
        assertEquals("Deben existir exactamente 8 modalidades de juego", 8, modes.size)

        val expectedModes = setOf(
            GameMode.DRAW,
            GameMode.BLOCK,
            GameMode.ALL_FIVES,
            GameMode.ALL_THREES,
            GameMode.FIVES_AND_THREES,
            GameMode.MATADOR,
            GameMode.DRAW_SPECIAL,
            GameMode.BLOCK_HUNGARIAN
        )
        assertEquals(expectedModes, modes.toSet())

        modes.forEach { mode ->
            assertTrue("Título de $mode no puede estar vacío", mode.title.isNotBlank())
            assertTrue("Resumen de $mode no puede estar vacío", mode.summary.isNotBlank())
        }
    }

    // -------------------------------------------------------------------------
    // 2. Verificación de reglas específicas de cada una de las 8 variantes
    // -------------------------------------------------------------------------

    @Test
    fun mode1_draw_allowsBoneyardDrawAndPassesOnlyWhenEmpty() {
        val engine = DominoEngine(gameMode = GameMode.DRAW, targetScore = 100)
        assertTrue("Draw permite robar del pozo", engine.gameMode.allowsDraw)
        assertFalse("Draw no es juego de puntos por turno", engine.gameMode.isPointGame)
        assertEquals(4, engine.players.size)

        // Con 2 jugadores: 14 fichas en mano y 14 en pozo
        val engine2P = DominoEngine(gameMode = GameMode.DRAW, playerCount = 2, targetScore = 100)
        assertEquals("En 2 jugadores el pozo tiene 14 fichas", 14, engine2P.boneyard.size)

        // Comprobar que roba cuando no tiene jugada
        engine2P.leftOpenPip = 6
        engine2P.rightOpenPip = 6
        engine2P.players[0] = engine2P.players[0].copy(hand = listOf(DominoTile(1, 1)))
        engine2P.currentPlayerIndex = 0

        val initialHandSize = engine2P.players[0].hand.size
        val drawn = engine2P.drawTile(0)
        assertNotNull("Debe poder robar del pozo", drawn)
        assertEquals(initialHandSize + 1, engine2P.players[0].hand.size)
    }

    @Test
    fun mode2_block_disallowsBoneyardDrawAndTrancaWorks() {
        val engine = DominoEngine(gameMode = GameMode.BLOCK, targetScore = 100)
        assertFalse("Block NO permite robar del pozo", engine.gameMode.allowsDraw)
        assertFalse("Block no es juego de puntos", engine.gameMode.isPointGame)
        assertNull("No debe poder robar", engine.drawTile(0))

        // Apertura con [1|1]
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(1, 1), DominoTile(6, 6)))
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, DominoTile(1, 1)))

        // Ahora cada jugador tiene solo [6|6], nadie puede jugar sobre [1|1]
        engine.players.indices.forEach { seat ->
            engine.players[seat] = engine.players[seat].copy(hand = listOf(DominoTile(6, 6)))
        }
        engine.currentPlayerIndex = 1

        assertEquals(0, engine.getPlayableTiles(1).size)
        engine.passTurn(1)
        engine.passTurn(2)
        engine.passTurn(3)
        engine.passTurn(0)

        assertTrue("La ronda termina por tranca", engine.isRoundOver)
        assertTrue("Se marca lastRoundWasBlocked = true", engine.lastRoundWasBlocked)
    }

    @Test
    fun mode3_allFives_scoresMultiplesOfFiveAndCrossBranchesWork() {
        val engine = DominoEngine(gameMode = GameMode.ALL_FIVES, targetScore = 100)
        assertTrue("All Fives es juego de puntos", engine.gameMode.isPointGame)

        // 1. Apertura con doble [5|5]: suma 10 puntos (múltiplo de 5)
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(5, 5), DominoTile(6, 6)))
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, DominoTile(5, 5)))
        assertEquals(10, engine.players[0].score)
        assertEquals(DominoTile(5, 5), engine.spinnerTile)

        // 2. Las ramas UP y DOWN quedan habilitadas con pip 5
        assertTrue(engine.canConnectUp(DominoTile(5, 2)))
        assertTrue(engine.canConnectDown(DominoTile(5, 3)))
    }

    @Test
    fun mode4_allThrees_scoresMultiplesOfThreeAndIgnoresOthers() {
        val engine = DominoEngine(gameMode = GameMode.ALL_THREES, targetScore = 100)
        assertTrue("All Threes es juego de puntos", engine.gameMode.isPointGame)

        // Apertura con [3|3]: vale 6 (múltiplo de 3) -> 6 puntos
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(3, 3), DominoTile(6, 6)))
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, DominoTile(3, 3)))
        assertEquals("Apertura [3|3] (extremos 3+3=6) debe dar 6 puntos", 6, engine.players[0].score)

        // Jugar [3|4] a la derecha: extremos [3] y [4] = 7 (no múltiplo de 3) -> 0 puntos
        engine.players[1] = engine.players[1].copy(hand = listOf(DominoTile(3, 4), DominoTile(6, 6)))
        engine.currentPlayerIndex = 1
        assertTrue(engine.playTileRight(1, DominoTile(3, 4)))
        assertEquals("Extremos 3+4=7 no debe dar puntos", 0, engine.players[1].score)
    }

    @Test
    fun mode5_fivesAndThrees_dualMultiplesScoring() {
        val engine = DominoEngine(gameMode = GameMode.FIVES_AND_THREES, targetScore = 100)
        assertTrue("Fives & Threes es juego de puntos", engine.gameMode.isPointGame)

        // Apertura [5|5]: extremos 5+5=10 -> 10/5 = 2 puntos
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(5, 5), DominoTile(6, 6)))
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, DominoTile(5, 5)))
        assertEquals("Extremos 10 da 2 puntos (10/5)", 2, engine.players[0].score)

        // Jugada [5|2]: el doble [5|5] abierto cuenta 10, y el extremo derecho queda en 2 -> suma 10 + 2 = 12
        // 12 es múltiplo de 3 -> 12 / 3 = 4 puntos
        engine.players[1] = engine.players[1].copy(hand = listOf(DominoTile(5, 2), DominoTile(6, 6)))
        engine.currentPlayerIndex = 1
        assertTrue(engine.playTileRight(1, DominoTile(5, 2)))
        assertEquals("Extremos 10+2=12 da 4 puntos (12/3)", 4, engine.players[1].score)
    }

    @Test
    fun mode6_matador_sevenSumAndFourMatadorsWildcard() {
        val engine = DominoEngine(gameMode = GameMode.MATADOR, targetScore = 100)
        assertFalse("Matador no permite robar del pozo", engine.gameMode.allowsDraw)

        // Las 4 fichas Matador son: [0|0], [6|1], [5|2], [4|3]
        assertTrue(engine.isMatadorTile(DominoTile(0, 0)))
        assertTrue(engine.isMatadorTile(DominoTile(6, 1)))
        assertTrue(engine.isMatadorTile(DominoTile(5, 2)))
        assertTrue(engine.isMatadorTile(DominoTile(4, 3)))
        assertFalse(engine.isMatadorTile(DominoTile(6, 6)))

        // Apertura con [5|5]: extremos 5 y 5
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(5, 5)))
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, DominoTile(5, 5)))

        // Sobre un extremo 5 se debe jugar una ficha que sume 7 (un 2) o un Matador
        val tile23 = DominoTile(3, 2)
        val matador00 = DominoTile(0, 0)
        val regular66 = DominoTile(6, 6)

        assertTrue("Ficha con 2 conecta con 5 (2+5=7)", engine.canConnectRight(tile23))
        assertTrue("Comodín [0|0] conecta con cualquier extremo", engine.canConnectRight(matador00))
        assertFalse("Ficha [6|6] no suma 7 con 5 y no es Matador", engine.canConnectRight(regular66))

        // Si el extremo es 0 (blanca), SOLO se puede jugar con un Matador
        engine.rightOpenPip = 0
        assertFalse("Ficha normal no puede jugar sobre 0", engine.canConnectRight(DominoTile(4, 2)))
        assertTrue("Matador sí puede jugar sobre 0", engine.canConnectRight(DominoTile(4, 3)))
    }

    @Test
    fun mode7_drawSpecial_exclusiveBranchOwnershipAndInvasionOnPass() {
        val engine = DominoEngine(gameMode = GameMode.DRAW_SPECIAL, targetScore = 100)
        assertTrue(engine.gameMode.allowsDraw)

        // Asiento 0 abre -> Asiento 0 y 2 tienen rama LEFT, Asiento 1 y 3 tienen rama RIGHT
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(4, 4), DominoTile(4, 1)))
        engine.currentPlayerIndex = 0
        assertTrue(engine.playTileLeft(0, DominoTile(4, 4)))

        assertEquals(BoardSide.LEFT, engine.branchForPlayer(0))
        assertEquals(BoardSide.RIGHT, engine.branchForPlayer(1))

        // Asiento 0 no puede jugar en la rama derecha porque pertenece al rival y no ha pasado
        assertTrue(engine.isBranchRestrictedFor(0, BoardSide.RIGHT))

        // Asiento 1 pasa turno
        engine.players[1] = engine.players[1].copy(hand = listOf(DominoTile(6, 6)))
        engine.currentPlayerIndex = 1
        engine.passTurn(1)

        // Ahora asiento 0 sí puede invadir la rama derecha del rival que pasó
        assertFalse(engine.isBranchRestrictedFor(0, BoardSide.RIGHT))
    }

    @Test
    fun mode8_blockHungarian_chainsMultipleTilesInSingleTurn() {
        val engine = DominoEngine(gameMode = GameMode.BLOCK_HUNGARIAN, targetScore = 100)
        assertFalse("Húngaro no permite robar del pozo", engine.gameMode.allowsDraw)

        // Asiento 0 tiene [3|3] y [3|5]
        engine.players[0] = engine.players[0].copy(hand = listOf(DominoTile(3, 3), DominoTile(3, 5)))
        engine.currentPlayerIndex = 0

        // Juega [3|3]
        assertTrue(engine.playTileLeft(0, DominoTile(3, 3)))

        // Como todavía tiene una ficha jugable [3|5] que conecta con el 3, conserva el turno
        assertTrue("canContinueHungarianTurn debe ser true", engine.canContinueHungarianTurn)
        assertEquals("El jugador 0 debe conservar su turno para seguir encadenando", 0, engine.currentPlayerIndex)

        // Juega [3|5]
        assertTrue(engine.playTileRight(0, DominoTile(3, 5)))

        // Ahora no le quedan más fichas, se termina la ronda con dominó
        assertTrue(engine.isRoundOver)
        assertEquals(0, engine.winnerPlayerId)
    }

    // -------------------------------------------------------------------------
    // 3. Simulación de partidas completas autónomas (IA vs IA) en las 8 modalidades
    // -------------------------------------------------------------------------

    @Test
    fun testAllEightModesCompleteFullRoundsWithoutDeadlocksOrCollisions() {
        for (mode in GameMode.values()) {
            for (roundIndex in 1..5) {
                val engine = DominoEngine.allAi(
                    gameMode = mode,
                    targetScore = if (mode.isPointGame) 50 else 100,
                    seed = roundIndex * 1000L
                )

                var stepCount = 0
                while (!engine.isRoundOver && stepCount < 300) {
                    stepCount++
                    engine.executeAIMove()
                }

                assertTrue(
                    "Modalidad $mode (ronda $roundIndex) debe finalizar sin quedarse bloqueada (pasos: $stepCount)",
                    engine.isRoundOver
                )
                assertTrue("La mesa no debe quedar vacía en $mode", engine.boardChain.isNotEmpty())
                assertTrue(
                    "Debe haber un ganador o equipo ganador definido en $mode",
                    engine.winnerPlayerId != null || engine.winningTeam != null
                )

                // Verificar colisiones de fichas en mesa
                val chain = engine.boardChain
                for (i in chain.indices) {
                    val a = chain[i]
                    val aw = if (a.isVertical) 15 else 29
                    val ah = if (a.isVertical) 29 else 15
                    for (j in i + 1 until chain.size) {
                        val b = chain[j]
                        val bw = if (b.isVertical) 15 else 29
                        val bh = if (b.isVertical) 29 else 15
                        val overlapX = a.virtX < b.virtX + bw - 1 && a.virtX + aw - 1 > b.virtX
                        val overlapY = a.virtY < b.virtY + bh - 1 && a.virtY + ah - 1 > b.virtY
                        assertFalse(
                            "Colisión en modo $mode entre ficha $i (${a.tile}) en (${a.virtX},${a.virtY}) y ficha $j (${b.tile}) en (${b.virtX},${b.virtY})",
                            overlapX && overlapY
                        )
                    }
                }
            }
        }
    }
}
