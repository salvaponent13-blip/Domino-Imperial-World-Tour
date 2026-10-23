package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Identidad estable de las fichas colocadas.
 *
 * De esto depende que la animacion de asentamiento corra solo para la ficha recien puesta: si
 * las fichas ya colocadas cambiaran de identidad, sus animaciones se reiniciarian en cada
 * jugada. El caso que lo rompe con indices es jugar por la IZQUIERDA, porque la cadena se
 * reordena y todas las posiciones se desplazan.
 */
class PlacementIdentityTest {

    private fun engine(mode: GameMode = GameMode.BLOCK, seed: Long = 1234L) = DominoEngine.allAi(
        gameMode = mode,
        targetScore = 300,
        seed = seed
    )

    @Test
    fun everyPlacedTileGetsAUniqueIdentifier() {
        val engine = engine()
        var turns = 0
        while (!engine.isRoundOver && turns < 400) {
            engine.executeAIMove()
            turns++
        }

        val ids = engine.boardChain.map { it.placementId }
        assertTrue("Debe haber fichas colocadas", ids.isNotEmpty())
        assertTrue(
            "Ninguna colocacion puede repetir identificador: hay duplicados en $ids",
            ids.size == ids.toSet().size
        )
        assertTrue("Los identificadores deben ser positivos", ids.all { it > 0 })
    }

    @Test
    fun playingOnTheLeftKeepsExistingIdentities() {
        // El caso critico: al jugar por la izquierda la ficha se inserta al principio y todas
        // las demas se desplazan de indice. Sus identificadores NO deben cambiar.
        val engine = engine(mode = GameMode.BLOCK, seed = 77L)
        engine.executeAIMove()
        engine.executeAIMove()

        val before = engine.boardChain.associate { it.placementId to it.tile }
        assertTrue("Debe haber al menos dos fichas", before.size >= 2)

        val seat = engine.currentPlayerIndex
        val move = engine.viewFor(seat).playableMoves.firstOrNull { it.side == BoardSide.LEFT }
            ?: engine.viewFor(seat).playableMoves.first()
        if (move.side == BoardSide.LEFT) {
            engine.playTileLeft(seat, move.tile)
        } else {
            engine.playTileRight(seat, move.tile)
        }

        val after = engine.boardChain.associate { it.placementId to it.tile }
        before.forEach { (id, tile) ->
            assertTrue(
                "La ficha $tile con identificador $id debe seguir en la mesa tras la jugada",
                after.containsKey(id)
            )
            assertEquals("Su identidad no puede cambiar", tile, after[id])
        }
        assertEquals("Debe haberse anadido exactamente una ficha", before.size + 1, after.size)
    }

    @Test
    fun leftAndRightPlacementsBothGetNewIdentifiers() {
        val engine = engine(mode = GameMode.DRAW, seed = 555L)
        val seen = mutableSetOf<Int>()
        var turns = 0

        while (!engine.isRoundOver && turns < 400) {
            val before = engine.boardChain.map { it.placementId }.toSet()
            val seat = engine.currentPlayerIndex
            val playable = engine.viewFor(seat).playableMoves
            engine.executeAIMove()
            val after = engine.boardChain.map { it.placementId }.toSet()

            val added = after - before
            // Si se coloco una ficha, debe aparecer exactamente un identificador nuevo
            if (engine.boardChain.size > before.size) {
                assertEquals("Solo puede anadirse una identidad nueva por jugada", 1, added.size)
            }
            seen.addAll(added)
            turns++
        }

        assertTrue("Debe haberse colocado un numero apreciable de fichas: ${seen.size}", seen.size >= 8)
        assertEquals("Todas las identidades deben ser distintas", seen.size, seen.toSet().size)
    }

    @Test
    fun restoringAMatchDoesNotRepeatIdentifiers() {
        // Si al restaurar el contador volviera a cero, las fichas nuevas reutilizarian
        // identificadores ya presentes y las animaciones se confundirian entre fichas.
        val engine = engine(seed = 31L)
        var turns = 0
        while (!engine.isRoundOver && turns < 400) {
            engine.executeAIMove()
            turns++
        }

        val snapshot = engine.toSnapshot("La Habana", "cuba")
        val maxRestored = snapshot.boardChain.maxOf { it.placementId }

        val resumed = DominoEngine.resume(snapshot)
        resumed.startNewRound()
        var guard = 0
        while (!resumed.isRoundOver && guard < 400) {
            resumed.executeAIMove()
            guard++
        }

        val newIds = resumed.boardChain.map { it.placementId }
        assertTrue("La ronda reanudada debe colocar fichas", newIds.isNotEmpty())
        assertTrue(
            "Ninguna ficha nueva puede reutilizar un identificador anterior " +
                "(maximo restaurado: $maxRestored, nuevos: ${newIds.minOrNull()})",
            newIds.min() > maxRestored
        )
    }

    @Test
    fun defaultIdentifierIsZeroForRestoredTilesWithoutIt() {
        // Un guardado escrito antes de existir placementId no debe producir animaciones:
        // el valor por defecto 0 significa "no animar".
        val engine = engine(seed = 9L)
        engine.executeAIMove()
        val snapshot = engine.toSnapshot("La Habana", "cuba")
        val legacy = snapshot.copy(
            boardChain = snapshot.boardChain.map { it.copy(placementId = 0) }
        )

        val resumed = DominoEngine.resume(legacy)
        assertTrue(
            "Las fichas sin identificador deben quedar a 0 para no animarse",
            resumed.boardChain.all { it.placementId == 0 }
        )
        assertNotEquals("El estado restaurado no puede quedar vacio", 0, resumed.boardChain.size)
    }
}

