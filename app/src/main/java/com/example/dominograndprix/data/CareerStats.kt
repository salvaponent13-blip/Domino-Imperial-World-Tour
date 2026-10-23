package com.example.dominograndprix.data

import com.example.dominograndprix.model.Difficulty
import kotlinx.serialization.Serializable

/**
 * Estadisticas acumuladas de la carrera.
 *
 * Se guardan numeros crudos, no logros: los logros se derivan de aqui. Asi, si mañana se anade
 * un logro nuevo, se puede desbloquear retroactivamente para quien ya cumple la condicion, y no
 * hay que guardar una lista de "cosas que pasaron" que crezca sin control.
 *
 * Todos los campos tienen valor por defecto, de modo que un guardado anterior a esta version
 * carga sin fallos: las claves que falten arrancan en cero.
 */
@Serializable
data class CareerStats(
    /** Partidas completas disputadas. */
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    /** Rondas cerradas por cualquiera de los dos bandos. */
    val roundsPlayed: Int = 0,
    val roundsWon: Int = 0,
    /** Rondas cerradas porque la mesa se quedo sin jugadas, con o sin victoria. */
    val blockedRounds: Int = 0,
    /** Rondas ganadas por tranca estando por delante en la cuenta de pips. */
    val trancasWon: Int = 0,
    /** Rondas ganadas vaciando la mano. */
    val dominos: Int = 0,
    /** Puntos de extremos anotados en los modos que puntuan. */
    val pointsScored: Int = 0,
    /** Rondas ganadas con una sola ficha en la mano al empezar el turno decisivo. */
    val lastTileWins: Int = 0,
    /** Victorias desglosadas por dificultad, que es lo que mide la progresion real. */
    val winsByDifficulty: Map<String, Int> = emptyMap()
) {
    /** Victorias contra el rival mas duro. */
    val toughWins: Int get() = winsByDifficulty[Difficulty.TOUGH.name] ?: 0

    /** Porcentaje de partidas ganadas, redondeado. */
    val winRatePercent: Int
        get() = if (matchesPlayed == 0) 0 else (matchesWon * 100) / matchesPlayed
}

/**
 * Lo que ocurrio en una partida, ya resumido para poder actualizar la carrera.
 *
 * Existe para que la capa de progreso no dependa del motor: recibe numeros, no un
 * `DominoEngine`. Eso permite probar toda la logica de logros sin simular partidas.
 */
@Serializable
data class MatchReport(
    val venueId: String?,
    val won: Boolean,




    val difficulty: Difficulty,
    /** Rondas jugadas en esta partida. */
    val roundsPlayed: Int,
    /** Rondas ganadas por el bando del jugador. */
    val roundsWon: Int,
    /** Rondas que terminaron en tranca. */
    val blockedRounds: Int,
    /** Rondas ganadas por el jugador cerrando con domino. */
    val dominos: Int,
    /** Rondas que el jugador gano estando la mesa trancada. */
    val trancasWon: Int,
    /** Puntos de extremos que anoto el jugador. */
    val pointsScored: Int,
    /** Mejor marcador del jugador en UNA sola ronda (lo piden las misiones de sede). */
    val bestRoundPoints: Int = 0,
    /** Si el jugador robo alguna vez del pozo en esta partida. */
    val drewFromBoneyard: Boolean = false,
    /** Si gano alguna ronda con una sola ficha en la mano. */
    val wonWithSingleTileLeft: Boolean,
    /** Si gano el desafio diario. */
    val isDailyChallenge: Boolean = false
)
