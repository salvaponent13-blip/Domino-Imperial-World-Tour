package com.example.dominograndprix

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dominograndprix.data.AchievementCatalog
import com.example.dominograndprix.i18n.AchievementStrings
import com.example.dominograndprix.i18n.AppLanguage
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.ui.achievements.AchievementCard
import com.example.dominograndprix.ui.achievements.AchievementRow
import com.example.dominograndprix.ui.achievements.AchievementsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renderizado de la vitrina de logros, en dispositivo.
 *
 * Comprueba lo que los tests unitarios no alcanzan: que la pantalla pinta de verdad lo que
 * decide la logica pura, y que un logro conseguido y uno bloqueado se distinguen en pantalla.
 *
 * NO se puede ejecutar sin un dispositivo o emulador conectado:
 *     ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class AchievementsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showShowcase(unlocked: Set<String>) {
        DominoStrings.setLanguage(AppLanguage.ES)
        composeRule.setContent {
            AchievementsScreen(unlockedIds = unlocked, onBack = {})
        }
    }

    @Test
    fun theShowcaseShowsTheProgressHeader() {
        showShowcase(setOf(AchievementCatalog.FIRST_BLOOD))

        // "Logros: 1 / 15 desbloqueados (6%)"
        composeRule.onNodeWithText(
            DominoStrings.achievementsProgress(
                unlocked = 1,
                total = AchievementCatalog.ALL.size,
                percent = 100 / AchievementCatalog.ALL.size
            )
        ).assertIsDisplayed()
    }

    @Test
    fun anUnlockedAchievementShowsItsTitleAndBadge() {
        showShowcase(setOf(AchievementCatalog.FIRST_BLOOD))

        composeRule.onNodeWithText(
            AchievementStrings.titleFor(AchievementCatalog.FIRST_BLOOD, AppLanguage.ES)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(DominoStrings.achievementUnlockedBadge()).assertIsDisplayed()
    }

    @Test
    fun aLockedAchievementShowsItsDescriptionSoThePlayerKnowsWhatToDo() {
        showShowcase(emptySet())

        // La descripcion de como conseguirlo tiene que estar visible, no solo el titulo.
        composeRule.onNodeWithText(
            AchievementStrings.descriptionFor(AchievementCatalog.DOMINO_MASTER, AppLanguage.ES)
        ).assertIsDisplayed()
    }

    @Test
    fun aCardRendersBothStatesDistinctly() {
        // El contraste entre conseguido y bloqueado no puede depender solo del color: cada
        // estado lleva su propio sello de texto.
        DominoStrings.setLanguage(AppLanguage.ES)
        composeRule.setContent {
            AchievementCard(AchievementRow(AchievementCatalog.FIRST_BLOOD, unlocked = true))
        }
        composeRule.onNodeWithText(DominoStrings.achievementUnlockedBadge()).assertIsDisplayed()

        composeRule.setContent {
            AchievementCard(AchievementRow(AchievementCatalog.FIRST_BLOOD, unlocked = false))
        }
        composeRule.onNodeWithText(DominoStrings.achievementLockedBadge()).assertIsDisplayed()
    }

    @Test
    fun unlockingMoreAchievementsRaisesTheHeaderCount() {
        // El punto 4 del sprint: el conteo de la vitrina refleja inmediatamente lo conseguido.
        showShowcase(emptySet())
        composeRule.onNodeWithText(
            DominoStrings.achievementsProgress(0, AchievementCatalog.ALL.size, 0)
        ).assertIsDisplayed()

        showShowcase(
            setOf(AchievementCatalog.FIRST_BLOOD, AchievementCatalog.DOMINO_MASTER)
        )
        composeRule.onNodeWithText(
            DominoStrings.achievementsProgress(
                unlocked = 2,
                total = AchievementCatalog.ALL.size,
                percent = (2 * 100) / AchievementCatalog.ALL.size
            )
        ).assertIsDisplayed()
    }
}
