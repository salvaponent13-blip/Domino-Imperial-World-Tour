package com.example.dominograndprix.engine.ai

import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.Personality
import kotlin.random.Random

/**
 * Elige con que estrategia juega cada rival.
 *
 * La dificultad de una sede no la decide el modo de juego ni la apuesta, sino esto: dos
 * asientos con la misma dificultad juegan igual de bien, y la personalidad solo matiza los
 * pesos. Asi "La Habana es facil" y "Las Vegas es dificil" son afirmaciones medibles y no
 * decorativas.
 *
 * Fuerza medida en el banco de pruebas (winrate contra la referencia historica):
 * - [Difficulty.RELAXED] -> EasyStrategy, 48,75%
 * - [Difficulty.STANDARD] -> MediumStrategy, 55,81%
 * - [Difficulty.TOUGH] -> MediumStrategy afinada
 */
object StrategyCatalog {

    /**
     * Pesos de la heuristica segun la personalidad del rival.
     *
     * Es lo que hace que dos rivales de la misma dificultad no jueguen identico:
     * - AGGRESSIVE prima vaciar la mano rapido, aunque regale algun punto por el camino.
     * - NICE es mas conservador con la tranca y aprovecha mejor los modos de puntuacion.
     * - CONFIDENT juega el equilibrio medido, que es la referencia.
     */
    fun weightsFor(personality: Personality, difficulty: Difficulty): MediumWeights {
        val base = when (difficulty) {
            // La referencia historica y el rival de iniciacion no usan heuristica; se quedan
            // con los pesos por defecto por si alguna vez se combinan con la MEDIA.
            Difficulty.LEGACY, Difficulty.RELAXED -> MediumWeights.ALL
            Difficulty.STANDARD -> MediumWeights.ALL
            // El rival duro castiga mas el quedarse con fichas pesadas y no regala puntos por
            // puntuar pronto: sube el drenaje y refuerza la tranca.
            Difficulty.TOUGH -> MediumWeights.ALL.copy(
                pipDrain = MediumWeights.ALL.pipDrain * 1.3,
                blockAwareness = MediumWeights.ALL.blockAwareness * 1.25
            )
        }
        return when (personality) {
            Personality.AGGRESSIVE -> base.copy(
                pipDrain = base.pipDrain * 1.15,
                scoring = base.scoring * 0.6
            )
            Personality.NICE -> base.copy(
                pipDrain = base.pipDrain * 0.85,
                scoring = base.scoring * 2.0,
                blockAwareness = base.blockAwareness * 0.75
            )
            Personality.CONFIDENT -> base
        }
    }

    /** Etiqueta legible de la combinacion, para logs y para el banco de pruebas. */
    fun labelFor(personality: Personality, difficulty: Difficulty): String =
        "${difficulty.name.lowercase()}-${personality.name.lowercase()}"
}
