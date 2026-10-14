package com.example.dominograndprix

import com.example.dominograndprix.ui.game.HandViewRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Que mano se muestra en la mesa.
 *
 * Este test existe por un defecto que llego a la Release Candidate: la pantalla pintaba la mano
 * del asiento que TENIA EL TURNO en el sitio del jugador. El sintoma que reporto el jugador fue
 * exacto: "cada vez que cambia el turno me salen sus fichas en mi posicion; cuando le toca a la
 * IA se ven claras, y cuando llega mi turno se ven turbias y borrosas".
 *
 * Las dos mitades de esa frase son el mismo error: la mano ajena se pintaba en el rack del
 * jugador, y ademas con el atenuado de "no jugable" activado, que le quitaba el relieve.
 */
class HandViewRulesTest {

    // --- Que mano se muestra ---

    @Test
    fun inSinglePlayerThePlayersOwnHandIsAlwaysShown() {
        // El caso central: aunque el turno sea de la maquina, abajo se ve MI mano. Si se
        // mostrara la del asiento activo, se estarian ensenando las fichas secretas del rival.
        val humanSeats = setOf(0)

        for (currentSeat in 0..3) {
            assertEquals(
                "Con el turno en el asiento $currentSeat, abajo debe verse mi mano (0)",
                0,
                HandViewRules.handSeat(
                    isLocalMatch = false,
                    currentSeat = currentSeat,
                    currentSeatIsHuman = currentSeat == 0,
                    lastHumanSeat = 0,
                    humanSeats = humanSeats
                )
            )
        }
    }

    @Test
    fun inSinglePlayerTheShownHandIsNeverAnotherSeat() {
        // Barrido defensivo: NINGUNA combinacion puede mostrar una mano que no sea la del
        // jugador. Es la propiedad que impide la fuga de informacion.
        for (currentSeat in 0..3) {
            for (currentIsHuman in listOf(true, false)) {
                val shown = HandViewRules.handSeat(
                    isLocalMatch = false,
                    currentSeat = currentSeat,
                    currentSeatIsHuman = currentIsHuman,
                    lastHumanSeat = 0,
                    humanSeats = setOf(0)
                )
                assertEquals("Nunca se debe mostrar otra mano que la del jugador", 0, shown)
            }
        }
    }

    @Test
    fun inPassAndPlayTheActingPersonSeesTheirOwnHand() {
        // 1v1 local: asientos 0 y 2 son personas. Cuando le toca a una, ve la suya.
        val humans = setOf(0, 2)

        assertEquals(
            0,
            HandViewRules.handSeat(true, currentSeat = 0, currentSeatIsHuman = true, lastHumanSeat = 0, humanSeats = humans)
        )
        assertEquals(
            2,
            HandViewRules.handSeat(true, currentSeat = 2, currentSeatIsHuman = true, lastHumanSeat = 0, humanSeats = humans)
        )
    }

    @Test
    fun inPassAndPlayWhileTheMachineThinksTheLastHumanHandStays() {
        // Mientras juega la IA, el movil lo tiene quien acaba de jugar: se sigue viendo su mano,
        // nunca la de la maquina.
        val shown = HandViewRules.handSeat(
            isLocalMatch = true,
            currentSeat = 1,
            currentSeatIsHuman = false,
            lastHumanSeat = 0,
            humanSeats = setOf(0, 2)
        )
        assertEquals("Mientras piensa la maquina se mantiene la ultima mano humana", 0, shown)
    }

    @Test
    fun theShownSeatIsAlwaysAHumanSeat() {
        // La propiedad que de verdad importa: la mano mostrada SIEMPRE pertenece a una persona.
        // Es lo contrario de lo que hacia el defecto, que mostraba la del asiento activo.
        val humans = setOf(0, 2)
        for (isLocal in listOf(true, false)) {
            for (currentSeat in 0..3) {
                for (currentIsHuman in listOf(true, false)) {
                    for (lastHuman in listOf(0, 2)) {
                        val shown = HandViewRules.handSeat(
                            isLocalMatch = isLocal,
                            currentSeat = currentSeat,
                            currentSeatIsHuman = currentIsHuman,
                            lastHumanSeat = lastHuman,
                            humanSeats = humans
                        )
                        assertTrue(
                            "Se iba a mostrar la mano del asiento $shown, que no es de una persona " +
                                "(local=$isLocal, turno=$currentSeat, turnoHumano=$currentIsHuman)",
                            shown in humans
                        )
                    }
                }
            }
        }
    }

    @Test
    fun aTableWithoutMarkedHumansFallsBackToSeatZeroInsteadOfCrashing() {
        // Defensivo: si nadie declaro asientos humanos, no puede reventar por indice invalido.
        assertEquals(
            0,
            HandViewRules.handSeat(false, 2, false, 0, humanSeats = emptySet())
        )
    }

    // --- Cuando se puede interactuar con la mano ---

    @Test
    fun theHandCanOnlyBePlayedWhenItsOwnerHasTheTurn() {
        assertTrue(
            "Turno de la persona y su propia mano: se puede jugar",
            HandViewRules.canInteractWithHand(
                handSeat = 0, currentSeat = 0, currentSeatIsHuman = true,
                handSeatIsHuman = true, isRoundOver = false, isPrivacyCurtainUp = false
            )
        )

        assertFalse(
            "Con el turno de la maquina no se juega",
            HandViewRules.canInteractWithHand(
                handSeat = 0, currentSeat = 1, currentSeatIsHuman = false,
                handSeatIsHuman = true, isRoundOver = false, isPrivacyCurtainUp = false
            )
        )
    }

    @Test
    fun youCanNeverPlayWithSomebodyElsesHand() {
        // Aunque el turno sea de una persona, si la mano mostrada es de OTRA, no se interactua.
        // Es la salvaguarda contra volver a mezclar los dos asientos.
        assertFalse(
            "No se juega con la mano de otro asiento",
            HandViewRules.canInteractWithHand(
                handSeat = 2, currentSeat = 0, currentSeatIsHuman = true,
                handSeatIsHuman = true, isRoundOver = false, isPrivacyCurtainUp = false
            )
        )
    }

    @Test
    fun thePrivacyCurtainAndTheEndOfTheRoundBlockInteraction() {
        assertFalse(
            "Con la cortina de pass-and-play puesta no se puede jugar",
            HandViewRules.canInteractWithHand(
                handSeat = 0, currentSeat = 0, currentSeatIsHuman = true,
                handSeatIsHuman = true, isRoundOver = false, isPrivacyCurtainUp = true
            )
        )
        assertFalse(
            "Con la ronda cerrada tampoco",
            HandViewRules.canInteractWithHand(
                handSeat = 0, currentSeat = 0, currentSeatIsHuman = true,
                handSeatIsHuman = true, isRoundOver = true, isPrivacyCurtainUp = false
            )
        )
    }
}
