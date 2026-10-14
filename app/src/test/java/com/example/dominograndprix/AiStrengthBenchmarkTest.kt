package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.engine.ai.DominoStrategy
import com.example.dominograndprix.engine.ai.EasyStrategy
import com.example.dominograndprix.engine.ai.LegacyStrategy
import com.example.dominograndprix.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Benchmark de fuerza de estrategias (arnes A/B).
 *
 * Mide cuanto juega mejor una estrategia que otra, sin UI y con reproducibilidad por semilla.
 *
 * Decisiones de diseno, y por que:
 *
 * 1. **La unidad de medida es la RONDA, no el "match corto"**. Medir partidas a `targetScore`
 *    bajo era un error: en los modos de puntuacion una sola ronda afortunada cierra el match,
 *    asi que el banco medía suerte de reparto en lugar de calidad de decision. Aqui cada
 *    enfrentamiento juega un numero fijo de RONDAS y se cuentan ronda a ronda, que es la
 *    cantidad con la que el dominó se juega de verdad.
 *
 * 2. **Diseno pareado por semilla**: cada semilla se juega dos veces, intercambiando los
 *    equipos de las dos estrategias. Asi se anula la ventaja de salida (quien abre lo decide
 *    el doble mas alto, que depende del reparto).
 *
 * 3. **Enfrentamiento equipo contra equipo**: 0 y 2 contra 1 y 3, como en una partida real.
 *
 * 4. **La muestra es grande y se declara el intervalo de confianza**: con ~2000 rondas, el
 *    error estandar de la proporcion ronda el 1%, suficiente para detectar diferencias de 3-4
 *    puntos. Los umbrales de aceptacion son laxos a proposito: aqui se viene a MEDIR, no a
 *    que un test pase.
 *
 * Los numeros que imprime este banco son el material con el que se decide si una heuristica
 * nueva (la estrategia MEDIA) merece la pena frente a la referencia.
 */
class AiStrengthBenchmarkTest {

    private companion object {
        /**
         * Escala de la muestra.
         *
         * Por defecto se juegan 2000 rondas, suficiente para detectar mejoras de ~5 puntos
         * porcentuales con una precision de +-2,2 puntos, y lo bastante rapido para el CI.
         * Con `-Dbenchmark.full=true` sube a 4800 rondas (+-1,4 puntos), que es lo que se
         * usa al comparar dos heuristicas que se esperan muy igualadas.
         */
        val PAIRS: Int = if (System.getProperty("benchmark.full") == "true") 300 else 125

        /** Rondas por partida. Cada ronda es un reparto nuevo. */
        const val ROUNDS = 8

        /** Modos que se rotan para que la medida no dependa de una sola modalidad. */
        val MODES = listOf(
            GameMode.DRAW,
            GameMode.BLOCK,
            GameMode.ALL_FIVES,
            GameMode.MATADOR,
            GameMode.DRAW_SPECIAL,
            GameMode.BLOCK_HUNGARIAN
        )
    }

    /** Resultado de un enfrentamiento: rondas ganadas por cada bando. */
    private data class Outcome(val roundsWonTeamA: Int, val roundsWonTeamB: Int) {
        val total: Int get() = roundsWonTeamA + roundsWonTeamB
    }

    /**
     * Juega [rounds] rondas entre dos estrategias: `strategyTeamA` en los asientos 0 y 2,
     * `strategyTeamB` en los 1 y 3. La puntuacion objetivo se pone muy alta para que el match
     * NO se cierre a mitad: lo que se cuentan son rondas completas.
     */
    private fun playRounds(
        mode: GameMode,
        seed: Long,
        strategyTeamA: DominoStrategy,
        strategyTeamB: DominoStrategy,
        rounds: Int = ROUNDS
    ): Outcome {
        val engine = DominoEngine(
            gameMode = mode,
            targetScore = Int.MAX_VALUE / 2, // el match nunca se cierra por puntos
            patternIndex = 3,
            random = Random(seed)
        )
        engine.setStrategyOverride(0, strategyTeamA)
        engine.setStrategyOverride(2, strategyTeamA)
        engine.setStrategyOverride(1, strategyTeamB)
        engine.setStrategyOverride(3, strategyTeamB)

        var wonA = 0
        var wonB = 0
        for (round in 0 until rounds) {
            if (round > 0) engine.startNewRound()
            var guard = 0
            while (!engine.isRoundOver && guard < 1500) {
                engine.executeAIMove()
                guard++
            }
            when (engine.winningTeam) {
                0 -> wonA++
                1 -> wonB++
            }
        }
        return Outcome(wonA, wonB)
    }

    /** Acumulador de rondas y puntos de una estrategia. */
    private class Tally {
        var roundsWon = 0
        var roundsLost = 0

        val rounds: Int get() = roundsWon + roundsLost

        val winRate: Double get() = if (rounds == 0) 0.0 else roundsWon.toDouble() / rounds

        /** Error estandar de la proporcion. */
        val standardError: Double
            get() = if (rounds == 0) 0.0 else sqrt(winRate * (1 - winRate) / rounds)

        /** Margen del intervalo de confianza al 95%. */
        val margin95: Double get() = 1.96 * standardError
    }

    /** Enfrenta `candidate` (equipos alternos) contra `baseline` con diseno pareado. */
    private fun duel(candidate: DominoStrategy, baseline: DominoStrategy, pairs: Int = PAIRS): Tally {
        val tally = Tally()
        for (pair in 0 until pairs) {
            val mode = MODES[pair % MODES.size]
            val seed = 700_000L + pair

            // Mitad 1: candidato en el equipo 0
            val first = playRounds(mode, seed, candidate, baseline)
            tally.roundsWon += first.roundsWonTeamA
            tally.roundsLost += first.roundsWonTeamB

            // Mitad 2: misma semilla, equipos intercambiados
            val second = playRounds(mode, seed, baseline, candidate)
            tally.roundsWon += second.roundsWonTeamB
            tally.roundsLost += second.roundsWonTeamA
        }
        return tally
    }

    /**
     * Escribe el informe del banco de pruebas en un fichero, ademas de sacarlo por consola.
     *
     * El fichero es el resultado util de este test: permite comparar mediciones hechas en
     * momentos distintos y queda disponible como artefacto en CI. `build/` esta ignorado por
     * git, asi que no ensucia el repositorio.
     */
    private fun dumpReport(fileName: String, contents: String) {
        val dir = java.io.File("build/reports/ai-benchmark")
        dir.mkdirs()
        java.io.File(dir, fileName).writeText(contents)
    }

    private fun report(title: String, tally: Tally): String = buildString {
        appendLine(title)
        appendLine("  rondas jugadas:  ${tally.rounds}")
        appendLine("  rondas ganadas:  ${tally.roundsWon}")
        appendLine("  rondas perdidas: ${tally.roundsLost}")
        appendLine("  winrate:         ${"%.2f".format(tally.winRate * 100)}%  (IC95 ±${"%.2f".format(tally.margin95 * 100)} puntos)")
        append("  diferencia:      ${"%+.2f".format((tally.winRate - 0.5) * 100)} puntos frente al 50%")
    }

    @Test
    fun easyStrategyAgainstLegacyStrategy() {
        // Enfrentamiento pedido en el Sprint 2.
        val candidate = duel(candidate = EasyStrategy(Random(1234L)), baseline = LegacyStrategy())
        val text = report("EasyStrategy (candidata) vs LegacyStrategy (referencia)", candidate)
        println(text)
        dumpReport("easy-vs-legacy.txt", text)

        // Coherencia interna del arnes
        assertEquals(
            "Las rondas contadas deben cuadrar con las jugadas",
            PAIRS * ROUNDS * 2,
            candidate.rounds
        )
        assertTrue(
            "La muestra debe ser grande: ${candidate.rounds} rondas",
            candidate.rounds >= 2000
        )
    }

    @Test
    fun legacyAgainstItselfIsBalancedAndTheHarnessHasNoBias() {
        // Control del arnes: la misma estrategia contra si misma debe dar ~50%. Si se desvia,
        // el diseno pareado tiene un sesgo (de asiento, de modo o de semilla) y cualquier
        // medicion posterior seria sospechosa.
        val selfPlay = duel(candidate = LegacyStrategy(), baseline = LegacyStrategy(), pairs = PAIRS / 2)
        val text = report("LegacyStrategy contra si misma (control de sesgo)", selfPlay)
        println(text)
        dumpReport("selfplay-control.txt", text)

        assertEquals(PAIRS / 2 * ROUNDS * 2, selfPlay.rounds)
        assertTrue(
            "La misma estrategia contra si misma debe quedar cerca del 50%: " +
                "${"%.2f".format(selfPlay.winRate * 100)}%",
            abs(selfPlay.winRate - 0.5) < 0.05
        )
    }

    @Test
    fun benchmarkIsDeterministicForAGivenSeed() {
        // Repetir el enfrentamiento debe dar exactamente lo mismo: es lo que permite comparar
        // mediciones hechas en momentos distintos.
        val first = duel(candidate = EasyStrategy(Random(77L)), baseline = LegacyStrategy(), pairs = 15)
        val second = duel(candidate = EasyStrategy(Random(77L)), baseline = LegacyStrategy(), pairs = 15)

        assertEquals("Mismas rondas ganadas", first.roundsWon, second.roundsWon)
        assertEquals("Mismas rondas perdidas", first.roundsLost, second.roundsLost)
    }
}
