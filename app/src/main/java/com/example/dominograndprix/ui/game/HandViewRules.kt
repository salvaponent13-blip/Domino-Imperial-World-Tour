package com.example.dominograndprix.ui.game

/**
 * Decide de QUE asiento se muestra la mano en la parte inferior de la mesa.
 *
 * Existe separado de la pantalla por una razon concreta: aqui vivia un defecto que llego a
 * produccion. La pantalla usaba el asiento que TIENE EL TURNO para pintar la mano de abajo, de
 * modo que al jugar la maquina se veian sus fichas en el sitio del jugador. Y como ademas la
 * mano ajena se pintaba con el atenuado de "no jugable", el jugador veia sus propias fichas
 * apagadas y sin relieve justo en su turno, que es lo que describio como "turbias y borrosas".
 *
 * Son dos asientos distintos y confundirlos es facil, asi que la decision se aisla y se prueba:
 * - El asiento que ACTUA: quien tiene el turno. Puede ser una persona o la maquina.
 * - El asiento que MIRA: de quien se muestra la mano. SIEMPRE una persona.
 */
object HandViewRules {

    /**
     * @param isLocalMatch si hay varias personas en la mesa (pass-and-play)
     * @param currentSeat asiento que tiene el turno
     * @param currentSeatIsHuman si ese asiento es de una persona
     * @param lastHumanSeat ultimo asiento humano que tuvo el turno
     * @param humanSeats asientos ocupados por personas
     */
    fun handSeat(
        isLocalMatch: Boolean,
        currentSeat: Int,
        currentSeatIsHuman: Boolean,
        lastHumanSeat: Int,
        humanSeats: Set<Int>
    ): Int {
        // El conjunto de asientos humanos es la fuente de verdad, no la bandera que llega. Si se
        // contradicen (turno marcado como humano en un asiento que no esta en el conjunto), se
        // hace caso al conjunto: es preferible mostrar la mano equivocada de una persona que
        // ensenar la de la maquina.
        val currentIsHumanSeat = currentSeatIsHuman && currentSeat in humanSeats
        val fallback = humanSeats.minOrNull() ?: 0
        val safeLastHuman = if (lastHumanSeat in humanSeats) lastHumanSeat else fallback

        return when {
            // Pass-and-play: cuando le toca a una persona se muestra SU mano; mientras piensa la
            // maquina se sigue viendo la del ultimo que jugo, que es quien tiene el movil.
            isLocalMatch -> if (currentIsHumanSeat) currentSeat else safeLastHuman
            // Un solo jugador: siempre la suya, aunque el turno sea de la maquina.
            else -> fallback
        }
    }

    /**
     * Si la mano mostrada puede interactuarse. Solo cuando el turno es de una persona Y la mano
     * que se ve es la de quien tiene el turno: nunca se juega con la mano de otro.
     */
    fun canInteractWithHand(
        handSeat: Int,
        currentSeat: Int,
        currentSeatIsHuman: Boolean,
        handSeatIsHuman: Boolean,
        isRoundOver: Boolean,
        isPrivacyCurtainUp: Boolean
    ): Boolean = currentSeatIsHuman &&
        !isRoundOver &&
        !isPrivacyCurtainUp &&
        handSeat == currentSeat &&
        handSeatIsHuman
}
