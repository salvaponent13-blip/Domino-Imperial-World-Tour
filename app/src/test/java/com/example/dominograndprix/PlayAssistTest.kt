package com.example.dominograndprix

import com.example.dominograndprix.data.TournamentCampaign
import com.example.dominograndprix.data.difficultyForVenue
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.engine.ai.AiMove
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Ayudas al jugador (deshacer y pista) y escalado de dificultad del torneo.
 *
 * El deshacer es delicado: si el motor no restaura TODO (manos, pozo, turno, marcador), el
 * jugador se queda con una partida corrupta. Estos tests comparan el estado completo antes y
 * despues, no solo el tablero.
 */
class PlayAssistTest {

    private fun engine(humanSeats: Set<Int> = setOf(0)) = DominoEngine(
        gameMode = GameMode.BLOCK,
        targetScore = 300,
        random = Random(900L),
        randomSeed = 900L,
        playerCount = 2,
        isTeamGame = false,
        humanPlayerIndices = humanSeats
    )

    /** Fija la mano de cada asiento indicado (sin fichas de relleno). */
    private fun stageHands(engine: DominoEngine, vararg hands: Pair<Int, List<DominoTile>>) {
        val bySeat = hands.toMap()
        engine.players.indices.forEach { seat ->
            engine.players[seat] = engine.players[seat].copy(hand = bySeat[seat] ?: emptyList())
        }
    }

    private fun tile(high: Int, low: Int) = DominoTile(high, low)

    private fun setupTwoSeatBlock(): DominoEngine {
        val engine = engine()
        engine.startNewRound()
        stageHands(
            engine,
            0 to listOf(tile(6, 3), tile(0, 1)),
            1 to listOf(tile(3, 2), tile(0, 4))
        )
        engine.currentPlayerIndex = 0
        return engine
    }

    private fun snapshotOf(engine: DominoEngine): String = buildString {
        append("turno=").append(engine.currentPlayerIndex)
        append("|tablero=").append(engine.boardChain.map { "${it.tile.high}-${it.tile.low}" })
        append("|manos=").append(engine.players.map { p -> p.hand.map { "${it.high}-${it.low}" } })
        append("|pozo=").append(engine.boneyard.size)
        append("|extremos=").append("${engine.leftOpenPip}/${engine.rightOpenPip}")
        append("|ronda=").append(engine.isRoundOver)
    }

    @Test
    fun undo_restoresTheWholeStateIncludingTheAiReply() {
        val engine = setupTwoSeatBlock()
        val before = snapshotOf(engine)

        engine.markUndoPoint()
        assertTrue(engine.playTileLeft(0, engine.players[0].hand.first { it.high == 6 && it.low == 3 }))
        assertTrue("La IA debe responder", engine.executeAIMove())
        assertTrue("Despues de jugar y de la respuesta, el tablero tiene dos fichas", engine.boardChain.size == 2)

        assertTrue(engine.canUndo)
        assertTrue(engine.undo())
        assertEquals("Deshacer devuelve la partida exactamente al estado anterior", before, snapshotOf(engine))
        assertFalse("Solo se puede deshacer una vez", engine.canUndo)
        assertFalse("Deshacer no deja la ronda cerrada", engine.isRoundOver)
    }

    @Test
    fun undo_isNotAvailableWithoutAPoint() {
        val engine = setupTwoSeatBlock()
        assertFalse(engine.canUndo)
        assertFalse(engine.undo())
    }

    @Test
    fun undo_pointIsDroppedWhenTheRoundStarts() {
        val engine = setupTwoSeatBlock()
        engine.markUndoPoint()
        assertTrue(engine.canUndo)
        engine.startNewRound()
        assertFalse("Una ronda nueva no puede deshacer la anterior", engine.canUndo)
    }

    @Test
    fun suggestedMove_isALegalPlay() {
        val engine = engine()
        engine.startNewRound()
        stageHands(
            engine,
            0 to listOf(tile(6, 3), tile(6, 5)),
            1 to listOf(tile(3, 2), tile(0, 4))
        )
        engine.currentPlayerIndex = 0
        engine.markUndoPoint()
        assertTrue(engine.playTileLeft(0, engine.players[0].hand.first { it.high == 6 && it.low == 3 }))
        assertTrue(engine.executeAIMove())
        // Vuelve a tocarle al asiento 0, que conserva el [6|5] conectable por la izquierda.
        val seat = engine.currentPlayerIndex
        assertEquals(0, seat)
        val suggestion = engine.suggestedMove(seat)
        assertNotNull("Con fichas jugables la pista debe proponer algo", suggestion)
        val play = suggestion as AiMove.Play
        assertTrue(
            "La pista debe ser una jugada legal: $play",
            engine.viewFor(seat).playableMoves.contains(play)
        )
    }

    @Test
    fun suggestedMove_isNullWhenThereIsNothingToPlay() {
        val engine = engine()
        engine.startNewRound()
        // Mesa abierta en 6/3 y una mano que no puede conectar con ninguno de los dos extremos.
        stageHands(
            engine,
            0 to listOf(tile(5, 4)),
            1 to listOf(tile(6, 3), tile(0, 0))
        )
        engine.currentPlayerIndex = 1
        assertTrue(engine.playTileLeft(1, engine.players[1].hand.first { it.high == 6 && it.low == 3 }))
        engine.currentPlayerIndex = 0
        assertEquals(0, engine.viewFor(0).playableMoves.size)
        assertEquals(null, engine.suggestedMove(0))
    }

    @Test
    fun tournamentDifficultyEscalatesWithTheVenue() {
        val difficulties = (0..6).map { difficultyForVenue(it) }
        assertEquals(Difficulty.RELAXED, difficulties.first())
        assertEquals(Difficulty.TOUGH, difficulties.last())
        // Nunca retrocede: el torneo se vuelve mas duro, no al reves.
        val order = listOf(Difficulty.RELAXED, Difficulty.STANDARD, Difficulty.TOUGH)
        difficulties.zipWithNext().forEach { (a, b) ->
            assertTrue("La dificultad no puede bajar de $a a $b", order.indexOf(b) >= order.indexOf(a))
        }
    }

    @Test
    fun everyChallengeCarriesItsVenueDifficulty() {
        for (venueIndex in 0..6) {
            val expected = difficultyForVenue(venueIndex)
            for (challengeIndex in 0..4) {
                assertEquals(
                    "Sede $venueIndex reto $challengeIndex",
                    expected,
                    TournamentCampaign.getChallenge(venueIndex, challengeIndex).difficulty
                )
            }
        }
    }
}
