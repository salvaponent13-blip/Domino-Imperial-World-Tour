package com.example.dominograndprix

import com.example.dominograndprix.i18n.AppLanguage
import com.example.dominograndprix.i18n.DominoStrings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Textos de accesibilidad de las fichas.
 *
 * Lo que se fija aqui es lo que un lector de pantalla acaba diciendo en voz alta. Los numeros
 * del domino se deletrean ("Ficha seis tres") y no se leen como cifras, porque los lectores
 * leen los digitos de uno en uno y "6 3" suena a "sesenta y tres" o a una fecha.
 */
class TileDescriptionTest {

    @After
    fun restoreLanguage() {
        DominoStrings.setLanguage(AppLanguage.ES)
    }

    private val expectedLanguages = listOf(
        AppLanguage.ES, AppLanguage.EN, AppLanguage.FR,
        AppLanguage.IT, AppLanguage.PT, AppLanguage.DE
    )

    /** Quita acentos para comparar palabras entre idiomas (portugues lleva "três"). */
    private fun String.unaccented(): String =
        java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()

    @Test
    fun everyLanguageDescribesANormalTile() {
        // Palabras posibles para las caras de la ficha (6 y 3) en los 6 idiomas.
        val wordsForSix = listOf("6", "seis", "six", "sechs", "sei")
        val wordsForThree = listOf("3", "tres", "three", "drei", "trois", "tre")

        for (language in expectedLanguages) {
            DominoStrings.setLanguage(language)
            val description = DominoStrings.tileDescription(6, 3)
            val lower = description.unaccented()

            assertTrue("La descripcion en $language no puede estar vacia", description.isNotBlank())
            assertTrue(
                "La descripcion en $language deberia nombrar el 6: '$description'",
                wordsForSix.any { lower.contains(it) }
            )
            assertTrue(
                "La descripcion en $language deberia nombrar el 3: '$description'",
                wordsForThree.any { lower.contains(it) }
            )
        }
    }

    @Test
    fun differentTilesAreDescribedDifferently() {
        // Control de que la descripcion no es un texto fijo: si lo fuera, el lector de pantalla
        // diria siempre lo mismo y la mano seria indistinguible.
        for (language in expectedLanguages) {
            DominoStrings.setLanguage(language)
            val descriptions = mutableSetOf<String>()
            for (high in 0..6) {
                for (low in high..6) {
                    descriptions.add(DominoStrings.tileDescription(high, low))
                }
            }
            // 28 fichas posibles: los dobles comparten texto con su cara repetida ("doble seis"),
            // asi que se esperan al menos 21 descripciones distintas (7 dobles + 21 mixtas).
            assertTrue(
                "En $language solo hay ${descriptions.size} descripciones distintas para 28 fichas",
                descriptions.size >= 21
            )
        }
    }

    @Test
    fun spanishSpellsTheNumbersInsteadOfReadingDigits() {
        DominoStrings.setLanguage(AppLanguage.ES)
        // Es el punto del test: nada de "6 3", que un lector leeria como una cifra.
        val description = DominoStrings.tileDescription(6, 3)
        assertTrue("Deberia deletrear el seis: '$description'", description.contains("seis"))
        assertTrue("Deberia deletrear el tres: '$description'", description.contains("tres"))
        assertTrue(
            "El cero se dice 'blanco' en el domino hispanohablante: '${DominoStrings.tileDescription(0, 5)}'",
            DominoStrings.tileDescription(0, 5).contains("blanco")
        )
    }

    @Test
    fun doublesAreAnnouncedAsDoubles() {
        // Un doble es una jugada especial: conviene que el lector lo diga.
        for (language in expectedLanguages) {
            DominoStrings.setLanguage(language)
            val single = DominoStrings.tileDescription(6, 3)
            val double = DominoStrings.tileDescription(6, 6)
            assertNotEquals(
                "Un doble no puede describirse igual que una ficha normal en $language",
                single, double
            )
        }
    }

    @Test
    fun playabilityIsAnnouncedAsState() {
        for (language in expectedLanguages) {
            DominoStrings.setLanguage(language)
            val playable = DominoStrings.playableLabel()
            val unplayable = DominoStrings.unplayableLabel()
            assertTrue("La etiqueta de jugable no puede estar vacia en $language", playable.isNotBlank())
            assertTrue("La etiqueta de no jugable no puede estar vacia en $language", unplayable.isNotBlank())
            assertNotEquals(
                "Jugable y no jugable deben distinguirse en $language",
                playable, unplayable
            )
        }
    }

    @Test
    fun theClickActionTellsWhatWillHappen() {
        // TalkBack anuncia esto al enfocar una ficha de la mano: debe describir la accion.
        for (language in expectedLanguages) {
            DominoStrings.setLanguage(language)
            assertTrue(
                "La accion de pulsar debe estar descrita en $language",
                DominoStrings.tileClickAction().isNotBlank()
            )
        }
    }

    @Test
    fun unknownPipValuesDoNotCrash() {
        // Defensivo: un valor fuera de 0..6 (no deberia ocurrir) no puede tumbar la app al
        // construir la descripcion.
        DominoStrings.setLanguage(AppLanguage.ES)
        val description = DominoStrings.tileDescription(9, 2)
        assertTrue(description.isNotBlank())
    }
}
