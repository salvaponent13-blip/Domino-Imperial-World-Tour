package com.example.dominograndprix.engine.ai

import com.example.dominograndprix.model.GameMode

/**
 * Pesos de la estrategia MEDIA. Cada factor se puede activar o desactivar por separado para
 * poder medir su aporte real en el benchmark en lugar de meterlos todos a la vez y no saber
 * cual funciona. Los valores por defecto son los que salieron de medir.
 */
data class MediumWeights(
    /**
     * Factor 1 — Dominó inmediato. Peso practicamente infinito: si una jugada vacia la mano,
     * gana la ronda al instante y no hay nada que valorar.
     */
    val domino: Double = 10_000.0,
    /** Factor 2 — Drenaje de pips, con penalizacion extra por retener dobles. */
    val pipDrain: Double = 1.0,
    /** Factor 3 — Cierre o tranca consciente de pips. */
    val blockAwareness: Double = 4.0,
    /**
     * Factor 4 — Puntos de extremos en los modos que puntuan.
     *
     * El valor es bajo A PROPOSITO y no es un descuido: medido en el banco de pruebas, dar
     * protagonismo a la puntuacion inmediata EMPEORA el resultado (con 6.0 la winrate baja
     * al 54,3%; con 0.25 sube al 55,7%). Razon: la puntuacion por jugada es un objetivo
     * cortoplacista, mientras que vaciar la mano es lo que gana rondas, y las rondas son lo
     * que suma puntos. Aqui la puntuacion solo desempata entre jugadas equivalentes en pips.
     */
    val scoring: Double = 0.25
) {
    companion object {
        /** Solo el factor 1 (dominó inmediato); el resto de decisiones caen a la referencia. */
        val DOMINO_ONLY = MediumWeights(pipDrain = 0.0, blockAwareness = 0.0, scoring = 0.0)

        /** Factores 1 y 2. */
        val DOMINO_AND_PIPS = DOMINO_ONLY.copy(pipDrain = 1.0)

        /** Factores 1, 2 y 3. */
        val WITHOUT_SCORING = DOMINO_AND_PIPS.copy(blockAwareness = 4.0)

        /** Los cuatro factores activos: la configuracion por defecto, con los pesos medidos. */
        val ALL = WITHOUT_SCORING.copy(scoring = 0.25)
    }
}

/**
 * Estrategia heuristica con cuatro factores de dominó clasico.
 *
 * A diferencia de una IA que solo mira el estado actual, esta **simula** cada jugada legal a
 * traves del motor ([com.example.dominograndprix.engine.DominoEngine.simulate]) para conocer
 * sus consecuencias: si vacia la mano, cuanto castiga la mano que deja, si tranca la mesa y
 * cuantos puntos otorga. Simular es lo que permite razonar sobre el futuro sin duplicar las
 * reglas de colocacion.
 */
class MediumStrategy(
    private val weights: MediumWeights = MediumWeights.ALL,
    /**
     * Penalizacion extra por cada doble que se queda en la mano. Un doble solo tiene una
     * salida en lugar de dos, asi que retenerlo es mas peligroso que retener una ficha
     * normal con los mismos pips.
     */
    private val doubleHoldingPenalty: Double = 2.5,
    /** Funcion que simula una jugada. La inyecta el motor. */
    private val simulate: (AiMove.Play) -> MoveSimulation
) : DominoStrategy {

    override val name: String = "medium"

    override fun decide(view: PlayerView): AiMove {
        if (view.hasNoPlayableMove) return TurnRules.whenNoPlayableMove(view)

        val evaluations = view.playableMoves.map { move -> move to evaluate(view, move) }
        val best = evaluations.maxByOrNull { it.second } ?: return view.playableMoves.first()

        // Empate: se respeta el orden del motor, que es estable entre ejecuciones.
        return best.first
    }

    /** Puntuacion de una jugada: cuanto mas alta, mejor. */
    internal fun evaluate(view: PlayerView, move: AiMove.Play): Double {
        val simulation = simulate(move)

        // Factor 1: ganar la ronda ahora mismo no admite discusion.
        if (simulation.winsTheRound) return weights.domino

        var score = 0.0

        // Factor 2: cuanto menos castigo quede en la mano, mejor. Los dobles que se quedan
        // pesan mas porque tienen una sola salida.
        if (weights.pipDrain != 0.0) {
            val doubleLoad = simulation.myRemainingDoubles * doubleHoldingPenalty
            score -= weights.pipDrain * (simulation.myRemainingPips + doubleLoad)
        }

        // Factor 3: trancar solo compensa si voy por delante en la cuenta de pips.
        if (weights.blockAwareness != 0.0 && simulation.blocksTheBoard) {
            score += if (simulation.pipAdvantage < 0) {
                weights.blockAwareness * -simulation.pipAdvantage
            } else {
                // Trancar con la mano mas pesada es regalar la ronda.
                -weights.blockAwareness * simulation.pipAdvantage
            }
        }

        // Factor 4: en los modos que puntuan extremos, sumar compite con vaciar la mano.
        if (weights.scoring != 0.0 && view.gameMode.isPointGame) {
            score += weights.scoring * simulation.pointsEarned
        }

        return score
    }
}

/** Si la modalidad otorga puntos por los extremos abiertos. */
private val GameMode.isPointGame: Boolean
    get() = this == GameMode.ALL_FIVES ||
        this == GameMode.ALL_THREES ||
        this == GameMode.FIVES_AND_THREES
