package com.example.dominograndprix

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.data.CareerProgressStore
import com.example.dominograndprix.data.DailyChallenge
import com.example.dominograndprix.data.DailyChallengeManager
import com.example.dominograndprix.data.DailyChallengeResult
import com.example.dominograndprix.data.DailyChallengeStore
import com.example.dominograndprix.data.JourneyProgress
import com.example.dominograndprix.data.MatchSnapshot
import com.example.dominograndprix.data.MatchSnapshotStore
import com.example.dominograndprix.data.WorldTourRepository
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.model.CalendarDate
import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.ui.game.DominoTableScreen
import com.example.dominograndprix.ui.main.MainTitleScreen
import com.example.dominograndprix.ui.rival.RivalShowdownScreen
import com.example.dominograndprix.ui.rules.RulesGuideScreen
import com.example.dominograndprix.ui.tour.WorldTourMapScreen
import com.example.dominograndprix.ui.vault.TrophyRoomScreen

enum class AppScreen {
    TITLE,
    WORLD_TOUR,
    RIVAL_SHOWDOWN,
    RULES,
    TROPHIES,
    MATCH_TABLE
}

@Composable
fun MainNavigation() {
    var currentScreen by remember { mutableStateOf(AppScreen.TITLE) }

    // Persistencia de la carrera World Tour (caja, WRP, ciudades y trofeos vendidos).
    // El progreso se carga una sola vez al arrancar y se vuelve a escribir en disco
    // automáticamente cada vez que cambia cualquiera de los cuatro valores.
    val context = LocalContext.current
    val savedProgress: JourneyProgress = remember { CareerProgressStore.load(context) }

    // Career persistent state
    var playerCash by remember { mutableIntStateOf(savedProgress.cash) }
    var playerWrp by remember { mutableIntStateOf(savedProgress.wrp) }
    var venues by remember {
        mutableStateOf(
            CareerProgressStore.applyToVenues(WorldTourRepository.getInitialVenues(), savedProgress)
        )
    }
    var trophies by remember {
        mutableStateOf(
            CareerProgressStore.applyToTrophies(WorldTourRepository.getLuxuryPrizes(), savedProgress)
        )
    }

    LaunchedEffect(playerCash, playerWrp, venues, trophies) {
        CareerProgressStore.save(
            context,
            CareerProgressStore.snapshot(
                cash = playerCash,
                wrp = playerWrp,
                venues = venues,
                trophies = trophies
            )
        )
    }

    // Botón atrás de Android: retrocede a la pantalla anterior en vez de cerrar la app.
    // En TITLE no se intercepta, para que el gesto/botón salga del juego con normalidad.
    BackHandler(enabled = currentScreen != AppScreen.TITLE) {
        DominoSoundManager.playTileTap()
        currentScreen = when (currentScreen) {
            AppScreen.WORLD_TOUR,
            AppScreen.RULES,
            AppScreen.TROPHIES,
            AppScreen.TITLE -> AppScreen.TITLE
            AppScreen.RIVAL_SHOWDOWN,
            AppScreen.MATCH_TABLE -> AppScreen.WORLD_TOUR
        }
    }

    // Active match parameters
    var activeMode by remember { mutableStateOf(GameMode.DRAW) }
    var activeCityName by remember { mutableStateOf("La Habana") }
    var activeBet by remember { mutableStateOf(100) }
    var activeTargetScore by remember { mutableStateOf(100) }
    var activeVenueId by remember { mutableStateOf<String?>("cuba") }
    var activeDifficulty by remember { mutableStateOf(Difficulty.STANDARD) }
    /** Semilla del desafio diario en curso, o null si la partida es normal. */
    var activeDailySeed by remember { mutableStateOf<Long?>(null) }
    /** Asientos que juegan personas en la partida activa. */
    var activeHumanSeats by remember { mutableStateOf(setOf(0)) }
    /** Nombres de los asientos de la partida activa, vacio = los del modo carrera. */
    var activePlayerNames by remember { mutableStateOf(emptyList<String>()) }
    var selectedVenue by remember { mutableStateOf<CityVenue?>(venues.first()) }

    // Partida en curso guardada: si existe al arrancar, la app ofrece continuarla en vez de
    // perder el progreso y las monedas cuando el sistema mata el proceso.
    var resumeSnapshot by remember { mutableStateOf<MatchSnapshot?>(null) }
    LaunchedEffect(Unit) {
        resumeSnapshot = MatchSnapshotStore.load(context)
    }

    // Desafío diario: el reto de hoy y el resultado si ya se ha disputado.
    var dailyChallenge by remember { mutableStateOf<DailyChallenge?>(null) }
    var dailyResult by remember { mutableStateOf<DailyChallengeResult?>(null) }
    /** Si la partida en curso es el desafío diario del día. */
    var runningDailyDay by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        dailyChallenge = DailyChallengeManager.today()
        dailyResult = DailyChallengeStore.load(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1722)
    ) {
        when (currentScreen) {
            AppScreen.TITLE -> {
                LaunchedEffect(Unit) {
                    DominoSoundManager.playTitleMusic()
                }
                MainTitleScreen(
                    dailyChallenge = dailyChallenge,
                    dailyResult = dailyResult,
                    onStartWorldTour = {
                        DominoSoundManager.playTileTap()
                        currentScreen = AppScreen.WORLD_TOUR
                    },
                    onQuickPlay = {
                        DominoSoundManager.playTileTap()
                        val havana = venues.first()
                        selectedVenue = havana
                        // Modo carrera: un solo jugador humano en el asiento 0.
                        activeHumanSeats = setOf(0)
                        activePlayerNames = emptyList()
                        activeDailySeed = null
                        activeDailySeed = null
                        currentScreen = AppScreen.RIVAL_SHOWDOWN
                    },
                    onLocalMatch = {
                        DominoSoundManager.playTileTap()
                        // Pass-and-play 1v1: dos personas que comparten el telefono, en los
                        // asientos 0 y 2, que ademas son equipos distintos.
                        activeMode = GameMode.DRAW
                        activeCityName = "Partida local"
                        activeBet = 0
                        activeTargetScore = 100
                        activeVenueId = null
                        activeDifficulty = Difficulty.STANDARD
                        activeDailySeed = null
                        activeHumanSeats = setOf(0, 2)
                        activePlayerNames = listOf(
                            DominoStrings.localPlayerName(0),
                            "Carlos",
                            DominoStrings.localPlayerName(1),
                            "Rodrigo"
                        )
                        MatchSnapshotStore.clear(context)
                        currentScreen = AppScreen.MATCH_TABLE
                    },
                    onPlayDaily = {
                        DominoSoundManager.playTileClack(isDouble = true)
                        dailyChallenge?.let { challenge ->
                            activeMode = challenge.mode
                            activeCityName = DominoStrings.dailyChallengeTitle()
                            activeBet = 0
                            activeTargetScore = challenge.targetScore
                            activeVenueId = null
                            activeDifficulty = challenge.difficulty
                            activeHumanSeats = setOf(0)
                            activePlayerNames = emptyList()
                            // El reto se juega con una semilla fija y viaja por el motor, no por
                            // el snapshot: no debe guardarse a medias ni reanudarse.
                            activeDailySeed = challenge.seed
                            runningDailyDay = challenge.dayNumber
                            MatchSnapshotStore.clear(context)
                            currentScreen = AppScreen.MATCH_TABLE
                        }
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

            AppScreen.WORLD_TOUR -> {
                LaunchedEffect(Unit) {
                    DominoSoundManager.playMapMusic()
                }
                WorldTourMapScreen(
                    venues = venues,
                    playerCash = playerCash,
                    playerWrp = playerWrp,
                    unfinishedMatchCity = resumeSnapshot?.cityName,
                    onResumeMatch = {
                        DominoSoundManager.playTileClack(isDouble = true)
                        resumeSnapshot?.let { snapshot ->
                            activeMode = snapshot.gameMode
                            activeCityName = snapshot.cityName
                            activeBet = snapshot.betAmount
                            activeTargetScore = snapshot.targetScore
                            activeVenueId = snapshot.venueId
                            activeDifficulty = snapshot.difficulty
                            currentScreen = AppScreen.MATCH_TABLE
                        }
                    },
                    onDiscardMatch = {
                        DominoSoundManager.playTileTap()
                        MatchSnapshotStore.clear(context)
                        resumeSnapshot = null
                    },
                    onSelectVenue = { venue ->
                        DominoSoundManager.playTileTap()
                        selectedVenue = venue
                        currentScreen = AppScreen.RIVAL_SHOWDOWN
                    },
                    onBack = {
                        DominoSoundManager.playTileTap()
                        currentScreen = AppScreen.TITLE
                    }
                )
            }

            AppScreen.RIVAL_SHOWDOWN -> {
                val venue = selectedVenue ?: venues.first()
                LaunchedEffect(venue.id) {
                    DominoSoundManager.playVenueMusic(venue.id)
                }
                RivalShowdownScreen(
                    venue = venue,
                    onAcceptMatch = {
                        DominoSoundManager.playTileClack(isDouble = true)
                        val locVenue = com.example.dominograndprix.i18n.DominoStrings.localizeVenue(venue)
                        activeMode = venue.defaultMode
                        activeCityName = locVenue.name
                        activeBet = venue.entryBet
                        activeTargetScore = venue.targetScore
                        activeVenueId = venue.id
                        activeDifficulty = venue.difficulty
                        // Modo carrera: un solo jugador humano en el asiento 0.
                        activeHumanSeats = setOf(0)
                        activePlayerNames = emptyList()
                        // Empezar una partida nueva descarta cualquier guardado anterior:
                        // si no, al entrar se reanudaria la partida vieja de esa sede.
                        MatchSnapshotStore.clear(context)
                        if (playerCash >= venue.entryBet) {
                            playerCash -= venue.entryBet
                        }
                        currentScreen = AppScreen.MATCH_TABLE
                    },
                    onCancel = {
                        DominoSoundManager.playTileTap()
                        currentScreen = AppScreen.WORLD_TOUR
                    }
                )
            }

            AppScreen.MATCH_TABLE -> {
                LaunchedEffect(activeVenueId) {
                    DominoSoundManager.playVenueMusic(activeVenueId)
                }
                DominoTableScreen(
                    gameMode = activeMode,
                    cityName = activeCityName,
                    venueId = activeVenueId,
                    betAmount = activeBet,
                    targetScore = activeTargetScore,
                    difficulty = activeDifficulty,
                    dailySeed = activeDailySeed,
                    humanPlayerIndices = activeHumanSeats,
                    playerNames = activePlayerNames,
                    resumeFrom = resumeSnapshot,
                    onMatchFinished = { won, reward ->
                        if (won) {
                            playerCash += reward
                            playerWrp += 1500
                            activeVenueId?.let { vId ->
                                val vIndex = venues.indexOfFirst { it.id == vId }
                                if (vIndex != -1) {
                                    val updated = venues.toMutableList()
                                    updated[vIndex] = updated[vIndex].copy(isCompleted = true)
                                    if (vIndex + 1 < updated.size) {
                                        updated[vIndex + 1] = updated[vIndex + 1].copy(isUnlocked = true)
                                    }
                                    venues = updated
                                }
                            }
                        } else if (playerCash < 100) {
                            // Ensure player always has enough minimum bankroll for Havana ($100)
                            playerCash = 300
                        }
                        // Si la partida era el desafio diario, se registra el resultado del
                        // dia y se calcula la racha. Un reto diario solo cuenta una vez.
                        runningDailyDay?.let { day ->
                            val date = dailyChallenge?.date ?: CalendarDate.today()
                            val points = if (won) reward else 0
                            val streak = DailyChallengeManager.nextStreak(dailyResult, date, won)
                            val result = DailyChallengeResult(
                                dayNumber = day,
                                won = won,
                                points = points,
                                streak = streak
                            )
                            DailyChallengeStore.save(context, result)
                            dailyResult = result
                            runningDailyDay = null
                        }
                        currentScreen = AppScreen.WORLD_TOUR
                    },
                    onBack = {
                        DominoSoundManager.playTileTap()
                        currentScreen = AppScreen.WORLD_TOUR
                    }
                )
            }

            AppScreen.RULES -> {
                RulesGuideScreen(
                    onBack = {
                        DominoSoundManager.playTileTap()
                        currentScreen = AppScreen.TITLE
                    }
                )
            }

            AppScreen.TROPHIES -> {
                TrophyRoomScreen(
                    items = trophies,
                    playerCash = playerCash,
                    onSellItem = { item ->
                        DominoSoundManager.playTileClack()
                        val index = trophies.indexOf(item)
                        if (index != -1 && !item.isSold) {
                            val updated = trophies.toMutableList()
                            updated[index] = item.copy(isSold = true)
                            trophies = updated
                            playerCash += item.value
                        }
                    },
                    onBack = {
                        DominoSoundManager.playTileTap()
                        currentScreen = AppScreen.TITLE
                    }
                )
            }
        }
    }
}
