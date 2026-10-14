package com.example.dominograndprix.engine.ai

/**
 * Estrategia de decision de un jugador automatico.
 *
 * Contrato:
 * - Recibe una [PlayerView] inmutable con lo que un humano veria en la mesa.
 * - Devuelve una [AiMove]. El motor la valida antes de aplicarla: si devuelve una jugada
 *   ilegal, se rechaza en lugar de corromper la partida.
 * - **No puede mutar el estado del juego**: no tiene acceso al motor. Esto es lo que hace
 *   que las estrategias sean testeables en aislamiento y comparables entre si con el
 *   benchmark A/B.
 */
interface DominoStrategy {

    /** Nombre legible, para logs y para identificar la estrategia en los benchmarks. */
    val name: String

    /**
     * Decide que hacer en el turno descrito por [view].
     *
     * Si [PlayerView.hasNoPlayableMove] es cierto, solo [AiMove.Draw] (si
     * [PlayerView.canDraw]) o [AiMove.Pass] tienen sentido.
     */
    fun decide(view: PlayerView): AiMove
}
