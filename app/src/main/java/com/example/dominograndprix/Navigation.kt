package com.example.dominograndprix

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.data.AchievementCatalog
import com.example.dominograndprix.data.CareerProgressStore
import com.example.dominograndprix.data.CareerReporter
import com.example.dominograndprix.data.DailyChallenge
import com.example.dominograndprix.data.DailyChallengeManager
import com.example.dominograndprix.data.DailyChallengeResult
import com.example.dominograndprix.data.DailyChallengeStore
import com.example.dominograndprix.data.JourneyProgress
import com.example.dominograndprix.data.MatchSnapshotStore
import com.example.dominograndprix.data.TournamentCampaign
import com.example.dominograndprix.data.WorldTourRepository
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.ui.achievements.AchievementUnlockedBanner
import com.example.dominograndprix.ui.achievements.AchievementsScreen
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.DominoTableScreen
import com.example.dominograndprix.ui.game.GoldAccent
import com.example.dominograndprix.ui.main.MainTitleScreen
import com.example.dominograndprix.ui.rules.RulesGuideScreen
import com.example.dominograndprix.ui.settings.SettingsDialog
import com.example.dominograndprix.ui.tour.VenueStageScreen
import com.example.dominograndprix.ui.vault.TrophyRoomScreen

enum class AppScreen {
    TITLE,
    VENUE_STAGE,
    MATCH_TABLE,
    RULES,
    TROPHIES,
    ACHIEVEMENTS
}

@Composable
fun MainNavigation() {
    var currentScreen by remember { mutableStateOf(AppScreen.TITLE) }

    val context = LocalContext.current
    var careerProgress by remember { mutableStateOf(CareerProgressStore.load(context)) }

    var pendingAchievement by remember { mutableStateOf<String?>(null) }
    var pendingMission by remember { mutableStateOf<String?>(null) }
    val hapticAnchor = LocalView.current

    var venues by remember {
        mutableStateOf(
            CareerProgressStore.applyToVenues(
                WorldTourRepository.getInitialVenues(),
                careerProgress
            )
        )
    }
    // Los trofeos son una coleccion de recuerdos: no se venden ni generan dinero.
    val trophies = remember { WorldTourRepository.getLuxuryPrizes() }

    LaunchedEffect(careerProgress, venues) {
        CareerProgressStore.save(
            context,
            CareerProgressStore.snapshot(
                progress = careerProgress,
                venues = venues
            )
        )
    }

    // Reto actual activo del torneo (7 sedes x 5 retos = 35 retos)
    val currentChallenge = remember(careerProgress.currentVenueIndex, careerProgress.currentChallengeIndex) {
        TournamentCampaign.getChallenge(careerProgress.currentVenueIndex, careerProgress.currentChallengeIndex)
    }

    // Parámetros de la partida activa
    var activeMode by remember { mutableStateOf(GameMode.BLOCK) }
    var activeCityName by remember { mutableStateOf("Ciudad de México") }
    var activeTargetScore by remember { mutableStateOf(20) }
    var activeVenueId by remember { mutableStateOf<String?>("mexico") }
    var activePlayerCount by remember { mutableIntStateOf(4) }
    var activeIsTeamGame by remember { mutableStateOf(false) }
    var activeDifficulty by remember { mutableStateOf(Difficulty.STANDARD) }
    var activeRivalName by remember { mutableStateOf("Carlos 'El Rayo'") }
    /** Cuantos asientos ocupa una persona en este movil (1 = contra la IA, 2 = pass-and-play). */
    var activeHumanCount by remember { mutableIntStateOf(1) }

    // --- Desafío diario ---
    val dailyChallenge = remember { DailyChallengeManager.today() }
    var dailyResult by remember { mutableStateOf(DailyChallengeStore.load(context)) }
    var activeDaily by remember { mutableStateOf<DailyChallenge?>(null) }
    var pendingDailySummary by remember { mutableStateOf<DailyChallengeResult?>(null) }
    val dailyAlreadyPlayed = dailyResult?.dayNumber == dailyChallenge.dayNumber
    val dailyStreak = dailyResult?.streak ?: 0

    // --- Ajustes ---
    var showSettings by remember { mutableStateOf(false) }

    // Botón atrás de Android: retrocede con suavidad
    BackHandler(enabled = currentScreen != AppScreen.TITLE) {
        DominoSoundManager.playTileTap()
        currentScreen = when (currentScreen) {
            AppScreen.VENUE_STAGE,
            AppScreen.RULES,
            AppScreen.TROPHIES,
            AppScreen.ACHIEVEMENTS -> AppScreen.TITLE
            AppScreen.MATCH_TABLE -> if (activeVenueId == null) AppScreen.TITLE else AppScreen.VENUE_STAGE
            AppScreen.TITLE -> AppScreen.TITLE
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1722)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AppScreen.TITLE -> {
                    LaunchedEffect(Unit) {
                        DominoSoundManager.playTitleMusic()
                    }
                    val hasProgress = CareerProgressStore.hasSavedProgress(context) &&
                        (careerProgress.currentVenueIndex > 0 || careerProgress.currentChallengeIndex > 0 || careerProgress.completedVenueIds.isNotEmpty())

                    MainTitleScreen(
                        hasSavedProgress = hasProgress,
                        currentVenueName = currentChallenge.venueName,
                        currentChallengeNumber = currentChallenge.challengeNumber,
                        onResumeTournament = {
                            DominoSoundManager.playTileClack(isDouble = true)
                            currentScreen = AppScreen.VENUE_STAGE
                        },
                        onStartNewTournament = {
                            DominoSoundManager.playTileClack(isDouble = true)
                            careerProgress = JourneyProgress()
                            CareerProgressStore.save(context, careerProgress)
                            currentScreen = AppScreen.VENUE_STAGE
                        },
                        onOpenQuickPlay = { config ->
                            DominoSoundManager.playTileClack(isDouble = true)
                            activeMode = config.gameMode
                            activeCityName = DominoStrings.modeName(config.gameMode)
                            activeTargetScore = config.targetScore
                            activeVenueId = null
                            activeDaily = null
                            activePlayerCount = config.playerCount
                            activeIsTeamGame = config.isTeamGame
                            activeDifficulty = config.difficulty
                            activeHumanCount = config.humanPlayerCount.coerceIn(1, 2)
                            activeRivalName = when (config.difficulty) {
                                Difficulty.RELAXED -> "IA Novata"
                                Difficulty.TOUGH -> "IA Maestra"
                                else -> "IA Experta"
                            }
                            // No se toca el guardado de la carrera: una partida libre no debe
                            // destruir el progreso de un reto a medias.
                            currentScreen = AppScreen.MATCH_TABLE
                        },
                        dailyChallenge = dailyChallenge,
                        dailyStreak = dailyStreak,
                        dailyAlreadyPlayed = dailyAlreadyPlayed,
                        dailySummary = pendingDailySummary,
                        onOpenDaily = {
                            DominoSoundManager.playTileClack(isDouble = true)
                            activeDaily = dailyChallenge
                            activeMode = dailyChallenge.mode
                            activeCityName = DominoStrings.modeName(dailyChallenge.mode)
                            activeTargetScore = dailyChallenge.targetScore
                            activeVenueId = null
                            activePlayerCount = 2
                            activeIsTeamGame = false
                            activeDifficulty = dailyChallenge.difficulty
                            activeHumanCount = 1
                            activeRivalName = "Rival del día"
                            currentScreen = AppScreen.MATCH_TABLE
                        },
                        onOpenSettings = { showSettings = true },
                        onDismissDailySummary = { pendingDailySummary = null },
                        onShowRules = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.RULES
                        },
                        onShowTrophies = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.TROPHIES
                        },
                        onShowAchievements = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.ACHIEVEMENTS
                        }
                    )
                }

                AppScreen.VENUE_STAGE -> {
                    LaunchedEffect(currentChallenge.musicResId) {
                        DominoSoundManager.playVenueMusic(currentChallenge.venueId)
                    }
                    VenueStageScreen(
                        challenge = currentChallenge,
                        venueIndex = careerProgress.currentVenueIndex,
                        challengeIndex = careerProgress.currentChallengeIndex,
                        onStartChallenge = {
                            DominoSoundManager.playTileClack(isDouble = true)
                            activeMode = currentChallenge.gameMode
                            activeCityName = currentChallenge.venueName
                            activeTargetScore = currentChallenge.targetScore
                            activeVenueId = currentChallenge.venueId
                            activePlayerCount = currentChallenge.playerCount
                            activeIsTeamGame = currentChallenge.isTeamGame
                            activeRivalName = currentChallenge.rivalName
                            // Cada sede tiene su nivel: el torneo empieza suave y acaba en TOUGH.
                            activeDifficulty = currentChallenge.difficulty
                            activeDaily = null
                            activeHumanCount = 1
                            MatchSnapshotStore.clear(context)
                            currentScreen = AppScreen.MATCH_TABLE
                        },
                        onBack = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.TITLE
                        }
                    )
                }

                AppScreen.MATCH_TABLE -> {
                    LaunchedEffect(activeVenueId) {
                        DominoSoundManager.playVenueMusic(activeVenueId)
                    }

                    val playerNames = if (activePlayerCount == 2) {
                        listOf("Tú", activeRivalName)
                    } else if (activeIsTeamGame) {
                        listOf("Tú", activeRivalName, "Mateo (Compañero)", "Rodrigo")
                    } else {
                        listOf("Tú", activeRivalName, "Mateo", "Rodrigo")
                    }

                    // Solo la carrera se reanuda desde un guardado: las partidas libres y el
                    // desafio diario empiezan siempre de cero (antes, un guardado viejo con
                    // venueId nulo resucitaba una partida distinta con otro modo y objetivo).
                    val savedSnapshot = remember(activeVenueId) {
                        if (activeVenueId == null) {
                            null
                        } else {
                            MatchSnapshotStore.load(context)?.takeIf { it.venueId == activeVenueId }
                        }
                    }

                    DominoTableScreen(
                        gameMode = activeMode,
                        cityName = activeCityName,
                        venueId = activeVenueId,
                        targetScore = activeTargetScore,
                        playerCount = activePlayerCount,
                        isTeamGame = activeIsTeamGame,
                        difficulty = activeDifficulty,
                        humanPlayerIndices = if (activeHumanCount > 1) {
                            (0 until activeHumanCount.coerceAtMost(activePlayerCount)).toSet()
                        } else {
                            setOf(0)
                        },
                        playerNames = playerNames,
                        resumeFrom = savedSnapshot,
                        dailySeed = activeDaily?.seed,
                        onMatchFinished = { won, _, finishedEngine ->
                            // 1. Desafío diario: guarda resultado + racha y alimenta los logros
                            // (daily_first / daily_streak_7), que hasta ahora eran inalcanzables.
                            val daily = activeDaily
                            if (daily != null) {
                                val streak = DailyChallengeManager.nextStreak(dailyResult, daily.date, won)
                                val result = DailyChallengeResult(
                                    dayNumber = daily.dayNumber,
                                    won = won,
                                    points = finishedEngine.players.firstOrNull()?.score ?: 0,
                                    streak = streak
                                )
                                DailyChallengeStore.save(context, result)
                                dailyResult = result
                                pendingDailySummary = result
                                val report = CareerReporter.reportFor(
                                    engine = finishedEngine,
                                    venueId = null,
                                    won = won,
                                    difficulty = activeDifficulty,
                                    isDailyChallenge = true
                                )
                                val (updatedProgress, newAchievements) = AchievementCatalog.applyReport(
                                    progress = careerProgress,
                                    report = report,
                                    dailyStreak = streak
                                )
                                careerProgress = updatedProgress
                                val achievementIds = AchievementCatalog.ALL.toSet()
                                newAchievements.firstOrNull { it in achievementIds }?.let {
                                    pendingAchievement = it
                                }
                                activeDaily = null
                                currentScreen = AppScreen.TITLE
                                return@DominoTableScreen
                            }
                            // 2. Partida libre: cuenta para estadisticas y logros, sin premios.
                            if (activeVenueId == null) {
                                val report = CareerReporter.reportFor(
                                    engine = finishedEngine,
                                    venueId = null,
                                    won = won,
                                    difficulty = activeDifficulty,
                                    isDailyChallenge = false
                                )
                                val (updatedProgress, newAchievements) = AchievementCatalog.applyReport(
                                    progress = careerProgress,
                                    report = report,
                                    dailyStreak = dailyStreak
                                )
                                careerProgress = updatedProgress
                                val achievementIds = AchievementCatalog.ALL.toSet()
                                newAchievements.firstOrNull { it in achievementIds }?.let {
                                    pendingAchievement = it
                                }
                                currentScreen = AppScreen.TITLE
                                return@DominoTableScreen
                            }
                            val report = CareerReporter.reportFor(
                                engine = finishedEngine,
                                venueId = activeVenueId,
                                won = won,
                                difficulty = activeDifficulty,
                                isDailyChallenge = false
                            )

                            val (updatedProgress, newAchievements) = AchievementCatalog.applyReport(
                                progress = careerProgress,
                                report = report,
                                dailyStreak = 0
                            )
                            careerProgress = updatedProgress

                            val achievementIds = AchievementCatalog.ALL.toSet()
                            newAchievements.firstOrNull { it in achievementIds }?.let {
                                pendingAchievement = it
                            }
                            newAchievements.firstOrNull { it !in achievementIds }?.let {
                                pendingMission = it
                            }

                            if (won) {
                                // ¡RETO SUPERADO! Avanzar directamente al siguiente reto
                                var nextVenue = careerProgress.currentVenueIndex
                                var nextChallenge = careerProgress.currentChallengeIndex + 1
                                val completedVenues = careerProgress.completedVenueIds.toMutableSet()
                                val unlockedVenues = careerProgress.unlockedVenueIds.toMutableSet()

                                // Si superó el reto 5 (índice 4), completa la sede y avanza a la siguiente
                                if (careerProgress.currentChallengeIndex >= 4) {
                                    completedVenues.add(currentChallenge.venueId)
                                    if (nextVenue < 6) {
                                        nextVenue++
                                        nextChallenge = 0
                                        val nextVenueId = TournamentCampaign.VENUE_IDS.getOrNull(nextVenue)
                                        if (nextVenueId != null) unlockedVenues.add(nextVenueId)
                                    } else {
                                        // Final del torneo mundial alcanzada
                                        nextChallenge = 4
                                    }
                                }

                                careerProgress = careerProgress.copy(
                                    currentVenueIndex = nextVenue,
                                    currentChallengeIndex = nextChallenge,
                                    completedVenueIds = completedVenues,
                                    unlockedVenueIds = unlockedVenues
                                )
                                CareerProgressStore.save(context, careerProgress)
                                // Regresa directamente a la pantalla de sede con el siguiente reto ya listo
                                currentScreen = AppScreen.VENUE_STAGE
                            } else {
                                // Sin economia no hay bancarrota: si pierdes, repites el reto.
                                currentScreen = AppScreen.VENUE_STAGE
                            }
                        },
                        onBack = {
                            DominoSoundManager.playTileTap()
                            // Salir de una partida libre o del desafio no debe dejar el modo
                            // "diario" activo para la siguiente partida.
                            activeDaily = null
                            currentScreen = if (activeVenueId == null) AppScreen.TITLE else AppScreen.VENUE_STAGE
                        }
                    )
                }

                AppScreen.RULES -> {
                    RulesGuideScreen(
                        onBack = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.TITLE
                        },
                        onPlayMode = { mode ->
                            DominoSoundManager.playTileTap()
                            activeMode = mode
                            activeCityName = DominoStrings.modeName(mode)
                            activeTargetScore = 50
                            activeVenueId = null
                            activePlayerCount = 4
                            activeIsTeamGame = false
                            activeDifficulty = Difficulty.STANDARD
                            activeRivalName = "Carlos 'El Rayo'"
                            activeDaily = null
                            activeHumanCount = 1
                            // Sin esto se reanudaba en fantasma la ultima partida libre guardada
                            // (su venueId tambien era null).
                            MatchSnapshotStore.clear(context)
                            currentScreen = AppScreen.MATCH_TABLE
                        }
                    )
                }

                AppScreen.TROPHIES -> {
                    TrophyRoomScreen(
                        items = trophies,
                        onBack = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.TITLE
                        }
                    )
                }

                AppScreen.ACHIEVEMENTS -> {
                    AchievementsScreen(
                        unlockedIds = careerProgress.unlockedAchievements,
                        onBack = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.TITLE
                        }
                    )
                }
            }

            // Ajustes (sonido, vibracion, idioma): persistentes entre sesiones
            if (showSettings) {
                SettingsDialog(onDismiss = { showSettings = false })
            }

            // Banner flotante de logros
            AchievementUnlockedBanner(
                achievementId = pendingAchievement,
                missionId = pendingMission,
                hapticsView = hapticAnchor,
                onDismissed = {
                    pendingAchievement = null
                    pendingMission = null
                }
            )
        }
    }
}
