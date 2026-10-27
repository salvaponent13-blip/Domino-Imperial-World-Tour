package com.example.dominograndprix.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dominograndprix.i18n.TutorialStrings
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent

/**
 * Tarjeta de ayuda que se dibuja encima de la mesa.
 *
 * Se usa para dos cosas: el tutorial de tres pasos de la primera partida y la tarjeta que explica
 * la modalidad la primera vez que se juega. No bloquea el juego: se cierra con "Saltar".
 *
 * La posición cambia a propósito: el paso que habla de la mano se pinta arriba para no tapar las
 * fichas, y los que hablan del tablero se pintan abajo.
 */
@Composable
fun TutorialCard(
    title: String,
    body: String,
    stepLabel: String? = null,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String,
    placeAtTop: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f))) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkNavy,
            border = androidx.compose.foundation.BorderStroke(2.dp, GoldAccent),
            modifier = Modifier
                .align(if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 22.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (stepLabel != null) {
                    Text(
                        text = stepLabel,
                        color = GoldAccent.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = title,
                    color = GoldAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = body,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text(TutorialStrings.skip(), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = DarkNavy)
                    ) {
                        Text(nextLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/** Tutorial completo de tres pasos sobre la mesa. */
@Composable
fun FirstMatchTutorial(
    step: Int,
    totalSteps: Int,
    onAdvance: () -> Unit,
    onFinish: () -> Unit
) {
    TutorialCard(
        title = TutorialStrings.stepTitle(step),
        body = TutorialStrings.stepBody(step),
        stepLabel = "${step + 1}/$totalSteps",
        onSkip = onFinish,
        onNext = onAdvance,
        nextLabel = if (step >= totalSteps - 1) TutorialStrings.done() else TutorialStrings.next(),
        placeAtTop = step == 0
    )
}

/** Tarjeta de una sola pantalla que explica la modalidad que se va a jugar. */
@Composable
fun ModeHintCard(modeTitle: String, hint: String, onDismiss: () -> Unit) {
    TutorialCard(
        title = TutorialStrings.modeCardTitle(),
        body = "$modeTitle\n\n$hint",
        stepLabel = null,
        onSkip = onDismiss,
        onNext = onDismiss,
        nextLabel = TutorialStrings.done(),
        placeAtTop = false
    )
}
