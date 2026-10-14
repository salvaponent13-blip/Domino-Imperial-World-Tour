package com.example.dominograndprix

import com.example.dominograndprix.data.CareerEconomy
import com.example.dominograndprix.data.JourneyProgress
import com.example.dominograndprix.data.WorldTourRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Economia de la gira.
 *
 * El dinero es lo mas facil de descuadrar de todo el juego: un error aqui no da un fallo
 * visible, da un jugador que gana partidas y se queda sin monedas. Por eso cada regla tiene su
 * test, incluida la que impide que el rescate por bancarrota se convierta en dinero infinito.
 */
class CareerEconomyTest {

    /**
     * Sedes reales del catalogo, no objetos inventados: asi el test comprueba tambien que las
     * entradas de verdad son las que se espera.
     */
    private val realVenues = WorldTourRepository.getInitialVenues()
    private fun realVenue(id: String) = realVenues.first { it.id == id }

    /** Sede de prueba con la entrada que interese, partiendo de una real. */
    private fun venue(id: String = "cuba", bet: Int = 100, unlocked: Boolean = true) =
        realVenue("cuba").copy(id = id, entryBet = bet, isUnlocked = unlocked)

    // --- Cobro de inscripcion ---

    @Test
    fun enteringAVenueChargesTheEntryBetImmediately() {
        val charged = CareerEconomy.chargeEntry(JourneyProgress(cash = 1000), venue(bet = 300))

        assertNotNull("Con dinero de sobra, la entrada se cobra", charged)
        assertEquals(700, charged!!.cash)
    }

    @Test
    fun enteringWithoutEnoughMoneyChargesNothing() {
        // No puede quedar caja negativa: si no alcanza, no se cobra y se avisa devolviendo null.
        val result = CareerEconomy.chargeEntry(JourneyProgress(cash = 300), venue(bet = 1000))
        assertNull("No se puede cobrar una entrada impagable", result)
    }

    @Test
    fun affordingIsDecidedByCashVersusEntryBet() {
        val progress = JourneyProgress(cash = 500)
        assertTrue(CareerEconomy.canAfford(progress, venue(bet = 500)))
        assertTrue(CareerEconomy.canAfford(progress, venue(bet = 100)))
        assertFalse(CareerEconomy.canAfford(progress, venue(bet = 501)))
    }

    @Test
    fun affordableVenuesOnlyListsUnlockedAndPayableOnes() {
        val progress = JourneyProgress(cash = 400)
        val venues = listOf(
            venue("cuba", bet = 100, unlocked = true),
            venue("mexico", bet = 200, unlocked = true),
            venue("puerto_rico", bet = 350, unlocked = true),
            venue("new_orleans", bet = 500, unlocked = true),
            venue("tokyo", bet = 800, unlocked = false)
        )
        val affordable = CareerEconomy.affordableVenues(progress, venues).map { it.id }

        assertEquals(listOf("cuba", "mexico", "puerto_rico"), affordable)
    }

    // --- Liquidacion del resultado ---

    @Test
    fun winningPaysThePrize() {
        // Cobrada la entrada, ganar ingresa el premio.
        val afterEntry = CareerEconomy.chargeEntry(JourneyProgress(cash = 1000), venue(bet = 300))!!
        val settled = CareerEconomy.settle(afterEntry, won = true, prize = 600)

        assertEquals(1300, settled.cash)
    }

    @Test
    fun losingReturnsNothingAtAll() {
        // La inscripcion ya se cobro al entrar: perder no devuelve NADA. La caja se queda como
        // estaba tras pagar la entrada, sin restar la apuesta otra vez.
        val afterEntry = CareerEconomy.chargeEntry(JourneyProgress(cash = 1000), venue(bet = 300))!!
        val settled = CareerEconomy.settle(afterEntry, won = false, prize = 600)

        assertEquals("El premio no puede cobrarse perdiendo", 700, settled.cash)
    }

    @Test
    fun aFullWinCycleLeavesThePlayerRicherAndALossLeavesThemPoorer() {
        val start = JourneyProgress(cash = 1000)
        val bet = 200

        val afterWin = CareerEconomy.settle(
            CareerEconomy.chargeEntry(start, venue(bet = bet))!!, won = true, prize = bet * 2
        )
        val afterLoss = CareerEconomy.settle(
            CareerEconomy.chargeEntry(start, venue(bet = bet))!!, won = false, prize = bet * 2
        )

        assertEquals("Ganar deja al jugador con 200 mas", 1200, afterWin.cash)
        assertEquals("Perder deja al jugador con 200 menos", 800, afterLoss.cash)
    }

    // --- Red de seguridad por bancarrota ---

    @Test
    fun bankruptcyGrantsTheRescueWhenThePlayerCannotAffordTheCheapestVenue() {
        val rescued = CareerEconomy.grantBankruptcyRescue(JourneyProgress(cash = 40))

        assertNotNull("Con 40 monedas no se puede jugar ni en La Habana", rescued)
        assertEquals(190, rescued!!.cash)
        assertTrue("Debe quedar marcado para no repetirse", rescued.bankruptcyRescueClaimed)
    }

    @Test
    fun theRescueIsGrantedOnlyOnceInTheWholeCareer() {
        // Es lo que impide que el rescate se convierta en dinero infinito: si no, un jugador en
        // quiebra permanente recibiria un prestamo en cada derrota.
        val first = CareerEconomy.grantBankruptcyRescue(JourneyProgress(cash = 0))!!
        assertTrue(first.bankruptcyRescueClaimed)

        // Se vuelve a quedar sin dinero.
        val brokeAgain = first.copy(cash = 10)
        assertNull("El segundo rescate no puede concederse", CareerEconomy.grantBankruptcyRescue(brokeAgain))
    }

    @Test
    fun theRescueDoesNotTriggerWhileThePlayerCanStillPlay() {
        assertNull(
            "Con 100 monedas justas se puede jugar en La Habana",
            CareerEconomy.grantBankruptcyRescue(JourneyProgress(cash = 100))
        )
        assertNull(
            "Con dinero de sobra, desde luego",
            CareerEconomy.grantBankruptcyRescue(JourneyProgress(cash = 5000))
        )
    }

    @Test
    fun theRescueIsEnoughToPlayTheCheapestVenueAgain() {
        // Se concede para "volver al ruedo", asi que tiene que alcanzar para la entrada mas
        // barata: un rescate que no permite jugar no rescata nada.
        val rescued = CareerEconomy.grantBankruptcyRescue(JourneyProgress(cash = 0))!!
        assertTrue(
            "El rescate debe permitir jugar en la sede mas barata",
            CareerEconomy.canAfford(rescued, venue(bet = CareerEconomy.MINIMUM_ENTRY_BET))
        )
    }

    @Test
    fun bankruptcyRescueClaimedSurvivesTheJsonRoundTrip() {
        // Sin persistencia, cerrar la app devolveria el rescate y volveria a ser infinito.
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val original = JourneyProgress(cash = 190, bankruptcyRescueClaimed = true)
        val restored = json.decodeFromString(
            JourneyProgress.serializer(),
            json.encodeToString(JourneyProgress.serializer(), original)
        )
        assertTrue(restored.bankruptcyRescueClaimed)
        assertEquals(190, restored.cash)
    }

    @Test
    fun anOldSaveWithoutTheRescueFlagStartsWithTheRescueAvailable() {
        // Un guardado de la version publicada no tiene el campo: debe poder recibir el rescate.
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val oldSave = """{"cash":20,"wrp":0}"""
        val loaded = json.decodeFromString(JourneyProgress.serializer(), oldSave)
        assertFalse(loaded.bankruptcyRescueClaimed)
        assertNotNull(
            "Un jugador de la version anterior sin dinero debe poder ser rescatado",
            CareerEconomy.grantBankruptcyRescue(loaded)
        )
    }
}
