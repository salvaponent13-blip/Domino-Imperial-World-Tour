package com.example.dominograndprix.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

/**
 * Guarda y recupera la partida en curso, para que el sistema no se lleve por delante el
 * progreso del jugador cuando mata el proceso.
 *
 * Mismo patron que [CareerProgressStore]: una unica cadena JSON en SharedPreferences. El
 * snapshot se escribe **al cerrar cada ronda**, que es el punto acordado, y se borra en
 * cuanto la partida termina (para que no reaparezca una partida ya jugada).
 */
object MatchSnapshotStore {

    private const val PREFS_NAME = "domino_imperial_match"
    private const val KEY_SNAPSHOT = "match_snapshot"

    private val json = Json {
        encodeDefaults = true
        // Tolerante con claves desconocidas: un guardado escrito por una version mas nueva
        // no debe tirar la app, y schemaVersion ya decide si se puede usar.
        ignoreUnknownKeys = true
    }

    /** Si hay una partida guardada en disco. */
    fun hasSnapshot(context: Context): Boolean =
        prefs(context).contains(KEY_SNAPSHOT)

    /**
     * Devuelve la partida guardada, o `null` si no hay, esta corrupta o es de un formato que
     * esta version no sabe interpretar.
     *
     * Ante cualquier duda se descarta en lugar de intentar interpretarla: arrancar con un
     * estado a medias seria peor que perder esa partida.
     */
    fun load(context: Context): MatchSnapshot? {
        val raw = prefs(context).getString(KEY_SNAPSHOT, null) ?: return null
        val snapshot = try {
            json.decodeFromString(MatchSnapshot.serializer(), raw)
        } catch (_: Exception) {
            null
        }
        if (snapshot == null || !snapshot.isCompatible) {
            clear(context)
            return null
        }
        return snapshot
    }

    /** Escribe la partida en curso. */
    fun save(context: Context, snapshot: MatchSnapshot) {
        try {
            prefs(context)
                .edit()
                .putString(KEY_SNAPSHOT, json.encodeToString(MatchSnapshot.serializer(), snapshot))
                .apply()
        } catch (_: Exception) {
            // Un fallo al guardar no debe interrumpir la partida en curso.
        }
    }

    /** Borra la partida guardada. Se llama al terminar el match. */
    fun clear(context: Context) {
        try {
            prefs(context).edit().remove(KEY_SNAPSHOT).apply()
        } catch (_: Exception) {
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
