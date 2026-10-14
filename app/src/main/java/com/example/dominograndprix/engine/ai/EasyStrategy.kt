package com.example.dominograndprix.engine.ai

import kotlin.random.Random

/**
 * Estrategia ingenua: elige una jugada legal al azar.
 *
 * No pretende jugar bien; existe como **control experimental**. Toda la aleatoriedad pasa
 * por el generador inyectado, asi que una partida sigue siendo reproducible con semilla
 * fija, que es lo que permite medirla contra [LegacyStrategy].
 *
 * En el benchmark sirve para dos cosas:
 * - Cuantificar cuanto aporta "jugar siempre la primera jugable" frente a no tener criterio.
 * - Detectar si una heuristica futura es peor que el azar, que seria una senal de alarma.
 */
class EasyStrategy(
    private val random: Random = Random.Default
) : DominoStrategy {

    override val name: String = "easy"

    override fun decide(view: PlayerView): AiMove {
        if (view.hasNoPlayableMove) return TurnRules.whenNoPlayableMove(view)
        return view.playableMoves[random.nextInt(view.playableMoves.size)]
    }
}
