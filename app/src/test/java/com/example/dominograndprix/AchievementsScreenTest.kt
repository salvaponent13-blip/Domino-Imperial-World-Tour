package com.example.dominograndprix

import com.example.dominograndprix.data.AchievementCatalog
import com.example.dominograndprix.i18n.AchievementStrings
import com.example.dominograndprix.i18n.AppLanguage
import com.example.dominograndprix.data.VenueMissionCatalog
import com.example.dominograndprix.ui.achievements.achievementProgress
import com.example.dominograndprix.ui.achievements.achievementRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vitrina de logros: lo que se muestra y lo que no.
 *
 * La vitrina no puede depender de un dispositivo para comprobarse, asi que la logica de que
 * filas se pintan y como se cuenta el progreso vive en funciones puras. Eso es lo que se fija
 * aqui; que los colores y el diseno se vean bien es cosa de la sesion de QA.
 */
class AchievementsScreenTest {

    private val allLanguages = listOf(
        AppLanguage.ES, AppLanguage.EN, AppLanguage.FR,
        AppLanguage.IT, AppLanguage.PT, AppLanguage.DE
    )

    // --- Filas de la vitrina ---

    @Test
    fun theShowcaseListsTheWholeCatalogNotOnlyWhatIsUnlocked() {
        // Un logro bloqueado con su descripcion es lo que le dice al jugador que hay mas que
        // hacer: esconderlo vaciaria de sentido la vitrina.
        val rows = achievementRows(unlockedIds = emptySet())

        assertEquals(AchievementCatalog.ALL.size, rows.size)
        assertTrue("Ninguna fila puede salir desbloqueada", rows.none { it.unlocked })
    }

    @Test
    fun theShowcaseKeepsTheCatalogOrder() {
        // El orden no puede bailar entre visitas.
        val rows = achievementRows(unlockedIds = setOf(AchievementCatalog.TOUGH_CROWN))
        assertEquals(AchievementCatalog.ALL, rows.map { it.id })
    }

    @Test
    fun unlockedAchievementsAreMarkedAndTheRestAreNot() {
        val unlocked = setOf(AchievementCatalog.FIRST_BLOOD, AchievementCatalog.VETERAN)
        val rows = achievementRows(unlocked)

        assertEquals(2, rows.count { it.unlocked })
        assertTrue(rows.first { it.id == AchievementCatalog.FIRST_BLOOD }.unlocked)
        assertTrue(rows.first { it.id == AchievementCatalog.VETERAN }.unlocked)
        assertFalse(rows.first { it.id == AchievementCatalog.DOMINO_MASTER }.unlocked)
    }

    @Test
    fun unknownIdentifiersInTheSaveDoNotBreakTheShowcase() {
        // Un guardado con identificadores que ya no existen (o de una version futura) no puede
        // romper la pantalla ni inventar filas.
        val rows = achievementRows(setOf("logro_que_ya_no_existe", AchievementCatalog.FIRST_BLOOD))
        assertEquals(AchievementCatalog.ALL.size, rows.size)
        assertEquals(1, rows.count { it.unlocked })
    }

    // --- Progreso ---

    @Test
    fun progressCountsUnlockedOverTotal() {
        val progress = achievementProgress(setOf(AchievementCatalog.FIRST_BLOOD))
        assertEquals(1, progress.unlocked)
        assertEquals(AchievementCatalog.ALL.size, progress.total)
        assertEquals((100 / AchievementCatalog.ALL.size), progress.percent)
    }

    @Test
    fun progressIsZeroWithNothingUnlocked() {
        val progress = achievementProgress(emptySet())
        assertEquals(0, progress.unlocked)
        assertEquals(0, progress.percent)
    }

    @Test
    fun progressIsOneHundredPercentWithEverythingUnlocked() {
        val progress = achievementProgress(AchievementCatalog.ALL.toSet())
        assertEquals(progress.total, progress.unlocked)
        assertEquals(100, progress.percent)
    }

    @Test
    fun anUnlockedAchievementIsReflectedInTheCountImmediately() {
        // El punto que pide el sprint: conseguir un logro tiene que verse en el conteo de la
        // vitrina sin reiniciar nada.
        val before = achievementProgress(emptySet())
        val after = achievementProgress(setOf(AchievementCatalog.FIRST_BLOOD))

        assertEquals("Antes no habia ninguno", 0, before.unlocked)
        assertEquals("Despues hay uno", 1, after.unlocked)
        assertTrue(
            "El porcentaje debe subir, no quedarse igual",
            after.percent > before.percent
        )
    }

    @Test
    fun progressIgnoresUnknownIdentifiersSoItCannotExceedOneHundredPercent() {
        // Defensivo: si el conteo contara identificadores desconocidos, el porcentaje podria
        // pasar de 100 y salirse de la barra de progreso.
        val progress = achievementProgress(
            AchievementCatalog.ALL.toSet() + setOf("inventado_1", "inventado_2")
        )
        assertEquals(progress.total, progress.unlocked)
        assertEquals(100, progress.percent)
    }

    // --- Textos ---

    @Test
    fun everyAchievementHasATitleAndADescriptionInEveryLanguage() {
        // Una vitrina con textos vacios es peor que no tener vitrina.
        for (language in allLanguages) {
            for (id in AchievementCatalog.ALL) {
                val title = AchievementStrings.titleFor(id, language)
                val description = AchievementStrings.descriptionFor(id, language)
                assertTrue("Falta el titulo de $id en $language", title.isNotBlank())
                assertTrue("Falta la descripcion de $id en $language", description.isNotBlank())
                assertFalse(
                    "El titulo de $id en $language no puede ser el identificador crudo",
                    title == id
                )
            }
        }
    }

    @Test
    fun everyMissionHasATitleAndADescriptionInEveryLanguage() {
        for (language in allLanguages) {
            for (mission in VenueMissionCatalog.allMissions()) {
                val title = AchievementStrings.missionTitleFor(mission, language)
                val description = AchievementStrings.missionDescriptionFor(mission, language)
                assertTrue("Falta el titulo de ${mission.id} en $language", title.isNotBlank())
                assertTrue("Falta la descripcion de ${mission.id} en $language", description.isNotBlank())
                assertFalse(
                    "El titulo de ${mission.id} en $language no puede ser el identificador crudo",
                    title == mission.id
                )
            }
        }
    }

    @Test
    fun descriptionsDifferBetweenUnlockedAndLockedGoals() {
        // La descripcion explica COMO conseguirlo, asi que no puede ser el titulo repetido.
        for (language in allLanguages) {
            for (id in AchievementCatalog.ALL) {
                assertFalse(
                    "En $language, la descripcion de $id no puede ser igual que su titulo",
                    AchievementStrings.titleFor(id, language) ==
                        AchievementStrings.descriptionFor(id, language)
                )
            }
        }
    }

    @Test
    fun everyCatalogIdentifierHasLocalizedText() {
        // Control de que el catalogo y los textos no se separan: si alguien anade un logro y
        // olvida sus textos, la vitrina lo mostraria con el identificador crudo.
        val missingTitles = AchievementCatalog.ALL.toSet() - AchievementStrings.localizedAchievementIds()
        assertTrue("Hay logros sin texto en la vitrina: $missingTitles", missingTitles.isEmpty())

        val missionIds = VenueMissionCatalog.allMissions().map { it.id }.toSet()
        val missingMissions = missionIds - AchievementStrings.localizedMissionIds()
        assertTrue("Hay misiones sin texto en la vitrina: $missingMissions", missingMissions.isEmpty())
    }

    @Test
    fun noTwoAchievementsShareTheSameTitleInTheSameLanguage() {
        // Dos logros con el mismo nombre serian indistinguibles en la lista.
        for (language in allLanguages) {
            val titles = AchievementCatalog.ALL.map { AchievementStrings.titleFor(it, language) }
            assertEquals(
                "Hay titulos repetidos en $language: $titles",
                titles.size,
                titles.toSet().size
            )
        }
    }
}
