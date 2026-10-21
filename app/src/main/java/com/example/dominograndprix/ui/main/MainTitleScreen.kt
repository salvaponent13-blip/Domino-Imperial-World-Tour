package com.example.dominograndprix.ui.main

import com.example.dominograndprix.ui.dialogs.QuickPlayDialog
import com.example.dominograndprix.ui.dialogs.QuickPlayConfig

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.R
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.data.DailyChallenge
import com.example.dominograndprix.data.DailyChallengeResult
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.i18n.LanguageSelectorButton
import com.example.dominograndprix.ui.components.CreditsButton
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

/**
 * Pantalla de inicio ultra-depurada y moderna.
 *
 * Sin menús saturados ni opciones dispersas:
 * - Si hay torneo en curso: "CONTINUAR TORNEO" (con la ciudad y reto actual) y "NUEVO TORNEO".
 * - Si es partida nueva: "INICIAR TORNEO".
 */
@Composable
fun MainTitleScreen(
    hasSavedProgress: Boolean = false,
    currentVenueName: String = "Ciudad de México",
    currentChallengeNumber: Int = 1,
    onResumeTournament: () -> Unit = {},
    onStartNewTournament: () -> Unit = {},
    onOpenQuickPlay: (QuickPlayConfig) -> Unit = {},
    onShowRules: () -> Unit = {},
    onShowTrophies: () -> Unit = {},
    dailyChallenge: DailyChallenge? = null,
    dailyStreak: Int = 0,
    dailyAlreadyPlayed: Boolean = false,
    dailySummary: DailyChallengeResult? = null,
    onOpenDaily: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onDismissDailySummary: () -> Unit = {}
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showQuickPlayDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Imagen de portada del torneo en pantalla completa
        Image(
            painter = painterResource(id = R.drawable.bg_cover),
            contentDescription = "Domino Imperial Cover",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Degradado cinematográfico
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Cabecera superior: Logo, Título, Idioma y Sonido
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_icon),
                    contentDescription = DominoStrings.appIconLabel(),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, GoldAccent, RoundedCornerShape(12.dp))
                )
                Column {
                    Text(
                        text = "DOMINO IMPERIAL",
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "GRAND PRIX MUNDIAL",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de Idioma (ES, EN, FR, IT, PT, DE)
                LanguageSelectorButton()

                // Créditos y licencias de audio
                CreditsButton()

                // Ajustes persistentes: sonido, vibracion e idioma
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkNavy.copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier.clickable {
                        DominoSoundManager.playTileTap()
                        onOpenSettings()
                    }
                ) {
                    Text(
                        text = "⚙",
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Botón silenciar música
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkNavy.copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier.clickable { DominoSoundManager.toggleMute() }
                ) {
                    Text(
                        text = if (DominoSoundManager.isMuted) DominoStrings.mutedLabel() else DominoStrings.musicOn(),
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Panel de Botones Principales en la mitad inferior.
        // Envoltorio con scroll: con la Partida Rapida el panel crecio y en pantallas bajas o
        // en apaisado el contenido se salia por arriba (el boton de continuar quedaba oculto).
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            if (hasSavedProgress) {
                // BOTÓN: CONTINUAR TORNEO (Destacado en Oro)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GoldAccent,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable {
                            DominoSoundManager.playTileClack(isDouble = true)
                            onResumeTournament()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "CONTINUAR TORNEO ➔",
                            color = DarkNavy,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "$currentVenueName • Reto $currentChallengeNumber de 5",
                            color = DarkNavy.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTÓN: NUEVO TORNEO (Reiniciar)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkNavy.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            DominoSoundManager.playTileTap()
                            showResetConfirmDialog = true
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "NUEVA PARTIDA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // BOTÓN: INICIAR TORNEO (Para partidas nuevas)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GoldAccent,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clickable {
                            DominoSoundManager.playTileClack(isDouble = true)
                            onStartNewTournament()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "INICIAR TORNEO MUNDIAL ➔",
                            color = DarkNavy,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "7 Sedes Mundiales • 35 Retos Progresivos",
                            color = DarkNavy.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BOTÓN: PARTIDA RÁPIDA (Prueba libremente cualquiera de las 8 modalidades)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E2836).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable {
                        DominoSoundManager.playTileTap()
                        showQuickPlayDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = DominoStrings.quickPlayButton(),
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DominoStrings.quickPlaySubtitle(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DESAFÍO DIARIO: un reto por dia, igual para todos, con racha
            if (dailyChallenge != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF13233A).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (dailyAlreadyPlayed) Color.White.copy(alpha = 0.25f) else Color(0xFF34D399)
                    ),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DominoStrings.dailyChallengeTitle(),
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Text(
                                text = DominoStrings.dailyStreakLabel(dailyStreak),
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = DominoStrings.dailyChallengeSub(
                                dailyChallenge.label,
                                DominoStrings.modeName(dailyChallenge.mode),
                                dailyChallenge.targetScore
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (dailyAlreadyPlayed) {
                            Text(
                                text = DominoStrings.dailyAlreadyPlayed(),
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = DominoStrings.dailyComeBackTomorrow(),
                                color = Color(0xFF34D399).copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF34D399),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable {
                                        DominoSoundManager.playTileClack(isDouble = true)
                                        onOpenDaily()
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = DominoStrings.dailyPlayBtn(),
                                        color = Color(0xFF06121F),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Enlaces rápidos a Reglas y Vitrina de Trofeos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2836).copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable(onClick = onShowRules)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                        Text(
                            DominoStrings.eightModesBtn(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2836).copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable(onClick = onShowTrophies)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                        Text(
                            DominoStrings.trophyRoomBtn(),
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "7 Sedes: México • La Habana • San Juan • Nueva Orleans • Miami • Las Vegas • Tokio",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            }
        }

        QuickPlayDialog(
            isOpen = showQuickPlayDialog,
            onClose = { showQuickPlayDialog = false },
            onStartMatch = { config ->
                showQuickPlayDialog = false
                onOpenQuickPlay(config)
            }
        )

        // Resultado del desafío diario recién jugado
        if (dailySummary != null) {
            AlertDialog(
                onDismissRequest = onDismissDailySummary,
                title = {
                    Text(
                        text = DominoStrings.dailyChallengeTitle(),
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Black
                    )
                },
                text = {
                    Text(
                        text = DominoStrings.dailyResultSummary(
                            dailySummary.won,
                            dailySummary.points,
                            dailySummary.streak
                        ),
                        color = Color.White
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismissDailySummary,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = Color(0xFF131924)
                        )
                    ) {
                        Text(DominoStrings.settingsClose(), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Diálogo de confirmación para reiniciar torneo
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = {
                    Text(
                        text = "¿Empezar nuevo torneo?",
                        color = GoldAccent,
                        fontWeight = FontWeight.Black
                    )
                },
                text = {
                    Text(
                        text = "Se reiniciará el progreso actual y comenzarás desde el Reto 1 en Ciudad de México. ¿Deseas continuar?",
                        color = Color.White
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            onStartNewTournament()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Text("SÍ, REINICIAR", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text(DominoStrings.closeLabel(), color = GoldAccent)
                    }
                },
                containerColor = DarkNavy,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}
