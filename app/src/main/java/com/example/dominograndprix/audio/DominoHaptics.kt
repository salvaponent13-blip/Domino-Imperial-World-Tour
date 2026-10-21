package com.example.dominograndprix.audio

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dominograndprix.data.SettingsStore

/**
 * Respuesta haptica del juego.
 *
 * Se usa `View.performHapticFeedback` en lugar de `LocalHapticFeedback` a proposito: esta via
 * respeta los ajustes de vibracion del sistema y el modo silencio sin necesidad de comprobar
 * nada a mano. Si el usuario tiene la respuesta tactil desactivada, el sistema simplemente no
 * vibra, que es justo lo que se quiere.
 *
 * Ademas hay un interruptor propio en los ajustes ([setEnabled]) que se guarda entre sesiones.
 *
 * Los nombres de las constantes cambian entre versiones de Android (API 24 frente a 27+), asi
 * que cada gesto elige la constante mas expresiva que exista en el dispositivo.
 */
object DominoHaptics {

    /** Interruptor del jugador; el sistema sigue pudiendo silenciar la vibracion por su cuenta. */
    var enabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        enabled = SettingsStore.isHapticsEnabled(context)
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        SettingsStore.setHapticsEnabled(context, value)
    }

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
        if (!enabled) return
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
