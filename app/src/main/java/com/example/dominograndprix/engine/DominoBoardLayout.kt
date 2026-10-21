package com.example.dominograndprix.engine

import com.example.dominograndprix.model.BoardPlacedTile
import com.example.dominograndprix.model.BoardSide
import com.example.dominograndprix.model.DominoTile

/**
 * Geometria exacta del tablero.
 *
 * Todas las coordenadas son el **centro** de la ficha y son multiplos de 14: la ficha mide
 * 56x28 (dos mitades de 28) y se dibuja sobre una rejilla de celdas de 14.
 *
 * - La ficha de salida se coloca en (0, 0) y, si es doble, en cruz (vertical).
 * - La rama DERECHA crece hacia la derecha, baja 84 unidades y vuelve; la IZQUIERDA crece
 *   hacia la izquierda, sube 84 y vuelve. Las filas paralelas nunca se tocan.
 * - Todos los dobles van en cruz (perpendiculares al sentido de la cadena).
 * - En modo de 4 ramas (ALL_FIVES con spinner) la franja central queda reservada para los
 *   brazos ARRIBA/ABAJO: las serpentinas giran dentro de su propio lado antes de entrar en
 *   esa franja, de modo que nunca se cruzan con los brazos por larga que sea la cadena.
 */
object DominoBoardLayout {
    const val HALF_SHORT = 14
    const val TILE_SHORT = 28
    const val HALF_LONG = 28
    const val TILE_LONG = 56

    /** Umbral horizontal (13 * 14 = 182) antes de iniciar el puente vertical de giro. */
    const val TURN_LIMIT_X = 182

    /** Separacion vertical entre filas paralelas de la serpentina (6 * 14 = 84). */
    const val ROW_SPACING_Y = 84

    /** Semiancho de la franja central que ocupan los brazos ARRIBA/ABAJO (|x| <= 14). */
    const val CENTER_ARM_HALF_WIDTH = 14

    /** Huelgo de una celda entre la franja central y cualquier ficha horizontal. */
    const val CENTER_CLEARANCE = CENTER_ARM_HALF_WIDTH + 14

    /** Extremo minimo (en valor absoluto) al que puede llegar una fila que vuelve al centro. */
    const val CROSS_MIN_END = CENTER_CLEARANCE + TILE_SHORT

    /**
     * Calcula la posicion y orientacion de todas las fichas de la mesa.
     *
     * @param crossMode reserva la franja central para los brazos del spinner. El motor lo
     *   activa en ALL_FIVES en cuanto hay un doble en el centro; si hay brazos, se activa solo.
     */
    fun layoutBoardTiles(
        firstTile: BoardPlacedTile?,
        leftBranch: List<BoardPlacedTile>,
        rightBranch: List<BoardPlacedTile>,
        upBranch: List<BoardPlacedTile> = emptyList(),
        downBranch: List<BoardPlacedTile> = emptyList(),
        crossMode: Boolean = false
    ): List<BoardPlacedTile> {
        if (firstTile == null) return emptyList()

        val reserveCorridor = crossMode || upBranch.isNotEmpty() || downBranch.isNotEmpty()

        // 1. Ficha de salida en el centro (0, 0)
        val isFirstDouble = firstTile.tile.isDouble
        val updatedFirst = firstTile.copy(
            virtX = 0,
            virtY = 0,
            isVertical = isFirstDouble,
            pipLeft = firstTile.tile.high,
            pipRight = firstTile.tile.low
        )

        // 2. Rama DERECHA: avanza a la derecha, baja 84 y vuelve
        val rightLaidOut = mutableListOf<BoardPlacedTile>()
        var rX = if (isFirstDouble) HALF_SHORT else HALF_LONG
        var rY = 0
        var rDir = 1 // 1 = derecha, -1 = izquierda
        var rTurnStep = 0 // 0 = horizontal, 1 = primer tramo del giro, 2 = puente, 3 = salida de fila
        var rCurrentOpenPip = updatedFirst.pipRight

        for (placed in rightBranch) {
            val isDbl = placed.tile.isDouble
            val matchPip = placed.connectedPip
                ?: if (placed.tile.high == rCurrentOpenPip) placed.tile.high else placed.tile.low
            val openPip = placed.openEndPip
                ?: if (placed.tile.high == rCurrentOpenPip) placed.tile.low else placed.tile.high
            val advance = if (isDbl) TILE_SHORT else TILE_LONG

            if (rTurnStep == 0 && !isDbl) {
                if (rDir == 1 && rX >= TURN_LIMIT_X) {
                    rTurnStep = 1
                } else if (rDir == -1 && reserveCorridor && rX - advance < CROSS_MIN_END) {
                    // La fila ya no puede seguir hacia el centro: gira dentro de su propio lado
                    rTurnStep = 1
                } else if (rDir == -1 && !reserveCorridor && rX <= -TURN_LIMIT_X) {
                    rTurnStep = 1
                }
            }

            val updatedPlaced: BoardPlacedTile
            if (rTurnStep == 1) {
                // Primer tramo vertical del giro (baja)
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (rDir == 1) rX + HALF_SHORT else rX - HALF_SHORT,
                    virtY = rY + HALF_SHORT,
                    pipLeft = matchPip,
                    pipRight = openPip
                )
                rTurnStep = 2
            } else if (rTurnStep == 2) {
                // Segundo tramo vertical: separa las filas para que no se toquen
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (rDir == 1) rX + HALF_SHORT else rX - HALF_SHORT,
                    virtY = rY + 70,
                    pipLeft = matchPip,
                    pipRight = openPip
                )
                rY += ROW_SPACING_Y
                rDir = -rDir
                rTurnStep = 3
            } else {
                val placeCrosswiseDouble = isDbl
                rTurnStep = 0

                if (placeCrosswiseDouble) {
                    // Doble SIEMPRE en cruz (vertical dentro de una fila horizontal)
                    if (rDir == 1) {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = rX + HALF_SHORT,
                            virtY = rY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        rX += TILE_SHORT
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = rX - HALF_SHORT,
                            virtY = rY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        rX -= TILE_SHORT
                    }
                } else {
                    // Ficha normal horizontal
                    if (rDir == 1) {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = rX + HALF_LONG,
                            virtY = rY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        rX += TILE_LONG
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = rX - HALF_LONG,
                            virtY = rY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        rX -= TILE_LONG
                    }
                }
            }

            rCurrentOpenPip = openPip
            rightLaidOut.add(updatedPlaced)
        }

        // 3. Rama IZQUIERDA: avanza a la izquierda, sube 84 y vuelve
        val leftLaidOut = mutableListOf<BoardPlacedTile>()
        var lX = if (isFirstDouble) -HALF_SHORT else -HALF_LONG
        var lY = 0
        var lDir = -1 // -1 = izquierda, 1 = derecha
        var lTurnStep = 0
        var lCurrentOpenPip = updatedFirst.pipLeft

        for (placed in leftBranch) {
            val isDbl = placed.tile.isDouble
            val matchPip = placed.connectedPip
                ?: if (placed.tile.high == lCurrentOpenPip) placed.tile.high else placed.tile.low
            val openPip = placed.openEndPip
                ?: if (placed.tile.high == lCurrentOpenPip) placed.tile.low else placed.tile.high
            val advance = if (isDbl) TILE_SHORT else TILE_LONG

            if (lTurnStep == 0 && !isDbl) {
                if (lDir == -1 && lX <= -TURN_LIMIT_X) {
                    lTurnStep = 1
                } else if (lDir == 1 && reserveCorridor && lX + advance > -CROSS_MIN_END) {
                    // La fila ya no puede seguir hacia el centro: gira dentro de su propio lado
                    lTurnStep = 1
                } else if (lDir == 1 && !reserveCorridor && lX >= TURN_LIMIT_X) {
                    lTurnStep = 1
                }
            }

            val updatedPlaced: BoardPlacedTile
            if (lTurnStep == 1) {
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (lDir == -1) lX - HALF_SHORT else lX + HALF_SHORT,
                    virtY = lY - HALF_SHORT,
                    pipLeft = openPip,
                    pipRight = matchPip
                )
                lTurnStep = 2
            } else if (lTurnStep == 2) {
                updatedPlaced = placed.copy(
                    isVertical = true,
                    virtX = if (lDir == -1) lX - HALF_SHORT else lX + HALF_SHORT,
                    virtY = lY - 70,
                    pipLeft = openPip,
                    pipRight = matchPip
                )
                lY -= ROW_SPACING_Y
                lDir = -lDir
                lTurnStep = 3
            } else {
                val placeCrosswiseDouble = isDbl
                lTurnStep = 0

                if (placeCrosswiseDouble) {
                    if (lDir == -1) {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = lX - HALF_SHORT,
                            virtY = lY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        lX -= TILE_SHORT
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = true,
                            virtX = lX + HALF_SHORT,
                            virtY = lY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        lX += TILE_SHORT
                    }
                } else {
                    if (lDir == -1) {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = lX - HALF_LONG,
                            virtY = lY,
                            pipLeft = openPip,
                            pipRight = matchPip
                        )
                        lX -= TILE_LONG
                    } else {
                        updatedPlaced = placed.copy(
                            isVertical = false,
                            virtX = lX + HALF_LONG,
                            virtY = lY,
                            pipLeft = matchPip,
                            pipRight = openPip
                        )
                        lX += TILE_LONG
                    }
                }
            }

            lCurrentOpenPip = openPip
            leftLaidOut.add(updatedPlaced)
        }

        // 4. Rama ARRIBA (solo en el modo de 4 ramas)
        val upLaidOut = mutableListOf<BoardPlacedTile>()
        if (upBranch.isNotEmpty()) {
            var uY = if (isFirstDouble) -HALF_LONG else -HALF_SHORT
            var uCurrentOpenPip = updatedFirst.pipLeft

            for (placed in upBranch) {
                val isDbl = placed.tile.isDouble
                val matchPip = placed.connectedPip
                    ?: if (placed.tile.high == uCurrentOpenPip) placed.tile.high else placed.tile.low
                val openPip = placed.openEndPip
                    ?: if (placed.tile.high == uCurrentOpenPip) placed.tile.low else placed.tile.high

                val updatedPlaced = if (isDbl) {
                    // Doble en cruz (horizontal dentro de una rama vertical)
                    placed.copy(
                        isVertical = false,
                        virtX = 0,
                        virtY = uY - HALF_SHORT,
                        pipLeft = matchPip,
                        pipRight = openPip
                    ).also { uY -= TILE_SHORT }
                } else {
                    placed.copy(
                        isVertical = true,
                        virtX = 0,
                        virtY = uY - HALF_LONG,
                        pipLeft = openPip,
                        pipRight = matchPip
                    ).also { uY -= TILE_LONG }
                }
                uCurrentOpenPip = openPip
                upLaidOut.add(updatedPlaced)
            }
        }

        // 5. Rama ABAJO (solo en el modo de 4 ramas)
        val downLaidOut = mutableListOf<BoardPlacedTile>()
        if (downBranch.isNotEmpty()) {
            var dY = if (isFirstDouble) HALF_LONG else HALF_SHORT
            var dCurrentOpenPip = updatedFirst.pipRight

            for (placed in downBranch) {
                val isDbl = placed.tile.isDouble
                val matchPip = placed.connectedPip
                    ?: if (placed.tile.high == dCurrentOpenPip) placed.tile.high else placed.tile.low
                val openPip = placed.openEndPip
                    ?: if (placed.tile.high == dCurrentOpenPip) placed.tile.low else placed.tile.high

                val updatedPlaced = if (isDbl) {
                    // Doble en cruz (horizontal dentro de una rama vertical)
                    placed.copy(
                        isVertical = false,
                        virtX = 0,
                        virtY = dY + HALF_SHORT,
                        pipLeft = matchPip,
                        pipRight = openPip
                    ).also { dY += TILE_SHORT }
                } else {
                    placed.copy(
                        isVertical = true,
                        virtX = 0,
                        virtY = dY + HALF_LONG,
                        pipLeft = matchPip,
                        pipRight = openPip
                    ).also { dY += TILE_LONG }
                }
                dCurrentOpenPip = openPip
                downLaidOut.add(updatedPlaced)
            }
        }

        val result = mutableListOf<BoardPlacedTile>()
        result.addAll(leftLaidOut.asReversed())
        result.add(updatedFirst)
        result.addAll(rightLaidOut)
        result.addAll(upLaidOut)
        result.addAll(downLaidOut)
        return result
    }

    data class BoardBounds(
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float,
        val totalCols: Float,
        val totalRows: Float
    ) {
        val centerX: Float get() = (minX + maxX) / 2f
        val centerY: Float get() = (minY + maxY) / 2f
    }

    /**
     * Limites de la mesa en unidades virtuales.
     *
     * El contenido queda **centrado**: si el tablero real ocupa menos de 8x6 celdas (por
     * ejemplo la ficha de salida sola) el margen se reparte a partes iguales entre los cuatro
     * lados, en lugar de crecer solo hacia la derecha y hacia abajo.
     */
    fun computeBounds(tiles: List<BoardPlacedTile>): BoardBounds {
        if (tiles.isEmpty()) {
            return BoardBounds(minX = -56f, maxX = 56f, minY = -42f, maxY = 42f, totalCols = 8f, totalRows = 6f)
        }
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (placed in tiles) {
            val halfW = if (placed.isVertical) HALF_SHORT.toFloat() else HALF_LONG.toFloat()
            val halfH = if (placed.isVertical) HALF_LONG.toFloat() else HALF_SHORT.toFloat()
            minX = minOf(minX, placed.virtX - halfW)
            maxX = maxOf(maxX, placed.virtX + halfW)
            minY = minOf(minY, placed.virtY - halfH)
            maxY = maxOf(maxY, placed.virtY + halfH)
        }

        // Ajuste a bordes de celda con una celda de seguridad por lado
        var snapMinX = kotlin.math.floor((minX - 14f) / 14f) * 14f
        var snapMaxX = kotlin.math.ceil((maxX + 14f) / 14f) * 14f
        var snapMinY = kotlin.math.floor((minY - 14f) / 14f) * 14f
        var snapMaxY = kotlin.math.ceil((maxY + 14f) / 14f) * 14f

        val rawCols = (snapMaxX - snapMinX) / 14f
        val rawRows = (snapMaxY - snapMinY) / 14f

        // Minimo de 8x6 celdas repartido de forma simetrica para no descentrar el tablero
        val extraCols = ((8f - rawCols).coerceAtLeast(0f) / 2f) * 14f
        val extraRows = ((6f - rawRows).coerceAtLeast(0f) / 2f) * 14f
        snapMinX -= extraCols
        snapMaxX += extraCols
        snapMinY -= extraRows
        snapMaxY += extraRows

        return BoardBounds(
            minX = snapMinX,
            maxX = snapMaxX,
            minY = snapMinY,
            maxY = snapMaxY,
            totalCols = maxOf(8f, rawCols),
            totalRows = maxOf(6f, rawRows)
        )
    }

    /** Hueco (posicion y orientacion) donde caeria [candidateTile] si se jugara en [side]. */
    fun getNextSlotPlacement(
        firstTile: BoardPlacedTile?,
        leftBranch: List<BoardPlacedTile>,
        rightBranch: List<BoardPlacedTile>,
        upBranch: List<BoardPlacedTile> = emptyList(),
        downBranch: List<BoardPlacedTile> = emptyList(),
        side: BoardSide,
        candidateTile: DominoTile?,
        crossMode: Boolean = false
    ): NextSlotPlacement {
        if (firstTile == null) {
            return NextSlotPlacement(
                virtX = 0,
                virtY = 0,
                isVertical = candidateTile?.isDouble == true,
                isForward = true
            )
        }
        val dummyTile = candidateTile ?: DominoTile(6, 0)
        val dummyPlaced = BoardPlacedTile(
            tile = dummyTile,
            pipLeft = dummyTile.high,
            pipRight = dummyTile.low,
            isDouble = dummyTile.isDouble,
            branchSide = side
        )
        val simulated = when (side) {
            BoardSide.LEFT -> layoutBoardTiles(firstTile, leftBranch + dummyPlaced, rightBranch, upBranch, downBranch, crossMode)
            BoardSide.RIGHT -> layoutBoardTiles(firstTile, leftBranch, rightBranch + dummyPlaced, upBranch, downBranch, crossMode)
            BoardSide.UP -> layoutBoardTiles(firstTile, leftBranch, rightBranch, upBranch + dummyPlaced, downBranch, crossMode)
            BoardSide.DOWN -> layoutBoardTiles(firstTile, leftBranch, rightBranch, upBranch, downBranch + dummyPlaced, crossMode)
        }
        // La ficha simulada queda al final de su rama en la lista resultante... salvo en la
        // rama izquierda, que se invierte: alli la mas lejana es la primera.
        val target = when (side) {
            BoardSide.LEFT -> simulated.firstOrNull { it.branchSide == BoardSide.LEFT }
            BoardSide.RIGHT -> simulated.lastOrNull { it.branchSide == BoardSide.RIGHT }
            BoardSide.UP -> simulated.lastOrNull { it.branchSide == BoardSide.UP }
            BoardSide.DOWN -> simulated.lastOrNull { it.branchSide == BoardSide.DOWN }
        } ?: simulated.last()

        return NextSlotPlacement(
            virtX = target.virtX,
            virtY = target.virtY,
            isVertical = target.isVertical,
            isForward = true
        )
    }
}
