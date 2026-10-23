package com.example.dominograndprix

import com.example.dominograndprix.data.MatchSnapshot
import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.Personality
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Guardado y reanudacion de la partida en curso (enfoque 80/20 del plan).
 *
 * Lo que se comprueba:
 * - El snapshot sobrevive a la serializacion de ida y vuelta sin perder nada.
 * - Una partida reanudada conserva marcadores, tablero, manos y ganador de la ronda.
 * - Continuar la partida desde el snapshot la deja jugable: la siguiente ronda se reparte
 *   y el match puede terminar.
 * - Un snapshot de otra version se descarta en lugar de intentar interpretarlo.
 */
class MatchSnapshotTest {

    private val json = Json { encodeDefaults = true }

    private fun engine(
        mode: GameMode = GameMode.BLOCK,
        seed: Long = 4242L,
        difficulty: Difficulty = Difficulty.STANDARD
    ) = DominoEngine.allAi(
        gameMode = mode,
        targetScore = 300,
        seed = seed,
        difficulty = difficulty
    )

    /** Juega hasta cerrar al menos una ronda, que es el punto que se guarda. */
    private fun playUntilRoundClosed(engine: DominoEngine, maxTurns: Int = 600): Int {
        var turns = 0
        while (!engine.isRoundOver && turns < maxTurns) {
            engine.executeAIMove()
            turns++
        }
        assertTrue("La ronda deberia cerrarse en $turns turnos", engine.isRoundOver)
        return turns
    }

    @Test
    fun snapshotSurvivesTheJsonRoundTrip() {
        val engine = engine()
        playUntilRoundClosed(engine)

        val original = engine.toSnapshot(cityName = "La Habana", venueId = "cuba")
        val restored = json.decodeFromString(
            MatchSnapshot.serializer(),
            json.encodeToString(MatchSnapshot.serializer(), original)
        )

        assertEquals("El snapshot entero debe sobrevivir al viaje", original, restored)
        assertEquals(MatchSnapshot.CURRENT_SCHEMA_VERSION, restored.schemaVersion)
        assertTrue("Un snapshot recien creado debe ser compatible", restored.isCompatible)
    }

    @Test
    fun snapshotCapturesProgressAndIdentities() {
        val engine = engine(mode = GameMode.ALL_FIVES, seed = 99L, difficulty = Difficulty.TOUGH)
        playUntilRoundClosed(engine)

        val snapshot = engine.toSnapshot(cityName = "Tokio", venueId = "tokyo")

        assertEquals(GameMode.ALL_FIVES, snapshot.gameMode)
        assertEquals(Difficulty.TOUGH, snapshot.difficulty)
        assertEquals("Tokio", snapshot.cityName)
        assertEquals("tokyo", snapshot.venueId)
        assertEquals(300, snapshot.targetScore)
        assertEquals(99L, snapshot.randomSeed)
        assertEquals(engine.players.map { it.score }, snapshot.scores)
        assertEquals(engine.players.map { it.personality }, snapshot.personalities)
        assertEquals(engine.players.map { it.hand }, snapshot.hands)
        assertEquals(engine.boardChain, snapshot.boardChain)
        assertEquals(engine.winnerPlayerId, snapshot.winnerPlayerId)
        assertEquals(engine.roundLog, snapshot.roundLog)
        assertEquals(engine.leftOpenPip, snapshot.leftOpenPip)
        assertEquals(engine.rightOpenPip, snapshot.rightOpenPip)
    }

    @Test
    fun resumedMatchKeepsScoresBoardHandsAndWinner() {
        val engine = engine(seed = 31L)
        playUntilRoundClosed(engine)

        val snapshot = engine.toSnapshot(cityName = "La Habana", venueId = "cuba")
        val expectedScores = engine.players.map { it.score }
        val expectedBoard = engine.boardChain.map { it.tile }
        val expectedHands = engine.players.map { it.hand }
        val expectedWinner = engine.winnerPlayerId
        val expectedTeam = engine.winningTeam

        // Se reanuda como lo hace la app.
        val resumed = DominoEngine.resume(snapshot)

        assertEquals("Los marcadores deben quedar intactos", expectedScores, resumed.players.map { it.score })
        assertEquals("El tablero debe quedar intacto", expectedBoard, resumed.boardChain.map { it.tile })
        assertEquals("Las manos deben quedar intactas", expectedHands, resumed.players.map { it.hand })
        assertEquals("El ganador de la ronda debe conservarse", expectedWinner, resumed.winnerPlayerId)
        assertEquals("El equipo ganador debe conservarse", expectedTeam, resumed.winningTeam)
        assertTrue("La ronda debe seguir cerrada para mostrar el resultado", resumed.isRoundOver)
        assertEquals("El modo debe conservarse", snapshot.gameMode, resumed.gameMode)
        assertEquals("La dificultad debe conservarse", snapshot.difficulty, resumed.difficulty)
        assertEquals("Las personalidades deben conservarse", snapshot.personalities, resumed.players.map { it.personality })
    }

    @Test
    fun resumedMatchIsPlayableAndCanFinishTheRound() {
        // Lo importante no es que restaure bien, sino que DESPUES se pueda seguir jugando.
        val engine = engine(seed = 7L)
        playUntilRoundClosed(engine)
        val snapshot = engine.toSnapshot(cityName = "La Habana", venueId = "cuba")
        val scoresBefore = snapshot.scores

        val resumed = DominoEngine.resume(snapshot)

        // Continuar: nueva ronda y jugarla entera.
        resumed.startNewRound()
        assertFalse("Tras empezar ronda el cierre debe levantarse", resumed.isRoundOver)
        assertEquals("La mesa debe estar vacia en una ronda nueva", 0, resumed.boardChain.size)
        assertEquals(
            "Los marcadores acumulados no pueden reiniciarse al empezar ronda",
            scoresBefore,
            resumed.players.map { it.score }
        )

        playUntilRoundClosed(resumed)
        assertTrue("La ronda reanudada debe poder cerrarse", resumed.isRoundOver)
        assertNotNull("Debe haber un ganador", resumed.winnerPlayerId)
        assertTrue(
            "Las puntuaciones solo pueden crecer",
            resumed.players.indices.all { it < scoresBefore.size && resumed.players[it].score >= scoresBefore[it] }
        )
    }

    @Test
    fun incompatibleSchemaIsRejectedInsteadOfInterpreted() {
        val engine = engine()
        playUntilRoundClosed(engine)
        val future = engine.toSnapshot("La Habana", "cuba")
            .copy(schemaVersion = MatchSnapshot.CURRENT_SCHEMA_VERSION + 99)

        assertFalse("Un formato del futuro no debe darse por bueno", future.isCompatible)
    }

    @Test
    fun unknownFieldsDoNotBreakLoading() {
        // Un guardado con campos de mas (version futura que solo anade datos) debe poder
        // leerse: la politica es tolerante con lo que sobra y estricta con la version.
        val engine = engine()
        playUntilRoundClosed(engine)
        val encoded = json.encodeToString(
            MatchSnapshot.serializer(),
            engine.toSnapshot("La Habana", "cuba")
        )
        val withExtra = encoded.dropLast(1) + ""","campoDelFuturo":123}"""

        val tolerant = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val loaded = tolerant.decodeFromString(MatchSnapshot.serializer(), withExtra)

        assertEquals("La Habana", loaded.cityName)
        assertTrue(loaded.isCompatible)
    }

    @Test
    fun storeRoundTripKeepsTheSnapshotIdentical() {
        // El almacen real usa SharedPreferences, que no existe en un test unitario. Se
        // verifica la parte que si vive aqui: que el JSON del almacen no pierde nada.
        val engine = engine(mode = GameMode.DRAW_SPECIAL, seed = 55L)
        playUntilRoundClosed(engine)
        val original = engine.toSnapshot("Tokio", "tokyo")

        val stored = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val reloaded = stored.decodeFromString(
            MatchSnapshot.serializer(),
            stored.encodeToString(MatchSnapshot.serializer(), original)
        )

        assertEquals(original, reloaded)
        assertTrue(reloaded.isCompatible)
        // Y el snapshot restaurado debe producir el mismo estado que el original.
        val fromOriginal = DominoEngine.resume(original)
        val fromReloaded = DominoEngine.resume(reloaded)
        assertEquals(fromOriginal.players.map { it.hand }, fromReloaded.players.map { it.hand })
        assertEquals(fromOriginal.players.map { it.score }, fromReloaded.players.map { it.score })
    }

    @Test
    fun venuesAreOrderedFromRelaxedToTough() {
        // La dificultad de una sede tiene que ser una progresion de la gira, no un adorno:
        // si un venue inicial fuera TOUGH, "La Habana es facil" seria mentira.
        val venues = com.example.dominograndprix.data.WorldTourRepository.getInitialVenues()
        val order = venues.map { it.difficulty }

        assertEquals("La primera sede debe ser la mas suave", Difficulty.RELAXED, order.first())
        assertEquals("La ultima sede debe ser la mas dura", Difficulty.TOUGH, order.last())
        assertTrue(
            "Las sedes no deben endurecerse de golpe: se esperaba una progresion gradual, " +
                "pero el reparto es $order",
            order.zipWithNext().all { (a, b) -> a.ordinal <= b.ordinal }
        )
    }

    @Test
    fun eachDifficultyResolvesToTheExpectedStrategyFamily() {
        // Es la comprobacion de que la conexion al juego hace algo: cada nivel debe producir
        // la familia de estrategia correcta.
        val relaxed = engine(difficulty = Difficulty.RELAXED)
        val standard = engine(difficulty = Difficulty.STANDARD)
        val tough = engine(difficulty = Difficulty.TOUGH)
        val legacy = engine(difficulty = Difficulty.LEGACY)

        assertTrue(
            "RELAXED debe jugar sin criterio",
            relaxed.strategyFor(1) is com.example.dominograndprix.engine.ai.EasyStrategy
        )
        assertTrue(
            "STANDARD debe jugar con la heuristica",
            standard.strategyFor(1) is com.example.dominograndprix.engine.ai.MediumStrategy
        )
        assertTrue(
            "TOUGH debe jugar con la heuristica",
            tough.strategyFor(1) is com.example.dominograndprix.engine.ai.MediumStrategy
        )
        assertTrue(
            "LEGACY debe resolver a la referencia historica",
            legacy.strategyFor(1) is com.example.dominograndprix.engine.ai.LegacyStrategy
        )

        // Y el rival duro no puede jugar con los mismos pesos que el estandar.
        val standardWeights = com.example.dominograndprix.engine.ai.StrategyCatalog
            .weightsFor(Personality.NICE, Difficulty.STANDARD)
        val toughWeights = com.example.dominograndprix.engine.ai.StrategyCatalog
            .weightsFor(Personality.NICE, Difficulty.TOUGH)
        assertTrue(
            "El rival duro debe castigar mas las fichas pesadas que el estandar",
            toughWeights.pipDrain > standardWeights.pipDrain
        )

        // La personalidad debe modular los pesos: dos rivales de la misma dificultad no juegan igual.
        val aggressive = com.example.dominograndprix.engine.ai.StrategyCatalog
            .weightsFor(Personality.AGGRESSIVE, Difficulty.STANDARD)
        val nice = com.example.dominograndprix.engine.ai.StrategyCatalog
            .weightsFor(Personality.NICE, Difficulty.STANDARD)
        assertTrue(
            "El agresivo debe primar vaciar la mano; el amable, aprovechar los puntos",
            aggressive.pipDrain > nice.pipDrain && aggressive.scoring < nice.scoring
        )
    }
}



