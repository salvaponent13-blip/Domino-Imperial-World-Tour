package com.example.dominograndprix.data

import android.content.Context
import android.content.SharedPreferences
import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.model.LuxuryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Estado persistente de la carrera World Tour: progreso de las sedes (desbloqueadas y
 * completadas), logros y estadisticas acumuladas.
 *
 * El juego no tiene moneda: el objetivo es completar la gira, y los trofeos son recuerdos,
 * no dinero. Cada campo tiene valor por defecto, asi que un guardado antiguo o incompleto se
 * carga sin fallos y las claves que falten arrancan en su valor inicial.
 */
@Serializable
data class JourneyProgress(
    val completedVenueIds: Set<String> = emptySet(),
    val unlockedVenueIds: Set<String> = emptySet(),
    /**
     * Logros conseguidos, por identificador del catalogo.
     *
     * Tiene valor por defecto para que los guardados anteriores a esta version carguen sin
     * fallos: quien ya tenga progreso seguira teniendolo y empezara sin logros.
     */
    val unlockedAchievements: Set<String> = emptySet(),
    /** Estadisticas acumuladas de la carrera. */
    val stats: CareerStats = CareerStats(),
    val currentVenueIndex: Int = 0,
    val currentChallengeIndex: Int = 0
)

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

    /**
     * Captura el estado actual de la carrera en un [JourneyProgress] listo para guardar.
     *
     * Se usa UNION con lo que ya hay en [progress]: las listas del catalogo no se actualizan al
     * jugar, asi que reemplazar por lo que dicen esas listas borraba las sedes completadas y
     * desbloqueadas durante la sesion. La union nunca puede quitar progreso, solo anadir.
     */
    fun snapshot(
        progress: JourneyProgress,
        venues: List<CityVenue>
    ): JourneyProgress = progress.copy(
        completedVenueIds = progress.completedVenueIds + venues.filter { it.isCompleted }.map { it.id },
        unlockedVenueIds = progress.unlockedVenueIds + venues.filter { it.isUnlocked }.map { it.id }
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
}
