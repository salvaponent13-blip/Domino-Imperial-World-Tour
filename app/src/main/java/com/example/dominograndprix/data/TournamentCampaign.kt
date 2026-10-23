package com.example.dominograndprix.data

import com.example.dominograndprix.R
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.LuxuryItem

/**
 * Reto individual dentro de una localidad del Torneo Mundial.
 */
data class VenueChallenge(
    val id: String,
    val challengeNumber: Int, // 1 a 5
    val venueId: String,
    val venueName: String,
    val venueRegion: String,
    val bgImageResId: Int,
    val musicResId: Int,
    val title: String,
    val gameMode: GameMode,
    val targetScore: Int,
    val playerCount: Int, // 2 o 4
    val isTeamGame: Boolean, // false = individual, true = parejas
    val rivalName: String,
    val description: String,
    /**
     * Nivel de la IA del rival. Hasta la v2.3.1 la carrera jugaba TODOS los retos en STANDARD,
     * asi que las 7 sedes se sentian iguales; ahora escala con la sede (ver [difficultyForVenue]).
     */
    val difficulty: Difficulty = Difficulty.STANDARD,
    /** Recuerdo que se gana al completar el reto. No se vende: es una coleccion. */
    val trophyTitle: String? = null
)

/** Dificultad por sede: el torneo empieza suave y acaba en el nivel mas duro. */
fun difficultyForVenue(venueIndex: Int): Difficulty = when (venueIndex.coerceIn(0, 6)) {
    0, 1 -> Difficulty.RELAXED      // Ciudad de Mexico, La Habana
    2, 3 -> Difficulty.STANDARD     // San Juan, Nueva Orleans
    4 -> Difficulty.STANDARD        // Miami (2v2)
    5 -> Difficulty.TOUGH           // Las Vegas
    else -> Difficulty.TOUGH        // Tokio
}

/**
 * Catálogo completo de la campaña del Torneo Mundial:
 * 7 Localidades x 5 Retos cada una = 35 Retos progresivos y variados.
 */
object TournamentCampaign {

    val VENUE_IDS = listOf(
        "mexico",
        "cuba",
        "puerto_rico",
        "new_orleans",
        "miami",
        "las_vegas",
        "tokyo"
    )

    fun getChallengesForVenue(venueIndex: Int): List<VenueChallenge> {
        return when (venueIndex.coerceIn(0, 6)) {
            0 -> listOf(
                VenueChallenge(
                    id = "mexico_1",
                    challengeNumber = 1,
                    venueId = "mexico",
                    venueName = "Ciudad de México",
                    venueRegion = "México • Terraza del Zócalo",
                    bgImageResId = R.drawable.venue_mexico,
                    musicResId = R.raw.mus_region_mexico,
                    title = "Dominó a Tranca",
                    gameMode = GameMode.BLOCK,
                    targetScore = 20,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Carlos 'El Rayo'",
                    description = "Sin pozo. Cada jugador recibe 7 fichas. Si te bloqueas, pasas. Gana quien llegue primero a 20 puntos."
                ),
                VenueChallenge(
                    id = "mexico_2",
                    challengeNumber = 2,
                    venueId = "mexico",
                    venueName = "Ciudad de México",
                    venueRegion = "México • Terraza del Zócalo",
                    bgImageResId = R.drawable.venue_mexico,
                    musicResId = R.raw.mus_region_mexico,
                    title = "Dominó Robando",
                    gameMode = GameMode.DRAW,
                    targetScore = 20,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Mateo Silva",
                    description = "Partida tradicional con pozo. Si no puedes jugar, roba hasta conseguir ficha o agotar la reserva."
                ),
                VenueChallenge(
                    id = "mexico_3",
                    challengeNumber = 3,
                    venueId = "mexico",
                    venueName = "Ciudad de México",
                    venueRegion = "México • Terraza del Zócalo",
                    bgImageResId = R.drawable.venue_mexico,
                    musicResId = R.raw.mus_region_mexico,
                    title = "Dominó de Tres",
                    gameMode = GameMode.ALL_THREES,
                    targetScore = 50,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Ignacio Vega",
                    description = "Suma puntos en cada turno si los extremos abiertos suman un múltiplo de 3. Los dobles en extremos cuentan doble."
                ),
                VenueChallenge(
                    id = "mexico_4",
                    challengeNumber = 4,
                    venueId = "mexico",
                    venueName = "Ciudad de México",
                    venueRegion = "México • Terraza del Zócalo",
                    bgImageResId = R.drawable.venue_mexico,
                    musicResId = R.raw.mus_region_mexico,
                    title = "Duelo Mano a Mano (1v1)",
                    gameMode = GameMode.DRAW,
                    targetScore = 20,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Valentina Ortiz",
                    description = "Mano a mano directo contra un único oponente. 7 fichas cada uno y 14 en el pozo. Velocidad y reflejos."
                ),
                VenueChallenge(
                    id = "mexico_5",
                    challengeNumber = 5,
                    venueId = "mexico",
                    venueName = "Ciudad de México",
                    venueRegion = "México • Terraza del Zócalo",
                    bgImageResId = R.drawable.venue_mexico,
                    musicResId = R.raw.mus_region_mexico,
                    title = "GRAN FINAL: Dominó de Cinco",
                    gameMode = GameMode.ALL_FIVES,
                    targetScore = 50,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Carlos 'El Rayo'",
                    description = "La especialidad azteca: suma múltiplos de 5 en cada jugada con spinner en cruz. ¡Supera a Carlos para viajar a La Habana!",
                    trophyTitle = "Medalla Azteca de Plata",
                )
            )
            1 -> listOf(
                VenueChallenge(
                    id = "cuba_1",
                    challengeNumber = 1,
                    venueId = "cuba",
                    venueName = "La Habana",
                    venueRegion = "Cuba • Club Social del Malecón",
                    bgImageResId = R.drawable.venue_havana,
                    musicResId = R.raw.mus_region_havana,
                    title = "Dominó de Cinco",
                    gameMode = GameMode.ALL_FIVES,
                    targetScore = 100,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Compay Cheo",
                    description = "Partida a 100 puntos en 4 direcciones. Apunta a combinaciones de 10, 15 y 20 puntos con los dobles abiertos."
                ),
                VenueChallenge(
                    id = "cuba_2",
                    challengeNumber = 2,
                    venueId = "cuba",
                    venueName = "La Habana",
                    venueRegion = "Cuba • Club Social del Malecón",
                    bgImageResId = R.drawable.venue_havana,
                    musicResId = R.raw.mus_region_havana,
                    title = "Dominó de Tres (1v1)",
                    gameMode = GameMode.ALL_THREES,
                    targetScore = 100,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Yordanis 'El Fino'",
                    description = "Duelo cerrado a múltiplos de 3. Cada turno cuenta para alcanzar los 100 puntos antes que el rival."
                ),
                VenueChallenge(
                    id = "cuba_3",
                    challengeNumber = 3,
                    venueId = "cuba",
                    venueName = "La Habana",
                    venueRegion = "Cuba • Club Social del Malecón",
                    bgImageResId = R.drawable.venue_havana,
                    musicResId = R.raw.mus_region_havana,
                    title = "Húngaro a Tranca",
                    gameMode = GameMode.BLOCK_HUNGARIAN,
                    targetScore = 60,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Maykel Barreto",
                    description = "Encadena múltiples fichas seguidas en el mismo turno si puedes colocarlas por los extremos. ¡Vacía tu mano de un tirón!"
                ),
                VenueChallenge(
                    id = "cuba_4",
                    challengeNumber = 4,
                    venueId = "cuba",
                    venueName = "La Habana",
                    venueRegion = "Cuba • Club Social del Malecón",
                    bgImageResId = R.drawable.venue_havana,
                    musicResId = R.raw.mus_region_havana,
                    title = "Robando 4 Ramas (1v1)",
                    gameMode = GameMode.DRAW,
                    targetScore = 30,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Lázaro Fonseca",
                    description = "Duelo a 30 puntos con tablero abierto. Estrategia rápida de cierre y bloqueo."
                ),
                VenueChallenge(
                    id = "cuba_5",
                    challengeNumber = 5,
                    venueId = "cuba",
                    venueName = "La Habana",
                    venueRegion = "Cuba • Club Social del Malecón",
                    bgImageResId = R.drawable.venue_havana,
                    musicResId = R.raw.mus_region_havana,
                    title = "GRAN FINAL: Tranca Cubana",
                    gameMode = GameMode.BLOCK,
                    targetScore = 30,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Compay Cheo",
                    description = "Tranca clásica en La Habana. Gana el torneo caribeño para desbloquear San Juan de Puerto Rico.",
                    trophyTitle = "Estuche Imperial de Habanos",
                )
            )
            2 -> listOf(
                VenueChallenge(
                    id = "pr_1",
                    challengeNumber = 1,
                    venueId = "puerto_rico",
                    venueName = "San Juan",
                    venueRegion = "Puerto Rico • Balcón de El Morro",
                    bgImageResId = R.drawable.venue_sanjuan,
                    musicResId = R.raw.mus_region_sanjuan,
                    title = "Robando Especial",
                    gameMode = GameMode.DRAW_SPECIAL,
                    targetScore = 80,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Rafael 'El Muro'",
                    description = "Juega en tu propia rama. Si un jugador no puede tirar y pasa, su rama queda abierta para invasión."
                ),
                VenueChallenge(
                    id = "pr_2",
                    challengeNumber = 2,
                    venueId = "puerto_rico",
                    venueName = "San Juan",
                    venueRegion = "Puerto Rico • Balcón de El Morro",
                    bgImageResId = R.drawable.venue_sanjuan,
                    musicResId = R.raw.mus_region_sanjuan,
                    title = "Dominó de Cinco (1v1)",
                    gameMode = GameMode.ALL_FIVES,
                    targetScore = 150,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Héctor Cotto",
                    description = "Duelo a 150 puntos en All Fives. Máxima concentración en múltiplos de 5."
                ),
                VenueChallenge(
                    id = "pr_3",
                    challengeNumber = 3,
                    venueId = "puerto_rico",
                    venueName = "San Juan",
                    venueRegion = "Puerto Rico • Balcón de El Morro",
                    bgImageResId = R.drawable.venue_sanjuan,
                    musicResId = R.raw.mus_region_sanjuan,
                    title = "Cinco y Tres",
                    gameMode = GameMode.FIVES_AND_THREES,
                    targetScore = 100,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Tito 'El Bravo'",
                    description = "Puntúa tanto si la suma es múltiplo de 5 como de 3. Si es múltiplo de 15, ¡recibes doble bonificación!"
                ),
                VenueChallenge(
                    id = "pr_4",
                    challengeNumber = 4,
                    venueId = "puerto_rico",
                    venueName = "San Juan",
                    venueRegion = "Puerto Rico • Balcón de El Morro",
                    bgImageResId = R.drawable.venue_sanjuan,
                    musicResId = R.raw.mus_region_sanjuan,
                    title = "Dominó de Tres",
                    gameMode = GameMode.ALL_THREES,
                    targetScore = 150,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Ángel Morales",
                    description = "Partida individual a 150 puntos en múltiplos de 3."
                ),
                VenueChallenge(
                    id = "pr_5",
                    challengeNumber = 5,
                    venueId = "puerto_rico",
                    venueName = "San Juan",
                    venueRegion = "Puerto Rico • Balcón de El Morro",
                    bgImageResId = R.drawable.venue_sanjuan,
                    musicResId = R.raw.mus_region_sanjuan,
                    title = "GRAN FINAL: Húngaro Boricua",
                    gameMode = GameMode.BLOCK_HUNGARIAN,
                    targetScore = 80,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Rafael 'El Muro'",
                    description = "Duelo decisivo en el Fuerte de San Juan para viajar a Nueva Orleans.",
                    trophyTitle = "Anillo Real del Caribe",
                )
            )
            3 -> listOf(
                VenueChallenge(
                    id = "no_1",
                    challengeNumber = 1,
                    venueId = "new_orleans",
                    venueName = "Nueva Orleans",
                    venueRegion = "EE. UU. • French Quarter Jazz Club",
                    bgImageResId = R.drawable.venue_new_orleans,
                    musicResId = R.raw.mus_region_neworleans,
                    title = "Húngaro a Tranca",
                    gameMode = GameMode.BLOCK_HUNGARIAN,
                    targetScore = 100,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Felix 'Golden Boy'",
                    description = "Partida a 100 puntos encadenando fichas sin descanso en Bourbon Street."
                ),
                VenueChallenge(
                    id = "no_2",
                    challengeNumber = 2,
                    venueId = "new_orleans",
                    venueName = "Nueva Orleans",
                    venueRegion = "EE. UU. • French Quarter Jazz Club",
                    bgImageResId = R.drawable.venue_new_orleans,
                    musicResId = R.raw.mus_region_neworleans,
                    title = "Cinco y Tres",
                    gameMode = GameMode.FIVES_AND_THREES,
                    targetScore = 200,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Clarence 'Sax'",
                    description = "El compás del jazz a 200 puntos combinando múltiplos de 5 y 3."
                ),
                VenueChallenge(
                    id = "no_3",
                    challengeNumber = 3,
                    venueId = "new_orleans",
                    venueName = "Nueva Orleans",
                    venueRegion = "EE. UU. • French Quarter Jazz Club",
                    bgImageResId = R.drawable.venue_new_orleans,
                    musicResId = R.raw.mus_region_neworleans,
                    title = "Robando Especial (1v1)",
                    gameMode = GameMode.DRAW_SPECIAL,
                    targetScore = 100,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Big Mike",
                    description = "Duelo mano a mano protegiendo tu rama exclusiva a 100 puntos."
                ),
                VenueChallenge(
                    id = "no_4",
                    challengeNumber = 4,
                    venueId = "new_orleans",
                    venueName = "Nueva Orleans",
                    venueRegion = "EE. UU. • French Quarter Jazz Club",
                    bgImageResId = R.drawable.venue_new_orleans,
                    musicResId = R.raw.mus_region_neworleans,
                    title = "Matador",
                    gameMode = GameMode.MATADOR,
                    targetScore = 100,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Louis 'The Duke'",
                    description = "Conecta únicamente si la suma da 7. Las fichas [6|1], [5|2], [4|3] y [0|0] son comodines."
                ),
                VenueChallenge(
                    id = "no_5",
                    challengeNumber = 5,
                    venueId = "new_orleans",
                    venueName = "Nueva Orleans",
                    venueRegion = "EE. UU. • French Quarter Jazz Club",
                    bgImageResId = R.drawable.venue_new_orleans,
                    musicResId = R.raw.mus_region_neworleans,
                    title = "GRAN FINAL: Tranca en el Delta",
                    gameMode = GameMode.BLOCK,
                    targetScore = 50,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Felix 'Golden Boy'",
                    description = "Supera a Felix en el duelo a tranca para obtener el pase a Miami.",
                    trophyTitle = "Saxofón de Oro Bourbon St.",
                )
            )
            4 -> listOf(
                VenueChallenge(
                    id = "miami_1",
                    challengeNumber = 1,
                    venueId = "miami",
                    venueName = "Miami",
                    venueRegion = "EE. UU. • South Beach Club",
                    bgImageResId = R.drawable.bg_rival_miami,
                    musicResId = R.raw.mus_region_miami,
                    title = "Tranca en Parejas (¡Primer reto 2v2!)",
                    gameMode = GameMode.BLOCK,
                    targetScore = 80,
                    playerCount = 4,
                    isTeamGame = true, // << PAREJAS!
                    rivalName = "Tony 'Vice'",
                    description = "¡Bienvenido a Miami! Primera partida oficial en parejas (2 contra 2). El trabajo en equipo es vital."
                ),
                VenueChallenge(
                    id = "miami_2",
                    challengeNumber = 2,
                    venueId = "miami",
                    venueName = "Miami",
                    venueRegion = "EE. UU. • South Beach Club",
                    bgImageResId = R.drawable.bg_rival_miami,
                    musicResId = R.raw.mus_region_miami,
                    title = "Dominó Robando",
                    gameMode = GameMode.DRAW,
                    targetScore = 80,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Scarlett Perez",
                    description = "Batalla individual con pozo a 80 puntos."
                ),
                VenueChallenge(
                    id = "miami_3",
                    challengeNumber = 3,
                    venueId = "miami",
                    venueName = "Miami",
                    venueRegion = "EE. UU. • South Beach Club",
                    bgImageResId = R.drawable.bg_rival_miami,
                    musicResId = R.raw.mus_region_miami,
                    title = "Dominó de Cinco en Parejas",
                    gameMode = GameMode.ALL_FIVES,
                    targetScore = 300,
                    playerCount = 4,
                    isTeamGame = true, // << PAREJAS!
                    rivalName = "Rico 'Speed'",
                    description = "Gran reto de All Fives por equipos a 300 puntos. Los puntos de tu compañero suman a tu equipo."
                ),
                VenueChallenge(
                    id = "miami_4",
                    challengeNumber = 4,
                    venueId = "miami",
                    venueName = "Miami",
                    venueRegion = "EE. UU. • South Beach Club",
                    bgImageResId = R.drawable.bg_rival_miami,
                    musicResId = R.raw.mus_region_miami,
                    title = "Cinco y Tres",
                    gameMode = GameMode.FIVES_AND_THREES,
                    targetScore = 300,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Victor Cruz",
                    description = "Individual a 300 puntos exprimiendo cada múltiplo de 5 y 3."
                ),
                VenueChallenge(
                    id = "miami_5",
                    challengeNumber = 5,
                    venueId = "miami",
                    venueName = "Miami",
                    venueRegion = "EE. UU. • South Beach Club",
                    bgImageResId = R.drawable.bg_rival_miami,
                    musicResId = R.raw.mus_region_miami,
                    title = "GRAN FINAL: Matador en Parejas",
                    gameMode = GameMode.MATADOR,
                    targetScore = 120,
                    playerCount = 4,
                    isTeamGame = true, // << PAREJAS!
                    rivalName = "Tony 'Vice'",
                    description = "Suma 7 en equipo contra los reyes de South Beach para viajar a Las Vegas.",
                    trophyTitle = "Lancha Deportiva Offshore",
                )
            )
            5 -> listOf(
                VenueChallenge(
                    id = "vegas_1",
                    challengeNumber = 1,
                    venueId = "las_vegas",
                    venueName = "Las Vegas",
                    venueRegion = "EE. UU. • Casino Bellagio VIP",
                    bgImageResId = R.drawable.venue_las_vegas,
                    musicResId = R.raw.mus_region_lasvegas,
                    title = "Húngaro Mano a Mano",
                    gameMode = GameMode.BLOCK_HUNGARIAN,
                    targetScore = 170,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Jack 'Ace'",
                    description = "Partida de alta tensión a 170 puntos encadenando tiradas."
                ),
                VenueChallenge(
                    id = "vegas_2",
                    challengeNumber = 2,
                    venueId = "las_vegas",
                    venueName = "Las Vegas",
                    venueRegion = "EE. UU. • Casino Bellagio VIP",
                    bgImageResId = R.drawable.venue_las_vegas,
                    musicResId = R.raw.mus_region_lasvegas,
                    title = "Cinco y Tres (1v1)",
                    gameMode = GameMode.FIVES_AND_THREES,
                    targetScore = 400,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Sammy 'Diamond'",
                    description = "Duelo maratoniano a 400 puntos en la capital del juego."
                ),
                VenueChallenge(
                    id = "vegas_3",
                    challengeNumber = 3,
                    venueId = "las_vegas",
                    venueName = "Las Vegas",
                    venueRegion = "EE. UU. • Casino Bellagio VIP",
                    bgImageResId = R.drawable.venue_las_vegas,
                    musicResId = R.raw.mus_region_lasvegas,
                    title = "Dominó de Tres en Parejas",
                    gameMode = GameMode.ALL_THREES,
                    targetScore = 300,
                    playerCount = 4,
                    isTeamGame = true, // << PAREJAS!
                    rivalName = "Casino Twins",
                    description = "Equipos a 300 puntos en All Threes sobre la mesa de terciopelo."
                ),
                VenueChallenge(
                    id = "vegas_4",
                    challengeNumber = 4,
                    venueId = "las_vegas",
                    venueName = "Las Vegas",
                    venueRegion = "EE. UU. • Casino Bellagio VIP",
                    bgImageResId = R.drawable.venue_las_vegas,
                    musicResId = R.raw.mus_region_lasvegas,
                    title = "Matador",
                    gameMode = GameMode.MATADOR,
                    targetScore = 150,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Lucky Luciano",
                    description = "4 jugadores individuales dominando el 7 y los comodines."
                ),
                VenueChallenge(
                    id = "vegas_5",
                    challengeNumber = 5,
                    venueId = "las_vegas",
                    venueName = "Las Vegas",
                    venueRegion = "EE. UU. • Casino Bellagio VIP",
                    bgImageResId = R.drawable.venue_las_vegas,
                    musicResId = R.raw.mus_region_lasvegas,
                    title = "GRAN FINAL: Especial en Parejas",
                    gameMode = GameMode.DRAW_SPECIAL,
                    targetScore = 150,
                    playerCount = 4,
                    isTeamGame = true, // << PAREJAS!
                    rivalName = "Jack 'Ace'",
                    description = "Defiende las ramas del casino para conseguir el pase al Torneo Mundial.",
                    trophyTitle = "Trofeo Diamante de Las Vegas",
                )
            )
            else -> listOf(
                VenueChallenge(
                    id = "tokyo_1",
                    challengeNumber = 1,
                    venueId = "tokyo",
                    venueName = "Torneo Mundial",
                    venueRegion = "Japón • Salón Imperial Asakusa",
                    bgImageResId = R.drawable.venue_tokyo,
                    musicResId = R.raw.mus_region_tokyo,
                    title = "Dominó Robando en Parejas",
                    gameMode = GameMode.DRAW,
                    targetScore = 150,
                    playerCount = 4,
                    isTeamGame = true,
                    rivalName = "Kenji 'Iron Hand'",
                    description = "Primera ronda del Campeonato Mundial de Maestros."
                ),
                VenueChallenge(
                    id = "tokyo_2",
                    challengeNumber = 2,
                    venueId = "tokyo",
                    venueName = "Torneo Mundial",
                    venueRegion = "Japón • Salón Imperial Asakusa",
                    bgImageResId = R.drawable.venue_tokyo,
                    musicResId = R.raw.mus_region_tokyo,
                    title = "Húngaro en Parejas",
                    gameMode = GameMode.BLOCK_HUNGARIAN,
                    targetScore = 200,
                    playerCount = 4,
                    isTeamGame = true,
                    rivalName = "Maestros de Kioto",
                    description = "Semifinales de Húngaro a 200 puntos."
                ),
                VenueChallenge(
                    id = "tokyo_3",
                    challengeNumber = 3,
                    venueId = "tokyo",
                    venueName = "Torneo Mundial",
                    venueRegion = "Japón • Salón Imperial Asakusa",
                    bgImageResId = R.drawable.venue_tokyo,
                    musicResId = R.raw.mus_region_tokyo,
                    title = "Robando Especial",
                    gameMode = GameMode.DRAW_SPECIAL,
                    targetScore = 200,
                    playerCount = 4,
                    isTeamGame = false,
                    rivalName = "Akira Sato",
                    description = "Cuartos de final en Draw Special individual a 200 puntos."
                ),
                VenueChallenge(
                    id = "tokyo_4",
                    challengeNumber = 4,
                    venueId = "tokyo",
                    venueName = "Torneo Mundial",
                    venueRegion = "Japón • Salón Imperial Asakusa",
                    bgImageResId = R.drawable.venue_tokyo,
                    musicResId = R.raw.mus_region_tokyo,
                    title = "Cinco y Tres en Parejas",
                    gameMode = GameMode.FIVES_AND_THREES,
                    targetScore = 500,
                    playerCount = 4,
                    isTeamGame = true,
                    rivalName = "Dragones de Tokio",
                    description = "Maratón por el pase a la Gran Final mundial a 500 puntos."
                ),
                VenueChallenge(
                    id = "tokyo_5",
                    challengeNumber = 5,
                    venueId = "tokyo",
                    venueName = "Torneo Mundial",
                    venueRegion = "Japón • Salón Imperial Asakusa",
                    bgImageResId = R.drawable.venue_tokyo,
                    musicResId = R.raw.mus_region_tokyo,
                    title = "GRAN FINAL MUNDIAL: Matador Definitivo",
                    gameMode = GameMode.MATADOR,
                    targetScore = 200,
                    playerCount = 2,
                    isTeamGame = false,
                    rivalName = "Kenji 'Iron Hand'",
                    description = "El duelo cumbre: mano a mano contra el Campeón Mundial por la Copa de Oro.",
                    trophyTitle = "Copa Mundial de Dominó Imperial",
                )
            )
        }
    }

    fun getChallenge(venueIndex: Int, challengeIndex: Int): VenueChallenge {
        val challenges = getChallengesForVenue(venueIndex)
        val challenge = challenges[challengeIndex.coerceIn(0, challenges.lastIndex)]
        // La dificultad se deriva de la sede, no se repite en los 35 retos.
        return challenge.copy(difficulty = difficultyForVenue(venueIndex))
    }
}
