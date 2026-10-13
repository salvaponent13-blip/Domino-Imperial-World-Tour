package com.example.dominograndprix.i18n

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dominograndprix.audio.DominoSoundManager
import com.example.dominograndprix.model.CityVenue
import com.example.dominograndprix.model.GameMode
import com.example.dominograndprix.model.LuxuryItem
import com.example.dominograndprix.ui.game.DarkNavy
import com.example.dominograndprix.ui.game.GoldAccent
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val shortLabel: String,
    val flag: String,
    val displayName: String
) {
    ES("es", "ES", "🇪🇸", "Español"),
    EN("en", "EN", "🇬🇧", "English"),
    FR("fr", "FR", "🇫🇷", "Français"),
    IT("it", "IT", "🇮🇹", "Italiano"),
    PT("pt", "PT", "🇵🇹", "Português"),
    DE("de", "DE", "🇩🇪", "Deutsch");

    companion object {
        fun fromCode(code: String): AppLanguage {
            val clean = code.lowercase(Locale.ROOT).take(2)
            return entries.firstOrNull { it.code == clean } ?: ES
        }
    }
}

object DominoStrings {

    var currentLanguage by mutableStateOf(AppLanguage.ES)
        private set

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences("domino_imperial_prefs", Context.MODE_PRIVATE)
        prefs = sp
        val saved = sp.getString("app_lang", null)
        currentLanguage = if (saved != null) {
            AppLanguage.fromCode(saved)
        } else {
            AppLanguage.ES
        }
    }

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
        prefs?.edit()?.putString("app_lang", lang.code)?.apply()
    }

    // --- COMMON & AUDIO ---
    fun editionSubtitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "WORLD TOUR • EDICIÓN 3D"
        AppLanguage.EN -> "WORLD TOUR • 3D EDITION"
        AppLanguage.FR -> "WORLD TOUR • ÉDITION 3D"
        AppLanguage.IT -> "WORLD TOUR • EDIZIONE 3D"
        AppLanguage.PT -> "WORLD TOUR • EDIÇÃO 3D"
        AppLanguage.DE -> "WORLD TOUR • 3D-EDITION"
    }

    fun musicOn(): String = when (currentLanguage) {
        AppLanguage.ES -> "🔊 Música"
        AppLanguage.EN -> "🔊 Music"
        AppLanguage.FR -> "🔊 Musique"
        AppLanguage.IT -> "🔊 Musica"
        AppLanguage.PT -> "🔊 Música"
        AppLanguage.DE -> "🔊 Musik"
    }

    fun mutedLabel(): String = when (currentLanguage) {
        AppLanguage.ES -> "🔇 Silencio"
        AppLanguage.EN -> "🔇 Muted"
        AppLanguage.FR -> "🔇 Muet"
        AppLanguage.IT -> "🔇 Muto"
        AppLanguage.PT -> "🔇 Mudo"
        AppLanguage.DE -> "🔇 Stumm"
    }

    fun selectLanguageTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "🌐 SELECCIONAR IDIOMA"
        AppLanguage.EN -> "🌐 SELECT LANGUAGE"
        AppLanguage.FR -> "🌐 CHOISIR LA LANGUE"
        AppLanguage.IT -> "🌐 SELEZIONA LINGUA"
        AppLanguage.PT -> "🌐 SELECIONAR IDIOMA"
        AppLanguage.DE -> "🌐 SPRACHE AUSWÄHLEN"
    }

    fun closeLabel(): String = when (currentLanguage) {
        AppLanguage.ES -> "CERRAR"
        AppLanguage.EN -> "CLOSE"
        AppLanguage.FR -> "FERMER"
        AppLanguage.IT -> "CHIUDI"
        AppLanguage.PT -> "FECHAR"
        AppLanguage.DE -> "SCHLIESSEN"
    }

    // --- MAIN TITLE SCREEN ---
    fun worldTourBtnTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "🌍 WORLD TOUR (7 CIUDADES Y MÚSICA)"
        AppLanguage.EN -> "🌍 WORLD TOUR (7 CITIES & MUSIC)"
        AppLanguage.FR -> "🌍 WORLD TOUR (7 VILLES & MUSIQUE)"
        AppLanguage.IT -> "🌍 WORLD TOUR (7 CITTÀ E MUSICA)"
        AppLanguage.PT -> "🌍 WORLD TOUR (7 CIDADES E MÚSICA)"
        AppLanguage.DE -> "🌍 WORLD TOUR (7 STÄDTE & MUSIK)"
    }

    fun worldTourBtnSub(): String = when (currentLanguage) {
        AppLanguage.ES -> "Viaja a La Habana, México, San Juan, Nueva Orleans, Tokio, Montecarlo y Las Vegas"
        AppLanguage.EN -> "Travel to Havana, Mexico City, San Juan, New Orleans, Tokyo, Monte Carlo & Las Vegas"
        AppLanguage.FR -> "Voyagez à La Havane, Mexico, San Juan, La Nouvelle-Orléans, Tokyo, Monte-Carlo et Las Vegas"
        AppLanguage.IT -> "Viaggia a L'Avana, Città del Messico, San Juan, New Orleans, Tokyo, Montecarlo e Las Vegas"
        AppLanguage.PT -> "Viaje para Havana, Cidade do México, San Juan, Nova Orleans, Tóquio, Monte Carlo e Las Vegas"
        AppLanguage.DE -> "Reise nach Havanna, Mexiko-Stadt, San Juan, New Orleans, Tokio, Monte Carlo & Las Vegas"
    }

    fun quickPlayBtnTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "⚡ JUGAR AHORA EN LA HABANA"
        AppLanguage.EN -> "⚡ PLAY NOW IN HAVANA"
        AppLanguage.FR -> "⚡ JOUER MAINTENANT À LA HAVANE"
        AppLanguage.IT -> "⚡ GIOCA ORA A L'AVANA"
        AppLanguage.PT -> "⚡ JOGAR AGORA EM HAVANA"
        AppLanguage.DE -> "⚡ JETZT IN HAVANNA SPIELEN"
    }

    fun quickPlayBtnSub(): String = when (currentLanguage) {
        AppLanguage.ES -> "Entra directo al Club del Malecón con música cubana y fichas 3D"
        AppLanguage.EN -> "Enter the Malecón Club directly with Cuban music and 3D tiles"
        AppLanguage.FR -> "Entrez directement au Club du Malecón avec musique cubaine et dominos 3D"
        AppLanguage.IT -> "Entra direttamente al Club del Malecón con musica cubana e tessere 3D"
        AppLanguage.PT -> "Entre direto no Clube do Malecón com música cubana e peças 3D"
        AppLanguage.DE -> "Betritt direkt den Malecón-Club mit kubanischer Musik und 3D-Steinen"
    }

    fun eightModesBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "📖 8 MODALIDADES"
        AppLanguage.EN -> "📖 8 GAME MODES"
        AppLanguage.FR -> "📖 8 MODES DE JEU"
        AppLanguage.IT -> "📖 8 MODALITÀ"
        AppLanguage.PT -> "📖 8 MODALIDADES"
        AppLanguage.DE -> "📖 8 SPIELMODI"
    }

    fun trophyRoomBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "💎 SALA DE TROFEOS"
        AppLanguage.EN -> "💎 TROPHY ROOM"
        AppLanguage.FR -> "💎 SALLE DES TROPHÉES"
        AppLanguage.IT -> "💎 SALA TROFEI"
        AppLanguage.PT -> "💎 SALA DE TROFÉUS"
        AppLanguage.DE -> "💎 TROPHÄENRAUM"
    }

    fun titleFooter(): String = when (currentLanguage) {
        AppLanguage.ES -> "Domino Imperial: World Tour • Fichas Acrílicas 3D Bicapa"
        AppLanguage.EN -> "Domino Imperial: World Tour • 3D Bi-Layer Acrylic Tiles"
        AppLanguage.FR -> "Domino Imperial: World Tour • Dominos Acryliques 3D Bicouches"
        AppLanguage.IT -> "Domino Imperial: World Tour • Tessere Acriliche 3D Doppio Strato"
        AppLanguage.PT -> "Domino Imperial: World Tour • Peças Acrílicas 3D Dupla Camada"
        AppLanguage.DE -> "Domino Imperial: World Tour • Zweischichtige 3D-Acrylsteine"
    }

    // --- WORLD TOUR MAP SCREEN ---
    fun mapSubtitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "ELIGE TU LOCALIDAD Y MÚSICA"
        AppLanguage.EN -> "CHOOSE YOUR CITY & MUSIC"
        AppLanguage.FR -> "CHOISISSEZ VOTRE VILLE & MUSIQUE"
        AppLanguage.IT -> "SCEGLI CITTÀ E MUSICA"
        AppLanguage.PT -> "ESCOLHA SUA CIDADE E MÚSICA"
        AppLanguage.DE -> "WÄHLE STADT & MUSIK"
    }

    fun cashLabel(amount: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "CAJA: $$amount"
        AppLanguage.EN -> "CASH: $$amount"
        AppLanguage.FR -> "CAISSE: $$amount"
        AppLanguage.IT -> "CASSA: $$amount"
        AppLanguage.PT -> "CAIXA: $$amount"
        AppLanguage.DE -> "KASSE: $$amount"
    }

    fun wonBadge(): String = when (currentLanguage) {
        AppLanguage.ES -> "✓ GANADO"
        AppLanguage.EN -> "✓ WON"
        AppLanguage.FR -> "✓ GAGNÉ"
        AppLanguage.IT -> "✓ VINTO"
        AppLanguage.PT -> "✓ VENCIDO"
        AppLanguage.DE -> "✓ GEWONNEN"
    }

    fun travelBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "VIAJAR"
        AppLanguage.EN -> "TRAVEL"
        AppLanguage.FR -> "VOYAGER"
        AppLanguage.IT -> "VIAGGIA"
        AppLanguage.PT -> "VIAJAR"
        AppLanguage.DE -> "REISEN"
    }

    fun backToMainMenu(): String = when (currentLanguage) {
        AppLanguage.ES -> "VOLVER AL MENÚ PRINCIPAL"
        AppLanguage.EN -> "BACK TO MAIN MENU"
        AppLanguage.FR -> "RETOUR AU MENU PRINCIPAL"
        AppLanguage.IT -> "TORNA AL MENU PRINCIPALE"
        AppLanguage.PT -> "VOLTAR AO MENU PRINCIPAL"
        AppLanguage.DE -> "ZURÜCK ZUM HAUPTMENÜ"
    }

    fun ruleAndTarget(ruleTitle: String, target: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Regla: $ruleTitle • Meta $target"
        AppLanguage.EN -> "Rule: $ruleTitle • Target $target"
        AppLanguage.FR -> "Règle : $ruleTitle • Objectif $target"
        AppLanguage.IT -> "Regola: $ruleTitle • Obiettivo $target"
        AppLanguage.PT -> "Regra: $ruleTitle • Meta $target"
        AppLanguage.DE -> "Regel: $ruleTitle • Ziel $target"
    }

    // --- CITY ARRIVAL / RIVAL SHOWDOWN SCREEN ---
    fun destinationHeader(cityName: String): String = when (currentLanguage) {
        AppLanguage.ES -> "✈️ DESTINO: ${cityName.uppercase()}"
        AppLanguage.EN -> "✈️ DESTINATION: ${cityName.uppercase()}"
        AppLanguage.FR -> "✈️ DESTINATION : ${cityName.uppercase()}"
        AppLanguage.IT -> "✈️ DESTINAZIONE: ${cityName.uppercase()}"
        AppLanguage.PT -> "✈️ DESTINO: ${cityName.uppercase()}"
        AppLanguage.DE -> "✈️ REISEZIEL: ${cityName.uppercase()}"
    }

    fun nowPlaying(track: String): String = when (currentLanguage) {
        AppLanguage.ES -> "🎵 Sonando ahora: $track"
        AppLanguage.EN -> "🎵 Now playing: $track"
        AppLanguage.FR -> "🎵 En écoute : $track"
        AppLanguage.IT -> "🎵 In riproduzione: $track"
        AppLanguage.PT -> "🎵 Tocando agora: $track"
        AppLanguage.DE -> "🎵 Läuft gerade: $track"
    }

    fun localRivalHeader(rivalName: String): String = when (currentLanguage) {
        AppLanguage.ES -> "RIVAL LOCAL: ${rivalName.uppercase()}"
        AppLanguage.EN -> "LOCAL RIVAL: ${rivalName.uppercase()}"
        AppLanguage.FR -> "RIVAL LOCAL : ${rivalName.uppercase()}"
        AppLanguage.IT -> "RIVALE LOCALE: ${rivalName.uppercase()}"
        AppLanguage.PT -> "RIVAL LOCAL: ${rivalName.uppercase()}"
        AppLanguage.DE -> "LOKALER RIVALE: ${rivalName.uppercase()}"
    }

    fun ruleLabel(title: String): String = when (currentLanguage) {
        AppLanguage.ES -> "Regla: $title"
        AppLanguage.EN -> "Rule: $title"
        AppLanguage.FR -> "Règle : $title"
        AppLanguage.IT -> "Regola: $title"
        AppLanguage.PT -> "Regra: $title"
        AppLanguage.DE -> "Regel: $title"
    }

    fun targetLabel(pts: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Meta: $pts pts"
        AppLanguage.EN -> "Target: $pts pts"
        AppLanguage.FR -> "Objectif : $pts pts"
        AppLanguage.IT -> "Obiettivo: $pts pt"
        AppLanguage.PT -> "Meta: $pts pts"
        AppLanguage.DE -> "Ziel: $pts Pkt."
    }

    fun entryBetLabel(bet: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Entrada: $$bet"
        AppLanguage.EN -> "Entry: $$bet"
        AppLanguage.FR -> "Mise : $$bet"
        AppLanguage.IT -> "Ingresso: $$bet"
        AppLanguage.PT -> "Entrada: $$bet"
        AppLanguage.DE -> "Einsatz: $$bet"
    }

    fun trophyLabel(trophy: String): String = when (currentLanguage) {
        AppLanguage.ES -> "Trofeo: $trophy"
        AppLanguage.EN -> "Trophy: $trophy"
        AppLanguage.FR -> "Trophée : $trophy"
        AppLanguage.IT -> "Trofeo: $trophy"
        AppLanguage.PT -> "Troféu: $trophy"
        AppLanguage.DE -> "Trophäe: $trophy"
    }

    fun mapBtnShort(): String = when (currentLanguage) {
        AppLanguage.ES -> "MAPA"
        AppLanguage.EN -> "MAP"
        AppLanguage.FR -> "CARTE"
        AppLanguage.IT -> "MAPPA"
        AppLanguage.PT -> "MAPA"
        AppLanguage.DE -> "KARTE"
    }

    fun playInCityBtn(city: String): String = when (currentLanguage) {
        AppLanguage.ES -> "¡JUGAR EN ${city.uppercase()}!"
        AppLanguage.EN -> "PLAY IN ${city.uppercase()}!"
        AppLanguage.FR -> "JOUER À ${city.uppercase()} !"
        AppLanguage.IT -> "GIOCA A ${city.uppercase()}!"
        AppLanguage.PT -> "JOGAR EM ${city.uppercase()}!"
        AppLanguage.DE -> "IN ${city.uppercase()} SPIELEN!"
    }

    // --- DOMINO TABLE SCREEN ---
    fun boneyardCount(count: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Pozo: $count"
        AppLanguage.EN -> "Boneyard: $count"
        AppLanguage.FR -> "Pioche : $count"
        AppLanguage.IT -> "Tallone: $count"
        AppLanguage.PT -> "Monte: $count"
        AppLanguage.DE -> "Stock: $count"
    }

    fun openEndsBadge(left: Int?, right: Int?): String = when (currentLanguage) {
        AppLanguage.ES -> "Extremos: [${left ?: 0}] y [${right ?: 0}]"
        AppLanguage.EN -> "Open Ends: [${left ?: 0}] & [${right ?: 0}]"
        AppLanguage.FR -> "Extrémités : [${left ?: 0}] et [${right ?: 0}]"
        AppLanguage.IT -> "Estremi: [${left ?: 0}] e [${right ?: 0}]"
        AppLanguage.PT -> "Pontas: [${left ?: 0}] e [${right ?: 0}]"
        AppLanguage.DE -> "Enden: [${left ?: 0}] & [${right ?: 0}]"
    }

    fun emptyTableTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "MESA DE DOMINÓ"
        AppLanguage.EN -> "DOMINO TABLE"
        AppLanguage.FR -> "TABLE DE DOMINOS"
        AppLanguage.IT -> "TAVOLO DA DOMINO"
        AppLanguage.PT -> "MESA DE DOMINÓ"
        AppLanguage.DE -> "DOMINOTISCH"
    }

    fun emptyTableSub(isMyTurn: Boolean): String = if (isMyTurn) {
        when (currentLanguage) {
            AppLanguage.ES -> "Toca cualquier ficha de tu mano para abrir la partida"
            AppLanguage.EN -> "Tap any tile in your hand to open the game"
            AppLanguage.FR -> "Touchez un domino de votre main pour ouvrir la partie"
            AppLanguage.IT -> "Tocca una tessera della tua mano per aprire la partita"
            AppLanguage.PT -> "Toque em qualquer peça da sua mão para abrir a partida"
            AppLanguage.DE -> "Tippe auf einen Stein in deiner Hand, um zu eröffnen"
        }
    } else {
        when (currentLanguage) {
            AppLanguage.ES -> "Esperando apertura..."
            AppLanguage.EN -> "Waiting for opening move..."
            AppLanguage.FR -> "En attente de l'ouverture..."
            AppLanguage.IT -> "In attesa dell'apertura..."
            AppLanguage.PT -> "Aguardando abertura..."
            AppLanguage.DE -> "Warte auf Eröffnungszug..."
        }
    }

    fun noPlayableTileTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "NO TIENES FICHA PARA JUGAR"
        AppLanguage.EN -> "NO PLAYABLE TILE IN HAND"
        AppLanguage.FR -> "AUCUN DOMINO JOUABLE"
        AppLanguage.IT -> "NESSUNA TESSERA GIOCABILE"
        AppLanguage.PT -> "NENHUMA PEÇA JOGÁVEL"
        AppLanguage.DE -> "KEIN PASSENDER STEIN"
    }

    fun pickFromBoneyardSub(): String = when (currentLanguage) {
        AppLanguage.ES -> "Escoge una ficha boca abajo del pozo al azar:"
        AppLanguage.EN -> "Pick a face-down tile from the boneyard:"
        AppLanguage.FR -> "Piochez un domino face cachée au hasard :"
        AppLanguage.IT -> "Pesca una tessera coperta dal tallone:"
        AppLanguage.PT -> "Escolha uma peça virada do monte ao acaso:"
        AppLanguage.DE -> "Ziehe einen verdeckten Stein aus dem Stock:"
    }

    fun drewTileMsg(tileStr: String): String = when (currentLanguage) {
        AppLanguage.ES -> "Robaste la ficha $tileStr del pozo"
        AppLanguage.EN -> "You drew $tileStr from the boneyard"
        AppLanguage.FR -> "Vous avez pioché le domino $tileStr"
        AppLanguage.IT -> "Hai pescato la tessera $tileStr"
        AppLanguage.PT -> "Você comprou a peça $tileStr do monte"
        AppLanguage.DE -> "Stein $tileStr aus dem Stock gezogen"
    }

    fun autoPassMsg(): String = when (currentLanguage) {
        AppLanguage.ES -> "No tienes jugada válida — Pasando turno automáticamente..."
        AppLanguage.EN -> "No valid move — Passing turn automatically..."
        AppLanguage.FR -> "Aucun coup valide — Tour passé automatiquement..."
        AppLanguage.IT -> "Nessuna mossa valida — Passo automatico..."
        AppLanguage.PT -> "Sem jogada válida — Passando a vez automaticamente..."
        AppLanguage.DE -> "Kein Zug möglich — Automatisch passen..."
    }

    fun rivalPlayingMsg(): String = when (currentLanguage) {
        AppLanguage.ES -> "Jugando rival..."
        AppLanguage.EN -> "Opponent thinking..."
        AppLanguage.FR -> "Adversaire joue..."
        AppLanguage.IT -> "Avversario in gioco..."
        AppLanguage.PT -> "Rival jogando..."
        AppLanguage.DE -> "Gegner zieht..."
    }

    fun chooseEndPrompt(left: Int?, right: Int?): String = when (currentLanguage) {
        AppLanguage.ES -> "Elige extremo [${left ?: 0}] o [${right ?: 0}] en la mesa"
        AppLanguage.EN -> "Choose end [${left ?: 0}] or [${right ?: 0}] on the table"
        AppLanguage.FR -> "Choisissez l'extrémité [${left ?: 0}] ou [${right ?: 0}]"
        AppLanguage.IT -> "Scegli l'estremo [${left ?: 0}] o [${right ?: 0}] sul tavolo"
        AppLanguage.PT -> "Escolha a ponta [${left ?: 0}] ou [${right ?: 0}] na mesa"
        AppLanguage.DE -> "Wähle Ende [${left ?: 0}] oder [${right ?: 0}] auf dem Tisch"
    }

    fun yourTurnPrompt(): String = when (currentLanguage) {
        AppLanguage.ES -> "TU TURNO: Toca tu ficha para jugarla"
        AppLanguage.EN -> "YOUR TURN: Tap a tile to play"
        AppLanguage.FR -> "VOTRE TOUR : Touchez un domino pour jouer"
        AppLanguage.IT -> "TUO TURNO: Tocca una tessera per giocare"
        AppLanguage.PT -> "SUA VEZ: Toque na peça para jogar"
        AppLanguage.DE -> "DEIN ZUG: Tippe auf einen Stein"
    }

    fun turnOfPlayer(name: String): String = when (currentLanguage) {
        AppLanguage.ES -> "Turno de $name"
        AppLanguage.EN -> "$name's turn"
        AppLanguage.FR -> "Tour de $name"
        AppLanguage.IT -> "Turno di $name"
        AppLanguage.PT -> "Vez de $name"
        AppLanguage.DE -> "$name ist am Zug"
    }

    /** Aviso de encadenamiento del modo Húngaro: puedes seguir tirando en el mismo turno. */
    fun hungarianChainPrompt(): String = when (currentLanguage) {
        AppLanguage.ES -> "¡ENCADENA! Sigues tirando (Húngaro): toca otra ficha válida"
        AppLanguage.EN -> "CHAIN ON! Keep playing (Hungarian): tap another valid tile"
        AppLanguage.FR -> "ENCHAÎNEZ ! Continuez à jouer (Hongrois) : touchez un autre domino valide"
        AppLanguage.IT -> "CONCATENA! Continui a giocare (Ungherese): tocca un'altra tessera valida"
        AppLanguage.PT -> "ENCADEIE! Continue jogando (Húngaro): toque em outra peça válida"
        AppLanguage.DE -> "WEITER ANLEGEN! Du bist noch dran (Ungarisch): tippe einen weiteren passenden Stein"
    }

    /** Draw Special: rama asignada al jugador humano. */
    fun branchAssignment(isLeft: Boolean): String = when (currentLanguage) {
        AppLanguage.ES -> "Tu rama: ${if (isLeft) "IZQUIERDA" else "DERECHA"}"
        AppLanguage.EN -> "Your branch: ${if (isLeft) "LEFT" else "RIGHT"}"
        AppLanguage.FR -> "Votre branche : ${if (isLeft) "GAUCHE" else "DROITE"}"
        AppLanguage.IT -> "Il tuo ramo: ${if (isLeft) "SINISTRA" else "DESTRA"}"
        AppLanguage.PT -> "Sua ponta: ${if (isLeft) "ESQUERDA" else "DIREITA"}"
        AppLanguage.DE -> "Dein Zweig: ${if (isLeft) "LINKS" else "RECHTS"}"
    }

    /** Draw Special: tu rama está cerrada porque el rival que la posee no ha pasado. */
    fun branchRestricted(isLeft: Boolean): String = when (currentLanguage) {
        AppLanguage.ES -> "Rama ${if (isLeft) "izquierda" else "derecha"} bloqueada por el rival"
        AppLanguage.EN -> "${if (isLeft) "Left" else "Right"} branch locked by opponent"
        AppLanguage.FR -> "Branche ${if (isLeft) "gauche" else "droite"} bloquée par l'adversaire"
        AppLanguage.IT -> "Ramo ${if (isLeft) "sinistro" else "destro"} bloccato dall'avversario"
        AppLanguage.PT -> "Ponta ${if (isLeft) "esquerda" else "direita"} bloqueada pelo rival"
        AppLanguage.DE -> "${if (isLeft) "Linker" else "Rechter"} Zweig vom Gegner gesperrt"
    }

    fun scoreSummary(you: Int, rival: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Tú: $you | Rival: $rival"
        AppLanguage.EN -> "You: $you | Rival: $rival"
        AppLanguage.FR -> "Vous : $you | Rival : $rival"
        AppLanguage.IT -> "Tu: $you | Rivale: $rival"
        AppLanguage.PT -> "Você: $you | Rival: $rival"
        AppLanguage.DE -> "Du: $you | Gegner: $rival"
    }

    fun endChoiceBtn(pip: Int?, isLeft: Boolean): String {
        val word = when (currentLanguage) {
            AppLanguage.ES -> "EXTREMO"
            AppLanguage.EN -> "END"
            AppLanguage.FR -> "EXTRÉMITÉ"
            AppLanguage.IT -> "ESTREMO"
            AppLanguage.PT -> "PONTA"
            AppLanguage.DE -> "ENDE"
        }
        return if (isLeft) "⬅ $word [${pip ?: 0}]" else "$word [${pip ?: 0}] ➡"
    }

    fun roundDialogTitle(won: Boolean): String = if (won) {
        when (currentLanguage) {
            AppLanguage.ES -> "¡RONDA GANADA!"
            AppLanguage.EN -> "ROUND WON!"
            AppLanguage.FR -> "MANCHE GAGNÉE !"
            AppLanguage.IT -> "MANO VINTA!"
            AppLanguage.PT -> "RODADA VENCIDA!"
            AppLanguage.DE -> "RUNDE GEWONNEN!"
        }
    } else {
        when (currentLanguage) {
            AppLanguage.ES -> "RONDA TERMINADA"
            AppLanguage.EN -> "ROUND OVER"
            AppLanguage.FR -> "MANCHE TERMINÉE"
            AppLanguage.IT -> "MANO TERMINATA"
            AppLanguage.PT -> "RODADA ENCERRADA"
            AppLanguage.DE -> "RUNDE BEENDET"
        }
    }

    fun teamScoreLine(teamA: Int, target: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Tu Equipo: $teamA pts / Meta: $target"
        AppLanguage.EN -> "Your Team: $teamA pts / Target: $target"
        AppLanguage.FR -> "Votre Équipe : $teamA pts / Objectif : $target"
        AppLanguage.IT -> "Tua Squadra: $teamA pt / Obiettivo: $target"
        AppLanguage.PT -> "Sua Equipe: $teamA pts / Meta: $target"
        AppLanguage.DE -> "Dein Team: $teamA Pkt. / Ziel: $target"
    }

    fun rivalScoreLine(teamB: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Rivales: $teamB pts"
        AppLanguage.EN -> "Opponents: $teamB pts"
        AppLanguage.FR -> "Adversaires : $teamB pts"
        AppLanguage.IT -> "Avversari: $teamB pt"
        AppLanguage.PT -> "Rivais: $teamB pts"
        AppLanguage.DE -> "Gegner: $teamB Pkt."
    }

    fun nextRoundOrFinishBtn(isMatchOver: Boolean): String = if (isMatchOver) {
        when (currentLanguage) {
            AppLanguage.ES -> "FINALIZAR PARTIDA"
            AppLanguage.EN -> "FINISH MATCH"
            AppLanguage.FR -> "TERMINER LA PARTIE"
            AppLanguage.IT -> "TERMINA PARTITA"
            AppLanguage.PT -> "FINALIZAR PARTIDA"
            AppLanguage.DE -> "SPIEL BEENDEN"
        }
    } else {
        when (currentLanguage) {
            AppLanguage.ES -> "SIGUIENTE RONDA"
            AppLanguage.EN -> "NEXT ROUND"
            AppLanguage.FR -> "MANCHE SUIVANTE"
            AppLanguage.IT -> "PROSSIMA MANO"
            AppLanguage.PT -> "PRÓXIMA RODADA"
            AppLanguage.DE -> "NÄCHSTE RUNDE"
        }
    }

    fun exitToMenuBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "SALIR AL MENÚ"
        AppLanguage.EN -> "EXIT TO MENU"
        AppLanguage.FR -> "QUITTER VERS LE MENU"
        AppLanguage.IT -> "ESCI AL MENU"
        AppLanguage.PT -> "SAIR PARA O MENU"
        AppLanguage.DE -> "ZUM MENÜ"
    }

    // --- RULES GUIDE SCREEN & GAME MODES ---
    fun rulesGuideTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "📖 GUÍA DE LAS 8 MODALIDADES"
        AppLanguage.EN -> "📖 8 GAME MODES GUIDE"
        AppLanguage.FR -> "📖 GUIDE DES 8 MODES DE JEU"
        AppLanguage.IT -> "📖 GUIDA ALLE 8 MODALITÀ"
        AppLanguage.PT -> "📖 GUIA DAS 8 MODALIDADES"
        AppLanguage.DE -> "📖 LEITFADEN DER 8 SPIELMODI"
    }

    fun modeSummary(mode: GameMode): String = when (currentLanguage) {
        AppLanguage.ES -> mode.summary
        AppLanguage.EN -> when (mode) {
            GameMode.DRAW -> "Classic dominoes with boneyard draw. First player to empty their hand wins."
            GameMode.BLOCK -> "Pure tactical block dominoes without drawing. Pass if you have no valid move."
            GameMode.ALL_FIVES -> "Score points immediately whenever the sum of open board ends is a multiple of 5."
            GameMode.ALL_THREES -> "Score points immediately whenever the sum of open board ends is a multiple of 3."
            GameMode.FIVES_AND_THREES -> "Score on multiples of 5 and 3. Double bonus points when divisible by 15!"
            GameMode.MATADOR -> "Connect tiles that add up to 7 with the open end, or play the 4 Matador wildcards."
            GameMode.DRAW_SPECIAL -> "Each player plays on their own assigned branch or on a blocked opponent's branch."
            GameMode.BLOCK_HUNGARIAN -> "Chain and play as many valid tiles in a row as you can during a single turn!"
        }
        AppLanguage.FR -> when (mode) {
            GameMode.DRAW -> "Dominos classiques avec pioche. Le premier à poser tous ses dominos gagne."
            GameMode.BLOCK -> "Blocage tactique pur sans pioche. Passez votre tour si vous êtes bloqué."
            GameMode.ALL_FIVES -> "Marquez des points dès que la somme des extrémités ouvertes est un multiple de 5."
            GameMode.ALL_THREES -> "Marquez des points dès que la somme des extrémités ouvertes est un multiple de 3."
            GameMode.FIVES_AND_THREES -> "Marquez sur les multiples de 5 et de 3. Points doublés sur les multiples de 15 !"
            GameMode.MATADOR -> "Connectez des dominos dont la somme fait 7 avec l'extrémité, ou utilisez les 4 Matadors."
            GameMode.DRAW_SPECIAL -> "Chaque joueur joue sur sa propre branche ou sur celle d'un adversaire bloqué."
            GameMode.BLOCK_HUNGARIAN -> "Enchaînez et posez autant de dominos valides que possible en un seul tour !"
        }
        AppLanguage.IT -> when (mode) {
            GameMode.DRAW -> "Domino classico con pesca dal tallone. Vince chi esaurisce per primo le tessere."
            GameMode.BLOCK -> "Blocco tattico puro senza tallone. Se non puoi giocare, passi il turno."
            GameMode.ALL_FIVES -> "Ottieni punti immediati se la somma degli estremi aperti è multiplo di 5."
            GameMode.ALL_THREES -> "Ottieni punti immediati se la somma degli estremi aperti è multiplo di 3."
            GameMode.FIVES_AND_THREES -> "Punti sui multipli di 5 e 3. Punteggio doppio se è multiplo di entrambi (15)!"
            GameMode.MATADOR -> "Collega tessere che sommano 7 con l'estremo aperto o usa i 4 jolly Matador."
            GameMode.DRAW_SPECIAL -> "Ogni giocatore gioca sul proprio ramo o su quello di un avversario bloccato."
            GameMode.BLOCK_HUNGARIAN -> "Concatena e gioca tutte le tessere valide consecutive che puoi in un solo turno!"
        }
        AppLanguage.PT -> when (mode) {
            GameMode.DRAW -> "Dominó clássico com compra no monte. Quem ficar sem peças primeiro vence."
            GameMode.BLOCK -> "Bloqueio puro sem monte. Se não tiver jogada, deve passar a vez."
            GameMode.ALL_FIVES -> "Some pontos imediatamente se a soma das pontas abertas for múltiplo de 5."
            GameMode.ALL_THREES -> "Some pontos imediatamente se a soma das pontas abertas for múltiplo de 3."
            GameMode.FIVES_AND_THREES -> "Pontue em múltiplos de 5 e 3. Pontuação dupla se for múltiplo de 15!"
            GameMode.MATADOR -> "Conecte peças que somem 7 com a ponta aberta ou use os 4 curingas Matadores."
            GameMode.DRAW_SPECIAL -> "Cada jogador joga em sua própria ponta ou na ponta de um rival bloqueado."
            GameMode.BLOCK_HUNGARIAN -> "Encadeie e jogue várias peças seguidas em um único turno!"
        }
        AppLanguage.DE -> when (mode) {
            GameMode.DRAW -> "Klassisches Domino mit Ziehen aus dem Stock. Wer zuerst keine Steine hat, gewinnt."
            GameMode.BLOCK -> "Reines Block-Domino ohne Stock. Wenn du nicht anlegen kannst, musst du passen."
            GameMode.ALL_FIVES -> "Sofortige Punkte, wenn die Summe der offenen Enden durch 5 teilbar ist."
            GameMode.ALL_THREES -> "Sofortige Punkte, wenn die Summe der offenen Enden durch 3 teilbar ist."
            GameMode.FIVES_AND_THREES -> "Punkte für Vielfache von 5 und 3. Doppelte Punktzahl bei Vielfachen von 15!"
            GameMode.MATADOR -> "Lege Steine an, die zusammen mit dem Ende 7 ergeben, oder nutze die 4 Matadore."
            GameMode.DRAW_SPECIAL -> "Jeder Spieler legt an seinem eigenen Zweig oder am Zweig eines blockierten Gegners an."
            GameMode.BLOCK_HUNGARIAN -> "Lege in einem einzigen Zug so viele passende Steine hintereinander an wie möglich!"
        }
    }

    fun modeStrategy(mode: GameMode): String = when (currentLanguage) {
        AppLanguage.ES -> when (mode) {
            GameMode.MATADOR -> "Estrategia clave: Comodines Matadores: [6|1], [5|2], [4|3] y [0|0]."
            GameMode.BLOCK_HUNGARIAN -> "Estrategia clave: ¡Juega todas las fichas válidas seguidas en una sola tirada!"
            GameMode.FIVES_AND_THREES -> "Estrategia clave: Busca sumar 15 en los extremos para duplicar tu puntuación."
            GameMode.ALL_FIVES -> "Estrategia clave: Suma 5, 10, 15 o 20 en los extremos abiertos en cada turno."
            GameMode.ALL_THREES -> "Estrategia clave: Suma 3, 6, 9, 12, 15 o 18 en los extremos abiertos."
            GameMode.DRAW_SPECIAL -> "Estrategia clave: Protege tu rama y aprovecha cuando el rival pase."
            GameMode.BLOCK -> "Estrategia clave: Cuenta las fichas jugadas para cerrar la mesa con menos puntos."
            GameMode.DRAW -> "Estrategia clave: Deshazte rápido de las fichas altas y controla los dobles."
        }
        AppLanguage.EN -> when (mode) {
            GameMode.MATADOR -> "Key Strategy: Matador wildcards are [6|1], [5|2], [4|3], and [0|0]."
            GameMode.BLOCK_HUNGARIAN -> "Key Strategy: Chain as many consecutive valid tiles as possible in one turn!"
            GameMode.FIVES_AND_THREES -> "Key Strategy: Aim for open ends totaling 15 to score double bonus points."
            GameMode.ALL_FIVES -> "Key Strategy: Make open ends total 5, 10, 15, or 20 on every turn."
            GameMode.ALL_THREES -> "Key Strategy: Make open ends total 3, 6, 9, 12, 15, or 18."
            GameMode.DRAW_SPECIAL -> "Key Strategy: Guard your own branch and attack when opponents pass."
            GameMode.BLOCK -> "Key Strategy: Track played pips to lock the board with the lowest hand count."
            GameMode.DRAW -> "Key Strategy: Play high-pip tiles early and control the doubles."
        }
        AppLanguage.FR -> "Stratégie : Contrôlez les doubles et calculez les extrémités ouvertes."
        AppLanguage.IT -> "Strategia: Controlla i doppi e calcola gli estremi aperti del tavolo."
        AppLanguage.PT -> "Estratégia: Controle as buchas (duplos) e domine as pontas da mesa."
        AppLanguage.DE -> "Strategie: Kontrolliere die Doppelsteine und berechne die offenen Enden."
    }

    // --- TROPHY ROOM SCREEN ---
    fun trophyRoomSub(): String = when (currentLanguage) {
        AppLanguage.ES -> "PREMIOS Y OBJETOS DE LUJO"
        AppLanguage.EN -> "LUXURY PRIZES & TROPHIES"
        AppLanguage.FR -> "PRIX ET OBJETS DE LUXE"
        AppLanguage.IT -> "PREMI E OGGETTI DI LUSSO"
        AppLanguage.PT -> "PRÊMIOS E ITENS DE LUXO"
        AppLanguage.DE -> "LUXUSPREISE & TROPHÄEN"
    }

    fun obtainedIn(city: String): String = when (currentLanguage) {
        AppLanguage.ES -> "Obtenido en: $city"
        AppLanguage.EN -> "Won in: $city"
        AppLanguage.FR -> "Obtenu à : $city"
        AppLanguage.IT -> "Ottenuto a: $city"
        AppLanguage.PT -> "Obtido em: $city"
        AppLanguage.DE -> "Gewonnen in: $city"
    }

    fun marketValue(v: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Valor de mercado: $$v"
        AppLanguage.EN -> "Market value: $$v"
        AppLanguage.FR -> "Valeur : $$v"
        AppLanguage.IT -> "Valore di mercato: $$v"
        AppLanguage.PT -> "Valor de mercado: $$v"
        AppLanguage.DE -> "Marktwert: $$v"
    }

    fun sellForBtn(v: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "VENDER (+$$v)"
        AppLanguage.EN -> "SELL (+$$v)"
        AppLanguage.FR -> "VENDRE (+$$v)"
        AppLanguage.IT -> "VENDI (+$$v)"
        AppLanguage.PT -> "VENDER (+$$v)"
        AppLanguage.DE -> "VERKAUFEN (+$$v)"
    }

    fun soldLabel(): String = when (currentLanguage) {
        AppLanguage.ES -> "VENDIDO"
        AppLanguage.EN -> "SOLD"
        AppLanguage.FR -> "VENDU"
        AppLanguage.IT -> "VENDUTO"
        AppLanguage.PT -> "VENDIDO"
        AppLanguage.DE -> "VERKAUFT"
    }

    // --- LOCALIZED CITY VENUES ---
    fun localizeVenue(venue: CityVenue): CityVenue {
        if (currentLanguage == AppLanguage.ES) return venue
        return when (venue.id) {
            "cuba" -> when (currentLanguage) {
                AppLanguage.EN -> venue.copy(
                    name = "Havana",
                    region = "Cuba • Malecón Social Club",
                    trophyTitle = "Imperial Cuban Cigar Box",
                    musicTitle = "Malecón Rhythm (Cuban Son)",
                    cityLore = "As golden sunset falls over Old Havana's Malecón, domino masters gather among classic 1950s convertibles and warm Caribbean breeze.",
                    rivalIntro = "Welcome to Havana! Right here by the Malecón, the tiles slam hard on mahogany. Let's see your game!"
                )
                AppLanguage.FR -> venue.copy(
                    name = "La Havane",
                    region = "Cuba • Club Social du Malecón",
                    trophyTitle = "Coffret Impérial de Havanes",
                    musicTitle = "Rythme du Malecón (Son Cubain)",
                    cityLore = "Au coucher du soleil sur le Malecón de La Havane, les maîtres du domino se réunissent entre voitures classiques et brise des Caraïbes.",
                    rivalIntro = "Bienvenue à La Havane ! Ici face au Malecón, les dominos claquent fort sur l'acajou. Voyons votre jeu !"
                )
                AppLanguage.IT -> venue.copy(
                    name = "L'Avana",
                    region = "Cuba • Club Sociale del Malecón",
                    trophyTitle = "Scatola Imperiale di Sigari",
                    musicTitle = "Ritmo del Malecón (Son Cubano)",
                    cityLore = "Al tramonto sul Malecón de L'Avana Vecchia, i maestri del domino si sfidano tra auto d'epoca anni '50 e brezza caraibica.",
                    rivalIntro = "Benvenuto a L'Avana! Qui sul Malecón le tessere battono forte sul mogano. Vediamo come giochi!"
                )
                AppLanguage.PT -> venue.copy(
                    name = "Havana",
                    region = "Cuba • Clube Social do Malecón",
                    trophyTitle = "Caixa Imperial de Charutos",
                    musicTitle = "Ritmo do Malecón (Son Cubano)",
                    cityLore = "Ao pôr do sol no Malecón de Havana Velha, os mestres do dominó se reúnem entre carros clássicos dos anos 50 e brisa caribenha.",
                    rivalIntro = "Bem-vindo a Havana! Aqui em frente ao Malecón as peças batem forte no mogno. Vamos ver o seu jogo!"
                )
                AppLanguage.DE -> venue.copy(
                    name = "Havanna",
                    region = "Kuba • Malecón Social Club",
                    trophyTitle = "Kaiserliche Zigarrenkiste",
                    musicTitle = "Malecón-Rhythmus (Son Cubano)",
                    cityLore = "Bei Sonnenuntergang am Malecón von Alt-Havanna treffen sich die Dominomeister zwischen 50er-Jahre-Oldtimern und Meeresbrise.",
                    rivalIntro = "Willkommen in Havanna! Hier am Malecón knallen die Steine hart auf das Mahagoniholz. Zeig dein Spiel!"
                )
                else -> venue
            }
            "mexico" -> when (currentLanguage) {
                AppLanguage.EN -> venue.copy(
                    name = "Mexico City",
                    region = "Mexico • Zócalo Royal Terrace",
                    trophyTitle = "Silver Aztec Medal",
                    musicTitle = "Garibaldi Son (Mariachi)",
                    cityLore = "Beneath colonial stone arches decorated with festive papel picado overlooking the Cathedral, All Fives matches demand sharp calculation.",
                    rivalIntro = "In Mexico City, multiples of five sing like mariachi! Score fast or get left behind."
                )
                AppLanguage.FR -> venue.copy(
                    name = "Mexico",
                    region = "Mexique • Terrasse Royale du Zócalo",
                    trophyTitle = "Médaille Aztèque d'Argent",
                    musicTitle = "Son de Garibaldi (Mariachi)",
                    cityLore = "Sous les arches coloniales décorées de papel picado face à la Cathédrale, les parties de multiples de 5 exigent une précision absolue.",
                    rivalIntro = "À Mexico, les multiples de cinq chantent comme les mariachis ! Comptez vite ou restez derrière."
                )
                AppLanguage.IT -> venue.copy(
                    name = "Città del Messico",
                    region = "Messico • Terrazza Reale dello Zócalo",
                    trophyTitle = "Medaglia Azteca d'Argento",
                    musicTitle = "Son di Garibaldi (Mariachi)",
                    cityLore = "Sotto gli archi coloniali illuminati di fronte alla Cattedrale, le sfide sui multipli di 5 richiedono calcolo millimetrico.",
                    rivalIntro = "A Città del Messico i multipli di cinque cantano come mariachi! Calcola veloce o resterai indietro."
                )
                AppLanguage.PT -> venue.copy(
                    name = "Cidade do México",
                    region = "México • Terraço Real do Zócalo",
                    trophyTitle = "Medalha Asteca de Prata",
                    musicTitle = "Son de Garibaldi (Mariachi)",
                    cityLore = "Sob os arcos coloniais iluminados em frente à Catedral, as partidas de múltiplos de 5 exigem cálculo rápido.",
                    rivalIntro = "Na Cidade do México os múltiplos de cinco cantam como mariachi! Some rápido para vencer."
                )
                AppLanguage.DE -> venue.copy(
                    name = "Mexiko-Stadt",
                    region = "Mexiko • Königliche Zócalo-Terrasse",
                    trophyTitle = "Silberne Azteken-Medaille",
                    musicTitle = "Klänge von Garibaldi (Mariachi)",
                    cityLore = "Unter kolonialen Steinbögen mit Blick auf die Kathedrale erfordern die All-Fives-Partien präzises Kopfrechnen.",
                    rivalIntro = "In Mexiko-Stadt zählen die Fünfer wie Mariachi-Musik! Rechne schnell, sonst bleibst du zurück."
                )
                else -> venue
            }
            "puerto_rico" -> when (currentLanguage) {
                AppLanguage.EN -> venue.copy(
                    name = "San Juan",
                    region = "Puerto Rico • El Morro Balcony",
                    trophyTitle = "Royal Caribbean Ring",
                    musicTitle = "Old San Juan Breeze",
                    cityLore = "Overlooking the turquoise bay and El Morro fortress along blue cobblestone streets, San Juan is famous for ruthless Block duels.",
                    rivalIntro = "In the cobblestone streets of Old San Juan we play pure Block dominoes. Nobody draws from the boneyard here!"
                )
                AppLanguage.FR -> venue.copy(
                    name = "San Juan",
                    region = "Porto Rico • Balcon d'El Morro",
                    trophyTitle = "Anneau Royal des Caraïbes",
                    musicTitle = "Brise du Vieux San Juan",
                    cityLore = "Surplombant la baie turquoise et la forteresse d'El Morro, le club de San Juan est célèbre pour ses duels de blocage sans merci.",
                    rivalIntro = "Dans les rues pavées du Vieux San Juan, nous jouons au blocage pur. Personne ne pioche ici !"
                )
                AppLanguage.IT -> venue.copy(
                    name = "San Juan",
                    region = "Porto Rico • Balcone di El Morro",
                    trophyTitle = "Anello Reale dei Caraibi",
                    musicTitle = "Brezza della Vecchia San Juan",
                    cityLore = "Con vista sulla baia turchese e sulla fortezza di El Morro, il club di San Juan è celebre per i suoi duelli di blocco.",
                    rivalIntro = "Nelle strade acciottolate di San Juan giochiamo al blocco puro. Qui nessuno pesca dal tallone!"
                )
                AppLanguage.PT -> venue.copy(
                    name = "San Juan",
                    region = "Porto Rico • Varanda de El Morro",
                    trophyTitle = "Anel Real do Caribe",
                    musicTitle = "Brisa de Velha San Juan",
                    cityLore = "Com vista para a baía turquesa e a fortaleza de El Morro, o clube de San Juan é famoso por seus duelos de bloqueio.",
                    rivalIntro = "Nas ruas de paralelepípedos de San Juan jogamos bloqueio puro. Aqui ninguém compra do monte!"
                )
                AppLanguage.DE -> venue.copy(
                    name = "San Juan",
                    region = "Puerto Rico • Balkon von El Morro",
                    trophyTitle = "Königlicher Karibik-Ring",
                    musicTitle = "Brise von Alt-San Juan",
                    cityLore = "Mit Blick auf die türkisfarbene Bucht und die Festung El Morro ist San Juan berühmt für kompromisslose Block-Duelle.",
                    rivalIntro = "In den Pflasterstraßen von Alt-San Juan spielen wir reines Block-Domino. Hier zieht niemand aus dem Stock!"
                )
                else -> venue
            }
            "new_orleans" -> when (currentLanguage) {
                AppLanguage.EN -> venue.copy(
                    name = "New Orleans",
                    region = "USA • French Quarter Jazz Club",
                    trophyTitle = "Golden Bourbon St. Saxophone",
                    musicTitle = "Bourbon Street Nocturne (Jazz)",
                    cityLore = "Among wrought-iron balconies and gas streetlamps reflecting on wet cobblestones, the jazz parlor hosts top Fives & Threes strategists.",
                    rivalIntro = "To the rhythm of Bourbon Street jazz we play Fives & Threes. Hit a multiple of 15 for double points!"
                )
                AppLanguage.FR -> venue.copy(
                    name = "La Nouvelle-Orléans",
                    region = "États-Unis • Club de Jazz du Vieux Carré",
                    trophyTitle = "Saxophone d'Or Bourbon St.",
                    musicTitle = "Nocturne de Bourbon Street (Jazz)",
                    cityLore = "Entre balcons en fer forgé et réverbères se reflétant sur les pavés, le club de jazz accueille les maîtres du 5 & 3.",
                    rivalIntro = "Au rythme du jazz de Bourbon Street, nous jouons au 5 & 3. Un multiple de 15 double vos points !"
                )
                AppLanguage.IT -> venue.copy(
                    name = "New Orleans",
                    region = "USA • French Quarter Jazz Club",
                    trophyTitle = "Sassofono d'Oro Bourbon St.",
                    musicTitle = "Bourbon Street Nocturne (Jazz)",
                    cityLore = "Tra balconi in ferro battuto e lampioni a gas sui ciottoli bagnati, il club jazz ospita i migliori strateghi di 5 & 3.",
                    rivalIntro = "Al ritmo del jazz di Bourbon Street giochiamo a Fives & Threes. Un multiplo di 15 vale doppio!"
                )
                AppLanguage.PT -> venue.copy(
                    name = "Nova Orleans",
                    region = "EUA • French Quarter Jazz Club",
                    trophyTitle = "Saxofone de Ouro Bourbon St.",
                    musicTitle = "Bourbon Street Nocturne (Jazz)",
                    cityLore = "Entre varandas de ferro forjado e postes de luz refletidos no calçamento, o clube de jazz reúne grandes estrategistas.",
                    rivalIntro = "Ao som do jazz na Bourbon Street jogamos Fives & Threes. Um múltiplo de 15 vale pontuação dupla!"
                )
                AppLanguage.DE -> venue.copy(
                    name = "New Orleans",
                    region = "USA • French Quarter Jazz Club",
                    trophyTitle = "Goldenes Bourbon-St.-Saxofon",
                    musicTitle = "Bourbon Street Nocturne (Jazz)",
                    cityLore = "Zwischen schmiedeeisernen Balkonen und Gaslaternen im French Quarter treffen sich die besten Fives-&-Threes-Strategen.",
                    rivalIntro = "Zum Rhythmus des Jazz spielen wir Fives & Threes. Ein Vielfaches von 15 bringt doppelte Punkte!"
                )
                else -> venue
            }
            "tokyo" -> when (currentLanguage) {
                AppLanguage.EN -> venue.copy(
                    name = "Tokyo",
                    region = "Japan • Asakusa Imperial Parlor",
                    trophyTitle = "Imperial Platinum Chronograph",
                    musicTitle = "Asakusa Red Lanterns (Tokyo)",
                    cityLore = "In a traditional wooden alleyway glowing with red paper lanterns and cherry blossoms, silence is broken only by the crisp clack of ivory.",
                    rivalIntro = "Beneath the red lanterns of Asakusa, each master guards their branch. If you get blocked, I will claim your end."
                )
                AppLanguage.FR -> venue.copy(
                    name = "Tokyo",
                    region = "Japon • Salon Impérial d'Asakusa",
                    trophyTitle = "Chronographe Impérial en Platine",
                    musicTitle = "Lanternes Rouges d'Asakusa (Tokyo)",
                    cityLore = "Dans une ruelle traditionnelle illuminée de lanternes rouges et de fleurs de cerisier, seul le claquement de l'ivoire brise le silence.",
                    rivalIntro = "Sous les lanternes d'Asakusa, chaque maître protège sa branche. Si vous bloquez, je prends votre extrémité."
                )
                AppLanguage.IT -> venue.copy(
                    name = "Tokyo",
                    region = "Giappone • Salone Imperiale Asakusa",
                    trophyTitle = "Cronografo Imperiale in Platino",
                    musicTitle = "Lanterne Rosse di Asakusa (Tokyo)",
                    cityLore = "In un vicolo tradizionale illuminato da lanterne rosse e petali di ciliegio, il silenzio è rotto solo dal tocco dell'avorio.",
                    rivalIntro = "Sotto le lanterne rosse di Asakusa ogni maestro protegge il suo ramo. Se ti blocchi, conquisterò il tuo lato."
                )
                AppLanguage.PT -> venue.copy(
                    name = "Tóquio",
                    region = "Japão • Salão Imperial Asakusa",
                    trophyTitle = "Cronógrafo Imperial de Platina",
                    musicTitle = "Lanternas de Asakusa (Tóquio)",
                    cityLore = "Em uma viela tradicional iluminada por lanternas vermelhas e pétalas de cerejeira, ouve-se apenas o toque seco do marfim.",
                    rivalIntro = "Sob as lanternas de Asakusa cada mestre protege sua ponta. Se você travar, conquistarei o seu lado."
                )
                AppLanguage.DE -> venue.copy(
                    name = "Tokio",
                    region = "Japan • Kaiserlicher Salon Asakusa",
                    trophyTitle = "Kaiserlicher Platin-Chronograph",
                    musicTitle = "Rote Laternen von Asakusa (Tokio)",
                    cityLore = "In einer traditionellen Gasse mit roten Papierlaternen und Kirschblüten durchbricht nur das Klacken des Elfenbeins die Stille.",
                    rivalIntro = "Unter den Laternen von Asakusa schützt jeder Meister seinen Zweig. Wenn du passt, erobere ich dein Ende."
                )
                else -> venue
            }
            "monte_carlo" -> when (currentLanguage) {
                AppLanguage.EN -> venue.copy(
                    name = "Monte Carlo",
                    region = "Monaco • Riviera Salle Impériale",
                    trophyTitle = "Riviera GT Sports Car",
                    musicTitle = "Grand Casino Waltz (Monaco)",
                    cityLore = "Facing the superyacht harbor under the Mediterranean moon, the Grand Casino's private salon hosts Europe's elite players.",
                    rivalIntro = "In Monte Carlo we play Hungarian Dominoes: chain as many consecutive valid tiles as you can in a single turn."
                )
                AppLanguage.FR -> venue.copy(
                    name = "Monte-Carlo",
                    region = "Monaco • Salle Impériale de la Riviera",
                    trophyTitle = "Voiture de Sport Riviera GT",
                    musicTitle = "Valse du Grand Casino (Monaco)",
                    cityLore = "Face au port de yachts illuminé sous la lune méditerranéenne, le salon privé du Grand Casino réunit l'élite européenne.",
                    rivalIntro = "À Monte-Carlo nous jouons au Domino Hongrois : enchaînez autant de dominos consécutifs que possible en un tour."
                )
                AppLanguage.IT -> venue.copy(
                    name = "Montecarlo",
                    region = "Monaco • Salle Impériale della Riviera",
                    trophyTitle = "Auto Sportiva Riviera GT",
                    musicTitle = "Valzer del Gran Casinò (Monaco)",
                    cityLore = "Di fronte al porto degli yacht illuminato sotto la luna del Mediterraneo, il salone del Gran Casinò riunisce l'élite europea.",
                    rivalIntro = "A Montecarlo giochiamo al Domino Ungherese: concatena tutte le tessere consecutive che puoi in un solo turno."
                )
                AppLanguage.PT -> venue.copy(
                    name = "Monte Carlo",
                    region = "Mônaco • Salle Impériale da Riviera",
                    trophyTitle = "Esportivo Riviera GT",
                    musicTitle = "Valsa do Grande Cassino (Mônaco)",
                    cityLore = "Em frente ao porto de iates iluminado sob a lua do Mediterrâneo, o salão privado reúne a elite europeia.",
                    rivalIntro = "Em Monte Carlo jogamos Dominó Húngaro: encadeie todas as peças consecutivas que puder em um só turno."
                )
                AppLanguage.DE -> venue.copy(
                    name = "Monte Carlo",
                    region = "Monaco • Salle Impériale der Riviera",
                    trophyTitle = "Riviera GT Sportwagen",
                    musicTitle = "Walzer des Grand Casino (Monaco)",
                    cityLore = "Gegenüber dem beleuchteten Jachthafen unter dem Mittelmeermond versammelt der Salon des Grand Casino Europas Elite.",
                    rivalIntro = "In Monte Carlo spielen wir Ungarisches Domino: Lege so viele passende Steine hintereinander an wie möglich."
                )
                else -> venue
            }
            "las_vegas" -> when (currentLanguage) {
                AppLanguage.EN -> venue.copy(
                    name = "Las Vegas",
                    region = "USA • World Grand Final on the Strip",
                    trophyTitle = "Domino Imperial World Cup",
                    musicTitle = "Strip Grand Finale (Showtime)",
                    cityLore = "Surrounded by golden fountains and searchlights across the Nevada night sky, the presidential table crowns the World Champion.",
                    rivalIntro = "You reached the Las Vegas Grand Final. The rule of 7 and the 4 Matadors will decide the World Champion!"
                )
                AppLanguage.FR -> venue.copy(
                    name = "Las Vegas",
                    region = "États-Unis • Grande Finale Mondiale sur le Strip",
                    trophyTitle = "Coupe du Monde Domino Imperial",
                    musicTitle = "Grande Finale du Strip (Showtime)",
                    cityLore = "Entre fontaines dorées et projecteurs illuminant le ciel du Nevada, la table présidentielle couronne le Champion du Monde.",
                    rivalIntro = "Vous voici en Grande Finale à Las Vegas. La règle du 7 et les 4 Matadors décideront du Champion du Monde !"
                )
                AppLanguage.IT -> venue.copy(
                    name = "Las Vegas",
                    region = "USA • Gran Finale Mondiale sullo Strip",
                    trophyTitle = "Coppa del Mondo Domino Imperial",
                    musicTitle = "Gran Finale dello Strip (Showtime)",
                    cityLore = "Tra fontane dorate e riflettori nel cielo del Nevada, il tavolo presidenziale incorona il Campione del Mondo.",
                    rivalIntro = "Sei arrivato alla Gran Finale di Las Vegas. La regola del 7 e i 4 Matador decideranno il Campione del Mondo!"
                )
                AppLanguage.PT -> venue.copy(
                    name = "Las Vegas",
                    region = "EUA • Grande Final Mundial na Strip",
                    trophyTitle = "Copa Mundial Domino Imperial",
                    musicTitle = "Grande Final da Strip (Showtime)",
                    cityLore = "Entre fontes douradas e holofotes no céu de Nevada, a mesa presidencial coroa o Campeão Mundial absoluto.",
                    rivalIntro = "Você chegou à Grande Final de Las Vegas. A regra do 7 e os 4 Matadores decidirão o Campeão do Mundo!"
                )
                AppLanguage.DE -> venue.copy(
                    name = "Las Vegas",
                    region = "USA • Weltfinale am Las Vegas Strip",
                    trophyTitle = "Domino Imperial Weltpokal",
                    musicTitle = "Großes Finale am Strip (Showtime)",
                    cityLore = "Zwischen goldenen Fontänen und Scheinwerfern am Nachthimmel von Nevada krönt der Präsidententisch den Weltmeister.",
                    rivalIntro = "Du hast das große Finale in Las Vegas erreicht. Die Siebener-Regel und die 4 Matadore entscheiden über den Weltmeister!"
                )
                else -> venue
            }
            else -> venue
        }
    }
}

@Composable
fun LanguageSelectorButton(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val lang = DominoStrings.currentLanguage

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkNavy.copy(alpha = 0.90f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
        modifier = modifier.clickable {
            DominoSoundManager.playTileTap()
            showDialog = true
        }
    ) {
        Text(
            text = "${lang.flag} ${lang.shortLabel}",
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkNavy,
                border = androidx.compose.foundation.BorderStroke(2.dp, GoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DominoStrings.selectLanguageTitle(),
                        color = GoldAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AppLanguage.entries.forEach { option ->
                        val isSelected = option == DominoStrings.currentLanguage
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GoldAccent else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.White else GoldAccent.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    DominoSoundManager.playTileTap()
                                    DominoStrings.setLanguage(option)
                                    showDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${option.flag}  ${option.displayName}",
                                    color = if (isSelected) DarkNavy else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = DarkNavy,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = { showDialog = false }) {
                        Text(DominoStrings.closeLabel(), color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
