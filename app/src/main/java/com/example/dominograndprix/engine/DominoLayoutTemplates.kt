package com.example.dominograndprix.engine

/**
 * Paso de recorrido del tablero en el espacio virtual de 240x320.
 * @param dx Desplazamiento X relativo al hueco anterior
 * @param dy Desplazamiento Y relativo al hueco anterior
 * @param mod 0 = Vertical (17x31), 1 = Horizontal (31x17)
 * @param flags 0 = Hacia delante (conecta arriba/izquierda, queda abierto abajo/derecha),
 *              1 o 2 = Invertido (conecta abajo/derecha, queda abierto arriba/izquierda)
 */
data class TemplateStep(
    val dx: Int,
    val dy: Int,
    val mod: Int,
    val flags: Int
) {
    val isVertical: Boolean get() = mod == 0
    val isForward: Boolean get() = (flags and 3) == 0
}

data class BoardPatternTemplate(
    val id: Int,
    val leftBranch: List<TemplateStep>,
    val rightBranch: List<TemplateStep>
)

data class FourBranchPatternTemplate(
    val leftBranch: List<TemplateStep>,
    val rightBranch: List<TemplateStep>,
    val upBranch: List<TemplateStep>,
    val downBranch: List<TemplateStep>
)

/**
 * Los 5 recorridos de tablero de dos ramas incluidos en el juego. Cada patrón enrolla
 * la cadena en escalones y serpentinas en S para que todas las fichas quepan en el
 * tablero vertical de 194x208 con las caras conectadas de forma exacta.
 */
object DominoLayoutTemplates {

    /**
     * Patrón oficial de 4 ramas en cruz para Dominó de Cinco (All Fives).
     * Cada rama se desenvuelve en su propio cuadrante exclusivo sin cruzarse jamás:
     * - Rama Izquierda (Branch 20): cuadrante superior-izquierdo
     * - Rama Derecha (Branch 21): cuadrante inferior-derecho
     * - Rama Superior (Branch 22): cuadrante superior-derecho
     * - Rama Inferior (Branch 23): cuadrante inferior-izquierdo
     */
    val FOUR_BRANCH_PATTERN = FourBranchPatternTemplate(
        leftBranch = listOf(
            TemplateStep(0, 0, 1, 0),
            TemplateStep(-29, 0, 1, 1),
            TemplateStep(-58, 0, 1, 1),
            TemplateStep(-58, -29, 0, 2),
            TemplateStep(-58, -44, 1, 0),
            TemplateStep(-58, -73, 0, 2),
            TemplateStep(-58, -88, 1, 0),
            TemplateStep(-29, -88, 1, 0),
            TemplateStep(-15, -117, 0, 2),
            TemplateStep(-44, -103, 1, 1),
            TemplateStep(-73, -103, 1, 1),
            TemplateStep(-102, -103, 1, 1),
            TemplateStep(-102, -88, 0, 0),
            TemplateStep(-102, -59, 0, 0),
            TemplateStep(-102, -30, 0, 0),
            TemplateStep(-102, -1, 0, 0),
            TemplateStep(-102, 28, 0, 0),
            TemplateStep(-102, 57, 0, 0),
            TemplateStep(-102, 86, 0, 0),
            TemplateStep(-102, 115, 0, 0),
            TemplateStep(-102, 144, 0, 0),
            TemplateStep(-102, 173, 0, 0)
        ),
        rightBranch = listOf(
            TemplateStep(0, 0, 1, 0),
            TemplateStep(29, 0, 1, 0),
            TemplateStep(58, 0, 1, 0),
            TemplateStep(72, 15, 0, 0),
            TemplateStep(43, 29, 1, 1),
            TemplateStep(43, 44, 0, 0),
            TemplateStep(58, 58, 1, 0),
            TemplateStep(72, 73, 0, 0),
            TemplateStep(43, 87, 1, 1),
            TemplateStep(43, 102, 0, 0),
            TemplateStep(58, 116, 1, 0),
            TemplateStep(72, 131, 0, 0),
            TemplateStep(43, 145, 1, 1),
            TemplateStep(43, 160, 0, 0),
            TemplateStep(58, 174, 1, 0),
            TemplateStep(72, 189, 0, 0),
            TemplateStep(43, 203, 1, 1),
            TemplateStep(43, 218, 0, 0),
            TemplateStep(58, 232, 1, 0),
            TemplateStep(72, 247, 0, 0),
            TemplateStep(43, 261, 1, 1),
            TemplateStep(43, 276, 0, 0)
        ),
        upBranch = listOf(
            TemplateStep(0, 0, 1, 0),
            TemplateStep(7, -29, 0, 2),
            TemplateStep(7, -58, 0, 2),
            TemplateStep(22, -58, 1, 0),
            TemplateStep(36, -87, 0, 2),
            TemplateStep(51, -87, 1, 0),
            TemplateStep(65, -72, 0, 0),
            TemplateStep(80, -58, 1, 0),
            TemplateStep(94, -87, 0, 2),
            TemplateStep(80, -102, 1, 1),
            TemplateStep(51, -102, 1, 1),
            TemplateStep(22, -102, 1, 1),
            TemplateStep(22, -131, 0, 2),
            TemplateStep(22, -160, 0, 2),
            TemplateStep(22, -189, 0, 2),
            TemplateStep(22, -218, 0, 2),
            TemplateStep(22, -247, 0, 2),
            TemplateStep(22, -276, 0, 2),
            TemplateStep(22, -305, 0, 2),
            TemplateStep(22, -334, 0, 2),
            TemplateStep(22, -363, 0, 2),
            TemplateStep(22, -392, 0, 2)
        ),
        downBranch = listOf(
            TemplateStep(0, 0, 1, 0),
            TemplateStep(7, 15, 0, 0),
            TemplateStep(7, 44, 0, 0),
            TemplateStep(-22, 58, 1, 1),
            TemplateStep(-22, 73, 0, 0),
            TemplateStep(-51, 87, 1, 1),
            TemplateStep(-51, 102, 0, 0),
            TemplateStep(-80, 102, 1, 1),
            TemplateStep(-80, 117, 0, 0),
            TemplateStep(-80, 146, 0, 0),
            TemplateStep(-80, 175, 0, 0),
            TemplateStep(-80, 204, 0, 0),
            TemplateStep(-80, 233, 0, 0),
            TemplateStep(-80, 262, 0, 0),
            TemplateStep(-80, 291, 0, 0),
            TemplateStep(-80, 320, 0, 0),
            TemplateStep(-80, 349, 0, 0),
            TemplateStep(-80, 378, 0, 0),
            TemplateStep(-80, 407, 0, 0),
            TemplateStep(-80, 436, 0, 0),
            TemplateStep(-80, 465, 0, 0)
        )
    )

    val PATTERNS: List<BoardPatternTemplate> = listOf(
        // Patrón 0: serpentina clásica extendida a 28 pasos
        BoardPatternTemplate(
            id = 0,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-43, -29, 1, 0), TemplateStep(-14, -29, 1, 0),
                TemplateStep(15, -29, 1, 0), TemplateStep(44, -29, 1, 0), TemplateStep(73, -43, 0, 2),
                TemplateStep(59, -58, 1, 1), TemplateStep(30, -58, 1, 1), TemplateStep(1, -58, 1, 1),
                TemplateStep(-28, -58, 1, 1), TemplateStep(-57, -58, 1, 1), TemplateStep(-57, -87, 0, 2),
                TemplateStep(-42, -87, 1, 0), TemplateStep(-13, -87, 1, 0), TemplateStep(16, -87, 1, 0),
                TemplateStep(45, -87, 1, 0), TemplateStep(74, -87, 1, 0), TemplateStep(74, -116, 0, 2),
                TemplateStep(59, -116, 1, 1), TemplateStep(30, -116, 1, 1), TemplateStep(1, -116, 1, 1),
                TemplateStep(-28, -116, 1, 1), TemplateStep(-57, -116, 1, 1), TemplateStep(-57, -145, 0, 2),
                TemplateStep(-42, -145, 1, 0), TemplateStep(-13, -145, 1, 0)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(43, 29, 1, 1), TemplateStep(14, 29, 1, 1),
                TemplateStep(-15, 29, 1, 1), TemplateStep(-44, 29, 1, 1), TemplateStep(-59, 29, 0, 0),
                TemplateStep(-59, 58, 1, 0), TemplateStep(-30, 58, 1, 0), TemplateStep(-1, 58, 1, 0),
                TemplateStep(28, 58, 1, 0), TemplateStep(57, 58, 1, 0), TemplateStep(71, 73, 0, 0),
                TemplateStep(71, 102, 0, 0), TemplateStep(57, 116, 1, 1), TemplateStep(28, 116, 1, 1),
                TemplateStep(-1, 116, 1, 1), TemplateStep(-30, 116, 1, 1), TemplateStep(-59, 116, 1, 1),
                TemplateStep(-59, 145, 0, 0), TemplateStep(-45, 159, 1, 0), TemplateStep(-16, 159, 1, 0),
                TemplateStep(13, 159, 1, 0), TemplateStep(42, 159, 1, 0), TemplateStep(71, 159, 1, 0),
                TemplateStep(71, 188, 0, 0), TemplateStep(57, 202, 1, 1)
            )
        ),
        // Patrón 1: serpentina arriba + escalones cuadrados abajo extendida a 28 pasos
        BoardPatternTemplate(
            id = 1,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-43, -29, 1, 0), TemplateStep(-14, -29, 1, 0),
                TemplateStep(15, -29, 1, 0), TemplateStep(44, -29, 1, 0), TemplateStep(73, -43, 0, 2),
                TemplateStep(59, -58, 1, 1), TemplateStep(30, -58, 1, 1), TemplateStep(1, -58, 1, 1),
                TemplateStep(-28, -58, 1, 1), TemplateStep(-57, -58, 1, 1), TemplateStep(-57, -87, 0, 2),
                TemplateStep(-42, -87, 1, 0), TemplateStep(-13, -87, 1, 0), TemplateStep(16, -87, 1, 0),
                TemplateStep(45, -87, 1, 0), TemplateStep(74, -87, 1, 0), TemplateStep(74, -116, 0, 2),
                TemplateStep(59, -116, 1, 1), TemplateStep(30, -116, 1, 1), TemplateStep(1, -116, 1, 1),
                TemplateStep(-28, -116, 1, 1), TemplateStep(-57, -116, 1, 1), TemplateStep(-57, -145, 0, 2),
                TemplateStep(-42, -145, 1, 0), TemplateStep(-13, -145, 1, 0)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(72, 44, 0, 0), TemplateStep(43, 58, 1, 1),
                TemplateStep(28, 44, 0, 2), TemplateStep(-1, 44, 1, 1), TemplateStep(-16, 44, 0, 0),
                TemplateStep(-45, 58, 1, 1), TemplateStep(-60, 58, 0, 0), TemplateStep(-60, 87, 0, 0),
                TemplateStep(-45, 101, 1, 0), TemplateStep(-16, 87, 0, 2), TemplateStep(-1, 87, 1, 0),
                TemplateStep(28, 87, 0, 0), TemplateStep(43, 101, 1, 0), TemplateStep(72, 101, 0, 0),
                TemplateStep(72, 130, 0, 0), TemplateStep(58, 144, 1, 1), TemplateStep(29, 144, 1, 1),
                TemplateStep(0, 144, 1, 1), TemplateStep(-29, 144, 1, 1), TemplateStep(-58, 144, 1, 1),
                TemplateStep(-58, 173, 0, 0), TemplateStep(-43, 188, 1, 0), TemplateStep(-14, 188, 1, 0),
                TemplateStep(15, 188, 1, 0), TemplateStep(44, 188, 1, 0)
            )
        ),
        // Patrón 2: escalones cuadrados arriba y abajo extendida a 28 pasos
        BoardPatternTemplate(
            id = 2,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-58, -58, 0, 2), TemplateStep(-43, -58, 1, 0),
                TemplateStep(-14, -58, 0, 0), TemplateStep(1, -44, 1, 0), TemplateStep(30, -58, 0, 2),
                TemplateStep(45, -58, 1, 0), TemplateStep(74, -72, 0, 2), TemplateStep(74, -101, 0, 2),
                TemplateStep(45, -101, 1, 1), TemplateStep(30, -101, 0, 0), TemplateStep(1, -87, 1, 1),
                TemplateStep(-14, -101, 0, 2), TemplateStep(-43, -101, 1, 1), TemplateStep(-58, -115, 0, 2),
                TemplateStep(-58, -144, 0, 2), TemplateStep(-43, -144, 1, 0), TemplateStep(-14, -144, 1, 0),
                TemplateStep(15, -144, 1, 0), TemplateStep(44, -144, 1, 0), TemplateStep(73, -144, 1, 0),
                TemplateStep(73, -173, 0, 2), TemplateStep(58, -173, 1, 1), TemplateStep(29, -173, 1, 1),
                TemplateStep(0, -173, 1, 1), TemplateStep(-29, -173, 1, 1)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(58, 44, 1, 1), TemplateStep(58, 59, 0, 0),
                TemplateStep(44, 88, 1, 1), TemplateStep(29, 74, 0, 2), TemplateStep(15, 59, 1, 1),
                TemplateStep(0, 59, 0, 0), TemplateStep(-14, 88, 1, 1), TemplateStep(-29, 74, 0, 2),
                TemplateStep(-29, 45, 0, 2), TemplateStep(-58, 45, 1, 1), TemplateStep(-58, 60, 0, 0),
                TemplateStep(-58, 89, 0, 0), TemplateStep(-58, 118, 1, 0), TemplateStep(-44, 133, 0, 0),
                TemplateStep(-44, 162, 1, 0), TemplateStep(-15, 148, 0, 2), TemplateStep(-15, 133, 1, 0),
                TemplateStep(14, 133, 0, 0), TemplateStep(14, 162, 1, 0), TemplateStep(43, 148, 0, 2),
                TemplateStep(43, 119, 0, 2), TemplateStep(58, 119, 1, 0), TemplateStep(72, 134, 0, 0),
                TemplateStep(72, 163, 0, 0), TemplateStep(58, 177, 1, 1)
            )
        ),
        // Patrón 3: escalones simétricos (usado por defecto en partidas de 2 ramas), extendido a 28 pasos
        BoardPatternTemplate(
            id = 3,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-58, -44, 1, 0), TemplateStep(-29, -58, 0, 2),
                TemplateStep(-14, -58, 1, 0), TemplateStep(15, -58, 0, 0), TemplateStep(30, -44, 1, 0),
                TemplateStep(59, -44, 1, 0), TemplateStep(73, -73, 0, 2), TemplateStep(59, -88, 1, 1),
                TemplateStep(44, -102, 0, 2), TemplateStep(15, -102, 1, 1), TemplateStep(0, -102, 0, 0),
                TemplateStep(-29, -88, 1, 1), TemplateStep(-58, -88, 1, 1), TemplateStep(-58, -117, 0, 2),
                TemplateStep(-43, -117, 1, 0), TemplateStep(-14, -117, 1, 0), TemplateStep(15, -117, 1, 0),
                TemplateStep(44, -117, 1, 0), TemplateStep(73, -117, 1, 0), TemplateStep(73, -88, 0, 0),
                TemplateStep(73, -59, 0, 0), TemplateStep(58, -59, 1, 1), TemplateStep(29, -59, 1, 1),
                TemplateStep(0, -59, 1, 1), TemplateStep(-29, -59, 1, 1)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(72, 44, 0, 0), TemplateStep(43, 58, 1, 1),
                TemplateStep(14, 58, 1, 1), TemplateStep(-1, 44, 0, 2), TemplateStep(-30, 44, 1, 1),
                TemplateStep(-59, 44, 1, 1), TemplateStep(-59, 59, 0, 0), TemplateStep(-59, 88, 0, 0),
                TemplateStep(-44, 102, 1, 0), TemplateStep(-15, 102, 1, 0), TemplateStep(14, 88, 0, 2),
                TemplateStep(29, 88, 1, 0), TemplateStep(58, 88, 1, 0), TemplateStep(72, 103, 0, 0),
                TemplateStep(72, 132, 0, 0), TemplateStep(58, 146, 1, 1), TemplateStep(29, 146, 1, 1),
                TemplateStep(0, 146, 1, 1), TemplateStep(-29, 146, 1, 1), TemplateStep(-58, 146, 1, 1),
                TemplateStep(-58, 175, 0, 0), TemplateStep(-43, 190, 1, 0), TemplateStep(-14, 190, 1, 0),
                TemplateStep(15, 190, 1, 0), TemplateStep(44, 190, 1, 0)
            )
        ),
        // Patrón 4: escalones anchos extendida a 28 pasos
        BoardPatternTemplate(
            id = 4,
            leftBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(-29, 0, 1, 1), TemplateStep(-58, 0, 1, 1),
                TemplateStep(-58, -29, 0, 2), TemplateStep(-58, -58, 0, 2), TemplateStep(-43, -58, 1, 0),
                TemplateStep(-14, -58, 1, 0), TemplateStep(15, -58, 0, 0), TemplateStep(30, -44, 1, 0),
                TemplateStep(59, -44, 1, 0), TemplateStep(73, -73, 0, 2), TemplateStep(73, -102, 0, 2),
                TemplateStep(44, -102, 1, 1), TemplateStep(15, -102, 1, 1), TemplateStep(0, -102, 0, 0),
                TemplateStep(-29, -88, 1, 1), TemplateStep(-58, -88, 1, 1), TemplateStep(-58, -117, 0, 2),
                TemplateStep(-43, -117, 1, 0), TemplateStep(-14, -117, 1, 0), TemplateStep(15, -117, 1, 0),
                TemplateStep(44, -117, 1, 0), TemplateStep(73, -117, 1, 0), TemplateStep(73, -88, 0, 0),
                TemplateStep(73, -59, 0, 0), TemplateStep(58, -59, 1, 1), TemplateStep(29, -59, 1, 1),
                TemplateStep(0, -59, 1, 1), TemplateStep(-29, -59, 1, 1)
            ),
            rightBranch = listOf(
                TemplateStep(0, 0, 1, 0), TemplateStep(29, 0, 1, 0), TemplateStep(58, 0, 1, 0),
                TemplateStep(72, 15, 0, 0), TemplateStep(58, 44, 1, 1), TemplateStep(43, 44, 0, 0),
                TemplateStep(14, 58, 1, 1), TemplateStep(-1, 44, 0, 2), TemplateStep(-30, 44, 1, 1),
                TemplateStep(-59, 44, 1, 1), TemplateStep(-59, 59, 0, 0), TemplateStep(-59, 88, 1, 0),
                TemplateStep(-30, 88, 0, 0), TemplateStep(-15, 102, 1, 0), TemplateStep(14, 88, 0, 2),
                TemplateStep(29, 88, 1, 0), TemplateStep(58, 88, 1, 0), TemplateStep(72, 103, 0, 0),
                TemplateStep(72, 132, 0, 0), TemplateStep(58, 146, 1, 1), TemplateStep(29, 146, 1, 1),
                TemplateStep(0, 146, 1, 1), TemplateStep(-29, 146, 1, 1), TemplateStep(-58, 146, 1, 1),
                TemplateStep(-58, 175, 0, 0), TemplateStep(-43, 190, 1, 0), TemplateStep(-14, 190, 1, 0),
                TemplateStep(15, 190, 1, 0), TemplateStep(44, 190, 1, 0)
            )
        )
    )
}
