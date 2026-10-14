package com.example.dominograndprix.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.data.AchievementCatalog
import com.example.dominograndprix.i18n.AchievementStrings
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

/** Un logro listo para pintar: su identificador y si el jugador lo tiene. */
data class AchievementRow(val id: String, val unlocked: Boolean)

/**
 * Prepara la lista de la vitrina a partir de los logros conseguidos.
 *
 * Es una funcion pura y por eso se puede probar: el orden es siempre el del catalogo, de modo
 * que la vitrina no baila entre visitas.
 */
fun achievementRows(unlockedIds: Set<String>): List<AchievementRow> =
    AchievementCatalog.ALL.map { id -> AchievementRow(id = id, unlocked = id in unlockedIds) }

/** Cuantos logros hay y cuantos estan conseguidos, mas el porcentaje ya redondeado. */
data class AchievementProgress(val unlocked: Int, val total: Int, val percent: Int)

/** Calcula el progreso. Con catalogo vacio devuelve 0% en lugar de dividir por cero. */
fun achievementProgress(unlockedIds: Set<String>): AchievementProgress {
    val total = AchievementCatalog.ALL.size
    val unlocked = AchievementCatalog.ALL.count { it in unlockedIds }
    val percent = if (total == 0) 0 else (unlocked * 100) / total
    return AchievementProgress(unlocked = unlocked, total = total, percent = percent)
}

/**
 * Vitrina de logros.
 *
 * Muestra TODO el catalogo, no solo lo conseguido: un logro bloqueado con su descripcion es lo
 * que le dice al jugador que hay algo mas que hacer.
 */
@Composable
fun AchievementsScreen(
    unlockedIds: Set<String>,
    onBack: () -> Unit = {}
) {
    val rows = achievementRows(unlockedIds)
    val progress = achievementProgress(unlockedIds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B1526), DarkNavy, Color(0xFF05080F))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera con el progreso global
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC05080F))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = DominoStrings.achievementsTitle(),
                    color = GoldAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = DominoStrings.achievementsProgress(
                        unlocked = progress.unlocked,
                        total = progress.total,
                        percent = progress.percent
                    ),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (progress.total == 0) 0f else progress.unlocked.toFloat() / progress.total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = GoldAccent,
                    trackColor = Color(0xFF243044)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(rows, key = { it.id }) { row ->
                    AchievementCard(row = row)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    Button(
                        onClick = {
                            DominoSoundManager.playTileTap()
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = DarkNavy
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(DominoStrings.backToMainMenu(), fontWeight = FontWeight.Bold)
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * Tarjeta de un logro.
 *
 * La diferencia entre conseguido y bloqueado no es solo el color (dorado frente a gris): el
 * conseguido lleva el sello "✓ CONSEGUIDO" y el bloqueado un candado, para que se distingan
 * aunque no se perciba bien el color.
 */
@Composable
fun AchievementCard(row: AchievementRow) {
    val language = DominoStrings.currentLanguage
    val title = AchievementStrings.titleFor(row.id, language)
    val description = AchievementStrings.descriptionFor(row.id, language)

    val background = if (row.unlocked) Color(0xFF2A2410) else Color(0xFF141C2A)
    val accent = if (row.unlocked) GoldAccent else Color(0xFF5A6A80)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, accent.copy(alpha = if (row.unlocked) 0.9f else 0.35f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (row.unlocked) "🏆" else "🔒",
            fontSize = 24.sp,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = if (row.unlocked) Color.White else Color(0xFFB8C2D0),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (row.unlocked) {
                    DominoStrings.achievementUnlockedBadge()
                } else {
                    DominoStrings.achievementLockedBadge()
                },
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color(0xFF9FB0C4),
                fontSize = 11.sp
            )
        }
    }
}
