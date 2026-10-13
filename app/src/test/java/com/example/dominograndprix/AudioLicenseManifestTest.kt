package com.example.dominograndprix

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Blindaje del manifiesto de audio.
 *
 * Este test impide que alguien (nosotros mismos dentro de seis meses, o una herramienta
 * automatica) anada un audio a `res/raw` sin dejar constancia de su procedencia y licencia
 * en `AUDIO_LICENSES.md`. En un juego publicado, un archivo de audio sin licencia
 * documentada es exactamente el tipo de detalle que aparece en una reclamacion y que nadie
 * recuerda de donde salio.
 *
 * Se ejecuta como test normal, asi que corre con `./gradlew test` y por tanto en el CI de
 * GitHub Actions: si alguien cuela una pista sin acreditar, la build se pone en rojo antes
 * de llegar a main.
 *
 * Reglas que comprueba:
 *  1. Todo audio de `res/raw` aparece mencionado en el manifiesto.
 *  2. Todo archivo mencionado en el manifiesto existe de verdad (detecta erratas y pistas
 *     que se borraron del proyecto pero siguen anunciadas).
 *  3. Ningun audio queda sin licencia: la mencion debe ir acompanada de un termino de
 *     licencia reconocible (CC0, CC-BY, dominio publico, original...) cerca del nombre.
 */
class AudioLicenseManifestTest {

    private companion object {
        /** Extensiones de audio que Android puede reproducir desde res/raw. */
        val AUDIO_EXTENSIONS = setOf("ogg", "wav", "mp3", "m4a", "aac", "flac", "mid", "midi", "opus")

        /** Terminos que acreditan que una pista tiene licencia declarada. */
        val LICENSE_MARKERS = listOf(
            "cc0", "cc-by", "cc by", "creative commons", "dominio publico", "dominio público",
            "public domain", "100% original", "original", "procedural", "propia", "propio"
        )

        /** Cuantas lineas alrededor de la mencion se consideran parte de su ficha. */
        const val FICHA_WINDOW = 4
    }

    private val rawDir = File("src/main/res/raw")
    private val manifest = File("../AUDIO_LICENSES.md")

    private fun audioFiles(): List<File> =
        rawDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
            ?.sortedBy { it.name }
            ?: emptyList()

    private fun manifestLines(): List<String> {
        assertTrue(
            "No encuentro el manifiesto de licencias en ${manifest.absolutePath}",
            manifest.isFile
        )
        return manifest.readLines()
    }

    private fun mentionsOf(fileName: String, lines: List<String>): List<Int> =
        lines.withIndex()
            .filter { (_, line) -> line.contains(fileName, ignoreCase = true) }
            .map { it.index }

    /** Texto de la ficha de una pista: la linea que la menciona y las siguientes. */
    private fun fichaFor(fileName: String, lines: List<String>): String {
        val indices = mentionsOf(fileName, lines)
        if (indices.isEmpty()) return ""
        // Si aparece varias veces (p. ej. listada y luego detallada), se unen las fichas
        return indices.joinToString("\n") { index ->
            lines.subList(index, minOf(index + FICHA_WINDOW, lines.size)).joinToString("\n")
        }
    }

    @Test
    fun rawDirectoryHasAudioToCheck() {
        assertTrue("No existe el directorio ${rawDir.absolutePath}", rawDir.isDirectory)
        val files = audioFiles()
        assertTrue("No hay ningun audio en res/raw: el test no estaria comprobando nada", files.isNotEmpty())
        println("Audios encontrados en res/raw: ${files.size}")
    }

    @Test
    fun everyAudioFileIsDocumentedInTheManifest() {
        val lines = manifestLines()
        val undocumented = audioFiles().filter { mentionsOf(it.name, lines).isEmpty() }

        assertTrue(
            buildString {
                append("Hay ${undocumented.size} audio(s) en res/raw sin entrada en AUDIO_LICENSES.md:\n")
                undocumented.forEach { append("  - ${it.name}\n") }
                append("\nCada audio debe documentar su procedencia y su licencia antes de publicarse.")
            },
            undocumented.isEmpty()
        )
    }

    @Test
    fun everyAudioFileDeclaresALicense() {
        val lines = manifestLines()
        val withoutLicense = mutableListOf<String>()

        for (file in audioFiles()) {
            val ficha = fichaFor(file.name, lines)
            if (ficha.isEmpty()) continue // ya lo reporta el test de documentacion
            val lower = ficha.lowercase()
            val hasMarker = LICENSE_MARKERS.any { marker -> lower.contains(marker) }
            if (!hasMarker) withoutLicense.add(file.name)
        }

        assertTrue(
            buildString {
                append("Estos audios se mencionan pero sin declarar una licencia reconocible:\n")
                withoutLicense.forEach { append("  - $it\n") }
                append("\nAnade el tipo de licencia (CC0, CC-BY 4.0, dominio publico, original...).")
            },
            withoutLicense.isEmpty()
        )
    }

    @Test
    fun manifestDoesNotAnnounceFilesThatNoLongerExist() {
        val lines = manifestLines()
        val existing = audioFiles().map { it.name }.toSet()

        // Nombres de audio citados en el manifiesto, con o sin carpeta delante
        val mentioned = Regex("""([A-Za-z0-9_\-]+\.(?:${AUDIO_EXTENSIONS.joinToString("|")}))""",
            RegexOption.IGNORE_CASE)
            .findAll(lines.joinToString("\n"))
            .map { it.groupValues[1] }
            .toSet()

        val ghosts = mentioned.filter { it !in existing }.sorted()

        assertTrue(
            buildString {
                append("El manifiesto anuncia audios que ya no existen en res/raw:\n")
                ghosts.forEach { append("  - $it\n") }
                append("\nPuede ser una errata en el nombre o una pista borrada que sigue anunciada.")
            },
            ghosts.isEmpty()
        )
    }
}
