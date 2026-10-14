package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Reproducibilidad del motor.
 *
 * Sin un generador aleatorio inyectable no se puede reproducir una partida, y sin eso no
 * hay ni benchmark de IA ni desafio diario: cada ejecucion daria un reparto distinto y
 * cualquier comparacion de estrategias seria ruido.
 */
class EngineReproducibilityTest {

    private fun engine(mode: GameMode = GameMode.DRAW, seed: Long? = null): DominoEngine =
        if (seed == null) {
            DominoEngine.allAi(gameMode = mode, targetScore = 100)
        } else {
            DominoEngine.allAi(gameMode = mode, targetScore = 100, seed = seed)
        }

    private fun handsOf(engine: DominoEngine): List<List<DominoTile>> =
        engine.players.map { it.hand }

    @Test
    fun sameSeedProducesTheSameDeal() {
        val first = engine(seed = 42L)
        val second = engine(seed = 42L)

        assertEquals(
            "Dos motores con la misma semilla deben repartir las mismas manos",
            handsOf(first),
            handsOf(second)
        )
        assertEquals(
            "El pozo restante tambien debe coincidir",
            first.boneyard,
            second.boneyard
        )
        assertEquals(
            "El jugador que abre debe coincidir",
            first.currentPlayerIndex,
            second.currentPlayerIndex
        )
    }

    @Test
    fun sameSeedSurvivesTheSecondRoundToo() {
        val first = engine(seed = 7L)
        val second = engine(seed = 7L)

        // El generador avanza con cada barajado: la segunda ronda debe seguir siendo
        // identica entre ambos motores, y distinta de la primera (avanza de verdad).
        val firstRoundHands = handsOf(first)
        first.startNewRound()
        second.startNewRound()

        assertEquals(
            "La segunda ronda debe ser identica entre motores con la misma semilla",
            handsOf(first),
            handsOf(second)
        )
        assertEquals(first.boneyard, second.boneyard)
        assertNotEquals(
            "El generador debe avanzar: no puede repartir lo mismo dos rondas seguidas",
            firstRoundHands,
            handsOf(first)
        )
    }

    @Test
    fun differentSeedsProduceDifferentDeals() {
        val first = engine(seed = 1L)
        val second = engine(seed = 2L)

        assertNotEquals(
            "Con semillas distintas el reparto debe diferir",
            handsOf(first),
            handsOf(second)
        )
    }

    @Test
    fun seededGamePlaysIdenticallyToTheEnd() {
        // Dos partidas completas con la misma semilla deben acabar igual: mismo ganador,
        // mismas puntuaciones y mismo numero de pasos. Es la base del benchmark A/B.
        val first = engine(mode = GameMode.BLOCK, seed = 99L)
        val second = engine(mode = GameMode.BLOCK, seed = 99L)

        var steps = 0
        while (!first.isRoundOver && steps < 500) {
            first.executeAIMove()
            second.executeAIMove()
            assertEquals(
                "Las partidas deben divergir solo si el generador no esta fijado (paso $steps)",
                handsOf(first),
                handsOf(second)
            )
            steps++
        }

        assertTrue("La ronda debe terminar en $steps pasos", first.isRoundOver)
        assertEquals(first.winnerPlayerId, second.winnerPlayerId)
        assertEquals(first.players.map { it.score }, second.players.map { it.score })
        assertEquals(first.boardChain.map { it.tile }, second.boardChain.map { it.tile })
    }

    @Test
    fun everyModeDealsTheSameWithAFixedSeed() {
        // La reproducibilidad no puede depender del modo: unos reparten 5 fichas y otros 7.
        for (mode in GameMode.values()) {
            val first = engine(mode = mode, seed = 1234L)
            val second = engine(mode = mode, seed = 1234L)
            assertEquals("El modo $mode debe repartir igual con la misma semilla",
                handsOf(first), handsOf(second))
            assertEquals("El modo $mode debe dejar el mismo pozo",
                first.boneyard, second.boneyard)
        }
    }

    /**
     * El reparto no puede depender del reloj: dos motores creados en momentos distintos con
     * la misma semilla deben coincidir. Este test es el que falla mientras el motor siga
     * barajando con Random(System.currentTimeMillis()).
     */
    @Test
    fun dealDoesNotDependOnTheClock() {
        val first = engine(seed = 2024L)
        Thread.sleep(25)
        val second = engine(seed = 2024L)

        assertEquals(handsOf(first), handsOf(second))
        assertEquals(first.boneyard, second.boneyard)
    }

    @Test
    fun playerViewExposesWhatAStrategyNeeds() {
        val engine = engine(mode = GameMode.BLOCK, seed = 5L)
        val view = engine.viewFor(engine.currentPlayerIndex)

        assertEquals(engine.currentPlayerIndex, view.playerIndex)
        assertEquals(GameMode.BLOCK, view.gameMode)
        assertEquals(engine.players[view.playerIndex].hand, view.myHand)
        assertEquals(engine.boneyard.size, view.boneyardSize)
        assertEquals(engine.leftOpenPip, view.leftOpenPip)
        assertEquals(engine.rightOpenPip, view.rightOpenPip)
        assertEquals(engine.players.map { it.score }, view.scores)
        assertTrue("Sin ramas asignadas en BLOCK", view.ownBranch == null)
    }

    @Test
    fun playerViewInDrawSpecialReportsTheBranchAndTheInvasionWindow() {
        val engine = engine(mode = GameMode.DRAW_SPECIAL, seed = 11L)

        // Se abre la mesa con cualquier ficha legal del jugador que tiene el turno.
        val opener = engine.currentPlayerIndex
        val tile = engine.getPlayableTiles(opener).first()
        val sides = engine.getPlayableSides(opener, tile)
        if (sides.contains(BoardSide.LEFT)) engine.playTileLeft(opener, tile)
        else engine.playTileRight(opener, tile)

        val owner = engine.viewFor(opener)
        assertEquals("Quien abre se queda la rama izquierda", BoardSide.LEFT, owner.ownBranch)

        // El siguiente asiento tiene la rama contraria y NO puede invadir sin un pase previo.
        val other = (opener + 1) % engine.players.size
        val otherView = engine.viewFor(other)
        assertEquals(BoardSide.RIGHT, otherView.ownBranch)
        assertNull("Sin pase previo no hay invasion", otherView.invadableBranch)

        // Se fuerza un pase del dueño de la rama derecha: se le deja sin jugada posible.
        engine.players[other] = engine.players[other].copy(hand = listOf(DominoTile(6, 6)))
        engine.rightOpenPip?.let { pip ->
            if (pip == 6) engine.rightOpenPip = 5
        }
        engine.currentPlayerIndex = other
        assertTrue(
            "El asiento debe quedarse sin jugada para que el pase sea realista",
            engine.getPlayableTiles(other).isEmpty()
        )
        engine.passTurn(other)

        // Ahora el asiento que abrio SI puede invadir la rama que el otro acaba de dejar.
        engine.currentPlayerIndex = opener
        val reopened = engine.viewFor(opener)
        assertEquals(
            "Tras el pase del dueño, la rama contraria queda invadible",
            BoardSide.RIGHT,
            reopened.invadableBranch
        )
    }

    @Test
    fun playerViewDoesNotExposeOtherPlayersHands() {
        // Garantia de diseno, no de comportamiento: si alguien anade las manos ajenas al
        // PlayerView, la IA tendria informacion que un humano no tiene y el benchmark
        // mediria una IA tramposa.
        val fields = com.example.dominograndprix.engine.ai.PlayerView::class.java.declaredFields
            .map { it.name }

        assertTrue("El PlayerView debe exponer la mano propia", fields.contains("myHand"))
        assertTrue(
            "El PlayerView NO debe exponer manos ajenas (encontrado: $fields)",
            fields.none { it.contains("hands", ignoreCase = true) || it.contains("opponent", ignoreCase = true) }
        )
    }
}
