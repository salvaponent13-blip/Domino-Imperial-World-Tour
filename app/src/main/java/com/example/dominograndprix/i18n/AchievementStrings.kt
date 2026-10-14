package com.example.dominograndprix.i18n

import com.example.dominograndprix.data.AchievementCatalog
import com.example.dominograndprix.data.VenueMission
import com.example.dominograndprix.data.VenueMissionCatalog

/**
 * Textos de la vitrina de logros y de las misiones de sede.
 *
 * Vive aparte de [DominoStrings] porque son muchas cadenas muy repetitivas (17 logros y 14
 * misiones por 6 idiomas) y mezclarlas con el resto hacia el fichero principal dificil de
 * recorrer.
 *
 * Los identificadores vienen del catalogo y NUNCA se traducen: son lo que se guarda en disco.
 * Si aqui falta un identificador, la interfaz usa uno generico en lugar de reventar, y el test
 * `AchievementLocalizationTest` comprueba que no falte ninguno.
 */
object AchievementStrings {

    private val titles: Map<String, List<String>> = mapOf(
        // AppLanguage: ES, EN, FR, IT, PT, DE (mismo orden que LanguageOrder)
        AchievementCatalog.FIRST_BLOOD to listOf(
            "Primera sangre", "First blood", "Premier sang", "Primo sangue", "Primeiro sangue", "Erstes Blut"
        ),
        AchievementCatalog.DOMINO_MASTER to listOf(
            "Maestro del dominó", "Domino master", "Maître du domino", "Maestro del domino",
            "Mestre do dominó", "Domino-Meister"
        ),
        AchievementCatalog.TRANCA_KING to listOf(
            "Rey de la tranca", "Block king", "Roi du blocage", "Re della chiusa",
            "Rei da tranca", "König der Sperre"
        ),
        AchievementCatalog.HAT_TRICK to listOf(
            "Triplete", "Hat trick", "Coup du chapeau", "Tripletta", "Hat-trick", "Hattrick"
        ),
        AchievementCatalog.VETERAN to listOf(
            "Veterano", "Veteran", "Vétéran", "Veterano", "Veterano", "Veteran"
        ),
        AchievementCatalog.SHARPSHOOTER to listOf(
            "Francotirador", "Sharpshooter", "Tireur d'élite", "Tiratore scelto",
            "Atirador de elite", "Scharfschütze"
        ),
        AchievementCatalog.LAST_TILE to listOf(
            "Al filo", "Down to the wire", "Jusqu'au bout", "All'ultima tessera",
            "No limite", "Auf Messers Schneide"
        ),
        AchievementCatalog.TOUGH_CROWN to listOf(
            "Corona difícil", "Hard-won crown", "Couronne difficile", "Corona difficile",
            "Coroa difícil", "Harte Krone"
        ),
        AchievementCatalog.VEGAS_CHAMPION to listOf(
            "Campeón de Las Vegas", "Las Vegas champion", "Champion de Las Vegas",
            "Campione di Las Vegas", "Campeão de Las Vegas", "Champion von Las Vegas"
        ),
        AchievementCatalog.HAVANA_DOMINO to listOf(
            "Dominó habanero", "Havana domino", "Domino de La Havane", "Domino all'Avana",
            "Dominó havaneiro", "Domino in Havanna"
        ),
        AchievementCatalog.TOKYO_TRANCA to listOf(
            "Tranca de Tokio", "Tokyo block", "Blocage de Tokyo", "Chiusa di Tokyo",
            "Tranca de Tóquio", "Sperre von Tokio"
        ),
        AchievementCatalog.DAILY_FIRST to listOf(
            "Primer reto", "First challenge", "Premier défi", "Prima sfida",
            "Primeiro desafio", "Erste Herausforderung"
        ),
        AchievementCatalog.DAILY_STREAK_7 to listOf(
            "Semana perfecta", "Perfect week", "Semaine parfaite", "Settimana perfetta",
            "Semana perfeita", "Perfekte Woche"
        ),
        AchievementCatalog.PERFECT_TOUR to listOf(
            "Vuelta perfecta", "Perfect tour", "Tournée parfaite", "Tour perfetto",
            "Volta perfeita", "Perfekte Tour"
        ),
        AchievementCatalog.HIGH_ROLLER to listOf(
            "Grande en la mesa", "High roller", "Gros joueur", "Alto puntatore",
            "Grande apostador", "High Roller"
        )
    )

    private val descriptions: Map<String, List<String>> = mapOf(
        AchievementCatalog.FIRST_BLOOD to listOf(
            "Gana tu primera partida.", "Win your first match.", "Gagnez votre première partie.",
            "Vinci la tua prima partita.", "Ganhe a sua primeira partida.", "Gewinne dein erstes Spiel."
        ),
        AchievementCatalog.DOMINO_MASTER to listOf(
            "Cierra 10 rondas con dominó.", "Close 10 rounds with a domino.", "Closez 10 manches avec un domino.",
            "Chiudi 10 mani con un domino.", "Feche 10 rodadas com dominó.", "Schließe 10 Runden mit einem Domino."
        ),
        AchievementCatalog.TRANCA_KING to listOf(
            "Gana 10 rondas por tranca.", "Win 10 rounds by blocking.", "Gagnez 10 manches par blocage.",
            "Vinci 10 mani per chiusa.", "Ganhe 10 rodadas por tranca.", "Gewinne 10 Runden durch Sperre."
        ),
        AchievementCatalog.HAT_TRICK to listOf(
            "Gana 3 rondas en una misma partida.", "Win 3 rounds in a single match.",
            "Gagnez 3 manches dans une même partie.", "Vinci 3 mani in una sola partita.",
            "Ganhe 3 rodadas numa mesma partida.", "Gewinne 3 Runden in einem Spiel."
        ),
        AchievementCatalog.VETERAN to listOf(
            "Juega 50 partidas.", "Play 50 matches.", "Jouez 50 parties.",
            "Gioca 50 partite.", "Jogue 50 partidas.", "Spiele 50 Partien."
        ),
        AchievementCatalog.SHARPSHOOTER to listOf(
            "Anota 500 puntos de extremos.", "Score 500 points from the open ends.",
            "Marquez 500 points aux extrémités.", "Segna 500 punti dalle estremità.",
            "Marque 500 pontos nas pontas.", "Erziele 500 Punkte an den Enden."
        ),
        AchievementCatalog.LAST_TILE to listOf(
            "Gana 5 rondas con una sola ficha en la mano.", "Win 5 rounds with a single tile left.",
            "Gagnez 5 manches avec une seule tuile en main.", "Vinci 5 mani con una sola tessera in mano.",
            "Ganhe 5 rodadas com uma só peça na mão.", "Gewinne 5 Runden mit nur einem Stein."
        ),
        AchievementCatalog.TOUGH_CROWN to listOf(
            "Gana 5 partidas en dificultad difícil.", "Win 5 matches on hard difficulty.",
            "Gagnez 5 parties en difficulté difficile.", "Vinci 5 partite a difficoltà difficile.",
            "Ganhe 5 partidas na dificuldade difícil.", "Gewinne 5 Partien auf schwer."
        ),
        AchievementCatalog.VEGAS_CHAMPION to listOf(
            "Gana en Las Vegas.", "Win in Las Vegas.", "Gagnez à Las Vegas.",
            "Vinci a Las Vegas.", "Ganhe em Las Vegas.", "Gewinne in Las Vegas."
        ),
        AchievementCatalog.HAVANA_DOMINO to listOf(
            "Cierra un dominó en La Habana.", "Close a domino in Havana.", "Closez un domino à La Havane.",
            "Chiudi un domino all'Avana.", "Feche um dominó em Havana.", "Schließe ein Domino in Havanna."
        ),
        AchievementCatalog.TOKYO_TRANCA to listOf(
            "Gana una tranca en Tokio.", "Win a blocked round in Tokyo.", "Gagnez un blocage à Tokyo.",
            "Vinci una chiusa a Tokyo.", "Ganhe uma tranca em Tóquio.", "Gewinne eine Sperre in Tokio."
        ),
        AchievementCatalog.DAILY_FIRST to listOf(
            "Supera tu primer desafío diario.", "Beat your first daily challenge.",
            "Réussissez votre premier défi quotidien.", "Supera la tua prima sfida giornaliera.",
            "Supere o seu primeiro desafio diário.", "Meistere deine erste Tagesherausforderung."
        ),
        AchievementCatalog.DAILY_STREAK_7 to listOf(
            "Gana el desafío diario 7 días seguidos.", "Win the daily challenge 7 days in a row.",
            "Gagnez le défi quotidien 7 jours de suite.", "Vinci la sfida giornaliera per 7 giorni di fila.",
            "Ganhe o desafio diário 7 dias seguidos.", "Gewinne die Tagesherausforderung 7 Tage in Folge."
        ),
        AchievementCatalog.PERFECT_TOUR to listOf(
            "Completa las 7 sedes del World Tour.", "Complete all 7 venues of the World Tour.",
            "Terminez les 7 villes du World Tour.", "Completa le 7 sedi del World Tour.",
            "Complete as 7 cidades do World Tour.", "Schließe alle 7 Städte der World Tour ab."
        ),
        AchievementCatalog.HIGH_ROLLER to listOf(
            "Acumula 20.000 en caja.", "Bank 20,000 in cash.", "Accumulez 20 000 en caisse.",
            "Accumula 20.000 in cassa.", "Acumule 20.000 em caixa.", "Sammle 20.000 in der Kasse."
        )
    )

    private val missionTitles: Map<String, List<String>> = mapOf(
        "cuba_domino" to listOf("Dominó habanero", "Havana domino", "Domino de La Havane", "Domino all'Avana", "Dominó havaneiro", "Domino in Havanna"),
        "cuba_good_round" to listOf("Ronda con fundamento", "Solid round", "Manche solide", "Mano solida", "Rodada sólida", "Solide Runde"),
        "mexico_big_round" to listOf("Ronda de 30", "30-point round", "Manche de 30", "Mano da 30", "Rodada de 30", "30-Punkte-Runde"),
        "mexico_two_dominos" to listOf("Dos dominós", "Two dominos", "Deux dominos", "Due domino", "Dois dominós", "Zwei Dominos"),
        "pr_tranca" to listOf("Cierra la mesa", "Shut the table", "Fermez la table", "Chiudi il tavolo", "Feche a mesa", "Tisch dicht"),
        "pr_long_match" to listOf("Aguantar tres", "Hold for three", "Tenir trois manches", "Resisti tre mani", "Aguentar três", "Drei Runden halten"),
        "no_close_call" to listOf("Al filo", "Down to the wire", "Jusqu'au bout", "All'ultima tessera", "No limite", "Auf Messers Schneide"),
        "no_tranca" to listOf("Victoria cerrada", "Locked win", "Victoire verrouillée", "Vittoria chiusa", "Vitória fechada", "Eingesperrt"),
        "tokyo_flawless" to listOf("Tres seguidas", "Three in a row", "Trois d'affilée", "Tre di fila", "Três seguidas", "Drei in Folge"),
        "tokyo_master" to listOf("Dominio doble", "Double domino", "Double domino", "Doppio domino", "Dominó duplo", "Doppel-Domino"),
        "mc_big_round" to listOf("Ronda de 40", "40-point round", "Manche de 40", "Mano da 40", "Rodada de 40", "40-Punkte-Runde"),
        "mc_domino" to listOf("Dos dominós", "Two dominos", "Deux dominos", "Due domino", "Dois dominós", "Zwei Dominos"),
        "vegas_shutout" to listOf("Dominio total", "Total dominance", "Domination totale", "Dominio totale", "Domínio total", "Totale Dominanz"),
        "vegas_double_domino" to listOf("Doble dominó", "Double domino", "Double domino", "Doppio domino", "Dominó duplo", "Doppel-Domino")
    )

    private val missionDescriptions: Map<String, List<String>> = mapOf(
        "cuba_domino" to listOf("Gana cerrando la ronda con un dominó.", "Win by closing the round with a domino.", "Gagnez en fermant la manche avec un domino.", "Vinci chiudendo la mano con un domino.", "Ganhe fechando a rodada com um dominó.", "Gewinne, indem du die Runde mit einem Domino schließt."),
        "cuba_good_round" to listOf("Anota 25 puntos en una sola ronda.", "Score 25 points in a single round.", "Marquez 25 points en une seule manche.", "Segna 25 punti in una sola mano.", "Marque 25 pontos numa só rodada.", "Erziele 25 Punkte in einer Runde."),
        "mexico_big_round" to listOf("Anota 30 puntos en una sola ronda.", "Score 30 points in a single round.", "Marquez 30 points en une seule manche.", "Segna 30 punti in una sola mano.", "Marque 30 pontos numa só rodada.", "Erziele 30 Punkte in einer Runde."),
        "mexico_two_dominos" to listOf("Cierra dos rondas con dominó.", "Close two rounds with a domino.", "Closez deux manches avec un domino.", "Chiudi due mani con un domino.", "Feche duas rodadas com dominó.", "Schließe zwei Runden mit einem Domino."),
        "pr_tranca" to listOf("Gana una ronda porque la mesa se tranca.", "Win a round because the table is blocked.", "Gagnez une manche par blocage.", "Vinci una mano per chiusa.", "Ganhe uma rodada por tranca.", "Gewinne eine Runde durch Sperre."),
        "pr_long_match" to listOf("Gana al menos tres rondas en la partida.", "Win at least three rounds in the match.", "Gagnez au moins trois manches.", "Vinci almeno tre mani.", "Ganhe pelo menos três rodadas.", "Gewinne mindestens drei Runden."),
        "no_close_call" to listOf("Gana con una sola ficha en la mano.", "Win with a single tile left in hand.", "Gagnez avec une seule tuile en main.", "Vinci con una sola tessera in mano.", "Ganhe com uma só peça na mão.", "Gewinne mit nur einem Stein."),
        "no_tranca" to listOf("Gana una ronda por tranca.", "Win a round by blocking.", "Gagnez une manche par blocage.", "Vinci una mano per chiusa.", "Ganhe uma rodada por tranca.", "Gewinne eine Runde durch Sperre."),
        "tokyo_flawless" to listOf("Gana tres rondas seguidas en la partida.", "Win three rounds in the match.", "Gagnez trois manches.", "Vinci tre mani.", "Ganhe três rodadas.", "Gewinne drei Runden."),
        "tokyo_master" to listOf("Cierra dos rondas con dominó.", "Close two rounds with a domino.", "Closez deux manches avec un domino.", "Chiudi due mani con un domino.", "Feche duas rodadas com dominó.", "Schließe zwei Runden mit einem Domino."),
        "mc_big_round" to listOf("Anota 40 puntos en una sola ronda.", "Score 40 points in a single round.", "Marquez 40 points en une seule manche.", "Segna 40 punti in una sola mano.", "Marque 40 pontos numa só rodada.", "Erziele 40 Punkte in einer Runde."),
        "mc_domino" to listOf("Cierra dos rondas con dominó.", "Close two rounds with a domino.", "Closez deux manches avec un domino.", "Chiudi due mani con un domino.", "Feche duas rodadas com dominó.", "Schließe zwei Runden mit einem Domino."),
        "vegas_shutout" to listOf("Suma tres rondas ganadas entre dominós y trancas.", "Win three rounds between dominos and blocks.", "Gagnez trois manches entre dominos et blocages.", "Vinci tre mani tra domino e chiuse.", "Ganhe três rodadas entre dominós e trancas.", "Gewinne drei Runden aus Dominos und Sperren."),
        "vegas_double_domino" to listOf("Cierra dos rondas con dominó.", "Close two rounds with a domino.", "Closez deux manches avec un domino.", "Chiudi due mani con un domino.", "Feche duas rodadas com dominó.", "Schließe zwei Runden mit einem Domino.")
    )

    /** Orden de idiomas, igual que en [DominoStrings]. */
    private fun indexOf(language: AppLanguage): Int = when (language) {
        AppLanguage.ES -> 0
        AppLanguage.EN -> 1
        AppLanguage.FR -> 2
        AppLanguage.IT -> 3
        AppLanguage.PT -> 4
        AppLanguage.DE -> 5
    }

    /** Titulo de un logro. Si el identificador es desconocido, devuelve el propio identificador. */
    fun titleFor(achievementId: String, language: AppLanguage): String =
        titles[achievementId]?.getOrNull(indexOf(language)) ?: achievementId

    /** Descripcion (como conseguirlo) de un logro. */
    fun descriptionFor(achievementId: String, language: AppLanguage): String =
        descriptions[achievementId]?.getOrNull(indexOf(language)) ?: ""

    /** Titulo de una mision de sede. */
    fun missionTitleFor(mission: VenueMission, language: AppLanguage): String =
        missionTitles[mission.id]?.getOrNull(indexOf(language)) ?: mission.id

    /** Descripcion de una mision de sede. */
    fun missionDescriptionFor(mission: VenueMission, language: AppLanguage): String =
        missionDescriptions[mission.id]?.getOrNull(indexOf(language)) ?: ""

    /** Identificadores de logro que tienen texto en la vitrina. */
    fun localizedAchievementIds(): Set<String> = titles.keys

    /** Identificadores de mision que tienen texto en la vitrina. */
    fun localizedMissionIds(): Set<String> = missionTitles.keys

    /** Todas las misiones del catalogo, para la interfaz. */
    fun allMissions(): List<VenueMission> = VenueMissionCatalog.allMissions()
}
