package com.example.dominograndprix

import com.example.dominograndprix.ui.game.PrivacyCurtainRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas de la cortina de privacidad del pass-and-play.
 *
 * El criterio de aceptacion del Sprint 6 dice: "ningun jugador humano ve las fichas del
 * siguiente al entregar el dispositivo". Lo unico de eso verificable sin un telefono es la
 * decision de cuando hay que levantar la cortina, que es lo que se fija aqui.
 */
class PrivacyCurtainRulesTest {

    private fun raise(
        humanSeatCount: Int = 2,
        previousActingSeat: Int = 0,
        previousSeatWasHuman: Boolean = true,
        currentSeat: Int = 2,
        currentSeatIsHuman: Boolean = true,
        isRoundOver: Boolean = false,
        isMatchOver: Boolean = false
    ) = PrivacyCurtainRules.shouldRaiseCurtain(
        humanSeatCount, previousActingSeat, previousSeatWasHuman,
        currentSeat, currentSeatIsHuman, isRoundOver, isMatchOver
    )

    @Test
    fun raisesWhenTheTurnGoesFromOneHumanToAnother() {
        // El caso central: 1v1 local, del asiento 0 al 2.
        assertTrue(raise(previousActingSeat = 0, currentSeat = 2))
        assertTrue(raise(previousActingSeat = 2, currentSeat = 0))
    }

    @Test
    fun doesNotRaiseInSinglePlayerMatches() {
        // En el modo carrera no hay nada que ocultar: solo hay una persona.
        assertFalse(raise(humanSeatCount = 1, previousActingSeat = 0, currentSeat = 0))
    }

    @Test
    fun doesNotRaiseWhenTheTurnComesFromTheMachine() {
        // Si venia de la IA, nadie estaba mirando una mano propia que ocultar.
        assertFalse(raise(previousActingSeat = 1, previousSeatWasHuman = false, currentSeat = 2))
    }

    @Test
    fun doesNotRaiseWhenGoingToTheMachine() {
        // Si el turno pasa a la IA, la pantalla puede seguir mostrando la mesa con normalidad.
        assertFalse(raise(previousActingSeat = 0, currentSeat = 1, currentSeatIsHuman = false))
    }

    @Test
    fun doesNotRaiseIfTheSamePersonKeepsTheTurn() {
        // Encadenamiento hungaro: el mismo jugador sigue tirando, no hay traspaso.
        assertFalse(raise(previousActingSeat = 2, currentSeat = 2))
    }

    @Test
    fun doesNotRaiseOnTheFirstTurnOfTheGame() {
        assertFalse(raise(previousActingSeat = -1, currentSeat = 0))
    }

    @Test
    fun doesNotRaiseWithTheRoundOrTheMatchClosed() {
        // Con la ronda cerrada sale el dialogo de resultado: una cortina encima seria un estorbo.
        assertFalse(raise(isRoundOver = true))
        assertFalse(raise(isMatchOver = true))
    }

    @Test
    fun fourHumansRaiseOnEveryHandover() {
        // Con cuatro personas, cada traspaso debe levantar la cortina.
        val seats = listOf(0, 1, 2, 3)
        seats.zipWithNext().forEach { (from, to) ->
            assertTrue(
                "El traspaso de $from a $to debe levantar la cortina",
                raise(humanSeatCount = 4, previousActingSeat = from, currentSeat = to)
            )
        }
        assertTrue(raise(humanSeatCount = 4, previousActingSeat = 3, currentSeat = 0))
    }

    @Test
    fun neverRaisesWhenThereIsNobodyToHideFrom() {
        // Barrido defensivo: en una mesa de una sola persona NINGUNA combinacion puede levantar
        // la cortina, porque no hay fichas ajenas que esconder.
        for (previous in -1..3) {
            for (current in 0..3) {
                for (previousHuman in listOf(true, false)) {
                    assertFalse(
                        "No debe levantarse con un solo humano (previo=$previous, actual=$current)",
                        raise(
                            humanSeatCount = 1,
                            previousActingSeat = previous,
                            previousSeatWasHuman = previousHuman,
                            currentSeat = current
                        )
                    )
                }
            }
        }
    }
}
