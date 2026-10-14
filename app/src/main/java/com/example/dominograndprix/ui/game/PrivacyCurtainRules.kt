package com.example.dominograndprix.ui.game

/**
 * Decide cuando hay que levantar la cortina de privacidad del pass-and-play.
 *
 * Vive separado de la pantalla a proposito: es la unica parte de la cortina que se puede
 * verificar sin un dispositivo, y es justo donde esta el riesgo. Si esta logica falla, un
 * jugador acaba viendo las fichas del siguiente al pasarle el movil, que es exactamente lo que
 * debe impedir.
 *
 * Reglas:
 * - Solo aplica en partidas con mas de una persona.
 * - Se levanta solo cuando el turno PASA de una persona a OTRA persona distinta.
 * - No se levanta si el turno anterior era de la maquina (nadie estaba mirando una mano
 *   propia), ni si es la misma persona (por ejemplo al encadenar en hungaro), ni con la ronda
 *   o el match cerrados.
 */
object PrivacyCurtainRules {

    /**
     * @param humanSeatCount cuantas personas hay en la mesa
     * @param previousActingSeat asiento del turno anterior, o -1 si es el primero
     * @param previousSeatWasHuman si ese asiento anterior era de una persona
     * @param currentSeat asiento que tiene el turno ahora
     * @param currentSeatIsHuman si el turno actual es de una persona
     * @param isRoundOver si la ronda esta cerrada
     * @param isMatchOver si el match ha terminado
     */
    fun shouldRaiseCurtain(
        humanSeatCount: Int,
        previousActingSeat: Int,
        previousSeatWasHuman: Boolean,
        currentSeat: Int,
        currentSeatIsHuman: Boolean,
        isRoundOver: Boolean,
        isMatchOver: Boolean
    ): Boolean {
        if (humanSeatCount <= 1) return false
        if (isRoundOver || isMatchOver) return false
        if (!currentSeatIsHuman) return false
        if (previousActingSeat < 0) return false
        if (!previousSeatWasHuman) return false
        if (previousActingSeat == currentSeat) return false
        return true
    }
}
