package com.example.dominograndprix.data

/**
 * Mision de una sede.
 *
 * Las misiones cumplen la misma funcion que los logros de sede, pero se muestran ANTES de
 * jugar: son el motivo concreto para volver a una ciudad ya completada. Cada una se evalua con
 * una condicion pura sobre el informe de la partida.
 */
data class VenueMission(
    /** Identificador estable. Nunca se traduce ni cambia: es lo que se guarda. */
    val id: String,
    /** Sede a la que pertenece. */
    val venueId: String,
    /** Condicion que la cumple. Pura, para poder probarla sin jugar. */
    val isFulfilled: (MatchReport) -> Boolean
)

/**
 * Catalogo de misiones de sede.
 *
 * Igual que con los logros, las misiones completadas se guardan como identificadores dentro de
 * `JourneyProgress.unlockedAchievements`. Se reutiliza ese conjunto a proposito: una mision
 * completada y un logro conseguido son la misma clase de hecho ("este jugador hizo esto"), y
 * tener dos colecciones obligaria a migrar el guardado sin ganar nada.
 */
object VenueMissionCatalog {

    /**
     * Misiones por sede.
     *
     * Cuidado con las condiciones que pueden ser triviales segun el modo: "gana sin robar del
     * pozo" NO vale como mision en Puerto Rico, Montecarlo ni Las Vegas, porque sus modalidades
     * (Block, Block Hungarian y Matador) no tienen pozo y la mision se cumpliria sola. Todas las
     * condiciones de aqui exigen algo que el modo si permite hacer mal.
     */
    private val missionsByVenue: Map<String, List<VenueMission>> = mapOf(
        // La Habana: la primera sede, para aprender. Cerrar limpio y una ronda con fundamento.
        "cuba" to listOf(
            VenueMission("cuba_domino", "cuba") { it.won && it.dominos >= 1 },
            VenueMission("cuba_good_round", "cuba") { it.won && it.bestRoundPoints >= 25 }
        ),
        // Mexico: la sede de la puntuacion por multiplos de cinco.
        "mexico" to listOf(
            VenueMission("mexico_big_round", "mexico") { it.won && it.bestRoundPoints >= 30 },
            VenueMission("mexico_two_dominos", "mexico") { it.won && it.dominos >= 2 }
        ),
        // Puerto Rico: resistencia. Partida larga y tranca.
        "puerto_rico" to listOf(
            VenueMission("pr_tranca", "puerto_rico") { it.won && it.trancasWon >= 1 },
            VenueMission("pr_long_match", "puerto_rico") { it.won && it.roundsWon >= 3 }
        ),
        // Nueva Orleans: la ciudad del juego sucio. Ganar por la minima.
        "new_orleans" to listOf(
            VenueMission("no_close_call", "new_orleans") { it.won && it.wonWithSingleTileLeft },
            VenueMission("no_tranca", "new_orleans") { it.won && it.trancasWon >= 1 }
        ),
        // Tokio: precision. Encadenar rondas sin ceder ninguna.
        "tokyo" to listOf(
            VenueMission("tokyo_flawless", "tokyo") { it.won && it.roundsWon >= 3 },
            VenueMission("tokyo_master", "tokyo") { it.won && it.dominos >= 2 }
        ),
        // Montecarlo: alto riesgo, rondas gordas.
        "monte_carlo" to listOf(
            VenueMission("mc_big_round", "monte_carlo") { it.won && it.bestRoundPoints >= 40 },
            VenueMission("mc_domino", "monte_carlo") { it.won && it.dominos >= 2 }
        ),
        // Las Vegas: la sede final. Dominio sostenido.
        "las_vegas" to listOf(
            VenueMission("vegas_shutout", "las_vegas") { it.won && it.trancasWon + it.dominos >= 3 },
            VenueMission("vegas_double_domino", "las_vegas") { it.won && it.dominos >= 2 }
        )
    )

    /** Misiones de una sede, o lista vacia si no tiene. */
    fun missionsFor(venueId: String): List<VenueMission> =
        missionsByVenue[venueId].orEmpty()

    /** Todas las misiones del catalogo, para la interfaz y para los tests. */
    fun allMissions(): List<VenueMission> = missionsByVenue.values.flatten()

    /**
     * Misiones de [venueId] que cumple el informe y que aun no estaban completadas.
     *
     * Devolver solo las nuevas evita que la interfaz anuncie como recien conseguido algo que el
     * jugador logro hace veinte partidas.
     */
    fun newlyFulfilled(venueId: String, report: MatchReport, completed: Set<String>): Set<String> =
        missionsFor(venueId)
            .filter { it.id !in completed && it.isFulfilled(report) }
            .map { it.id }
            .toSet()

    /** Cuantas misiones tiene una sede. */
    fun countFor(venueId: String): Int = missionsFor(venueId).size

    /** Cuantas de una sede estan completadas. */
    fun completedCountFor(venueId: String, completed: Set<String>): Int =
        missionsFor(venueId).count { it.id in completed }
}
