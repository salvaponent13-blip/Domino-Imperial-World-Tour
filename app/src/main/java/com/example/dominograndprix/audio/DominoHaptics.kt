package com.example.dominograndprix.audio

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Respuesta haptica del juego.
 *
 * Se usa `View.performHapticFeedback` en lugar de `LocalHapticFeedback` a proposito: esta via
 * respeta los ajustes de vibracion del sistema y el modo silencio sin necesidad de comprobar
 * nada a mano. Si el usuario tiene la respuesta tactil desactivada, el sistema simplemente no
 * vibra, que es justo lo que se quiere.
 *
 * Los nombres de las constantes cambian entre versiones de Android (API 24 frente a 27+), asi
 * que cada gesto elige la constante mas expresiva que exista en el dispositivo.
 */
object DominoHaptics {

    /** Colocar una ficha: un toque seco y corto, como el golpe sobre la mesa. */
    fun tilePlaced(view: View) {
        perform(view, HapticFeedbackConstants.KEYBOARD_TAP, fallback = HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Cantar dominó o cerrar la ronda: mas rotundo. */
    fun dominoCalled(view: View) {
        perform(view, HapticFeedbackConstants.CONFIRM, fallback = HapticFeedbackConstants.LONG_PRESS)
    }

    /** Anotar puntos en los modos de tanteo. */
    fun score(view: View) {
        perform(view, HapticFeedbackConstants.CLOCK_TICK, fallback = HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Robar del pozo o pasar turno: apenas perceptible. */
    fun softTap(view: View) {
        perform(view, HapticFeedbackConstants.CLOCK_TICK, fallback = HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun perform(view: View, constant: Int, fallback: Int) {
        // Las constantes nuevas no existen en API 24-26: si la del sistema no esta disponible
        // en esta version, se usa la de reserva, que si lo esta.
        val chosen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) constant else fallback
        try {
            view.performHapticFeedback(chosen)
        } catch (_: Exception) {
            // Un dispositivo sin motor haptico no debe romper la partida.
        }
    }
}
