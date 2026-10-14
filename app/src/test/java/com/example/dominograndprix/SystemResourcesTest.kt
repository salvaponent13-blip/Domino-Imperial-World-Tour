package com.example.dominograndprix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Recursos de SISTEMA por idioma.
 *
 * Android resuelve los textos del lanzador, del gestor de tareas y de los servicios de
 * accesibilidad con `res/values-<idioma>/strings.xml`, que es un mecanismo SEPARADO de los
 * textos internos del juego (`DominoStrings`). Es facil anadir un idioma al juego y olvidarse
 * del sistema, y el sintoma es que a un jugador con el movil en frances le sale el nombre de la
 * app en otro idioma en su pantalla de inicio.
 *
 * Este test recorre el arbol de recursos y comprueba que estan los seis idiomas y que ninguno
 * se ha quedado sin traduccion.
 */
class SystemResourcesTest {

    /** Los seis idiomas que el juego ofrece, con el sufijo de carpeta de Android. */
    private val languages = mapOf(
        "es (por defecto)" to "values",
        "en" to "values-en",
        "fr" to "values-fr",
        "it" to "values-it",
        "pt" to "values-pt",
        "de" to "values-de"
    )

    /**
     * Ruta relativa al directorio de trabajo del test JVM.
     *
     * Gradle ejecuta los tests unitarios con el directorio de trabajo puesto en el modulo
     * (`app/`), asi que `src/main/res` resuelve al arbol de recursos. Si algun dia se cambia esa
     * configuracion, este test fallara con "falta el fichero", que es un sintoma claro.
     */
    private val resDir: File = File("src/main/res")

    private fun stringsFile(folder: String): File = File(resDir, "$folder/strings.xml")

    /** Extrae el contenido de un `<string name="...">`. */
    private fun stringValue(folder: String, name: String): String? {
        val file = stringsFile(folder)
        if (!file.exists()) return null
        val text = file.readText()
        val regex = Regex("""<string name="$name"\s*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }

    @Test
    fun everyLanguageHasItsOwnSystemStrings() {
        // Si falta una carpeta, Android cae al idioma por defecto y el jugador ve el nombre de
        // la app en otro idioma.
        for ((language, folder) in languages) {
            assertTrue(
                "Falta el fichero de recursos de sistema para $language ($folder/strings.xml)",
                stringsFile(folder).exists()
            )
        }
    }

    @Test
    fun everyLanguageDefinesTheAppName() {
        for ((language, folder) in languages) {
            val name = stringValue(folder, "app_name")
            assertFalse(
                "El nombre de la app no puede faltar ni estar vacio en $language",
                name.isNullOrBlank()
            )
        }
    }

    @Test
    fun everyLanguageDefinesTheAppDescription() {
        // La descripcion la usan el lanzador y los servicios de accesibilidad: sin ella, un
        // lector de pantalla no tiene nada que anunciar sobre la app.
        for ((language, folder) in languages) {
            val description = stringValue(folder, "app_description")
            assertFalse(
                "La descripcion de la app no puede faltar ni estar vacia en $language",
                description.isNullOrBlank()
            )
        }
    }

    @Test
    fun theDescriptionsAreActuallyTranslatedAndNotCopiedFromSpanish() {
        // Control del error mas comodo: copiar el fichero y no traducir nada. Se compara la
        // descripcion de cada idioma con la espanola.
        val spanish = stringValue("values", "app_description")!!
        for ((language, folder) in languages) {
            if (folder == "values") continue
            val other = stringValue(folder, "app_description")!!
            assertFalse(
                "La descripcion en $language es identica a la espanola: parece sin traducir",
                other == spanish
            )
        }
    }

    @Test
    fun allLanguagesShipTheSameKeysSoNothingIsHalfTranslated() {
        // Si un idioma define una clave que otro no, Android la resuelve al idioma por defecto y
        // el resultado es una pantalla con dos idiomas mezclados.
        val keysByLanguage = languages.mapValues { (_, folder) ->
            val file = stringsFile(folder)
            if (!file.exists()) emptySet()
            else Regex("""<string name="([^"]+)"""")
                .findAll(file.readText())
                .map { it.groupValues[1] }
                .toSet()
        }

        val reference = keysByLanguage["es (por defecto)"]!!
        assertTrue("El idioma por defecto no define ninguna clave", reference.isNotEmpty())

        for ((language, keys) in keysByLanguage) {
            assertEquals(
                "El idioma $language no define las mismas claves que el resto",
                reference,
                keys
            )
        }
    }

    @Test
    fun theAppNameIsTheSameInEveryLanguageBecauseItIsABrand() {
        // El nombre del juego no se traduce: es marca. Este test lo deja escrito para que nadie
        // lo "arregle" por error.
        val names = languages.map { (_, folder) -> stringValue(folder, "app_name") }
        assertEquals(
            "El nombre de la app debe ser identico en todos los idiomas",
            1,
            names.toSet().size
        )
    }
}
