package com.example.dominograndprix.ui.tour

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.R
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.i18n.LanguageSelectorButton
import com.example.dominograndprix.model.CityVenue
import androidx.compose.ui.text.style.TextDecoration
import com.example.dominograndprix.data.VenueMissionCatalog
import com.example.dominograndprix.i18n.AchievementStrings
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

@Composable
fun WorldTourMapScreen(
    venues: List<CityVenue>,
    playerCash: Int = 2500,
    playerWrp: Int = 1500,
    /** Ciudad de la partida guardada, si hay una a medias. */
    unfinishedMatchCity: String? = null,
    onResumeMatch: () -> Unit = {},
    onDiscardMatch: () -> Unit = {},
    onSelectVenue: (CityVenue) -> Unit = {},
    onBack: () -> Unit = {},
    /** Logros y misiones ya conseguidos, para marcar las misiones de cada sede. */
    completedIds: Set<String> = emptySet(),
    /** Abre la vitrina de logros desde la barra superior. */
    onShowAchievements: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // World Map Artistic Background
        Image(
            painter = painterResource(id = R.drawable.bg_world_map),
            contentDescription = "World Tour Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.50f
        )

        // Semi-transparent overlay to ensure contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D131A).copy(alpha = 0.85f),
                            Color(0xFF1B2838).copy(alpha = 0.90f),
                            Color(0xFF0F141C).copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Header with Player Career Stats + Language Selector + Audio Toggle
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = DarkNavy.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DOMINO IMPERIAL", color = GoldAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text(DominoStrings.mapSubtitle(), color = Color.LightGray, fontSize = 10.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(DominoStrings.cashLabel(playerCash), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("WRP: $playerWrp pts", color = GoldAccent, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                            }
                            LanguageSelectorButton()
                            // Vitrina de logros, tambien desde el mapa para no tener que volver
                            // al menu principal solo para consultarla.
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                                modifier = Modifier.clickable(onClick = onShowAchievements)
                            ) {
                                Text(
                                    text = "🏆",
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { DominoSoundManager.toggleMute() }
                            ) {
                                Text(
                                    text = if (DominoSoundManager.isMuted) "🔇" else "🔊",
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Partida sin terminar: se ofrece continuarla antes que la lista de ciudades
                if (unfinishedMatchCity != null) {
                    UnfinishedMatchBanner(
                        cityName = unfinishedMatchCity,
                        onResume = onResumeMatch,
                        onDiscard = onDiscardMatch
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. City Venues List with Postcard Thumbnails
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(venues) { rawVenue ->
                        val venue = DominoStrings.localizeVenue(rawVenue)
                        VenueCard(
                            venue = venue,
                            canAfford = playerCash >= rawVenue.entryBet,
                            onEnter = { onSelectVenue(rawVenue) },
                            completedIds = completedIds
                        )
                    }
                }

                // 4. Bottom Navigation
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(DominoStrings.backToMainMenu(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun UnfinishedMatchBanner(
    cityName: String,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1B3A2B).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = DominoStrings.resumeMatchTitle(),
                color = GoldAccent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
            Text(
                text = DominoStrings.resumeMatchSub(cityName),
                color = Color.White,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.8f).height(38.dp)
                ) {
                    Text(DominoStrings.resumeMatchBtn(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(
                    onClick = onDiscard,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(DominoStrings.discardMatchBtn(), color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun VenueMissionLines(venueId: String, completedIds: Set<String>) {
    val missions = VenueMissionCatalog.missionsFor(venueId)
    if (missions.isEmpty()) return

    val completed = VenueMissionCatalog.completedCountFor(venueId, completedIds)

    Spacer(modifier = Modifier.height(5.dp))
    Text(
        text = DominoStrings.venueMissionsHeader(completed, missions.size),
        color = Color(0xFF9FB0C4),
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp
    )
    for (mission in missions) {
        val isDone = mission.id in completedIds
        Row(verticalAlignment = Alignment.CenterVertically) {
            // La marca es un simbolo ademas del color: se distingue sin depender del tono.
            Text(
                text = if (isDone) "✓" else "○",
                color = if (isDone) Color(0xFF4CAF50) else Color(0xFF6B7A8F),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = AchievementStrings.missionTitleFor(mission, DominoStrings.currentLanguage),
                color = if (isDone) Color(0xFF8BC34A) else Color(0xFF8A97A8),
                fontSize = 9.sp,
                textDecoration = if (isDone) TextDecoration.LineThrough else null
            )
        }
    }
}

@Composable
fun VenueCard(
    venue: CityVenue,
    canAfford: Boolean,
    onEnter: () -> Unit,
    /** Logros y misiones ya conseguidos, para marcar las misiones de esta sede. */
    completedIds: Set<String> = emptySet()
) {
    val isEnabled = venue.isUnlocked
    val postcardRes = if (venue.postcardResId != 0) venue.postcardResId else R.drawable.venue_havana

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (venue.isUnlocked) Color(0xFF1E2633).copy(alpha = 0.94f) else Color(0xFF141A22).copy(alpha = 0.90f),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (venue.isCompleted) Color(0xFF4CAF50) else if (venue.isUnlocked) GoldAccent.copy(alpha = 0.65f) else Color.DarkGray
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onEnter)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Illustrated Postcard Preview Thumbnail
            Image(
                painter = painterResource(id = postcardRes),
                contentDescription = venue.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 86.dp, height = 68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GoldAccent, RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue.name,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (venue.isCompleted) {
                        Text(DominoStrings.wonBadge(), color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(venue.region, color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("🎵 ${venue.musicTitle}", color = Color(0xFFB3E5FC), fontSize = 10.sp)
                Text(
                    DominoStrings.ruleAndTarget(venue.defaultMode.title, venue.targetScore),
                    color = Color.LightGray,
                    fontSize = 10.sp
                )

                // Misiones de la sede: es el motivo concreto para volver a una ciudad ya
                // completada, asi que se muestran en la propia tarjeta y no en un submenu.
                VenueMissionLines(venueId = venue.id, completedIds = completedIds)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${venue.entryBet}",
                    color = GoldAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onEnter,
                    enabled = isEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = DarkNavy
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = DominoStrings.travelBtn(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
