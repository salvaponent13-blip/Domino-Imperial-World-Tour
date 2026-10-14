package com.example.dominograndprix.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import com.example.dominograndprix.model.DominoTile

/**
 * Ficha colocada, con animacion de asentamiento la primera vez que aparece.
 *
 * El detalle importante es que la animacion corre **una sola vez por colocacion**, no en cada
 * recomposicion ni cada vez que la cadena se reordena: la identidad la da [placementId], que
 * asigna el motor al poner la ficha. Sin eso, jugar por la izquierda reiniciaria la animacion
 * de todas las fichas ya colocadas, porque sus indices en la lista cambian.
 *
 * El posicionamiento (offset y tamano) lo aplica quien la usa: aqui solo van la escala y la
 * opacidad, para no anidar modificadores que se pisarian entre si.
 */
@Composable
fun AnimatedPlacedTile(
    tile: DominoTile,
    pip1: Int,
    pip2: Int,
    isHorizontal: Boolean,
    width: Dp,
    height: Dp,
    /** Identificador unico de la colocacion. Si es 0, no se anima (fichas restauradas). */
    placementId: Int,
    modifier: Modifier = Modifier
) {
    // 0.85 -> 1.0 con desaceleracion: se asienta rapido y sin rebote exagerado, como una ficha
    // que se apoya sobre el fieltro.
    val scale = remember(placementId) { Animatable(if (placementId == 0) 1f else 0.85f) }
    val alpha = remember(placementId) { Animatable(if (placementId == 0) 1f else 0.35f) }

    LaunchedEffect(placementId) {
        if (placementId == 0) return@LaunchedEffect
        scale.animateTo(1f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
    }

    DominoTileView(
        tile = tile,
        pip1 = pip1,
        pip2 = pip2,
        isHorizontal = isHorizontal,
        width = width,
        height = height,
        modifier = modifier
            .scale(scale.value)
            .alpha(alpha.value)
    )
}

/** Anchura de una ficha segun su orientacion y la escala de la mesa. */
fun tileWidth(unit: Dp, isVertical: Boolean): Dp = unit * (if (isVertical) 17f else 31f)

/** Altura de una ficha segun su orientacion y la escala de la mesa. */
fun tileHeight(unit: Dp, isVertical: Boolean): Dp = unit * (if (isVertical) 31f else 17f)
