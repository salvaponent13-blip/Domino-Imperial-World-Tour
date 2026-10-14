package com.example.dominograndprix.data

import com.example.dominograndprix.engine.DominoEngine
import com.example.dominograndprix.model.Difficulty

/**
 * Construye el informe de una partida terminada a partir del estado real del motor.
 *
 * Vive separado de la interfaz para que la traduccion de "estado del motor" a "estadisticas de
 * carrera" sea una funcion pura y comprobable: la interfaz no deberia decidir que cuenta como
 * domino ni como tranca.
 */
object CareerReporter {

    /**
     * @param engine motor ya terminado
     * @param venueId sede jugada, o null en partidas fuera de la gira (reto diario, local)
     * @param won si gano el bando del jugador
     * @param reward monedas ganadas, tal y como las reporta el motor
     * @param bet monedas apostadas al entrar, que se descuentan al perder
     * @param difficulty nivel del rival
     * @param isDailyChallenge si la partida era el desafio diario
     */
    fun reportFor(
        engine: DominoEngine,
        venueId: String?,
        won: Boolean,
        reward: Int,
        bet: Int,
        difficulty: Difficulty,
        isDailyChallenge: Boolean
    ): MatchReport {
        return MatchReport(
            venueId = venueId,
            won = won,
            // Al ganar se ingresan las monedas del premio; al perder se pierde lo apostado.
            cashDelta = if (won) reward else -bet,
            wrpDelta = if (won) 1500 else 0,
            difficulty = difficulty,
            // Los contadores de partida del motor, no los de la ultima ronda: una partida de
            // cinco rondas debe contar cinco.
            roundsPlayed = engine.matchRoundsPlayed,
            roundsWon = engine.matchRoundsWon,
            blockedRounds = engine.matchBlockedRounds,
            dominos = engine.matchDominos,
            trancasWon = engine.matchTrancasWon,
            pointsScored = engine.matchPointsScored,
            wonWithSingleTileLeft = engine.matchWinsWithSingleTile > 0,
            isDailyChallenge = isDailyChallenge
        )
    }
}
