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
import com.example.dominograndprix.data.CareerEconomy
import com.example.dominograndprix.data.CareerProgressStore
import com.example.dominograndprix.data.CareerReporter
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

    fun playerCash(): Int = careerProgress.cash
    fun playerWrp(): Int = careerProgress.wrp

    var pendingAchievement by remember { mutableStateOf<String?>(null) }
    var pendingMission by remember { mutableStateOf<String?>(null) }
    val hapticAnchor = LocalView.current
    var showBankruptcyNotice by remember { mutableStateOf(false) }

    var venues by remember {
        mutableStateOf(
            CareerProgressStore.applyToVenues(
                WorldTourRepository.getInitialVenues(),
                careerProgress
            )
        )
    }
    var trophies by remember {
        mutableStateOf(
            CareerProgressStore.applyToTrophies(
                WorldTourRepository.getLuxuryPrizes(),
                careerProgress
            )
        )
    }

    LaunchedEffect(careerProgress, venues, trophies) {
        CareerProgressStore.save(
            context,
            CareerProgressStore.snapshot(
                progress = careerProgress,
                venues = venues,
                trophies = trophies
            )
        )
    }

    // Reto actual activo del torneo (7 sedes x 5 retos = 35 retos)
    val currentChallenge = remember(careerProgress.currentVenueIndex, careerProgress.currentChallengeIndex) {
        TournamentCampaign.getChallenge(careerProgress.currentVenueIndex, careerProgress.currentChallengeIndex)
    }

    // Botón atrás de Android: retrocede con suavidad
    BackHandler(enabled = currentScreen != AppScreen.TITLE) {
        DominoSoundManager.playTileTap()
        currentScreen = when (currentScreen) {
            AppScreen.VENUE_STAGE,
            AppScreen.RULES,
            AppScreen.TROPHIES,
            AppScreen.ACHIEVEMENTS -> AppScreen.TITLE
            AppScreen.MATCH_TABLE -> AppScreen.VENUE_STAGE
            AppScreen.TITLE -> AppScreen.TITLE
        }
    }

    // Parámetros de la partida activa
    var activeMode by remember { mutableStateOf(GameMode.BLOCK) }
    var activeCityName by remember { mutableStateOf("Ciudad de México") }
    var activeBet by remember { mutableStateOf(0) }
    var activeTargetScore by remember { mutableStateOf(20) }
    var activeVenueId by remember { mutableStateOf<String?>("mexico") }
    var activePlayerCount by remember { mutableIntStateOf(4) }
    var activeIsTeamGame by remember { mutableStateOf(false) }
    var activeRivalName by remember { mutableStateOf("Carlos 'El Rayo'") }

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
                        onShowRules = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.RULES
                        },
                        onShowTrophies = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.TROPHIES
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
                        playerCash = playerCash(),
                        playerWrp = playerWrp(),
                        onStartChallenge = {
                            DominoSoundManager.playTileClack(isDouble = true)
                            // Deduce apuesta de entrada si aplica
                            if (currentChallenge.entryBet > 0 && careerProgress.cash >= currentChallenge.entryBet) {
                                careerProgress = careerProgress.copy(cash = careerProgress.cash - currentChallenge.entryBet)
                            }
                            activeMode = currentChallenge.gameMode
                            activeCityName = currentChallenge.venueName
                            activeBet = currentChallenge.entryBet
                            activeTargetScore = currentChallenge.targetScore
                            activeVenueId = currentChallenge.venueId
                            activePlayerCount = currentChallenge.playerCount
                            activeIsTeamGame = currentChallenge.isTeamGame
                            activeRivalName = currentChallenge.rivalName
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

                    DominoTableScreen(
                        gameMode = activeMode,
                        cityName = activeCityName,
                        venueId = activeVenueId,
                        betAmount = activeBet,
                        targetScore = activeTargetScore,
                        playerCount = activePlayerCount,
                        isTeamGame = activeIsTeamGame,
                        difficulty = Difficulty.STANDARD,
                        humanPlayerIndices = setOf(0),
                        playerNames = playerNames,
                        onMatchFinished = { won, _, finishedEngine ->
                            val report = CareerReporter.reportFor(
                                engine = finishedEngine,
                                venueId = activeVenueId,
                                won = won,
                                reward = if (won) currentChallenge.rewardCash else 0,
                                bet = activeBet,
                                difficulty = Difficulty.STANDARD,
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
                                    cash = careerProgress.cash + currentChallenge.rewardCash,
                                    wrp = careerProgress.wrp + currentChallenge.rewardWrp,
                                    currentVenueIndex = nextVenue,
                                    currentChallengeIndex = nextChallenge,
                                    completedVenueIds = completedVenues,
                                    unlockedVenueIds = unlockedVenues
                                )
                                CareerProgressStore.save(context, careerProgress)
                                // Regresa directamente a la pantalla de sede con el siguiente reto ya listo
                                currentScreen = AppScreen.VENUE_STAGE
                            } else {
                                if (careerProgress.cash < CareerEconomy.MINIMUM_ENTRY_BET) {
                                    CareerEconomy.grantBankruptcyRescue(careerProgress)?.let { rescued ->
                                        careerProgress = rescued
                                        showBankruptcyNotice = true
                                    }
                                }
                                currentScreen = AppScreen.VENUE_STAGE
                            }
                        },
                        onBack = {
                            DominoSoundManager.playTileTap()
                            currentScreen = AppScreen.VENUE_STAGE
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
                            activeBet = 0
                            activeTargetScore = 50
                            activeVenueId = null
                            activePlayerCount = 4
                            activeIsTeamGame = false
                            activeRivalName = "Carlos 'El Rayo'"
                            currentScreen = AppScreen.MATCH_TABLE
                        }
                    )
                }

                AppScreen.TROPHIES -> {
                    TrophyRoomScreen(
                        items = trophies,
                        playerCash = playerCash(),
                        onSellItem = { item ->
                            DominoSoundManager.playTileClack()
                            val index = trophies.indexOf(item)
                            if (index != -1 && !item.isSold) {
                                val updated = trophies.toMutableList()
                                updated[index] = item.copy(isSold = true)
                                trophies = updated
                                careerProgress = careerProgress.copy(cash = playerCash() + item.value)
                            }
                        },
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

            // Aviso de prestamo de honor
            if (showBankruptcyNotice) {
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Text(
                            text = DominoStrings.bankruptcyTitle(),
                            color = GoldAccent,
                            fontWeight = FontWeight.Black
                        )
                    },
                    text = {
                        Text(DominoStrings.bankruptcyBody(CareerEconomy.BANKRUPTCY_RESCUE_AMOUNT))
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                DominoSoundManager.playTileTap()
                                showBankruptcyNotice = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = DarkNavy
                            )
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                )
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
