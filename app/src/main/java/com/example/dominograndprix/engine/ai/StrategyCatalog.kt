package com.example.dominograndprix.engine.ai

import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.Personality

/**
 * Elige con que estrategia juega cada rival.
 *
 * La dificultad de una sede no la decide el modo de juego ni la apuesta, sino esto: dos
 * asientos con la misma dificultad juegan igual de bien, y la personalidad solo matiza los
 * pesos. Asi "La Habana es facil" y "Las Vegas es dificil" son afirmaciones medibles y no
 * decorativas.
 *
 * Diferenciacion real entre dificultades en los 8 modos:
 * - [Difficulty.RELAXED] -> EasyStrategy (eleccion aleatoria entre jugadas legales).
 * - [Difficulty.STANDARD] -> MediumStrategy enfocada en soltar pips y mantener una continuidad
 *   basica de palo sin explotar memoria de fallas rivales (`passExploit = 0.0`).
 * - [Difficulty.TOUGH] -> MediumStrategy completa con fuerte control de palo (`suitAffinity`)
 *   y explotacion activa de las fallas cantadas por los rivales (`passExploit = 2.4`), lo que
 *   produce decisiones distintas y mas duras incluso en los 5 modos sin puntos en mesa.
 */
object StrategyCatalog {

    /**
     * Pesos de la heuristica segun la dificultad y personalidad del rival.
     */
    fun weightsFor(personality: Personality, difficulty: Difficulty): MediumWeights {
        val base = when (difficulty) {
            Difficulty.LEGACY, Difficulty.RELAXED -> MediumWeights.ALL
            Difficulty.STANDARD -> MediumWeights.ALL.copy(
                suitAffinity = 0.55,
                passExploit = 0.0
            )
            Difficulty.TOUGH -> MediumWeights.ALL.copy(
                pipDrain = MediumWeights.ALL.pipDrain * 1.3,
                blockAwareness = MediumWeights.ALL.blockAwareness * 1.25,
                suitAffinity = 1.75,
                passExploit = 2.4
            )
        }
        return when (personality) {
            Personality.AGGRESSIVE -> base.copy(
                pipDrain = base.pipDrain * 1.15,
                scoring = base.scoring * 0.6,
                passExploit = base.passExploit * 1.2
            )
            Personality.NICE -> base.copy(
                pipDrain = base.pipDrain * 0.85,
                scoring = base.scoring * 2.0,
                blockAwareness = base.blockAwareness * 0.75,
                suitAffinity = base.suitAffinity * 1.15
            )
            Personality.CONFIDENT -> base
        }
    }

    /** Etiqueta legible de la combinacion, para logs y para el banco de pruebas. */
    fun labelFor(personality: Personality, difficulty: Difficulty): String =
        "${difficulty.name.lowercase()}-${personality.name.lowercase()}"
}
