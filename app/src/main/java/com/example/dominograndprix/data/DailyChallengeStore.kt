package com.example.dominograndprix.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

/**
 * Guarda el resultado del desafio diario: que dia se disputo, si se gano y la racha.
 *
 * Mismo patron que el resto de la persistencia del proyecto (una cadena JSON en
 * SharedPreferences), y por la misma razon: el progreso no puede depender de que la app siga
 * viva en memoria.
 */
object DailyChallengeStore {

    private const val PREFS_NAME = "domino_imperial_daily"
    private const val KEY_RESULT = "daily_result"

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Resultado guardado, o `null` si no hay ninguno o esta corrupto. */
    fun load(context: Context): DailyChallengeResult? {
        val raw = prefs(context).getString(KEY_RESULT, null) ?: return null
        return try {
            json.decodeFromString(DailyChallengeResult.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    fun save(context: Context, result: DailyChallengeResult) {
        try {
            prefs(context)
                .edit()
                .putString(KEY_RESULT, json.encodeToString(DailyChallengeResult.serializer(), result))
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
