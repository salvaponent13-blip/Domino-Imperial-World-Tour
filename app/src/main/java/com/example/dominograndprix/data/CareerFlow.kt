package com.example.dominograndprix.data

/**
 * Transiciones de la carrera del Torneo Mundial.
 *
 * Viven aqui y no dentro de la pantalla para poder probarlas: son las que deciden que pasa al
 * ganar un reto y al volver a empezar la gira.
 *
 * El defecto que arreglan: al ganar el ultimo reto de la ultima sede, el progreso se quedaba
 * en "Reto 5 de 5" para siempre. El menu seguia diciendo "CONTINUAR TORNEO" y volvia a jugar
 * la misma final, sin forma de empezar la gira otra vez sin borrar trofeos y logros.
 */
object CareerFlow {

    /** Sedes de la gira. */
    const val VENUES = 7

    /** Retos por sede. */
    const val CHALLENGES_PER_VENUE = 5

    /** Total de retos de la gira (35). */
    const val TOTAL_CHALLENGES = VENUES * CHALLENGES_PER_VENUE

    /**
     * Progreso despues de ganar el reto [challengeIndex] de la sede [venueIndex].
     *
     * Si era el ultimo reto de una sede intermedia, avanza a la siguiente y la desbloquea.
     * Si era el ultimo de la ultima sede, marca el torneo como terminado y deja el progreso
     * apuntando a ese reto (la pantalla de sede lo narra como "torneo completado").
     */
    fun afterWin(
        progress: JourneyProgress,
        venueIndex: Int,
        challengeIndex: Int,
        venueId: String
    ): JourneyProgress {
        val venue = venueIndex.coerceIn(0, VENUES - 1)
        val challenge = challengeIndex.coerceIn(0, CHALLENGES_PER_VENUE - 1)
        val completed = progress.completedVenueIds + venueId
        val unlocked = progress.unlockedVenueIds.toMutableSet()

        var nextVenue = venue
        var nextChallenge = challenge + 1
        var finished = progress.tournamentFinished

        if (challenge == CHALLENGES_PER_VENUE - 1) {
            if (venue < VENUES - 1) {
                nextVenue = venue + 1
                nextChallenge = 0
                TournamentCampaign.VENUE_IDS.getOrNull(nextVenue)?.let { unlocked.add(it) }
            } else {
                // Ultima sede y ultimo reto: la gira esta completa.
                nextChallenge = CHALLENGES_PER_VENUE - 1
                finished = true
            }
        }

        return progress.copy(
            currentVenueIndex = nextVenue,
            currentChallengeIndex = nextChallenge,
            completedVenueIds = completed,
            unlockedVenueIds = unlocked,
            tournamentFinished = finished
        )
    }

    /**
     * Vuelve a empezar la gira por la primera sede.
     *
     * Conserva trofeos (sedes completadas), logros y estadisticas: repetir el torneo no es
     * borrar la coleccion, eso es lo que hace "Nuevo torneo".
     */
    fun replayTour(progress: JourneyProgress): JourneyProgress = progress.copy(
        currentVenueIndex = 0,
        currentChallengeIndex = 0,
        tournamentFinished = false
    )

    /** El jugador ya ha terminado la gira y toca ofrecerle repetirla. */
    fun isTourFinished(progress: JourneyProgress): Boolean = progress.tournamentFinished
}
