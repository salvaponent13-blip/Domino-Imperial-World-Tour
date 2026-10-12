package com.example.dominograndprix.ui.rival

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.R
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

@Composable
fun RivalShowdownScreen(
    venue: CityVenue,
    onAcceptMatch: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val postcardRes = if (venue.postcardResId != 0) venue.postcardResId else R.drawable.venue_havana

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A121C),
                        Color(0xFF142234),
                        Color(0xFF090E16)
                    )
                )
            )
    ) {
        // Subtle blurred/dimmed ambient backdrop of the city
        Image(
            painter = painterResource(id = postcardRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.22f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Bar with Destination Badge + Music Indicator + Mute Toggle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavy.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent)
                    ) {
                        Text(
                            text = "✈️ DESTINO: ${venue.name.uppercase()}",
                            color = GoldAccent,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Audio toggle button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkNavy.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
                        modifier = Modifier.clickable { DominoSoundManager.toggleMute() }
                    ) {
                        Text(
                            text = if (DominoSoundManager.isMuted) "🔇 Silencio" else "🔊 Música ON",
                            color = if (DominoSoundManager.isMuted) Color.LightGray else GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Now Playing Regional Music Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B3A2B).copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (DominoSoundManager.isMuted) "🔇 Audio silenciado"
                        else "🎵 Sonando ahora: ${venue.musicTitle.ifEmpty { DominoSoundManager.currentTrackTitle }}",
                        color = Color(0xFFFFF4C2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Full Framed Postcard Illustration of the Locality (4:3 uncropped!)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(2.5.dp, GoldAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = postcardRes),
                        contentDescription = venue.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Bottom gradient banner inside the postcard with Venue Region & Lore
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.78f),
                                        Color.Black.copy(alpha = 0.94f)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = venue.region,
                                color = GoldAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            if (venue.cityLore.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = venue.cityLore,
                                    color = Color(0xFFF2EFE9),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Rival Challenge & Match Rules Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rival Quote Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF141C28).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "RIVAL LOCAL: ${venue.rivalName.uppercase()}",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "\"${venue.rivalIntro}\"",
                            color = Color.White,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Match Stakes & Rule Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkNavy.copy(alpha = 0.96f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Regla: ${venue.defaultMode.title}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Meta: ${venue.targetScore} pts",
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = venue.defaultMode.summary,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Entrada: $${venue.entryBet}",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Trofeo: ${venue.trophyTitle}",
                                color = Color(0xFF90CAF9),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 4. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(50.dp)
                    ) {
                        Text("MAPA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onAcceptMatch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = DarkNavy
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.7f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "¡JUGAR EN ${venue.name.uppercase()}!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
