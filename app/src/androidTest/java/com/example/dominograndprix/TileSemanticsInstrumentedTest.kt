package com.example.dominograndprix

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dominograndprix.i18n.AppLanguage
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.DominoTile
import com.example.dominograndprix.ui.components.DominoTileView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Semantica accesible de las fichas, en dispositivo.
 *
 * Este test EXISTE porque `DominoTileView` se dibuja sobre un `Canvas`: para TalkBack eso es un
 * rectangulo mudo, y sin semantica la mano del jugador es inaccesible. Los tests unitarios no
 * pueden comprobarlo, porque la semantica la resuelve el arbol de Compose en tiempo de
 * ejecucion.
 *
 * NO se puede ejecutar sin un dispositivo o emulador conectado:
 *     ./gradlew :app:connectedDebugAndroidTest
 *
 * Forma parte del plan de accesibilidad (punto 4.1) y esta escrito para que la comprobacion sea
 * inmediata en cuanto haya un telefono a mano.
 */
@RunWith(AndroidJUnit4::class)
class TileSemanticsInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showTile(
        high: Int,
        low: Int,
        isPlayable: Boolean = true,
        onClick: (() -> Unit)? = null
    ) {
        DominoStrings.setLanguage(AppLanguage.ES)
        composeRule.setContent {
            DominoTileView(
                tile = DominoTile(high, low),
                isPlayable = isPlayable,
                dimUnplayable = !isPlayable,
                width = 48.dp,
                height = 90.dp,
                onClick = onClick,
                modifier = Modifier.testTag("tile")
            )
        }
    }

    @Test
    fun tileExposesItsFacesToTheScreenReader() {
        showTile(high = 6, low = 3)

        // El lector debe poder anunciar QUE ficha es, no un rectangulo sin nombre.
        composeRule.onNodeWithContentDescription(DominoStrings.tileDescription(6, 3))
            .assertIsDisplayed()
    }

    @Test
    fun tileAnnouncesWhetherItCanBePlayed() {
        showTile(high = 6, low = 3, isPlayable = true)
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                DominoStrings.playableLabel()
            )
        ).assertIsDisplayed()

        showTile(high = 6, low = 3, isPlayable = false)
        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                DominoStrings.unplayableLabel()
            )
        ).assertIsDisplayed()
    }

    @Test
    fun playableTileCanBeActivatedWithoutSeeingIt() {
        // Un jugador invidente debe poder jugar: enfocar la ficha y activarla.
        var played = false
        showTile(high = 5, low = 5, isPlayable = true, onClick = { played = true })

        composeRule.onNodeWithContentDescription(DominoStrings.tileDescription(5, 5))
            .performClick()

        assertTrue("La ficha jugable debe poder activarse por accesibilidad", played)
    }

    @Test
    fun unplayableTileIsNotClickable() {
        // Y una que no se puede jugar no debe responder, o el jugador recibe una accion falsa.
        var played = false
        showTile(high = 5, low = 5, isPlayable = false, onClick = { played = true })

        composeRule.onNodeWithContentDescription(DominoStrings.tileDescription(5, 5))
            .performClick()

        assertEquals("Una ficha no jugable no debe activarse", false, played)
    }

    @Test
    fun doublesAreAnnouncedAsDoubles() {
        showTile(high = 4, low = 4)
        composeRule.onNodeWithContentDescription(DominoStrings.tileDescription(4, 4))
            .assertIsDisplayed()
    }
}
