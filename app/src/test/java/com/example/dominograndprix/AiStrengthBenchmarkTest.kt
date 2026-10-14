package com.example.dominograndprix

import com.example.dominograndprix.engine.ai.EasyStrategy
import com.example.dominograndprix.engine.ai.LegacyStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Benchmark base: cuanto aporta tener criterio frente a elegir al azar.
 *
 * Mide EasyStrategy contra LegacyStrategy, y el self-play de control que valida que el arnes
 * no tiene sesgo. El detalle del diseno esta documentado en [AiBenchmarkHarness].
 */
class AiStrengthBenchmarkTest {

    private companion object {
        /**
         * Escala de la muestra.
         *
         * Por defecto 125 parejas = 2000 rondas, suficiente para detectar diferencias de ~5
         * puntos porcentuales (+-2,2) y rapido para el CI. Con `-Dbenchmark.full=true` sube a
         * 300 parejas = 4800 rondas (+-1,4), que es lo que se usa al comparar heuristicas que
         * se esperan muy igualadas.
         */
        val PAIRS: Int = if (System.getProperty("benchmark.full") == "true") 300 else 125
    }

    @Test
    fun easyStrategyAgainstLegacyStrategy() {
        val candidate = AiBenchmarkHarness.duel(
            candidate = { _, seatSeed -> EasyStrategy(Random(seatSeed)) },
            baseline = { _, _ -> LegacyStrategy() },
            pairs = PAIRS
        )
        val text = AiBenchmarkHarness.report(
            "EasyStrategy (candidata) vs LegacyStrategy (referencia)", candidate
        )
        println(text)
        AiBenchmarkHarness.dumpReport("easy-vs-legacy.txt", text)

        assertEquals(
            "Las rondas contadas deben cuadrar con las jugadas",
            PAIRS * AiBenchmarkHarness.ROUNDS * 2,
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
        // el diseno pareado tiene un sesgo y cualquier medicion posterior seria sospechosa.
        val selfPlay = AiBenchmarkHarness.duel(
            candidate = { _, _ -> LegacyStrategy() },
            baseline = { _, _ -> LegacyStrategy() },
            pairs = PAIRS / 2
        )
        val text = AiBenchmarkHarness.report(
            "LegacyStrategy contra si misma (control de sesgo)", selfPlay
        )
        println(text)
        AiBenchmarkHarness.dumpReport("selfplay-control.txt", text)

        assertEquals(PAIRS / 2 * AiBenchmarkHarness.ROUNDS * 2, selfPlay.rounds)
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
        val first = AiBenchmarkHarness.duel(
            candidate = { _, seatSeed -> EasyStrategy(Random(seatSeed)) },
            baseline = { _, _ -> LegacyStrategy() },
            pairs = 15
        )
        val second = AiBenchmarkHarness.duel(
            candidate = { _, seatSeed -> EasyStrategy(Random(seatSeed)) },
            baseline = { _, _ -> LegacyStrategy() },
            pairs = 15
        )

        assertEquals("Mismas rondas ganadas", first.roundsWon, second.roundsWon)
        assertEquals("Mismas rondas perdidas", first.roundsLost, second.roundsLost)
    }
}
