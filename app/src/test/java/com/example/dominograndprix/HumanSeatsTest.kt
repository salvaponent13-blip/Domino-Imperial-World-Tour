package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.Personality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Asientos humanos (parametrizacion para pass-and-play local).
 *
 * El motor no juega por una persona: [DominoEngine.executeAIMove] se niega a actuar cuando el
 * turno pertenece a un asiento humano, y es la interfaz quien coloca esas fichas. Estos tests
 * fijan ese contrato antes de tocar la UI, porque una regresion aqui haria que la IA moviera
 * fichas de un jugador sin que se entere.
 */
class HumanSeatsTest {

    private fun engine(
        humanSeats: Set<Int>,
        mode: GameMode = GameMode.BLOCK,
        seed: Long = 500L,
        personalities: List<Personality> = DominoEngine.DEFAULT_PERSONALITIES
    ) = DominoEngine(
        gameMode = mode,
        targetScore = 300,
        random = Random(seed),
        randomSeed = seed,
        personalities = personalities,
        humanPlayerIndices = humanSeats
    )

    /** Juega un turno humano cualquiera: basta una jugada legal, robar o pasar. */
    private fun playHumanTurn(engine: DominoEngine): Boolean {
        val seat = engine.currentPlayerIndex
        assertTrue("El turno deberia ser humano", engine.isCurrentPlayerHuman)
        val moves = engine.viewFor(seat).playableMoves
        if (moves.isNotEmpty()) {
            val move = moves.first()
            return engine.playTile(seat, move.tile, move.side)
        }
        return if (engine.gameMode.allowsDraw && engine.boneyard.isNotEmpty()) {
            engine.drawTile(seat, 0) != null
        } else {
            engine.passTurn(seat)
            true
        }
    }

    @Test
    fun defaultConfigurationIsOneHumanAtSeatZero() {
        // El modo carrera debe seguir siendo exactamente lo de antes.
        val engine = engine(setOf(0))
        assertEquals(setOf(0), engine.humanPlayerIndices)
        assertEquals(setOf(1, 2, 3), engine.aiPlayerIndices)
        assertEquals(1, engine.players.count { it.isHuman })
        assertTrue("El asiento 0 es humano", engine.players[0].isHuman)
        assertFalse("Los demas son automaticos", engine.players[1].isHuman)
    }

    @Test
    fun aiRefusesToPlayForAHumanSeat() {
        // Contrato central: si el turno es de una persona, el motor no mueve.
        val engine = engine(setOf(0, 1, 2, 3))
        val boardBefore = engine.boardChain.size
        val handsBefore = engine.players.map { it.hand }

        assertTrue("El turno debe ser humano", engine.isCurrentPlayerHuman)
        assertFalse(
            "executeAIMove no puede actuar en un asiento humano",
            engine.executeAIMove()
        )
        assertEquals("La mesa no puede cambiar", boardBefore, engine.boardChain.size)
        assertEquals("Las manos no pueden cambiar", handsBefore, engine.players.map { it.hand })
    }

    @Test
    fun fourHumansPlayAWholeRoundWithoutAnyAiMove() {
        val engine = engine(setOf(0, 1, 2, 3))
        var aiMoves = 0
        var humanTurns = 0
        var guard = 0

        while (!engine.isRoundOver && guard < 2000) {
            if (engine.isCurrentPlayerHuman) {
                humanTurns++
                playHumanTurn(engine)
            } else {
                aiMoves++
                engine.executeAIMove()
            }
            guard++
        }

        assertTrue("La ronda debe cerrarse en $guard turnos", engine.isRoundOver)
        assertEquals("La IA no puede haber movido ni una vez", 0, aiMoves)
        assertTrue("Debe haber turnos humanos de sobra: $humanTurns", humanTurns > 5)
    }

    @Test
    fun twoHumansOnlyLetTheAiPlayItsOwnSeats() {
        // 1v1: los asientos 0 y 2 son personas, el 1 y el 3 son automaticos.
        val engine = engine(setOf(0, 2))
        val aiSeatsUsed = mutableSetOf<Int>()

        var guard = 0
        while (!engine.isRoundOver && guard < 2000) {
            val seat = engine.currentPlayerIndex
            if (engine.isCurrentPlayerHuman) {
                assertTrue("Solo 0 y 2 son humanos, pero toca $seat", seat in setOf(0, 2))
                playHumanTurn(engine)
            } else {
                assertTrue("Solo 1 y 3 son automaticos, pero toca $seat", seat in setOf(1, 3))
                aiSeatsUsed.add(seat)
                engine.executeAIMove()
            }
            guard++
        }

        assertTrue("La ronda debe cerrarse", engine.isRoundOver)
        assertTrue(
            "La IA debe haber jugado en sus asientos: $aiSeatsUsed",
            aiSeatsUsed.isNotEmpty() && aiSeatsUsed.all { it in setOf(1, 3) }
        )
    }

    @Test
    fun twoHumansInTheSameTeamOnlyLetTheAiPlayTheRivals() {
        // 2v2 local: el equipo del jugador son personas (0 y 2) y los rivales son la IA.
        val engine = engine(setOf(0, 2))
        var steps = 0
        while (!engine.isRoundOver && steps < 2000) {
            if (engine.isCurrentPlayerHuman) playHumanTurn(engine) else engine.executeAIMove()
            steps++
        }
        assertTrue(engine.isRoundOver)
        assertTrue(
            "Los asientos humanos deben seguir siendo las mismas personas al final",
            engine.players.filter { it.isHuman }.map { it.id }.toSet() == setOf(0, 2)
        )
    }

    @Test
    fun oneHumanBehavesExactlyAsBeforeWithNoHumanOrAiFlagConfusion() {
        // Con un solo humano, cualquier turno que no sea el 0 debe resolverlo la IA sin
        // intervencion, y el turno humano debe rechazar la via automatica.
        val engine = engine(setOf(0))
        var guard = 0
        var humanTurns = 0
        while (!engine.isRoundOver && guard < 2000) {
            if (engine.isCurrentPlayerHuman) {
                assertEquals("Solo el asiento 0 es humano", 0, engine.currentPlayerIndex)
                humanTurns++
                playHumanTurn(engine)
            } else {
                assertTrue("La IA debe poder resolver este turno", engine.executeAIMove())
            }
            guard++
        }
        assertTrue("La ronda debe cerrarse", engine.isRoundOver)
        assertTrue("El humano debe haber tenido turnos: $humanTurns", humanTurns > 0)
    }

    @Test
    fun allModesResolveWithFourHumanSeats() {
        // El contrato de asientos no puede depender del modo: se comprueba en los 8.
        for (mode in GameMode.values()) {
            val engine = engine(setOf(0, 1, 2, 3), mode = mode, seed = mode.ordinal * 17L + 3)
            var steps = 0
            while (!engine.isRoundOver && steps < 2500) {
                if (engine.isCurrentPlayerHuman) playHumanTurn(engine) else engine.executeAIMove()
                steps++
            }
            assertTrue("El modo $mode deberia cerrar la ronda (pasos: $steps)", engine.isRoundOver)
        }
    }

    @Test
    fun seatsSurviveBeingSavedAndResumed() {
        // Una partida local reanudada debe seguir teniendo personas en los mismos asientos, o
        // la IA empezaria a mover fichas de los jugadores.
        val engine = engine(setOf(0, 2), mode = GameMode.DRAW, seed = 91L)
        var steps = 0
        while (!engine.isRoundOver && steps < 2000) {
            if (engine.isCurrentPlayerHuman) playHumanTurn(engine) else engine.executeAIMove()
            steps++
        }
        val snapshot = engine.toSnapshot("La Habana", "cuba", 100)
        assertEquals(setOf(0, 2), snapshot.humanPlayerIndices)

        val resumed = DominoEngine.resume(snapshot)
        assertEquals("Los asientos humanos deben conservarse", setOf(0, 2), resumed.humanPlayerIndices)
        assertEquals(
            "Las personas deben seguir en sus asientos",
            setOf(0, 2),
            resumed.players.filter { it.isHuman }.map { it.id }.toSet()
        )
        resumed.startNewRound()
        assertTrue(
            "Tras reanudar, el turno debe volver a un asiento humano si le toca",
            resumed.isCurrentPlayerHuman || resumed.currentPlayerIndex in setOf(1, 3)
        )
    }

    @Test
    fun snapshotFromOlderVersionDefaultsToOneHumanSeat() {
        // Compatibilidad: un guardado sin asientos humanos debe leerse como un jugador.
        val engine = engine(setOf(0), seed = 5L)
        engine.executeAIMove()
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val encoded = json.encodeToString(
            com.example.dominograndprix.data.MatchSnapshot.serializer(),
            engine.toSnapshot("La Habana", "cuba", 100)
        )
        // Se elimina el campo como si viniera de una version anterior.
        val withoutField = encoded.replace(Regex(",\"humanPlayerIndices\":\\[[^]]*]"), "")
        val loaded = json.decodeFromString(
            com.example.dominograndprix.data.MatchSnapshot.serializer(),
            withoutField
        )
        assertEquals(
            "Sin el campo, debe asumirse el modo de un jugador",
            setOf(0),
            loaded.humanPlayerIndices
        )
    }
}
