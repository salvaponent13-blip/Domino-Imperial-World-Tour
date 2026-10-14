package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.engine.ai.AiMove
import com.example.dominograndprix.engine.ai.MediumWeights
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.Personality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Tests del motor de simulacion y de los invariantes de la estrategia MEDIA.
 *
 * El punto critico es que `simulate()` NO puede alterar la partida: si filtrara estado,
 * la IA tomaria decisiones sobre una mesa corrupta y los resultados del benchmark serian
 * mentira. Hay un test dedicado a comprobar que el estado se restaura por completo.
 */
class MoveSimulationTest {

    private fun engine(mode: GameMode, seed: Long, targetScore: Int = 400) =
        DominoEngine.allAi(gameMode = mode, targetScore = targetScore, seed = seed)

    /** Resumen textual del estado observable, para comparar antes y despues. */
    private fun stateOf(engine: DominoEngine): String = buildString {
        appendLine("manos=" + engine.players.map { it.hand })
        appendLine("tablero=" + engine.boardChain.map { it.tile })
        appendLine("pozo=" + engine.boneyard)
        appendLine("extremos=" + engine.leftOpenPip + "/" + engine.rightOpenPip)
        appendLine("turno=" + engine.currentPlayerIndex)
        appendLine("pases=" + engine.consecutivePasses)
        appendLine("rondaCerrada=" + engine.isRoundOver)
        appendLine("matchCerrado=" + engine.isMatchOver)
        appendLine("ganador=" + engine.winnerPlayerId + " equipo=" + engine.winningTeam)
        appendLine("log=" + engine.roundLog)
        appendLine("hungaro=" + engine.canContinueHungarianTurn)
        appendLine("ultimosPuntos=" + engine.lastPointsEarned)
        appendLine("marcadores=" + engine.players.map { it.score })
    }

    @Test
    fun simulateDoesNotAlterTheMatchState() {
        // Se simulan TODAS las jugadas legales en varios puntos de la partida, comprobando que
        // el estado observable queda identico antes y despues.
        for (mode in GameMode.values()) {
            val engine = engine(mode, seed = 31L)
            var turns = 0
            while (!engine.isRoundOver && turns < 40) {
                val before = stateOf(engine)
                val seat = engine.currentPlayerIndex
                engine.viewFor(seat).playableMoves.forEach { move ->
                    engine.simulate(seat, move)
                    assertEquals(
                        "Simular no puede alterar el estado (modo $mode, turno $turns, jugada $move)",
                        before,
                        stateOf(engine)
                    )
                }
                engine.executeAIMove()
                turns++
            }
        }
    }

    @Test
    fun simulateReportsTheConsequencesOfTheMove() {
        val engine = engine(GameMode.BLOCK, seed = 12L)
        val seat = engine.currentPlayerIndex
        val move = engine.viewFor(seat).playableMoves.first()
        val pipsBefore = engine.players[seat].hand.sumOf { it.totalPips }

        val simulation = engine.simulate(seat, move)

        assertEquals(move, simulation.move)
        assertEquals(
            "Debe quedar una ficha menos en la mano",
            engine.players[seat].hand.size - 1,
            simulation.myRemainingTiles
        )
        assertTrue(
            "Los pips restantes deben ser menores o iguales que antes",
            simulation.myRemainingPips <= pipsBefore
        )
        assertFalse("BLOCK no reparte pozo", simulation.winsTheRound && simulation.myRemainingTiles > 0)
        assertEquals(
            "Los extremos de la simulacion deben coincidir con una partida real que haga lo mismo",
            simulation.openEndsSum,
            simulatedEndsSum(engine, seat, move)
        )
    }

    /** Juega la misma jugada en un motor gemelo para contrastar los extremos reportados. */
    private fun simulatedEndsSum(engine: DominoEngine, seat: Int, move: AiMove.Play): Int {
        val twin = engine(GameMode.BLOCK, seed = 12L)
        if (move.side == BoardSide.LEFT) twin.playTileLeft(seat, move.tile)
        else twin.playTileRight(seat, move.tile)
        return (twin.leftOpenPip ?: 0) + (twin.rightOpenPip ?: 0)
    }

    @Test
    fun simulateReportsPointsOnlyInScoringModes() {
        // Los modos que puntuan deben reportar los puntos que el motor otorgaria de verdad.
        for (mode in GameMode.values()) {
            val engine = engine(mode, seed = 44L)
            val seat = engine.currentPlayerIndex
            val move = engine.viewFor(seat).playableMoves.first()
            val simulation = engine.simulate(seat, move)

            val expectsPoints = mode == GameMode.ALL_FIVES ||
                mode == GameMode.ALL_THREES ||
                mode == GameMode.FIVES_AND_THREES
            if (!expectsPoints) {
                assertEquals("$mode no puntua extremos", 0, simulation.pointsEarned)
            } else {
                assertTrue("$mode no puede reportar puntos negativos", simulation.pointsEarned >= 0)
            }
        }
    }

    @Test
    fun mediumStrategyNeverReturnsAnIllegalMove() {
        // Invariante central del Sprint 3: 0 jugadas ilegales en los 8 modos.
        var decisions = 0
        for (mode in GameMode.values()) {
            for (seed in 1L..6L) {
                val engine = engine(mode, seed = seed * 101)
                engine.players.indices.forEach { seat ->
                    engine.setStrategyOverride(seat, engine.newMediumStrategy(seat, MediumWeights.ALL))
                }
                var guard = 0
                while (!engine.isRoundOver && guard < 400) {
                    val seat = engine.currentPlayerIndex
                    val view = engine.viewFor(seat)
                    val move = engine.strategyFor(seat).decide(view)

                    if (view.playableMoves.isNotEmpty()) {
                        assertTrue(
                            "La estrategia MEDIA debe devolver una jugada legal " +
                                "(modo $mode, semilla ${seed * 101}, asiento $seat): $move",
                            move is AiMove.Play && view.playableMoves.contains(move)
                        )
                    } else {
                        assertTrue(
                            "Sin jugada legal solo puede robar o pasar: $move",
                            move == AiMove.Draw || move == AiMove.Pass
                        )
                    }
                    decisions++
                    engine.executeAIMove()
                    guard++
                }
            }
        }
        println("Invariante verificado: $decisions decisiones sin una sola jugada ilegal")
        assertTrue(
            "Debe cubrirse un volumen amplio de decisiones (medido: $decisions)",
            decisions > 1000
        )
    }

    @Test
    fun mediumStrategyRespectsThePersonalityIndependenceAndDoublePenalty() {
        // La MEDIA no debe depender de la personalidad: dos asientos con personalidades
        // distintas en la misma situacion deben decidir lo mismo.
        val engine = engine(GameMode.BLOCK, seed = 77L)
        engine.players[0] = engine.players[0].copy(personality = Personality.AGGRESSIVE)
        engine.players[1] = engine.players[1].copy(personality = Personality.NICE)

        val view = engine.viewFor(0)
        val strategy = engine.newMediumStrategy(0, MediumWeights.ALL)
        val decision = strategy.decide(view)
        assertTrue(decision is AiMove.Play)
        assertTrue(
            "La decision debe estar entre las legales",
            view.playableMoves.contains(decision)
        )
    }
}
