package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Arrastre de mano y cierre inmediato al cruzar la meta.
 *
 * Las dos reglas cambian el resultado del juego, no solo su presentacion: quien abre tiene una
 * ventaja tactica real, y dejar seguir cuando alguien ya ha cruzado la meta puede dar la vuelta
 * a una partida que estaba decidida.
 */
class RoundFlowRulesTest {

    private fun allAiEngine(
        mode: GameMode = GameMode.BLOCK,
        targetScore: Int = 100_000,
        seed: Long = 77L
    ) = DominoEngine.allAi(gameMode = mode, targetScore = targetScore, seed = seed)

    /** Cierra una ronda entera con la mesa llena de IA, con guarda de pasos. */
    private fun playRoundToTheEnd(engine: DominoEngine, maxSteps: Int = 5_000): Boolean {
        var steps = 0
        while (!engine.isRoundOver && !engine.isMatchOver && steps < maxSteps) {
            engine.executeAIMove()
            steps++
        }
        return engine.isRoundOver
    }

    // --- Arrastre de mano ---

    @Test
    fun theRoundWinnerOpensTheNextRound() {
        // La regla central del punto 6.3: tener la salida es una ventaja, asi que la conserva
        // quien gana. Repetido en varias partidas para no depender de una mano concreta.
        for (seed in listOf(11L, 22L, 33L, 44L, 55L)) {
            val engine = allAiEngine(seed = seed)
            assertTrue("La ronda debe cerrarse (semilla $seed)", playRoundToTheEnd(engine))

            val winner = engine.winnerPlayerId
            assertNotNull("La ronda debe tener ganador (semilla $seed)", winner)

            val opener = engine.nextRoundOpener()
            assertEquals(
                "Quien gano debe ser quien abra (semilla $seed)",
                winner,
                opener
            )

            engine.startNewRound(openerOverride = opener)
            assertEquals(
                "Y debe abrir de verdad (semilla $seed)",
                winner,
                engine.currentPlayerIndex
            )
        }
    }

    @Test
    fun withoutAnOverrideTheHighestDoubleStillOpens() {
        // Empezar partida sin apertura impuesta mantiene la regla clasica del doble mas alto,
        // que es el comportamiento historico y no debe cambiar.
        val engine = allAiEngine()
        val highestDoubleHolder = engine.players
            .flatMap { player -> player.hand.map { player.id to it } }
            .filter { (_, tile) -> tile.isDouble }
            .maxByOrNull { (_, tile) -> tile.high }
            ?.first

        if (highestDoubleHolder != null) {
            assertEquals(
                "Sin apertura impuesta abre el doble mas alto",
                highestDoubleHolder,
                engine.currentPlayerIndex
            )
        }
    }

    @Test
    fun aBlockedRoundMakesTheLowestPipHandOpen() {
        // Si la ronda se cerro por tranca, abre quien menos puntos tiene en la mano, que es el
        // mismo criterio con el que se resolvio la ronda.
        val engine = allAiEngine(seed = 99L)
        assertTrue(playRoundToTheEnd(engine))

        if (engine.lastRoundWasBlocked) {
            val minPips = engine.players.minOf { it.hand.sumOf { t -> t.totalPips } }
            val opener = engine.nextRoundOpener()
            assertNotNull("Una tranca tambien tiene apertura", opener)
            assertEquals(
                "Abre la mano con menos puntos",
                minPips,
                engine.players.first { it.id == opener }.hand.sumOf { t -> t.totalPips }
            )
        }
    }

    @Test
    fun thereIsNoOpenerWhenNoRoundHasClosed() {
        // Defensivo: sin ronda cerrada no hay conclusion que sacar, y el llamante debe poder
        // distinguirlo para dejar actuar a la regla por defecto.
        val engine = allAiEngine()
        assertNull(engine.nextRoundOpener())
    }

    @Test
    fun anOutOfRangeOpenerFallsBackToTheDefaultRule() {
        // Un asiento inexistente no puede tumbar la partida ni dejar el turno en el aire.
        val engine = allAiEngine()
        engine.startNewRound(openerOverride = 99)
        assertTrue(
            "El turno debe quedar en un asiento valido",
            engine.currentPlayerIndex in engine.players.indices
        )
    }

    // --- Cierre inmediato al cruzar la meta ---

    @Test
    fun reachingTheTargetWithTablePointsEndsTheMatchAtOnce() {
        // En All Fives, cruzar la meta en una jugada debe cerrar la partida en ese instante.
        // Con objetivo 1, cualquier punto de extremos lo cruza en la primera jugada que puntue.
        val engine = allAiEngine(mode = GameMode.ALL_FIVES, targetScore = 1, seed = 5L)

        var steps = 0
        while (!engine.isMatchOver && steps < 5_000) {
            engine.executeAIMove()
            steps++
        }

        assertTrue("El match debe cerrarse al cruzar la meta", engine.isMatchOver)
        val leader = engine.players.maxByOrNull { it.score }!!
        assertTrue("Debe haber puntos de verdad", leader.score >= 1)
        assertTrue(
            "El equipo ganador debe ser el del jugador que cruzo la meta",
            engine.winningTeam == leader.team
        )
    }

    @Test
    fun aMatchEndedByTablePointsStillNamesTheWinner() {
        // Este era el defecto: cerrar por puntos fijaba el equipo pero NO el jugador, asi que la
        // pantalla final y el informe de carrera se quedaban sin ganador.
        val engine = allAiEngine(mode = GameMode.ALL_FIVES, targetScore = 1, seed = 7L)

        var steps = 0
        while (!engine.isMatchOver && steps < 5_000) {
            engine.executeAIMove()
            steps++
        }

        assertTrue(engine.isMatchOver)
        val winner = engine.winnerPlayerId
        assertNotNull("Un cierre por puntos debe nombrar al ganador", winner)
        assertEquals(
            "El ganador nombrado debe ser del equipo ganador",
            engine.winningTeam,
            engine.players.first { it.id == winner }.team
        )
    }

    @Test
    fun theMatchDoesNotContinueOnceTheTargetIsCrossed() {
        // Cruzada la meta, el motor no puede seguir aceptando jugadas: si siguiera, el rival
        // podria dar la vuelta a una partida ya decidida.
        val engine = allAiEngine(mode = GameMode.ALL_FIVES, targetScore = 1, seed = 9L)
        var steps = 0
        while (!engine.isMatchOver && steps < 5_000) {
            engine.executeAIMove()
            steps++
        }
        assertTrue(engine.isMatchOver)

        val scoresAtTheEnd = engine.players.map { it.score }
        val boardAtTheEnd = engine.boardChain.size
        assertTrue("El motor debe negarse a jugar mas", !engine.executeAIMove())
        assertEquals("Los marcadores no pueden cambiar", scoresAtTheEnd, engine.players.map { it.score })
        assertEquals("El tablero no puede crecer", boardAtTheEnd, engine.boardChain.size)
    }

    @Test
    fun aPointGameBelowTheTargetKeepsPlaying() {
        // Control del contrario: por debajo de la meta no se cierra nada.
        val engine = allAiEngine(mode = GameMode.ALL_FIVES, targetScore = 10_000, seed = 13L)
        var steps = 0
        while (!engine.isRoundOver && !engine.isMatchOver && steps < 5_000) {
            engine.executeAIMove()
            steps++
        }
        assertTrue("La ronda debe cerrarse con normalidad", engine.isRoundOver)
        assertTrue("Con la meta lejos, el match sigue vivo", !engine.isMatchOver)
    }
}
