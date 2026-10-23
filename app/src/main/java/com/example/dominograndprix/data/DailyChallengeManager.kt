package com.example.dominograndprix.data

import com.example.dominograndprix.model.CalendarDate
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import kotlinx.serialization.Serializable

/**
 * Desafio diario: una partida concreta, igual para todo el mundo el mismo dia.
 *
 * Todo se deriva de una unica semilla calculada a partir de la fecha, de modo que dos
 * jugadores en dias distintos reciben retos distintos y dos jugadores el mismo dia reciben
 * exactamente el mismo. Eso es lo que lo hace comparable: sin determinismo no hay reto, solo
 * una partida mas.
 */
@Serializable
data class DailyChallenge(
    /** Fecha a la que corresponde el reto. */
    val date: CalendarDate,
    /** Semilla del generador: fija el reparto de todas las rondas de esta partida. */
    val seed: Long,
    val mode: GameMode,
    val difficulty: Difficulty,
    val targetScore: Int
) {
    /** Identificador estable del dia, para guardar el resultado. */
    val dayNumber: Int get() = date.dayNumber

    /** Etiqueta corta para la interfaz: "Reto #20461". */
    val label: String get() = "Reto ${date.day}/${date.month}"
}

/**
 * Resultado del desafio de un dia.
 *
 * Se guarda el dia para saber si el reto de hoy ya se ha disputado, y la racha para dar algo
 * que mantener. Cuando cambia el dia, el resultado anterior deja de contar como "de hoy" y la
 * racha solo continua si el ultimo dia disputado fue el de ayer.
 */
@Serializable
data class DailyChallengeResult(
    /** Numero de dia del reto disputado. */
    val dayNumber: Int,
    /** Si se gano. */
    val won: Boolean,
    /** Puntos conseguidos, para tener algo mas que un si/no. */
    val points: Int,
    /** Racha de dias consecutivos ganados hasta este. */
    val streak: Int
)

/**
 * Calculo del desafio diario. Sin estado y sin reloj propio: la fecha entra por parametro, que
 * es lo que permite probarlo de forma determinista.
 */
object DailyChallengeManager {

    /**
     * Modalidades que puede tocar. Es una lista curada y no las ocho: quedan fuera las que
     * dependen de robar del pozo de forma larga (DRAW, DRAW_SPECIAL) porque un reto diario debe
     * resolverse en una partida corta y sin sorpresas de duracion.
     */
    private val MODES = listOf(
        GameMode.BLOCK,
        GameMode.ALL_FIVES,
        GameMode.MATADOR,
        GameMode.BLOCK_HUNGARIAN,
        GameMode.ALL_THREES
    )

    /** Dificultades que puede tocar, de mas suave a mas dura. */
    private val DIFFICULTIES = listOf(
        Difficulty.RELAXED,
        Difficulty.STANDARD,
        Difficulty.TOUGH
    )

    /** Objetivos posibles: partidas cortas, para que el reto no se eternice. */
    private val TARGETS = listOf(50, 60, 75)

    /**
     * Semilla del dia.
     *
     * Se mezcla el numero de dia con una constante y se pasa por un removedor de bits para que
     * dias consecutivos no produzcan semillas parecidas: con el numero de dia a secas, dos
     * retos seguidos empezarian con manos muy similares.
     */
    fun dailySeed(date: CalendarDate): Long {
        val mixed = date.dayNumber.toLong() * 0x9E3779B97F4A7C15uL.toLong()
        // Mezcla final estilo splitmix64, que reparte bien los bits altos y bajos.
        var z = mixed + 0x2545F4914F6CDD1DuL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
        return z xor (z ushr 31)
    }

    /** Reto de una fecha concreta. */
    fun challengeFor(date: CalendarDate): DailyChallenge {
        val seed = dailySeed(date)
        // Se usan trozos distintos de la semilla para cada eleccion, para que no vayan
        // correlacionadas: si se usara el mismo residuo, cambiar de modo implicaria cambiar
        // de dificultad arrastrada.
        val modeIndex = ((seed ushr 3) and 0xFFFF).toInt().mod(MODES.size)
        val difficultyIndex = ((seed ushr 19) and 0xFFFF).toInt().mod(DIFFICULTIES.size)
        val targetIndex = ((seed ushr 35) and 0xFFFF).toInt().mod(TARGETS.size)
        return DailyChallenge(
            date = date,
            seed = seed,
            mode = MODES[modeIndex],
            difficulty = DIFFICULTIES[difficultyIndex],
            targetScore = TARGETS[targetIndex]
        )
    }

    /** Reto de hoy, segun el reloj del dispositivo. */
    fun today(): DailyChallenge = challengeFor(CalendarDate.today())

    /**
     * Racha que corresponde a un resultado nuevo.
     *
     * Si el ultimo reto disputado fue el de ayer y se gano, la racha continua; en cualquier
     * otro caso empieza de cero. Se pierde la racha al fallar y tambien al dejar pasar un dia,
     * que es lo que le da valor a mantenerla.
     */
    fun nextStreak(previous: DailyChallengeResult?, today: CalendarDate, won: Boolean): Int {
        if (!won) return 0
        val last = previous ?: return 1
        if (!last.won) return 1
        val isYesterday = last.dayNumber == today.dayNumber - 1
        return if (isYesterday) last.streak + 1 else 1
    }
}

