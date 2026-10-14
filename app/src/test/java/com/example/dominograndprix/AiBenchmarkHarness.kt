package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.engine.ai.DominoStrategy
import com.example.dominograndprix.model.GameMode
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Arnes de medicion de fuerza de estrategias, compartido por los tests de benchmark.
 *
 * Decisiones de diseno (razonamiento completo en AiStrengthBenchmarkTest):
 * - La unidad de medida es la RONDA, no el match: con un objetivo de puntos bajo, una sola
 *   ronda afortunada cerraria la partida y el banco mediria suerte de reparto.
 * - Diseno pareado por semilla: cada semilla se juega dos veces intercambiando los equipos,
 *   para anular la ventaja de salida.
 * - Se declara el intervalo de confianza, porque un porcentaje sin su margen no es un dato.
 */
object AiBenchmarkHarness {

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

    /**
     * Fabrica de estrategias.
     *
     * Recibe el motor de la partida (las estrategias que simulan se enlazan a el) y una
     * semilla por asiento, para que cada asiento tenga su propia aleatoriedad y ninguno
     * quede sistematicamente favorecido por compartir generador.
     */
    fun interface StrategyFactory {
        fun create(engine: DominoEngine, seatSeed: Long): DominoStrategy
    }

    /** Acumulador de rondas de una estrategia. */
    class Tally {
        var roundsWon = 0
        var roundsLost = 0

        val rounds: Int get() = roundsWon + roundsLost
        val winRate: Double get() = if (rounds == 0) 0.0 else roundsWon.toDouble() / rounds

        /** Error estandar de la proporcion. */
        val standardError: Double
            get() = if (rounds == 0) 0.0 else sqrt(winRate * (1 - winRate) / rounds)

        /** Margen del intervalo de confianza al 95%, en puntos porcentuales. */
        val margin95: Double get() = 1.96 * standardError * 100

        /** Diferencia en puntos porcentuales frente al 50%. */
        val deltaVsEven: Double get() = (winRate - 0.5) * 100
    }

    private data class Outcome(val roundsWonTeamA: Int, val roundsWonTeamB: Int)

    private fun playRounds(
        mode: GameMode,
        seed: Long,
        teamA: StrategyFactory,
        teamB: StrategyFactory,
        rounds: Int
    ): Outcome {
        val engine = DominoEngine(
            gameMode = mode,
            targetScore = Int.MAX_VALUE / 2,
            patternIndex = 3,
            random = Random(seed)
        )
        // Una instancia por asiento y con semilla derivada del asiento: misma aleatoriedad
        // para todos, sin que dos asientos compartan generador.
        engine.setStrategyOverride(0, teamA.create(engine, seed * 4 + 0))
        engine.setStrategyOverride(2, teamA.create(engine, seed * 4 + 2))
        engine.setStrategyOverride(1, teamB.create(engine, seed * 4 + 1))
        engine.setStrategyOverride(3, teamB.create(engine, seed * 4 + 3))

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

    /** Enfrenta dos estrategias con diseno pareado y devuelve el recuento del candidato. */
    fun duel(candidate: StrategyFactory, baseline: StrategyFactory, pairs: Int): Tally {
        val tally = Tally()
        for (pair in 0 until pairs) {
            val mode = MODES[pair % MODES.size]
            val seed = 700_000L + pair

            val first = playRounds(mode, seed, candidate, baseline, ROUNDS)
            tally.roundsWon += first.roundsWonTeamA
            tally.roundsLost += first.roundsWonTeamB

            val second = playRounds(mode, seed, baseline, candidate, ROUNDS)
            tally.roundsWon += second.roundsWonTeamB
            tally.roundsLost += second.roundsWonTeamA
        }
        return tally
    }

    /** Informe en texto de un enfrentamiento. */
    fun report(title: String, tally: Tally): String = buildString {
        appendLine(title)
        appendLine("  rondas jugadas:  ${tally.rounds}")
        appendLine("  rondas ganadas:  ${tally.roundsWon}")
        appendLine("  rondas perdidas: ${tally.roundsLost}")
        appendLine(
            "  winrate:         ${"%.2f".format(tally.winRate * 100)}%  " +
                "(IC95 +-${"%.2f".format(tally.margin95)} puntos)"
        )
        append(
            "  diferencia:      ${"%+.2f".format(tally.deltaVsEven)} puntos frente al 50%"
        )
    }

    /** Escribe un informe en `build/reports/ai-benchmark`, ignorado por git. */
    fun dumpReport(fileName: String, contents: String) {
        val dir = java.io.File("build/reports/ai-benchmark")
        dir.mkdirs()
        java.io.File(dir, fileName).writeText(contents)
    }
}
