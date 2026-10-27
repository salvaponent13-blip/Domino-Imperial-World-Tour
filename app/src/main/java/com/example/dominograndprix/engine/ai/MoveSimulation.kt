package com.example.dominograndprix.engine.ai

import com.example.dominograndprix.model.BoardSide

/**
 * Resultado observable de aplicar una jugada, sobre una copia del estado.
 *
 * Existe porque algunas decisiones estrategicas no se pueden tomar mirando solo el estado
 * actual: para saber si una jugada TRANCA la mesa hay que ver si alguien mas puede seguir
 * jugando, y eso exige mirar las manos de todos. Eso solo lo puede resolver el motor, que es
 * quien conoce las reglas de los 8 modos.
 *
 * Lo produce [com.example.dominograndprix.engine.DominoEngine.simulate], que aplica la jugada
 * sobre una copia y restaura el estado: simular nunca altera la partida en curso.
 */
data class MoveSimulation(
    /** La jugada simulada. */
    val move: AiMove.Play,
    /** Fichas que le quedarian en la mano a quien juega. */
    val myRemainingTiles: Int,
    /** Puntos que le quedarian en la mano a quien juega. */
    val myRemainingPips: Int,
    /** Dobles que le quedarian en la mano: atascan mas porque solo tienen una salida. */
    val myRemainingDoubles: Int,
    /** Suma de pips de todos los rivales (en equipos: los dos asientos contrarios). */
    val opponentsPips: Int,
    /** Suma de pips del equipo de quien juega, incluido el mismo. */
    val ownTeamPips: Int,
    /** Extremos abiertos despues de la jugada. */
    val leftOpenPip: Int?,
    val rightOpenPip: Int?,
    /**
     * Si la jugada deja la mesa bloqueada: nadie puede jugar y la ronda se resolveria
     * comparando manos. Es la condicion de "tranca".
     */
    val blocksTheBoard: Boolean,
    /** Puntos inmediatos que otorga la jugada en los modos que puntuan extremos. */
    val pointsEarned: Int,
    /** Si la jugada vacia la mano y gana la ronda al instante. */
    val winsTheRound: Boolean,
    /**
     * Cuantas fichas de la mano restante de quien juega encajan con los nuevos extremos
     * abiertos (continuidad o afinidad de palo propia).
     */
    val mySuitSupport: Int = 0,
    /**
     * Cuantos extremos abiertos coinciden con fallas publicas cantadas por los rivales
     * (pases previos observables en la mesa).
     */
    val opponentPassedEndsMatched: Int = 0
) {
    /** Suma de los extremos abiertos. */
    val openEndsSum: Int get() = (leftOpenPip ?: 0) + (rightOpenPip ?: 0)

    /**
     * Diferencia de pips entre mi equipo y los rivales. Negativa = voy por delante en la
     * cuenta de la tranca, que es lo que hace conveniente cerrar la mesa.
     */
    val pipAdvantage: Int get() = ownTeamPips - opponentsPips

    override fun toString(): String {
        val side = when (move.side) {
            BoardSide.LEFT -> "izquierda"
            BoardSide.RIGHT -> "derecha"
            BoardSide.UP -> "arriba"
            BoardSide.DOWN -> "abajo"
        }
        return "Simulacion(${move.tile} por $side, quedan $myRemainingTiles fichas / " +
            "$myRemainingPips pips, rivales $opponentsPips, tranca=$blocksTheBoard, " +
            "puntos=$pointsEarned, gana=$winsTheRound)"
    }
}
