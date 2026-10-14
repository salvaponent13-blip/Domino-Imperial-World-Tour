package com.example.dominograndprix

import com.example.dominograndprix.engine.ai.LegacyStrategy
import com.example.dominograndprix.engine.ai.MediumWeights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint 3: medicion incremental del aporte de cada factor de la estrategia MEDIA.
 *
 * El objetivo es no meter cuatro heuristicas a la vez y no saber cual funciona. Se mide la
 * winrate contra la referencia con las cuatro configuraciones acumulativas:
 *
 *   1. Solo domino inmediato
 *   2. + drenaje de pips y liberacion de dobles
 *   3. + tranca consciente de pips
 *   4. + suma de extremos en modos de puntuacion
 *
 * El incremento de cada paso es el aporte de ese factor, medido en puntos porcentuales.
 *
 * Escala: por defecto 100 parejas (1600 rondas) por configuracion, que es suficiente para ver
 * el efecto de cada factor con un margen de +-2,5 puntos. Con `-Dbenchmark.full=true` sube a
 * 300 parejas (4800 rondas, +-1,4) para la medicion final.
 */
class MediumStrategyBenchmarkTest {

    private companion object {
        val PAIRS: Int = if (System.getProperty("benchmark.full") == "true") 300 else 100

        /** Criterio de exito del Sprint 3. */
        const val TARGET_WIN_RATE = 0.55

        /**
         * Asiento a partir de la semilla por asiento que reparte el arnes.
         *
         * La MEDIA necesita saber PARA QUIEN decide: simula la jugada de un jugador concreto,
         * y si simulase para el que tiene el turno en ese instante, evaluaria otra mano.
         */
        fun seatOf(seatSeed: Long): Int = (seatSeed % 4).toInt()
    }

    private fun measure(weights: MediumWeights, label: String): AiBenchmarkHarness.Tally =
        AiBenchmarkHarness.duel(
            candidate = { engine, seatSeed -> engine.newMediumStrategy(seatOf(seatSeed), weights) },
            baseline = { _, _ -> LegacyStrategy() },
            pairs = PAIRS
        ).also { tally ->
            println(
                "%-46s %6.2f%%  (IC95 +-%4.2f)  %+6.2f".format(
                    label, tally.winRate * 100, tally.margin95, tally.deltaVsEven
                )
            )
        }

    @Test
    fun eachFactorAddsMeasurableStrength() {
        println("Factores de MediumStrategy frente a LegacyStrategy (${PAIRS * AiBenchmarkHarness.ROUNDS * 2} rondas por fila)")
        println("%-46s %7s  %-14s %6s".format("configuracion", "winrate", "IC95", "delta"))

        val dominoOnly = measure(MediumWeights.DOMINO_ONLY, "1. domino inmediato")
        val withPips = measure(MediumWeights.DOMINO_AND_PIPS, "2. + drenaje de pips y dobles")
        val withBlock = measure(MediumWeights.WITHOUT_SCORING, "3. + tranca consciente de pips")
        val all = measure(MediumWeights.ALL, "4. + suma de extremos (completa)")

        val table = buildString {
            appendLine("Medicion incremental de MediumStrategy contra LegacyStrategy")
            appendLine("Parejas: $PAIRS (${PAIRS * AiBenchmarkHarness.ROUNDS * 2} rondas por configuracion)")
            appendLine()
            appendLine("%-46s %9s %10s %10s".format("configuracion", "winrate", "IC95", "delta vs 50%"))
            listOf(
                "1. domino inmediato" to dominoOnly,
                "2. + drenaje de pips y dobles" to withPips,
                "3. + tranca consciente de pips" to withBlock,
                "4. + suma de extremos (completa)" to all
            ).forEach { (label, tally) ->
                appendLine(
                    "%-46s %8.2f%% %9.2f %+9.2f".format(
                        label, tally.winRate * 100, tally.margin95, tally.deltaVsEven
                    )
                )
            }
            appendLine()
            appendLine("Aporte de cada factor (incremento sobre el paso anterior):")
            appendLine("  factor 2 (pips y dobles):   ${"%+.2f".format((withPips.winRate - dominoOnly.winRate) * 100)} puntos")
            appendLine("  factor 3 (tranca):          ${"%+.2f".format((withBlock.winRate - withPips.winRate) * 100)} puntos")
            appendLine("  factor 4 (puntuacion):      ${"%+.2f".format((all.winRate - withBlock.winRate) * 100)} puntos")
            appendLine()
            append("Winrate final: ${"%.2f".format(all.winRate * 100)}% " +
                "(objetivo >= ${"%.0f".format(TARGET_WIN_RATE * 100)}%)")
        }
        println(table)
        AiBenchmarkHarness.dumpReport("medium-incremental.txt", table)

        // Coherencia del arnes
        listOf(dominoOnly, withPips, withBlock, all).forEach { tally ->
            assertEquals(
                "Cada configuracion debe jugar las mismas rondas",
                PAIRS * AiBenchmarkHarness.ROUNDS * 2,
                tally.rounds
            )
        }

        // Criterio de exito del Sprint 3.
        assertTrue(
            "MediumStrategy debe superar el ${"%.0f".format(TARGET_WIN_RATE * 100)}% contra Legacy. " +
                "Medido: ${"%.2f".format(all.winRate * 100)}%",
            all.winRate >= TARGET_WIN_RATE
        )
        assertTrue(
            "La estrategia completa debe ser al menos tan buena como solo el factor 1",
            all.winRate >= dominoOnly.winRate - 0.02
        )
    }

    /**
     * Ajuste del peso del factor 4 (suma de extremos).
     *
     * Existe porque la primera medicion dejo el factor 4 plano: con `scoring = 6` por punto,
     * la puntuacion inmediata dominaba sobre el drenaje de pips, que es el factor que de
     * verdad mueve la winrate. Aqui se prueban varios pesos para ver si el factor 4 aporta
     * cuando se le da un papel de desempate en lugar de protagonismo.
     *
     * No corre por defecto (es exploratorio y alarga el CI). Se lanza con:
     *   ./gradlew test -Dbenchmark.tuning=true
     */
    @Test
    fun tuneScoringWeight() {
        org.junit.Assume.assumeTrue(
            "Ajuste exploratorio: se activa con -Dbenchmark.tuning=true",
            System.getProperty("benchmark.tuning") == "true"
        )

        val candidates = listOf(
            "sin factor 4 (referencia)" to MediumWeights.WITHOUT_SCORING,
            "scoring 0.25 (desempate suave)" to MediumWeights.WITHOUT_SCORING.copy(scoring = 0.25),
            "scoring 0.5" to MediumWeights.WITHOUT_SCORING.copy(scoring = 0.5),
            "scoring 1.0" to MediumWeights.WITHOUT_SCORING.copy(scoring = 1.0),
            "scoring 2.0" to MediumWeights.WITHOUT_SCORING.copy(scoring = 2.0),
            "scoring 6.0 (el actual)" to MediumWeights.ALL
        )

        val tuningPairs = 60
        println("Ajuste del peso de puntuacion (${tuningPairs * AiBenchmarkHarness.ROUNDS * 2} rondas por fila)")
        val results = candidates.map { (label, weights) ->
            val tally = AiBenchmarkHarness.duel(
                candidate = { engine, seatSeed -> engine.newMediumStrategy(seatOf(seatSeed), weights) },
                baseline = { _, _ -> LegacyStrategy() },
                pairs = tuningPairs
            )
            println(
                "%-34s %6.2f%%  (IC95 +-%4.2f)".format(label, tally.winRate * 100, tally.margin95)
            )
            label to tally
        }
        AiBenchmarkHarness.dumpReport(
            "medium-scoring-tuning.txt",
            results.joinToString("\n") { (label, tally) ->
                "%-34s %6.2f%%  IC95 +-%4.2f".format(label, tally.winRate * 100, tally.margin95)
            }
        )
    }
}
