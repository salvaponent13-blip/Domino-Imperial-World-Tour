package com.example.dominograndprix.data

import android.content.Context
import android.content.SharedPreferences
import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.model.LuxuryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Estado persistente de la carrera World Tour: caja, puntos WRP, progreso de las
 * ciudades (desbloqueadas / completadas) e items de lujo ya vendidos.
 *
 * Cada campo tiene valor por defecto, así que un guardado antiguo o incompleto
 * se carga sin fallos y las claves que falten simplemente arrancan en su valor inicial.
 */
@Serializable
data class JourneyProgress(
    val cash: Int = DEFAULT_CASH,
    val wrp: Int = DEFAULT_WRP,
    val completedVenueIds: Set<String> = emptySet(),
    val unlockedVenueIds: Set<String> = emptySet(),
    val soldTrophyIds: Set<String> = emptySet()
) {
    companion object {
        const val DEFAULT_CASH = 2500
        const val DEFAULT_WRP = 1500
    }
}

/**
 * Guarda y recupera el progreso de la carrera en SharedPreferences como una única
 * cadena JSON versionada con kotlinx.serialization.
 *
 * Es un objeto sin estado: se puede llamar desde cualquier punto y todos los métodos
 * son síncronos y baratos (el JSON resultante son unos pocos cientos de bytes), por lo
 * que es seguro invocarlos directamente desde el hilo principal en respuesta a cambios de UI.
 */
object CareerProgressStore {

    private const val PREFS_NAME = "domino_imperial_career"
    private const val KEY_PROGRESS = "journey_progress"
    private const val KEY_SAVED = "journey_saved"

    /**
     * Sin `ignoreUnknownKeys`: si el JSON guardado no encaja con el esquema actual
     * (por ejemplo tras un cambio incompatible de formato), la carga falla y devolvemos
     * un progreso inicial en lugar de propagar la excepción al arrancar la app.
     */
    private val json = Json {
        encodeDefaults = true
    }

    /** `true` solo si existe un progreso guardado con formato válido. */
    fun hasSavedProgress(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SAVED, false)

    /** Devuelve el progreso guardado o un progreso inicial si no hay nada válido en disco. */
    fun load(context: Context): JourneyProgress {
        val store = prefs(context)
        if (!store.getBoolean(KEY_SAVED, false)) return JourneyProgress()
        val raw = store.getString(KEY_PROGRESS, null) ?: return JourneyProgress()
        return try {
            json.decodeFromString(JourneyProgress.serializer(), raw)
        } catch (_: Exception) {
            JourneyProgress()
        }
    }

    /** Escribe el progreso en disco sobrescribiendo el guardado anterior. */
    fun save(context: Context, progress: JourneyProgress) {
        try {
            prefs(context)
                .edit()
                .putString(KEY_PROGRESS, json.encodeToString(JourneyProgress.serializer(), progress))
                .putBoolean(KEY_SAVED, true)
                .apply()
        } catch (_: Exception) {
            // Un fallo de escritura nunca debe tirar la partida en curso.
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Captura el estado actual de la carrera en un [JourneyProgress] listo para guardar. */
    fun snapshot(
        cash: Int,
        wrp: Int,
        venues: List<CityVenue>,
        trophies: List<LuxuryItem>
    ): JourneyProgress = JourneyProgress(
        cash = cash,
        wrp = wrp,
        completedVenueIds = venues.filter { it.isCompleted }.map { it.id }.toSet(),
        unlockedVenueIds = venues.filter { it.isUnlocked }.map { it.id }.toSet(),
        soldTrophyIds = trophies.filter { it.isSold }.map { it.id }.toSet()
    )

    /**
     * Aplica el progreso guardado sobre las listas del catálogo (que actúan como
     * valores por defecto), conservando su orden original.
     *
     * Se usa unión para desbloqueo/completado: una ciudad nunca vuelve a bloquearse
     * por culpa de un guardado antiguo, y una ciudad nueva del catálogo entra con los
     * valores por defecto que declare [WorldTourRepository].
     */
    fun applyToVenues(venues: List<CityVenue>, progress: JourneyProgress): List<CityVenue> =
        venues.map { venue ->
            val unlocked = venue.isUnlocked || venue.id in progress.unlockedVenueIds
            val completed = venue.isCompleted || venue.id in progress.completedVenueIds
            if (unlocked == venue.isUnlocked && completed == venue.isCompleted) {
                venue
            } else {
                venue.copy(isUnlocked = unlocked, isCompleted = completed)
            }
        }

    /** Marca como vendidos los items de lujo que ya se vendieron en partidas anteriores. */
    fun applyToTrophies(trophies: List<LuxuryItem>, progress: JourneyProgress): List<LuxuryItem> =
        trophies.map { item ->
            if (progress.soldTrophyIds.contains(item.id) && !item.isSold) {
                item.copy(isSold = true)
            } else {
                item
            }
        }
}
