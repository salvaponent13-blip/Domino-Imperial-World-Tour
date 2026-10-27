package com.example.dominograndprix.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferencias del jugador que deben sobrevivir al cierre de la app.
 *
 * Antes de esto el silencio del audio se perdia en cada arranque, la respuesta haptica no se
 * podia desactivar y las opciones de camara/iluminacion/velocidad de la mesa se reiniciaban al
 * salir. Todo eso vive aqui, en un unico sitio y con valores por defecto sensatos.
 */
object SettingsStore {

    private const val PREFS_NAME = "domino_imperial_settings"
    private const val KEY_MUTED = "sound_muted"
    private const val KEY_HAPTICS = "haptics_enabled"
    private const val KEY_CAMERA = "camera_mode"
    private const val KEY_LIGHT = "ambient_light"
    private const val KEY_SPEED = "game_speed"
    private const val KEY_PRO = "is_pro_no_ads"
    private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
    private const val KEY_MODE_HINTS = "mode_hints_seen"

    const val DEFAULT_CAMERA = "TOP_2D"
    const val DEFAULT_LIGHT = "WARM_CLUB"
    const val DEFAULT_SPEED = 1

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isMuted(context: Context): Boolean = prefs(context).getBoolean(KEY_MUTED, false)

    fun setMuted(context: Context, muted: Boolean) {
        prefs(context).edit().putBoolean(KEY_MUTED, muted).apply()
    }

    fun isHapticsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_HAPTICS, true)

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HAPTICS, enabled).apply()
    }

    /** Nombre del enum `CameraViewMode` elegido en la mesa (2D / 3D / 3D+). */
    fun cameraMode(context: Context): String =
        prefs(context).getString(KEY_CAMERA, DEFAULT_CAMERA) ?: DEFAULT_CAMERA

    fun setCameraMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_CAMERA, mode).apply()
    }

    /** Nombre del enum `AmbientLightMode` elegido en la mesa. */
    fun ambientLight(context: Context): String =
        prefs(context).getString(KEY_LIGHT, DEFAULT_LIGHT) ?: DEFAULT_LIGHT

    fun setAmbientLight(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_LIGHT, mode).apply()
    }

    /** Velocidad de la IA: 1x o 2x. */
    fun gameSpeed(context: Context): Int = prefs(context).getInt(KEY_SPEED, DEFAULT_SPEED)

    fun setGameSpeed(context: Context, speed: Int) {
        prefs(context).edit().putInt(KEY_SPEED, speed).apply()
    }

    /** Estado de la compra integrada para eliminar la publicidad permanentemente. */
    fun isPro(context: Context): Boolean = prefs(context).getBoolean(KEY_PRO, false)

    fun setPro(context: Context, pro: Boolean) {
        prefs(context).edit().putBoolean(KEY_PRO, pro).apply()
    }

    // --- Tutorial de la primera partida ---

    /** Si el jugador ya ha visto el tutorial de tres pasos sobre la mesa. */
    fun isTutorialSeen(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TUTORIAL_SEEN, false)

    fun setTutorialSeen(context: Context, seen: Boolean) {
        prefs(context).edit().putBoolean(KEY_TUTORIAL_SEEN, seen).apply()
    }

    /** Vuelve a activar el tutorial para que salga en la próxima partida. */
    fun resetTutorial(context: Context) {
        prefs(context).edit().putBoolean(KEY_TUTORIAL_SEEN, false).apply()
    }

    /** Si ya se explicó la modalidad [mode] (nombre del enum `GameMode`). */
    fun hasSeenModeHint(context: Context, mode: String): Boolean =
        prefs(context).getStringSet(KEY_MODE_HINTS, emptySet())?.contains(mode) == true

    fun setModeHintSeen(context: Context, mode: String) {
        val actual = prefs(context).getStringSet(KEY_MODE_HINTS, emptySet()).orEmpty()
        prefs(context).edit().putStringSet(KEY_MODE_HINTS, actual + mode).apply()
    }
}
