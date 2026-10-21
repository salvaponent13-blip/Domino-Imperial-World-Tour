package com.example.dominograndprix.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dominograndprix.model.DominoTile
import kotlinx.coroutines.launch

/**
 * Ficha colocada con impacto fsico 3D y emisin sutil de partculas/tiza al asentarse sobre el fieltro.
 */
@Composable
fun AnimatedPlacedTile(
    tile: DominoTile,
    pip1: Int,
    pip2: Int,
    isHorizontal: Boolean,
    width: Dp,
    height: Dp,
    /** Identificador nico de la colocacin. Si es 0, no se anima (fichas restauradas). */
    placementId: Int,
    modifier: Modifier = Modifier
) {
    val scale = remember(placementId) { Animatable(if (placementId == 0) 1f else 0.88f) }
    val alpha = remember(placementId) { Animatable(if (placementId == 0) 1f else 0.40f) }
    val dustAnim = remember(placementId) { Animatable(0f) }

    LaunchedEffect(placementId) {
        if (placementId == 0) return@LaunchedEffect
        launch {
            scale.animateTo(1.03f, tween(durationMillis = 90, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(durationMillis = 110, easing = FastOutSlowInEasing))
        }
        launch {
            alpha.animateTo(1f, tween(durationMillis = 100, easing = FastOutSlowInEasing))
        }
        launch {
            dustAnim.animateTo(1f, tween(durationMillis = 580, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        DominoTileView(
            tile = tile,
            pip1 = pip1,
            pip2 = pip2,
            isHorizontal = isHorizontal,
            width = width,
            height = height
        )

        // Rfaga sutil de micro-partculas de tiza/fieltro que se dispersa en el impacto
        if (placementId != 0 && dustAnim.value in 0.01f..0.98f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = dustAnim.value
                val particleCount = if (tile.isDouble) 16 else 10
                val fade = (1f - p) * 0.70f
                val w = size.width
                val h = size.height

                for (i in 0 until particleCount) {
                    val angle = (i.toFloat() / particleCount) * 2f * Math.PI.toFloat()
                    val dist = (8.dp.toPx() + (i % 4) * 3.dp.toPx()) * p
                    val px = (w / 2f) + kotlin.math.cos(angle) * ((w / 2f) + dist)
                    val py = (h / 2f) + kotlin.math.sin(angle) * ((h / 2f) + dist)
                    val r = 1.6.dp.toPx() * (1f - p * 0.35f)

                    val color = when (i % 3) {
                        0 -> Color(0xFFFFF6DD).copy(alpha = fade)
                        1 -> Color(0xFFFFD700).copy(alpha = fade * 0.75f)
                        else -> Color(0xFFE2E8F0).copy(alpha = fade * 0.6f)
                    }
                    drawCircle(color = color, radius = r, center = Offset(px, py))
                }
            }
        }
    }
}

/** Anchura de una ficha según su orientación y el tamaño de celda (rejilla de 14: 2x4 celdas). */
fun tileWidth(unit: Dp, isVertical: Boolean): Dp = unit * (if (isVertical) 2f else 4f)

/** Altura de una ficha según su orientación y el tamaño de celda (rejilla de 14: 2x4 celdas). */
fun tileHeight(unit: Dp, isVertical: Boolean): Dp = unit * (if (isVertical) 4f else 2f)
