package com.example.dominograndprix.ui.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.audio.DominoHaptics
import com.example.dominograndprix.i18n.AchievementStrings
import com.example.dominograndprix.i18n.DominoStrings
import com.example.dominograndprix.ui.game.GoldAccent
import kotlinx.coroutines.delay

/** Cuanto tiempo se queda el aviso en pantalla antes de irse solo. */
private const val BANNER_VISIBLE_MILLIS = 3200L

/**
 * Aviso emergente de logro o mision conseguida.
 *
 * Se coloca arriba y se va solo: interrumpe lo justo para que el jugador se entere, sin obligarle
 * a cerrar nada en mitad de una partida.
 *
 * @param achievementId logro conseguido, o null si no hay ninguno
 * @param missionId mision conseguida, o null
 */
@Composable
fun AchievementUnlockedBanner(
    achievementId: String? = null,
    missionId: String? = null,
    hapticsView: android.view.View? = null,
    onDismissed: () -> Unit = {}
) {
    val hasSomething = achievementId != null || missionId != null
    var visible by remember(achievementId, missionId) { mutableStateOf(hasSomething) }

    // Vibracion suave y desaparicion automatica. La clave del efecto incluye el identificador
    // para que dos logros seguidos vuelvan a disparar el aviso en lugar de reutilizar el estado.
    LaunchedEffect(achievementId, missionId) {
        if (!hasSomething) return@LaunchedEffect
        hapticsView?.let { DominoHaptics.score(it) }
        delay(BANNER_VISIBLE_MILLIS)
        visible = false
        onDismissed()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        val isAchievement = achievementId != null
        val title = if (achievementId != null) {
            DominoStrings.achievementUnlockedToast(
                AchievementStrings.titleFor(achievementId, DominoStrings.currentLanguage)
            )
        } else {
            val mission = AchievementStrings.allMissions().firstOrNull { it.id == missionId }
            if (mission != null) {
                DominoStrings.missionCompletedToast(
                    AchievementStrings.missionTitleFor(mission, DominoStrings.currentLanguage)
                )
            } else {
                DominoStrings.missionCompletedToast(missionId ?: "")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF21B1405), RoundedCornerShape(12.dp))
                    .border(2.dp, GoldAccent, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (isAchievement) "🏆" else "✓", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = DominoStrings.achievementsBtn(),
                        color = Color(0xFFB8C2D0),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
