package com.example.dominograndprix.data

import com.example.dominograndprix.model.CityVenue

/**
 * Economia de la gira: cobro de inscripcion, premio y red de seguridad por bancarrota.
 *
 * Vive aparte de la interfaz y como funciones puras porque el dinero es lo mas facil de
 * desincronizar de todo el juego: un error aqui no da un fallo visible, da un jugador que gana
 * partidas y se queda sin monedas, o al contrario.
 */
object CareerEconomy {

    /** Entrada minima del circuito: la de La Habana, la primera sede. */
    const val MINIMUM_ENTRY_BET = 100

    /** Cuanto concede el club al jugador en quiebra. */
    const val BANKRUPTCY_RESCUE_AMOUNT = 150

    /**
     * Si el jugador puede pagar la entrada de una sede.
     *
     * Es la unica condicion para bloquear el boton de viajar: perder la apuesta no puede dejarte
     * con la posibilidad de apostar dinero que no tienes.
     */
    fun canAfford(progress: JourneyProgress, venue: CityVenue): Boolean =
        progress.cash >= venue.entryBet

    /** Las sedes que el jugador puede permitirse ahora mismo, para orientarle si esta tieso. */
    fun affordableVenues(progress: JourneyProgress, venues: List<CityVenue>): List<CityVenue> =
        venues.filter { it.isUnlocked && canAfford(progress, it) }

    /**
     * Cobra la inscripcion al entrar a una sede.
     *
     * Nunca deja la caja en negativo: si no alcanza, no se cobra nada y devuelve `null`, para que
     * el llamante sepa que la entrada no se pudo pagar en lugar de encontrarse con un descuadre.
     */
    fun chargeEntry(progress: JourneyProgress, venue: CityVenue): JourneyProgress? {
        if (!canAfford(progress, venue)) return null
        return progress.copy(cash = progress.cash - venue.entryBet)
    }

    /**
     * Aplica el resultado de una partida a la caja.
     *
     * Al ganar se ingresan las monedas del premio; al perder NO se devuelve nada, porque la
     * inscripcion ya se cobro al entrar. La apuesta perdida no se resta aqui: restarla otra vez
     * la cobraria dos veces.
     */
    fun settle(progress: JourneyProgress, won: Boolean, prize: Int): JourneyProgress =
        progress.copy(cash = progress.cash + if (won) prize else 0)

    /**
     * Rescate de cortesia del club si el jugador se queda sin poder jugar.
     *
     * Se concede UNA sola vez en toda la carrera. Se marca en el progreso para que un jugador en
     * quiebra permanente no reciba un prestamo en cada derrota: entonces el dinero volveria a ser
     * infinito y la economia entera perderia el sentido.
     *
     * @return el progreso con el prestamo, o `null` si no procede (le queda dinero, o ya lo uso).
     */
    fun grantBankruptcyRescue(progress: JourneyProgress): JourneyProgress? {
        if (progress.bankruptcyRescueClaimed) return null
        if (progress.cash >= MINIMUM_ENTRY_BET) return null
        return progress.copy(
            cash = progress.cash + BANKRUPTCY_RESCUE_AMOUNT,
            bankruptcyRescueClaimed = true
        )
    }
}
