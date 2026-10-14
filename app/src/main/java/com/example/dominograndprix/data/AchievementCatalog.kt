package com.example.dominograndprix.data

import com.example.dominograndprix.model.Difficulty

/**
 * Catalogo de logros.
 *
 * Cada logro se evalua con una condicion pura sobre el progreso y el informe de la partida
 * recien terminada, de modo que:
 * - Anadir un logro no obliga a cambiar el formato guardado (solo se evalua de nuevo).
 * - Un jugador que ya cumplia la condicion lo desbloquea al terminar su siguiente partida.
 *
 * Los identificadores son estables y no se traducen: el texto vive en `DominoStrings`.
 */
object AchievementCatalog {

    // --- Identificadores ---
    const val FIRST_BLOOD = "first_win"
    const val DOMINO_MASTER = "domino_master"
    const val TRANCA_KING = "tranca_king"
    const val HAT_TRICK = "hat_trick"
    const val VETERAN = "veteran"
    const val SHARPSHOOTER = "sharpshooter"
    const val LAST_TILE = "last_tile"
    const val TOUGH_CROWN = "tough_crown"
    const val VEGAS_CHAMPION = "vegas_champion"
    const val HAVANA_DOMINO = "havana_domino"
    const val TOKYO_TRANCA = "tokyo_tranca"
    const val DAILY_FIRST = "daily_first"
    const val DAILY_STREAK_7 = "daily_streak_7"
    const val PERFECT_TOUR = "perfect_tour"
    const val HIGH_ROLLER = "high_roller"

    /** Todos los identificadores conocidos. Util para validar y para la interfaz. */
    val ALL: List<String> = listOf(
        FIRST_BLOOD, DOMINO_MASTER, TRANCA_KING, HAT_TRICK, VETERAN, SHARPSHOOTER,
        LAST_TILE, TOUGH_CROWN, VEGAS_CHAMPION, HAVANA_DOMINO, TOKYO_TRANCA,
        DAILY_FIRST, DAILY_STREAK_7, PERFECT_TOUR, HIGH_ROLLER
    )

    /**
     * Logros que deben quedar desbloqueados tras aplicar [report] sobre [progress].
     *
     * Devuelve solo los NUEVOS: el llamante se encarga de la union con los que ya tenia.
     */
    fun evaluate(
        progress: JourneyProgress,
        report: MatchReport,
        dailyStreak: Int = 0
    ): Set<String> {
        val stats = progress.stats
        val unlocked = mutableSetOf<String>()

        if (stats.matchesWon >= 1) unlocked += FIRST_BLOOD
        if (stats.dominos >= 10) unlocked += DOMINO_MASTER
        if (stats.trancasWon >= 10) unlocked += TRANCA_KING
        if (stats.roundsWon >= 3) unlocked += HAT_TRICK
        if (stats.matchesPlayed >= 50) unlocked += VETERAN
        if (stats.pointsScored >= 500) unlocked += SHARPSHOOTER
        if (stats.lastTileWins >= 5) unlocked += LAST_TILE
        if (stats.toughWins >= 5) unlocked += TOUGH_CROWN

        // Logros de sede: exigen ganar en un sitio concreto, que es lo que da motivo para
        // volver a una ciudad ya completada.
        if (report.won && report.venueId == "las_vegas") unlocked += VEGAS_CHAMPION
        if (report.venueId == "cuba" && report.dominos >= 1) unlocked += HAVANA_DOMINO
        if (report.venueId == "tokyo" && report.trancasWon >= 1) unlocked += TOKYO_TRANCA

        // Desafio diario.
        if (report.isDailyChallenge && report.won) unlocked += DAILY_FIRST
        if (dailyStreak >= 7) unlocked += DAILY_STREAK_7

        // Gira completa: las siete sedes cerradas.
        if (progress.completedVenueIds.size >= 7) unlocked += PERFECT_TOUR

        // Caja alta: da algo que perseguir a quien ya ha terminado la gira.
        if (progress.cash >= 20000) unlocked += HIGH_ROLLER

        return unlocked - progress.unlockedAchievements
    }

    /**
     * Aplica un informe a la carrera y devuelve el progreso actualizado junto con los logros
     * recien desbloqueados.
     *
     * Es una funcion pura: todo lo que necesita entra por parametro, lo que permite probar la
     * economia, las estadisticas y los logros sin tocar disco ni motor.
     */
    fun applyReport(
        progress: JourneyProgress,
        report: MatchReport,
        dailyStreak: Int = 0
    ): Pair<JourneyProgress, Set<String>> {
        val stats = progress.stats
        val winsByDifficulty = stats.winsByDifficulty.toMutableMap()
        if (report.won) {
            val key = report.difficulty.name
            winsByDifficulty[key] = (winsByDifficulty[key] ?: 0) + 1
        }

        val updatedStats = stats.copy(
            matchesPlayed = stats.matchesPlayed + 1,
            matchesWon = stats.matchesWon + if (report.won) 1 else 0,
            roundsPlayed = stats.roundsPlayed + report.roundsPlayed,
            roundsWon = stats.roundsWon + report.roundsWon,
            blockedRounds = stats.blockedRounds + report.blockedRounds,
            trancasWon = stats.trancasWon + report.trancasWon,
            dominos = stats.dominos + report.dominos,
            pointsScored = stats.pointsScored + report.pointsScored,
            lastTileWins = stats.lastTileWins + if (report.wonWithSingleTileLeft) 1 else 0,
            winsByDifficulty = winsByDifficulty
        )

        val updatedProgress = progress.copy(
            cash = progress.cash + report.cashDelta,
            wrp = progress.wrp + report.wrpDelta,
            // Ganar una sede la marca como completada aqui mismo. Si no, la condicion de "gira
            // completa" quedaria siempre una victoria por detras: el jugador tendria que volver
            // a ganar en la ultima ciudad para que el logro se diese cuenta.
            completedVenueIds = progress.completedVenueIds +
                (if (report.won && report.venueId != null) setOf(report.venueId) else emptySet()),
            stats = updatedStats
        )

        val newAchievements = evaluate(updatedProgress, report, dailyStreak)
        return updatedProgress.copy(
            unlockedAchievements = updatedProgress.unlockedAchievements + newAchievements
        ) to newAchievements
    }
}
