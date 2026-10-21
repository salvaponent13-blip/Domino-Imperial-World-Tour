package com.example.dominograndprix

import com.example.dominograndprix.engine.DominoBoardLayout
import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Blindaje geometrico del tablero.
 *
 * La v2.3.0 introdujo un fallo que ningun test detectaba: la fila de vuelta de la serpentina
 * cruzaba por encima de los brazos ARRIBA/ABAJO en ALL_FIVES con cadenas largas, porque las
 * unicas aserciones de colision que existian seguian usando la convencion antigua (esquina
 * superior izquierda y fichas de 15x29). Estas pruebas fijan la convencion real (centro y
 * 28x56) y barren todas las longitudes de rama posibles.
 */
class DominoBoardLayoutTest {

    private fun box(t: BoardPlacedTile): FloatArray {
        val hw = if (t.isVertical) DominoBoardLayout.HALF_SHORT else DominoBoardLayout.HALF_LONG
        val hh = if (t.isVertical) DominoBoardLayout.HALF_LONG else DominoBoardLayout.HALF_SHORT
        return floatArrayOf(
            t.virtX - hw.toFloat(),
            t.virtY - hh.toFloat(),
            t.virtX + hw.toFloat(),
            t.virtY + hh.toFloat()
        )
    }

    private fun overlapPixels(a: BoardPlacedTile, b: BoardPlacedTile): Float {
        val (ax0, ay0, ax1, ay1) = box(a).toList()
        val (bx0, by0, bx1, by1) = box(b).toList()
        val ox = minOf(ax1, bx1) - maxOf(ax0, bx0)
        val oy = minOf(ay1, by1) - maxOf(ay0, by0)
        return if (ox > 0.01f && oy > 0.01f) ox * oy else 0f
    }

    /** La primera ficha de la mesa: doble 6-6 en el centro, como en ALL_FIVES. */
    private fun centerDouble(): BoardPlacedTile = BoardPlacedTile(
        tile = DominoTile(6, 6),
        pipLeft = 6,
        pipRight = 6,
        isDouble = true,
        isFirstTile = true,
        virtX = 0,
        virtY = 0,
        isVertical = true,
        branchSide = null,
        branchStepIndex = 0,
        placementId = 1
    )

    /** Cadena valida de [count] fichas que empieza conectando con [startPip]. */
    private fun chain(startPip: Int, count: Int, side: BoardSide): List<BoardPlacedTile> {
        val out = mutableListOf<BoardPlacedTile>()
        var previous = startPip
        for (i in 0 until count) {
            val next = (startPip + 1 + i) % 7
            out.add(
                BoardPlacedTile(
                    tile = DominoTile(maxOf(previous, next), minOf(previous, next)),
                    pipLeft = 0,
                    pipRight = 0,
                    isFirstTile = false,
                    virtX = 0,
                    virtY = 0,
                    isVertical = false,
                    branchSide = side,
                    branchStepIndex = i + 1,
                    placementId = i + 2
                )
            )
            previous = next
        }
        return out
    }

    private fun assertNoOverlap(tiles: List<BoardPlacedTile>, context: String) {
        for (i in tiles.indices) {
            for (j in i + 1 until tiles.size) {
                val area = overlapPixels(tiles[i], tiles[j])
                assertFalse(
                    "$context: las fichas $i (${tiles[i].tile} en ${tiles[i].virtX},${tiles[i].virtY}) y " +
                        "$j (${tiles[j].tile} en ${tiles[j].virtX},${tiles[j].virtY}) se solapan ($area u2)",
                    area > 0f
                )
            }
        }
    }

    private fun assertConnected(tiles: List<BoardPlacedTile>, context: String) {
        if (tiles.size < 2) return
        for (t in tiles) {
            val touches = tiles.any { other ->
                other !== t && overlapPixelsOrTouch(t, other)
            }
            assertTrue("$context: la ficha ${t.tile} en (${t.virtX},${t.virtY}) quedo aislada", touches)
        }
    }

    /** Contacto por borde (las fichas se tocan, no se solapan ni dejan hueco). */
    private fun overlapPixelsOrTouch(a: BoardPlacedTile, b: BoardPlacedTile): Boolean {
        val (ax0, ay0, ax1, ay1) = box(a).toList()
        val (bx0, by0, bx1, by1) = box(b).toList()
        val shareVertical = kotlin.math.abs(ax1 - bx0) < 0.01f || kotlin.math.abs(bx1 - ax0) < 0.01f
        val shareHorizontal = kotlin.math.abs(ay1 - by0) < 0.01f || kotlin.math.abs(by1 - ay0) < 0.01f
        if (shareVertical) return minOf(ay1, by1) - maxOf(ay0, by0) >= 14f
        if (shareHorizontal) return minOf(ax1, bx1) - maxOf(ax0, bx0) >= 14f
        return false
    }

    @Test
    fun fourBranchLayout_neverOverlaps() {
        for (left in 0..12) {
            for (right in 0..12) {
                for (up in 0..8) {
                    for (down in 0..8) {
                        val tiles = DominoBoardLayout.layoutBoardTiles(
                            firstTile = centerDouble(),
                            leftBranch = chain(6, left, BoardSide.LEFT),
                            rightBranch = chain(6, right, BoardSide.RIGHT),
                            upBranch = chain(6, up, BoardSide.UP),
                            downBranch = chain(6, down, BoardSide.DOWN),
                            crossMode = true
                        )
                        val context = "4 ramas (L=$left R=$right U=$up D=$down)"
                        assertNoOverlap(tiles, context)
                        assertConnected(tiles, context)
                    }
                }
            }
        }
    }

    @Test
    fun twoBranchLayout_neverOverlaps() {
        for (left in 0..26) {
            for (right in 0..26) {
                val tiles = DominoBoardLayout.layoutBoardTiles(
                    firstTile = centerDouble(),
                    leftBranch = chain(6, left, BoardSide.LEFT),
                    rightBranch = chain(6, right, BoardSide.RIGHT)
                )
                val context = "2 ramas (L=$left R=$right)"
                assertNoOverlap(tiles, context)
                assertConnected(tiles, context)
            }
        }
    }

    @Test
    fun fourBranchLayout_keepsCenterCorridorFree() {
        // Los brazos viven en |x| <= 14. Ninguna ficha de las ramas horizontales puede meterse
        // en esa franja (la primera ficha de cada fila en y = 0 solo la toca por el borde).
        val tiles = DominoBoardLayout.layoutBoardTiles(
            firstTile = centerDouble(),
            leftBranch = chain(6, 12, BoardSide.LEFT),
            rightBranch = chain(6, 12, BoardSide.RIGHT),
            upBranch = chain(6, 4, BoardSide.UP),
            downBranch = chain(6, 4, BoardSide.DOWN),
            crossMode = true
        )
        tiles.filter { it.branchSide == BoardSide.LEFT || it.branchSide == BoardSide.RIGHT }
            .forEach { t ->
                val (x0, _, x1, _) = box(t).toList()
                val outside = x0 >= DominoBoardLayout.CENTER_ARM_HALF_WIDTH - 0.01f ||
                    x1 <= -(DominoBoardLayout.CENTER_ARM_HALF_WIDTH - 0.01f)
                assertTrue(
                    "La ficha ${t.tile} en (${t.virtX},${t.virtY}) invade la franja de los brazos ($x0..$x1)",
                    outside
                )
            }
    }

    @Test
    fun computeBounds_isCenteredWhenBoardIsSmall() {
        val single = centerDouble()
        val bounds = DominoBoardLayout.computeBounds(listOf(single))
        val contentCenterX = (box(single)[0] + box(single)[2]) / 2f
        val gridCenterX = (bounds.minX + bounds.maxX) / 2f
        assertTrue(
            "El tablero debe quedar centrado: contenido en $contentCenterX, rejilla en $gridCenterX",
            kotlin.math.abs(contentCenterX - gridCenterX) < 0.5f
        )
    }

    @Test
    fun nextSlot_withDoubleCandidate_matchesWhereTheDoubleReallyGoes() {
        // Salida [6|3]: el extremo derecho vale 3, asi que un [3|3] entra por ahi.
        val first = BoardPlacedTile(
            tile = DominoTile(6, 3),
            pipLeft = 6,
            pipRight = 3,
            isFirstTile = true,
            virtX = 0,
            virtY = 0,
            isVertical = false,
            branchSide = null,
            branchStepIndex = 0,
            placementId = 1
        )

        val doubleSlot = DominoBoardLayout.getNextSlotPlacement(
            firstTile = first,
            leftBranch = emptyList(),
            rightBranch = emptyList(),
            side = BoardSide.RIGHT,
            candidateTile = DominoTile(3, 3)
        )
        assertTrue("Un doble se coloca en cruz", doubleSlot.isVertical)
        // El doble ocupa 2 celdas de ancho: centro a 28 + 14 = 42, no a 56 como una ficha normal.
        assertTrue("El hueco del doble debe caer en 42, no en ${doubleSlot.virtX}", doubleSlot.virtX == 42)

        val normalSlot = DominoBoardLayout.getNextSlotPlacement(
            firstTile = first,
            leftBranch = emptyList(),
            rightBranch = emptyList(),
            side = BoardSide.RIGHT,
            candidateTile = DominoTile(3, 5)
        )
        assertFalse("Una ficha normal va horizontal", normalSlot.isVertical)
        assertTrue("El hueco de una ficha normal cae en 56, no en ${normalSlot.virtX}", normalSlot.virtX == 56)
    }
}
