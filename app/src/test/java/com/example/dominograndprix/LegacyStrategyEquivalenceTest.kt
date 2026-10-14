package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.engine.ai.EasyStrategy
import com.example.dominograndprix.engine.ai.LegacyStrategy
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.Personality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Prueba de que la IA sigue comportandose EXACTAMENTE igual que antes de conectar el motor
 * al contrato `DominoStrategy`.
 *
 * Sin esto, "no se ha roto nada" seria una afirmacion sin respaldo: los tests de reglas
 * seguirian pasando aunque la IA hubiera empezado a jugar distinto.
 *
 * El metodo: se juegan partidas completas con un motor de referencia que NO usa estrategias
 * inyectadas (la ruta historica, intacta dentro de `DominoEngine`) y se comparan turno a
 * turno con otro motor identico pero resuelto a traves de `setStrategyOverride`. Si la
 * abstraccion cambia una sola decision, la traza diverge y el test lo senala.
 *
 * Al usar el mismo motor como referencia, no hay logica duplicada que pueda desincronizarse
 * del original: lo unico que cambia entre ambos bandos es POR DONDE se toma la decision.
 */
class LegacyStrategyEquivalenceTest {

    private fun engine(mode: GameMode, seed: Long, targetScore: Int = 400): DominoEngine =
        DominoEngine(gameMode = mode, targetScore = targetScore, patternIndex = 3, random = Random(seed))

    /**
     * Motor de control: mismas condiciones, pero todos los asientos resueltos a traves de la
     * estrategia inyectada en lugar de la ruta interna.
     */
    private fun withLegacyStrategyOverrides(source: DominoEngine): DominoEngine {
        source.players.indices.forEach { seat ->
            source.setStrategyOverride(seat, LegacyStrategy(source.players[seat].personality))
        }
        return source
    }

    @Test
    fun legacyStrategyPlaysIdenticallyToTheFrozenEngineRoute() {
        var comparedMoves = 0
        var gamesPlayed = 0

        for (mode in GameMode.values()) {
            for (seed in 1L..10L) {
                val reference = engine(mode, seed)                    // ruta historica
                val underTest = withLegacyStrategyOverrides(engine(mode, seed)) // via estrategia

                var guard = 0
                while (!reference.isRoundOver && guard < 500) {
                    assertEquals(
                        "Mismo asiento al turno (modo $mode, semilla $seed)",
                        reference.currentPlayerIndex,
                        underTest.currentPlayerIndex
                    )

                    assertEquals(
                        "Misma decision (modo $mode, semilla $seed, turno $guard)",
                        reference.executeAIMove(),
                        underTest.executeAIMove()
                    )

                    assertEquals(
                        "Mismo estado de mesa tras el turno (modo $mode, semilla $seed, turno $guard)",
                        reference.boardChain.map { it.tile },
                        underTest.boardChain.map { it.tile }
                    )
                    assertEquals(
                        "Mismos extremos abiertos (modo $mode, semilla $seed, turno $guard)",
                        listOf(reference.leftOpenPip, reference.rightOpenPip),
                        listOf(underTest.leftOpenPip, underTest.rightOpenPip)
                    )
                    assertEquals(
                        "Mismo pozo (modo $mode, semilla $seed, turno $guard)",
                        reference.boneyard,
                        underTest.boneyard
                    )
                    assertEquals(
                        "Mismas manos (modo $mode, semilla $seed, turno $guard)",
                        reference.players.map { it.hand },
                        underTest.players.map { it.hand }
                    )
                    comparedMoves++
                    guard++
                }

                assertTrue("La partida debe terminar (modo $mode, semilla $seed)", reference.isRoundOver)
                assertEquals(
                    "Mismo ganador (modo $mode, semilla $seed)",
                    reference.winnerPlayerId,
                    underTest.winnerPlayerId
                )
                assertEquals(
                    "Mismos marcadores (modo $mode, semilla $seed)",
                    reference.players.map { it.score },
                    underTest.players.map { it.score }
                )
                gamesPlayed++
            }
        }

        println("Equivalencia verificada: $comparedMoves turnos en $gamesPlayed partidas completas")
        assertTrue(
            "Debe compararse un volumen significativo de turnos, no cuatro casos",
            comparedMoves > 1000
        )
    }

    @Test
    fun theTwoRoutesActuallyDifferWhenTheStrategyIsEasy() {
        // Control imprescindible: si la ruta de estrategia fuera ignorada por el motor, el
        // test anterior pasaria igual y no probaria nada. Con EasyStrategy (aleatoria) la
        // traza DEBE divergir.
        val reference = engine(GameMode.BLOCK, seed = 21L)
        val randomized = engine(GameMode.BLOCK, seed = 21L)
        randomized.players.indices.forEach { seat ->
            randomized.setStrategyOverride(seat, EasyStrategy(Random(21L + seat)))
        }

        var guard = 0
        while (!reference.isRoundOver && guard < 500) {
            reference.executeAIMove()
            randomized.executeAIMove()
            guard++
        }

        assertNotEquals(
            "EasyStrategy no puede producir exactamente la misma partida que la ruta historica; " +
                "si coinciden, el motor no esta usando la estrategia inyectada",
            reference.boardChain.map { it.tile },
            randomized.boardChain.map { it.tile }
        )
    }

    @Test
    fun clearingOverridesReturnsToTheHistoricalRoute() {
        val engine = engine(GameMode.DRAW, seed = 8L)
        engine.setStrategyOverride(0, EasyStrategy(Random(1L)))
        assertTrue(
            "El override deberia estar activo",
            engine.strategyFor(0) is EasyStrategy
        )
        assertFalse(
            "Sin override, el asiento usa la estrategia historica",
            engine.strategyFor(1) is EasyStrategy
        )

        engine.clearStrategyOverride(0)
        assertTrue(
            "Tras limpiar el override vuelve la referencia",
            engine.strategyFor(0) is LegacyStrategy
        )

        engine.setStrategyOverride(2, EasyStrategy(Random(2L)))
        engine.clearAllStrategyOverrides()
        engine.players.indices.forEach { seat ->
            assertTrue(
                "El asiento $seat debe volver a la referencia",
                engine.strategyFor(seat) is LegacyStrategy
            )
        }
    }

    @Test
    fun legacyStrategyRespectsTheSeatPersonality() {
        // La equivalencia depende de que cada asiento reciba su personalidad: el agresivo
        // debe seguir siendo el que mas pips juega. La mesa por defecto tiene un AGGRESSIVE.
        val engine = engine(GameMode.DRAW, seed = 4L)
        val aggressive = engine.players.filter { it.personality == Personality.AGGRESSIVE }
        assertEquals("La mesa por defecto tiene un asiento agresivo", 1, aggressive.size)

        val seat = aggressive.first().id
        val strategy = engine.strategyFor(seat)
        assertTrue("El asiento agresivo debe resolverse como LegacyStrategy", strategy is LegacyStrategy)

        // Y su decision debe ser la de mas pips cuando hay varias jugables.
        val view = engine.viewFor(seat)
        if (view.playableMoves.size > 1) {
            val decided = strategy.decide(view)
            assertTrue("Debe decidir una jugada", decided is com.example.dominograndprix.engine.ai.AiMove.Play)
            val tile = (decided as com.example.dominograndprix.engine.ai.AiMove.Play).tile
            assertEquals(
                "El asiento agresivo debe elegir la ficha de mas pips",
                view.playableMoves.maxOf { it.tile.totalPips },
                tile.totalPips
            )
        }
    }
}
