package com.example.dominograndprix.data

import com.example.dominograndprix.R
import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.model.Difficulty
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.LuxuryItem

object WorldTourRepository {

    fun getInitialVenues(): List<CityVenue> = listOf(
        CityVenue(
            id = "cuba",
            name = "La Habana",
            region = "Cuba • Club Social del Malecón",
            entryBet = 100,
            targetScore = 100,
            defaultMode = GameMode.DRAW,
            difficulty = Difficulty.RELAXED,
            wrpReward = 1500,
            trophyTitle = "Estuche Imperial de Habanos",
            trophyValue = 600,
            rivalName = "Compay Cheo",
            rivalIntro = "¡Bienvenido a La Habana! Aquí frente al Malecón las fichas suenan duro sobre la caoba. Veamos tu juego.",
            rivalDefeatLine = "¡Qué clase de cierre! Te has ganado el respeto de todo el Malecón.",
            postcardResId = R.drawable.venue_havana,
            musicTitle = "Ritmo del Malecón (Son Cubano)",
            cityLore = "Al caer la tarde sobre el Malecón de La Habana Vieja, los maestros del dominó se reúnen entre coches clásicos del 56 y brisa caribeña.",
            isUnlocked = true
        ),
        CityVenue(
            id = "mexico",
            name = "Ciudad de México",
            region = "México • Terraza Real del Zócalo",
            entryBet = 200,
            targetScore = 100,
            defaultMode = GameMode.ALL_FIVES,
            difficulty = Difficulty.RELAXED,
            wrpReward = 1500,
            trophyTitle = "Medalla Azteca de Plata",
            trophyValue = 900,
            rivalName = "Carlos 'El Rayo'",
            rivalIntro = "¡En la Ciudad de México los múltiplos de cinco cantan como mariachi! Si no sumas rápido, te quedas atrás.",
            rivalDefeatLine = "¡Increíble jugada! Tienes verdadera madera de campeón.",
            postcardResId = R.drawable.venue_mexico,
            musicTitle = "La Llorona (Mariachi de Garibaldi)",
            cityLore = "Bajo los arcos coloniales iluminados con papel picado frente a la Catedral, las partidas de múltiplos de 5 exigen cálculo milimétrico.",
            isUnlocked = true
        ),
        CityVenue(
            id = "puerto_rico",
            name = "San Juan",
            region = "Puerto Rico • Balcón de El Morro",
            entryBet = 350,
            targetScore = 120,
            defaultMode = GameMode.BLOCK,
            difficulty = Difficulty.STANDARD,
            wrpReward = 1500,
            trophyTitle = "Anillo Real del Caribe",
            trophyValue = 1400,
            rivalName = "Rafael 'El Muro'",
            rivalIntro = "En las calles adoquinas del Viejo San Juan jugamos al bloqueo puro. ¡Aquí nadie roba del pozo!",
            rivalDefeatLine = "Me cerraste la mesa con precisión quirúrgica...",
            postcardResId = R.drawable.venue_sanjuan,
            musicTitle = "Celebración Boricua (San Juan)",
            cityLore = "Con vistas a la bahía turquesa y la fortaleza de El Morro, el club de San Juan es famoso por sus duelos de tranca sin cuartel.",
            isUnlocked = true
        ),
        CityVenue(
            id = "new_orleans",
            name = "Nueva Orleans",
            region = "EE. UU. • French Quarter Jazz Club",
            entryBet = 500,
            targetScore = 150,
            defaultMode = GameMode.FIVES_AND_THREES,
            difficulty = Difficulty.STANDARD,
            wrpReward = 1800,
            trophyTitle = "Saxofón de Oro Bourbon St.",
            trophyValue = 2800,
            rivalName = "Felix 'Golden Boy'",
            rivalIntro = "Al compás del jazz en Bourbon Street jugamos Fives & Threes. ¡Un múltiplo de 15 te dará puntuación doble!",
            rivalDefeatLine = "No puede ser... ¿cómo calculaste ese doble múltiplo en el último turno?",
            postcardResId = R.drawable.venue_new_orleans,
            musicTitle = "The Entertainer (Ragtime de Scott Joplin)",
            cityLore = "Entre balcones de hierro forjado y farolas de gas reflejadas sobre el adoquín húmedo, el club de jazz acoge a los mejores estrategas.",
            isUnlocked = true
        ),
        CityVenue(
            id = "tokyo",
            name = "Tokio",
            region = "Japón • Salón Imperial Asakusa",
            entryBet = 800,
            targetScore = 150,
            defaultMode = GameMode.DRAW_SPECIAL,
            difficulty = Difficulty.STANDARD,
            wrpReward = 2000,
            trophyTitle = "Cronógrafo Imperial de Platino",
            trophyValue = 5500,
            rivalName = "Kenji 'Iron Hand'",
            rivalIntro = "Bajo los farolillos rojos de Asakusa cada maestro protege su extremo. Si te bloqueas, conquistaré tu rama.",
            rivalDefeatLine = "Tu disciplina en el tablero es digna de un gran maestro.",
            postcardResId = R.drawable.venue_tokyo,
            musicTitle = "Festival de Asakusa (Koto & Shamisen)",
            cityLore = "En un callejón tradicional iluminado por linternas rojas y pétalos de cerezo, el silencio solo se rompe con el golpe seco del marfil.",
            isUnlocked = true
        ),
        CityVenue(
            id = "monte_carlo",
            name = "Montecarlo",
            region = "Mónaco • Salle Impériale de la Riviera",
            entryBet = 1200,
            targetScore = 180,
            defaultMode = GameMode.BLOCK_HUNGARIAN,
            difficulty = Difficulty.TOUGH,
            wrpReward = 2200,
            trophyTitle = "Deportivo Riviera GT",
            trophyValue = 15000,
            rivalName = "Valérie 'La Reine'",
            rivalIntro = "En Montecarlo jugamos Dominó Húngaro: encadena todas las fichas consecutivas que puedas en un solo turno.",
            rivalDefeatLine = "Esa cadena consecutiva fue sencillamente imparable...",
            postcardResId = R.drawable.venue_monte_carlo,
            musicTitle = "Vals de la Riviera (Montecarlo)",
            cityLore = "Frente al puerto de superyates iluminado bajo la luna mediterránea, el salón privado del Gran Casino reúne a la élite europea.",
            isUnlocked = true
        ),
        CityVenue(
            id = "las_vegas",
            name = "Las Vegas",
            region = "EE. UU. • Gran Final Mundial en el Strip",
            entryBet = 1500,
            targetScore = 200,
            defaultMode = GameMode.MATADOR,
            difficulty = Difficulty.TOUGH,
            wrpReward = 3500,
            trophyTitle = "Copa Mundial Domino Imperial",
            trophyValue = 50000,
            rivalName = "Don Rodrigo",
            rivalIntro = "Llegaste a la Gran Final de Las Vegas. La regla del 7 y los 4 Matadores decidirán quién es el campeón del mundo.",
            rivalDefeatLine = "¡Imposible! Te coronas nuevo Campeón Mundial de Domino Imperial.",
            postcardResId = R.drawable.venue_las_vegas,
            musicTitle = "Hot Swing Casino (Las Vegas)",
            cityLore = "Entre fuentes doradas y reflectores que cruzan el cielo de Nevada, la mesa presidencial decide al campeón absoluto del mundo.",
            isUnlocked = true
        )
    )

    fun getLuxuryPrizes(): List<LuxuryItem> = listOf(
        LuxuryItem("cigars", "Estuche Imperial de Habanos", 600, "La Habana"),
        LuxuryItem("medal", "Medalla Azteca de Plata", 900, "Ciudad de México"),
        LuxuryItem("ring", "Anillo Real del Caribe", 1400, "San Juan"),
        LuxuryItem("sax", "Saxofón de Oro Bourbon St.", 2800, "Nueva Orleans"),
        LuxuryItem("watch", "Cronógrafo Imperial de Platino", 5500, "Tokio"),
        LuxuryItem("car", "Deportivo Riviera GT", 15000, "Montecarlo"),
        LuxuryItem("trophy", "Copa Mundial Domino Imperial", 50000, "Las Vegas")
    )
}
