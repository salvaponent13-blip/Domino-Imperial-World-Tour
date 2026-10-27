package com.example.dominograndprix.i18n

import com.example.dominograndprix.i18n.DominoStrings.currentLanguage
import com.example.dominograndprix.model.GameMode

/**
 * Textos del tutorial de la primera partida y de las tarjetas que explican cada modalidad.
 *
 * Viven aparte de [DominoStrings] para no engordar mas ese archivo: aqui solo hay textos de
 * ayuda, no de interfaz principal.
 */
object TutorialStrings {

    private fun t(es: String, en: String, fr: String, it: String, pt: String, de: String): String =
        when (currentLanguage) {
            AppLanguage.ES -> es
            AppLanguage.EN -> en
            AppLanguage.FR -> fr
            AppLanguage.IT -> it
            AppLanguage.PT -> pt
            AppLanguage.DE -> de
        }

    fun skip(): String = t("Saltar", "Skip", "Passer", "Salta", "Saltar", "Überspringen")

    fun next(): String = t("Siguiente", "Next", "Suivant", "Avanti", "Seguinte", "Weiter")

    fun done(): String = t("Entendido", "Got it", "Compris", "Capito", "Entendido", "Verstanden")

    /** Título del paso [index] (0-based) del tutorial de la primera partida. */
    fun stepTitle(index: Int): String = when (index) {
        0 -> t("Estas son tus fichas", "These are your tiles", "Voici vos dominos", "Queste sono le tue tessere", "Estas são as tuas peças", "Das sind deine Steine")
        1 -> t("Los extremos abiertos", "The open ends", "Les extrémités ouvertes", "Le estremità aperte", "As extremidades abertas", "Die offenen Enden")
        else -> t("Si no puedes, roba o pasa", "If you can't, draw or pass", "Sinon, pioche ou passe", "Altrimenti pesca o passa", "Se não puderes, compra ou passa", "Sonst ziehen oder passen")
    }

    /** Cuerpo del paso [index] (0-based). */
    fun stepBody(index: Int): String = when (index) {
        0 -> t(
            "Las fichas con borde brillante encajan ahora mismo. Toca una para jugarla; si conecta con un solo extremo, entra directa.",
            "Tiles with a glowing border fit right now. Tap one to play it; if it fits only one end, it goes straight in.",
            "Les dominos à bord lumineux passent maintenant. Touchez-en un pour jouer ; s'il ne va que d'un côté, il se pose directement.",
            "Le tessere con bordo luminoso entrano subito. Tocca una per giocarla; se combacia con un solo lato, va dentro direttamente.",
            "As peças com bordo brilhante encaixam agora. Toca numa para jogar; se só encaixa num lado, entra direto.",
            "Steine mit leuchtendem Rand passen gerade. Tippe einen an, um ihn zu spielen; passt er nur an ein Ende, geht er direkt hinein."
        )
        1 -> t(
            "Esos dos números de fuera, junto al reloj de la esquina, son los extremos: solo puedes jugar fichas que acaben en uno de ellos.",
            "Those two numbers at the edge, next to the corner badge, are the ends: you can only play tiles that match one of them.",
            "Ces deux nombres sur le bord, à côté du badge d'angle, sont les extrémités : vous ne pouvez jouer que des dominos qui en portent un.",
            "Quei due numeri sul bordo, accanto al badge d'angolo, sono le estremità: puoi giocare solo tessere che ne riportano uno.",
            "Esses dois números na borda, junto ao selo do canto, são as extremidades: só podes jogar peças que tenham um deles.",
            "Diese zwei Zahlen am Rand neben der Ecke sind die Enden: du kannst nur Steine spielen, die eine davon tragen."
        )
        else -> t(
            "Si ninguna encaja, roba del pozo o pasa el turno. Ganas la ronda quedándote sin fichas, o con menos puntos si la mesa se tranca.",
            "If none fits, draw from the boneyard or pass. You win the round by running out of tiles, or by holding fewer pips if the board blocks.",
            "Si aucun ne va, piochez ou passez. Vous gagnez la manche en n'ayant plus de dominos, ou avec moins de points si le jeu se bloque.",
            "Se nessuna entra, pesca dal pozzo o passa. Vinci la mano restando senza tessere, o con meno punti se il gioco si blocca.",
            "Se nenhuma encaixa, compra do poço ou passa. Ganhas a ronda ficando sem peças, ou com menos pontos se o jogo fechar.",
            "Passt keiner, ziehe aus dem Vorrat oder passe. Du gewinnst die Runde ohne Steine oder mit weniger Punkten, wenn das Spiel blockiert."
        )
    }

    fun modeCardTitle(): String = t(
        "Cómo se juega este modo", "How this mode works", "Comment se joue ce mode",
        "Come si gioca questa modalità", "Como se joga este modo", "So wird dieser Modus gespielt"
    )

    /** Una linea con la regla esencial de cada modalidad. */
    fun modeHint(mode: GameMode): String = when (mode) {
        GameMode.DRAW -> t(
            "Roba del pozo cuando no puedas jugar. Gana quien se queda sin fichas.",
            "Draw from the boneyard when you can't play. First to run out of tiles wins.",
            "Piochez quand vous ne pouvez pas jouer. Le premier sans dominos gagne.",
            "Pesca quando non puoi giocare. Vince chi resta senza tessere.",
            "Compra do poço quando não puderes jogar. Ganha quem ficar sem peças.",
            "Ziehe, wenn du nicht spielen kannst. Wer zuerst keine Steine hat, gewinnt."
        )
        GameMode.BLOCK -> t(
            "Sin pozo: si no encaja, pasas. Si la mesa se tranca, gana quien tenga menos puntos en la mano.",
            "No boneyard: if it doesn't fit, you pass. If the board blocks, fewest pips in hand wins.",
            "Sans pioche : sinon, passez. Si le jeu se bloque, le moins de points en main gagne.",
            "Senza pozzo: se non entra, passi. Se il gioco si blocca, vince chi ha meno punti in mano.",
            "Sem poço: se não encaixa, passas. Se fechar, ganha quem tiver menos pontos na mão.",
            "Kein Vorrat: passt es nicht, passt du. Blockiert das Spiel, gewinnt die kleinere Hand."
        )
        GameMode.ALL_FIVES -> t(
            "Cada jugada puntúa si los extremos abiertos suman múltiplo de 5. Los dobles abiertos cuentan doble.",
            "Every move scores when the open ends add up to a multiple of 5. Open doubles count twice.",
            "Chaque coup marque si les extrémités font un multiple de 5. Les doubles ouverts comptent double.",
            "Ogni mossa segna se le estremità fanno un multiplo di 5. I doppi aperti contano doppio.",
            "Cada jogada pontua se as extremidades somarem múltiplo de 5. Os dobres abertos contam a dobrar.",
            "Jeder Zug zählt, wenn die offenen Enden ein Vielfaches von 5 ergeben. Offene Doppel zählen doppelt."
        )
        GameMode.ALL_THREES -> t(
            "Puntúa cuando los extremos abiertos suman múltiplo de 3. Ritmo rápido y muchas jugadas cortas.",
            "Scores when the open ends add up to a multiple of 3. Fast pace, lots of short moves.",
            "Marque quand les extrémités font un multiple de 3. Rythme rapide et coups courts.",
            "Segna quando le estremità fanno un multiplo di 3. Ritmo veloce e mosse corte.",
            "Pontua quando as extremidades somarem múltiplo de 3. Ritmo rápido e jogadas curtas.",
            "Zählt, wenn die offenen Enden ein Vielfaches von 3 ergeben. Schnelles Tempo, kurze Züge."
        )
        GameMode.FIVES_AND_THREES -> t(
            "Suma las dos reglas: puntúan los múltiplos de 5 y de 3, y el 15 paga doble.",
            "Both rules at once: multiples of 5 and of 3 score, and 15 pays double.",
            "Les deux règles : les multiples de 5 et de 3 marquent, et 15 paie double.",
            "Entrambe le regole: segnano i multipli di 5 e di 3, e il 15 paga doppio.",
            "As duas regras: pontuam múltiplos de 5 e de 3, e o 15 paga a dobrar.",
            "Beide Regeln: Vielfache von 5 und 3 zählen, die 15 zahlt doppelt."
        )
        GameMode.MATADOR -> t(
            "Conecta fichas que sumen 7 con el extremo, o usa los cuatro comodines Matadores (0-0, 6-1, 5-2, 4-3).",
            "Connect tiles that add up to 7 with the end, or use the four Matador wildcards (0-0, 6-1, 5-2, 4-3).",
            "Reliez les dominos qui font 7 avec l'extrémité, ou utilisez les quatre jokers Matador (0-0, 6-1, 5-2, 4-3).",
            "Collega tessere che sommano 7 con l'estremità, o usa i quattro jolly Matador (0-0, 6-1, 5-2, 4-3).",
            "Liga peças que somem 7 à extremidade, ou usa os quatro coringas Matador (0-0, 6-1, 5-2, 4-3).",
            "Verbinde Steine, die mit dem Ende 7 ergeben, oder nutze die vier Matador-Joker (0-0, 6-1, 5-2, 4-3)."
        )
        GameMode.DRAW_SPECIAL -> t(
            "Cada jugador tiene su propio extremo asignado y suma puntos cuando el rival se bloquea en el suyo.",
            "Each player owns one end and scores when the opponent gets stuck on theirs.",
            "Chaque joueur possède une extrémité et marque quand l'adversaire y est bloqué.",
            "Ogni giocatore ha un'estremità e segna quando l'avversario si blocca sulla sua.",
            "Cada jogador tem a sua extremidade e pontua quando o rival fica bloqueado na dele.",
            "Jeder Spieler besitzt ein Ende und punktet, wenn der Gegner dort festhängt."
        )
        GameMode.BLOCK_HUNGARIAN -> t(
            "Encadena todas las fichas consecutivas que puedas en un mismo turno. Cuanto más larga la cadena, mejor.",
            "Chain all the consecutive tiles you can in a single turn. The longer the chain, the better.",
            "Enchaînez tous les dominos consécutifs possibles en un tour. Plus la chaîne est longue, mieux c'est.",
            "Incatena più tessere consecutive possibile in un turno. Più lunga la catena, meglio è.",
            "Encadeia todas as peças consecutivas que puderes num turno. Quanto maior a cadeia, melhor.",
            "Verkette in einem Zug so viele aufeinanderfolgende Steine wie möglich. Je länger, desto besser."
        )
    }

    /** Texto para volver a ver el tutorial desde Ajustes. */
    fun replayHint(): String = t(
        "El tutorial volverá a salir al empezar tu próxima partida.",
        "The tutorial will show again when you start your next match.",
        "Le tutoriel réapparaîtra au début de votre prochaine partie.",
        "Il tutorial ricomparirà all'inizio della prossima partita.",
        "O tutorial volta a aparecer no início da tua próxima partida.",
        "Das Tutorial erscheint wieder beim Start deiner nächsten Partie."
    )
}
