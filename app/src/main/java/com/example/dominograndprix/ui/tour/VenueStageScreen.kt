package com.example.dominograndprix.ui.tour

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.dominograndprix.data.VenueChallenge
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.i18n.LanguageSelectorButton
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

/**
 * Pantalla de presentación a pantalla completa de la localidad del torneo.
 *
 * Muestra la imagen paisajística de la ciudad en toda la pantalla, con ambientación
 * musical de la región y una tarjeta flotante de cristal con el reto actual,
 * las reglas específicas, el rival y el botón directo para iniciar la partida.
 */
@Composable
fun VenueStageScreen(
    challenge: VenueChallenge,
    venueIndex: Int,
    challengeIndex: Int,
    onStartChallenge: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Imagen de la localidad a pantalla completa
        Image(
            painter = painterResource(id = challenge.bgImageResId),
            contentDescription = challenge.venueName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Degradados superior e inferior para máxima legibilidad y estética cinematográfica
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- BARRA SUPERIOR ELEGANTE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón volver
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkNavy.copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
                    modifier = Modifier.clickable {
                        DominoSoundManager.playTileTap()
                        onBack()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "⬅", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "MENÚ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Controles de sonido y ajustes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón silenciar música
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkNavy.copy(alpha = 0.88f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { DominoSoundManager.toggleMute() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (DominoSoundManager.isMuted) "🔇" else "🔊",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // --- CABECERA DE LA CIUDAD / SEDE ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldAccent.copy(alpha = 0.20f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "LOCALIDAD ${venueIndex + 1} DE 7",
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = challenge.venueName.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = 1.sp
                )

                Text(
                    text = challenge.venueRegion,
                    color = GoldAccent.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            // --- TARJETA FLOTANTE DE RETO (GLASSMORPHISM) ---
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkNavy.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Insignia del número de reto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (challenge.challengeNumber == 5) Color(0xFF8B1D1D) else Color(0xFF1B4E2B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (challenge.challengeNumber == 5) Color(0xFFFF6B6B) else Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                text = if (challenge.challengeNumber == 5) "★ GRAN FINAL DE SEDE ★" else "RETO ${challenge.challengeNumber} DE 5",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Formato de juego (Individual / Parejas / 1v1)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            val formatLabel = when {
                                challenge.isTeamGame -> "👥 Parejas (2v2)"
                                challenge.playerCount == 2 -> "⚔️ Mano a Mano (1v1)"
                                else -> "👤 4 Jugadores Individual"
                            }
                            Text(
                                text = formatLabel,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Título del reto
                    Text(
                        text = challenge.title,
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips de modo y puntuación objetivo
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF243246)
                        ) {
                            Text(
                                text = "Modalidad: ${challenge.gameMode.title}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF243246)
                        ) {
                            Text(
                                text = "Meta: ${challenge.targetScore} pts",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Descripción de reglas específicas del reto
                    Text(
                        text = challenge.description,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    // Trofeo si es la final
                    if (challenge.trophyTitle != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF332608),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🏆", fontSize = 18.sp)
                                Column {
                                    Text(
                                        text = "Premio de la Sede: ${challenge.trophyTitle}",
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Trofeo de la sede • Desbloquea la siguiente ciudad",
                                        color = Color.LightGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rival y recompensa económica
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rival: ${challenge.rivalName}",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Trofeo: ${challenge.trophyTitle}",
                            color = GoldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BOTÓN PRINCIPAL: JUGAR RETO
                    Button(
                        onClick = {
                            DominoSoundManager.playTileClack(isDouble = true)
                            onStartChallenge()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = DarkNavy
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (challengeIndex == 0 && venueIndex == 0) "INICIAR PRIMER RETO ➔" else "JUGAR RETO ${challenge.challengeNumber} ➔",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

