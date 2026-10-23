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
import com.example.dominograndprix.model.BoardSide
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
        AppLanguage.ES -> "⚡ PARTIDA RÁPIDA (8 MODALIDADES)"
        AppLanguage.EN -> "⚡ QUICK PLAY (8 GAME MODES)"
        AppLanguage.FR -> "⚡ PARTIE RAPIDE (8 MODES)"
        AppLanguage.IT -> "⚡ PARTITA RAPIDA (8 MODALITÀ)"
        AppLanguage.PT -> "⚡ PARTIDA RÁPIDA (8 MODALIDADES)"
        AppLanguage.DE -> "⚡ SCHNELLSPIEL (8 MODI)"
    }

    fun quickPlayBtnSub(): String = when (currentLanguage) {
        AppLanguage.ES -> "Juega con Dominó de Cinco, Matador, Húngaro, Tradicional y más"
        AppLanguage.EN -> "Play All Fives, Matador, Hungarian, Classic Draw and more"
        AppLanguage.FR -> "Jouez à All Fives, Matador, Hongrois, Traditionnel et plus"
        AppLanguage.IT -> "Gioca a All Fives, Matador, Ungherese, Classico e altro"
        AppLanguage.PT -> "Jogue Dominó de Cinco, Matador, Húngaro, Clássico e mais"
        AppLanguage.DE -> "Spiele All Fives, Matador, Ungarisch, Klassisch und mehr"
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

    fun openEndsBadge(left: Int?, right: Int?, up: Int? = null, down: Int? = null): String {
        val extra = buildString {
            if (up != null) append(" [${up}]")
            if (down != null) append(" [${down}]")
        }
        return when (currentLanguage) {
            AppLanguage.ES -> "Extremos: [${left ?: 0}], [${right ?: 0}]$extra"
            AppLanguage.EN -> "Open Ends: [${left ?: 0}], [${right ?: 0}]$extra"
            AppLanguage.FR -> "Extrémités : [${left ?: 0}], [${right ?: 0}]$extra"
            AppLanguage.IT -> "Estremi: [${left ?: 0}], [${right ?: 0}]$extra"
            AppLanguage.PT -> "Pontas: [${left ?: 0}], [${right ?: 0}]$extra"
            AppLanguage.DE -> "Enden: [${left ?: 0}], [${right ?: 0}]$extra"
        }
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
        return endChoiceBtn(if (isLeft) BoardSide.LEFT else BoardSide.RIGHT, pip)
    }

    fun endChoiceBtn(side: BoardSide, pip: Int?): String {
        val word = when (currentLanguage) {
            AppLanguage.ES -> "EXTREMO"
            AppLanguage.EN -> "END"
            AppLanguage.FR -> "EXTRÉMITÉ"
            AppLanguage.IT -> "ESTREMO"
            AppLanguage.PT -> "PONTA"
            AppLanguage.DE -> "ENDE"
        }
        val p = pip ?: 0
        return when (side) {
            BoardSide.LEFT -> "⬅ $word [$p]"
            BoardSide.RIGHT -> "$word [$p] ➡"
            BoardSide.UP -> "⬆ $word [$p]"
            BoardSide.DOWN -> "⬇ $word [$p]"
        }
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

    // --- ACCESIBILIDAD ---
    /**
     * Nombre hablado de una ficha, para lectores de pantalla.
     *
     * Se deletrea el numero ("seis tres") en lugar de leer "6 3", porque los lectores leen los
     * digitos de uno en uno y suena a "sesenta y tres" o a una fecha. Los numeros del domino
     * son valores, no cifras.
     */
    fun tileDescription(high: Int, low: Int): String {
        val a = spokenPip(high)
        val b = spokenPip(low)
        return when (currentLanguage) {
            AppLanguage.ES -> if (high == low) "Ficha doble $a" else "Ficha $a $b"
            AppLanguage.EN -> if (high == low) "Double $a tile" else "Tile $a $b"
            AppLanguage.FR -> if (high == low) "Domino double $a" else "Domino $a $b"
            AppLanguage.IT -> if (high == low) "Tessera doppio $a" else "Tessera $a $b"
            AppLanguage.PT -> if (high == low) "Peça dupla $a" else "Peça $a $b"
            AppLanguage.DE -> if (high == low) "Doppel-$a-Stein" else "Stein $a $b"
        }
    }

    /** Como se dice cada numero en el idioma activo. */
    private fun spokenPip(pip: Int): String {
        val words = when (currentLanguage) {
            AppLanguage.ES -> listOf("blanco", "uno", "dos", "tres", "cuatro", "cinco", "seis")
            AppLanguage.EN -> listOf("blank", "one", "two", "three", "four", "five", "six")
            AppLanguage.FR -> listOf("blanc", "un", "deux", "trois", "quatre", "cinq", "six")
            AppLanguage.IT -> listOf("bianco", "uno", "due", "tre", "quattro", "cinque", "sei")
            AppLanguage.PT -> listOf("branco", "um", "dois", "três", "quatro", "cinco", "seis")
            AppLanguage.DE -> listOf("blank", "eins", "zwei", "drei", "vier", "fünf", "sechs")
        }
        return words.getOrElse(pip) { pip.toString() }
    }

    fun playableLabel(): String = when (currentLanguage) {
        AppLanguage.ES -> "jugable"
        AppLanguage.EN -> "playable"
        AppLanguage.FR -> "jouable"
        AppLanguage.IT -> "giocabile"
        AppLanguage.PT -> "jogável"
        AppLanguage.DE -> "spielbar"
    }

    fun unplayableLabel(): String = when (currentLanguage) {
        AppLanguage.ES -> "no jugable"
        AppLanguage.EN -> "not playable"
        AppLanguage.FR -> "non jouable"
        AppLanguage.IT -> "non giocabile"
        AppLanguage.PT -> "não jogável"
        AppLanguage.DE -> "nicht spielbar"
    }

    // ---------------------------------------------------------------------------------------
    // Textos de la mesa, el contador de fichas y el resumen de ronda.
    //
    // Hasta la v2.3.0 estos textos estaban escritos en espanol directamente en la UI, asi que
    // la app "traducida" seguia mostrando espanol. Todos pasan por aqui.
    // ---------------------------------------------------------------------------------------

    private fun multi(es: String, en: String, fr: String, it: String, pt: String, de: String): String =
        when (currentLanguage) {
            AppLanguage.ES -> es
            AppLanguage.EN -> en
            AppLanguage.FR -> fr
            AppLanguage.IT -> it
            AppLanguage.PT -> pt
            AppLanguage.DE -> de
        }

    fun hudYou(): String = multi("TÚ", "YOU", "VOUS", "TU", "VOCÊ", "DU")

    fun hudRival(): String = multi("RIVAL", "RIVAL", "RIVAL", "RIVALE", "RIVAL", "GEGNER")

    fun targetLabel(): String = multi("Meta", "Target", "Objectif", "Obiettivo", "Meta", "Ziel")

    fun countTilesLabel(): String = multi("CONTAR", "COUNT", "COMPTER", "CONTARE", "CONTAR", "ZÄHLEN")

    fun passedTo(pips: String): String = multi(
        "Pasó al $pips",
        "Passed on $pips",
        "A passé sur $pips",
        "Ha passato $pips",
        "Passou em $pips",
        "Hat $pips gepasst"
    )

    fun youPassedTo(pips: String): String = multi(
        "Pasaste al $pips",
        "You passed on $pips",
        "Vous avez passé sur $pips",
        "Hai passato $pips",
        "Você passou em $pips",
        "Du hast $pips gepasst"
    )

    /** Etiqueta corta del chip de fallas en la ficha del rival. */
    fun passedChip(): String = multi("Pasó:", "Passed:", "Passé :", "Passato:", "Passou:", "Gepasst:")

    fun pointsAnnouncement(points: Int, who: String): String = multi(
        "+$points pts ($who)",
        "+$points pts ($who)",
        "+$points pts ($who)",
        "+$points pt ($who)",
        "+$points pts ($who)",
        "+$points Pkt. ($who)"
    )

    fun dominoAnnouncement(name: String): String = multi(
        "¡$name cerró la mano! (Dominó)",
        "$name closed the hand! (Domino)",
        "$name a fermé la main ! (Domino)",
        "$name ha chiuso la mano! (Domino)",
        "$name fechou a mão! (Dominó)",
        "$name hat die Hand geschlossen! (Domino)"
    )

    fun chooseEndTitle(high: Int, low: Int): String = multi(
        "¿En qué extremo colocas [$high|$low]?",
        "Where do you place [$high|$low]?",
        "Où placez-vous [$high|$low] ?",
        "Dove metti [$high|$low]?",
        "Onde coloca [$high|$low]?",
        "Wohin legst du [$high|$low]?"
    )

    // --- Contador de fichas ---

    fun pipCounterTitle(): String =
        multi("CONTADOR DE FICHAS", "TILE COUNTER", "COMPTEUR DE PIÈCES", "CONTATORE PEDINE", "CONTADOR DE PEÇAS", "STEINZÄHLER")

    fun pipCounterSubtitle(): String = multi(
        "7 fichas totales por número (0 al 6)",
        "7 tiles per number (0 to 6)",
        "7 pièces par numéro (0 à 6)",
        "7 pedine per numero (0 a 6)",
        "7 peças por número (0 a 6)",
        "7 Steine pro Zahl (0 bis 6)"
    )

    fun colNumber(): String = multi("NÚMERO", "NUMBER", "NUMÉRO", "NUMERO", "NÚMERO", "ZAHL")

    fun colOnBoard(): String =
        multi("EN MESA", "ON TABLE", "SUR TABLE", "SUL TAVOLO", "NA MESA", "AUF DEM TISCH")

    fun colInHand(): String = multi("TU MANO", "YOUR HAND", "TA MAIN", "TUA MANO", "SUA MÃO", "DEINE HAND")

    fun colRemaining(): String = multi("RESTAN", "LEFT", "RESTENT", "RIMANGONO", "RESTAM", "ÜBRIG")

    fun exhaustedLabel(): String =
        multi("AGOTADO", "EXHAUSTED", "ÉPUISÉ", "ESAURITO", "ESGOTADO", "ERSCHÖPFT")

    // --- Resumen de ronda ---

    fun summaryChallengeWon(): String = multi(
        "¡VICTORIA EN EL RETO!", "CHALLENGE WON!", "DÉFI REMPORTÉ !", "SFIDA VINTA!",
        "DESAFIO VENCIDO!", "HERAUSFORDERUNG GEWONNEN!"
    )

    fun summaryChallengeLost(): String = multi(
        "DERROTA EN EL RETO", "CHALLENGE LOST", "DÉFI PERDU", "SFIDA PERSA",
        "DESAFIO PERDIDO", "HERAUSFORDERUNG VERLOREN"
    )

    fun summaryHandWon(): String =
        multi("¡MANO GANADA!", "HAND WON!", "MANCHE GAGNÉE !", "MANO VINTA!", "MÃO VENCIDA!", "RUNDE GEWONNEN!")

    fun summaryHandLost(): String =
        multi("MANO PERDIDA", "HAND LOST", "MANCHE PERDUE", "MANO PERSA", "MÃO PERDIDA", "RUNDE VERLOREN")

    fun summaryClosedDomino(name: String): String = multi(
        "$name cerró la mano con ¡Dominó!",
        "$name closed the hand with a domino!",
        "$name a fermé la main avec un domino !",
        "$name ha chiuso la mano con un domino!",
        "$name fechou a mão com um dominó!",
        "$name hat mit einem Domino geschlossen!"
    )

    fun summaryReachedTarget(name: String, target: Int): String = multi(
        "$name alcanzó la meta de $target puntos",
        "$name reached the $target-point target",
        "$name a atteint l'objectif de $target points",
        "$name ha raggiunto l'obiettivo di $target punti",
        "$name alcançou a meta de $target pontos",
        "$name hat das Ziel von $target Punkten erreicht"
    )

    fun summaryBlocked(name: String): String = multi(
        "Mesa trancada · gana $name por menor conteo",
        "Board blocked · $name wins with fewer pips",
        "Table bloquée · $name gagne avec moins de points",
        "Tavolo bloccato · $name vince con meno punti",
        "Mesa travada · $name vence com menos pontos",
        "Tisch blockiert · $name gewinnt mit weniger Augen"
    )

    fun summaryHandsTitle(): String = multi(
        "FICHAS EN MANO AL CERRARSE LA RONDA",
        "TILES LEFT IN HAND AT ROUND END",
        "PIÈCES EN MAIN À LA FIN DE LA MANCHE",
        "PEDINE IN MANO A FINE MANO",
        "PEÇAS NA MÃO NO FIM DA RODADA",
        "STEINE AUF DER HAND BEIM RUNDENENDE"
    )

    fun summaryBreakdownTitle(): String = multi(
        "DESGLOSE DE PUNTOS SUMADOS",
        "POINTS BREAKDOWN",
        "DÉTAIL DES POINTS",
        "DETTAGLIO PUNTI",
        "DETALHE DOS PONTOS",
        "PUNKTEAUFSCHLÜSSELUNG"
    )

    fun summaryRivalPips(): String = multi(
        "Suma de fichas rivales:",
        "Opponents' pips:",
        "Points des adversaires :",
        "Punti degli avversari:",
        "Pontos dos adversários:",
        "Augen der Gegner:"
    )

    fun summaryTablePoints(): String = multi(
        "Puntos de mesa de la ronda:",
        "Table points this round:",
        "Points de table de la manche :",
        "Punti da tavolo della mano:",
        "Pontos de mesa da rodada:",
        "Tischpunkte dieser Runde:"
    )

    fun summaryTotalTo(name: String): String = multi(
        "Total sumado a $name:",
        "Total added to $name:",
        "Total ajouté à $name :",
        "Totale aggiunto a $name:",
        "Total somado a $name:",
        "Gesamt für $name:"
    )

    fun summaryStandings(target: Int): String = multi(
        "MARCADOR ACUMULADO (OBJETIVO: $target PTS)",
        "RUNNING SCORE (TARGET: $target PTS)",
        "SCORE CUMULÉ (OBJECTIF : $target PTS)",
        "PUNTEGGIO (OBIETTIVO: $target PT)",
        "PLACAR (META: $target PTS)",
        "SPIELSTAND (ZIEL: $target Pkt.)"
    )

    fun summaryTeamYou(): String = multi("Equipo Tú:", "Your team:", "Votre équipe :", "Tua squadra:", "Sua equipe:", "Dein Team:")

    fun summaryTeamRival(): String =
        multi("Equipo Rival:", "Rival team:", "Équipe adverse :", "Squadra rivale:", "Equipe rival:", "Gegnerteam:")

    fun summaryEmptyHand(): String = multi("¡Sin fichas! (0 pts)", "No tiles left! (0 pts)", "Plus de pièces ! (0 pts)", "Nessuna pedina! (0 pt)", "Sem peças! (0 pts)", "Keine Steine! (0 Pkt.)")

    fun summaryCountsForWinner(points: Int): String = multi(
        "Suma al ganador: +$points pts",
        "Adds to winner: +$points pts",
        "Ajouté au gagnant : +$points pts",
        "Somma al vincitore: +$points pt",
        "Soma ao vencedor: +$points pts",
        "Zählt für den Sieger: +$points Pkt."
    )

    fun summaryNextRound(): String =
        multi("SIGUIENTE RONDA ➔", "NEXT ROUND ➔", "MANCHE SUIVANTE ➔", "MANO SUCCESSIVA ➔", "PRÓXIMA RODADA ➔", "NÄCHSTE RUNDE ➔")

    fun summaryNextChallenge(): String = multi(
        "SIGUIENTE RETO ➔", "NEXT CHALLENGE ➔", "DÉFI SUIVANT ➔", "SFIDA SUCCESSIVA ➔",
        "PRÓXIMO DESAFIO ➔", "NÄCHSTE HERAUSFORDERUNG ➔"
    )

    fun summaryRetry(): String = multi(
        "REINTENTAR RETO", "RETRY CHALLENGE", "RÉESSAYER LE DÉFI", "RIPROVA SFIDA",
        "TENTAR DE NOVO", "HERAUSFORDERUNG WIEDERHOLEN"
    )

    fun summaryBackToMenu(): String =
        multi("VOLVER AL MENÚ", "BACK TO MENU", "RETOUR AU MENU", "TORNA AL MENU", "VOLTAR AO MENU", "ZURÜCK ZUM MENÜ")

    fun summaryExit(): String = multi("SALIR", "EXIT", "QUITTER", "ESCI", "SAIR", "VERLASSEN")

    fun bonusCapicua(): String = multi("✨ CAPICÚA ✨", "✨ CAPICUA ✨", "✨ CAPICUA ✨", "✨ CAPICUA ✨", "✨ CAPICUA ✨", "✨ CAPICUA ✨")

    fun bonusDoubleCapicua(): String = multi(
        "🌟 DOBLE CAPICÚA 🌟", "🌟 DOUBLE CAPICUA 🌟", "🌟 DOUBLE CAPICUA 🌟",
        "🌟 DOPPIA CAPICUA 🌟", "🌟 CAPICUA DUPLA 🌟", "🌟 DOPPEL-CAPICUA 🌟"
    )

    fun bonusDominoWithDouble(): String = multi(
        "🎯 DOMINÓ CON DOBLE 🎯", "🎯 DOMINO WITH DOUBLE 🎯", "🎯 DOMINO AVEC DOUBLE 🎯",
        "🎯 DOMINO CON DOPPIA 🎯", "🎯 DOMINÓ COM DUPLA 🎯", "🎯 DOMINO MIT DOPPEL 🎯"
    )

    fun quickPlayButton(): String =
        multi("⚡ PARTIDA RÁPIDA", "⚡ QUICK PLAY", "⚡ PARTIE RAPIDE", "⚡ PARTITA RAPIDA", "⚡ PARTIDA RÁPIDA", "⚡ SCHNELLSPIEL")

    fun quickPlaySubtitle(): String = multi(
        "• 8 Modalidades Libres",
        "• 8 free modes",
        "• 8 modes libres",
        "• 8 modalità libere",
        "• 8 modalidades livres",
        "• 8 freie Modi"
    )

    fun quickPlayModeSection(): String = multi("6. MODO", "6. MODE", "6. MODE", "6. MODO", "6. MODO", "6. MODUS")

    fun quickPlayOneHuman(): String = multi(
        "1 humano + IA",
        "1 human + AI",
        "1 humain + IA",
        "1 umano + IA",
        "1 humano + IA",
        "1 Mensch + KI"
    )

    fun quickPlayTwoHumans(): String = multi(
        "2 humanos (pasa el móvil)",
        "2 humans (pass the phone)",
        "2 humains (passe le téléphone)",
        "2 umani (passa il telefono)",
        "2 humanos (passe o celular)",
        "2 Menschen (Handy weitergeben)"
    )

    // --- Mesa: deshacer y pista ---

    fun undoLabel(): String = multi("Deshacer", "Undo", "Annuler", "Annulla", "Desfazer", "Rückgängig")

    fun hintLabel(): String = multi("Pista", "Hint", "Indice", "Suggerimento", "Dica", "Tipp")

    fun hintPlaySuggestion(tile: String, side: String): String = multi(
        "Prueba $tile $side",
        "Try $tile $side",
        "Essayez $tile $side",
        "Prova $tile $side",
        "Tente $tile $side",
        "Versuche $tile $side"
    )

    fun hintNoMoves(): String = multi(
        "No hay jugada posible",
        "No playable move",
        "Aucun coup possible",
        "Nessuna mossa possibile",
        "Nenhuma jogada possível",
        "Kein Zug möglich"
    )

    fun sideName(side: BoardSide): String = when (side) {
        BoardSide.LEFT -> multi("a la izquierda", "on the left", "à gauche", "a sinistra", "à esquerda", "links")
        BoardSide.RIGHT -> multi("a la derecha", "on the right", "à droite", "a destra", "à direita", "rechts")
        BoardSide.UP -> multi("arriba", "above", "en haut", "in alto", "em cima", "oben")
        BoardSide.DOWN -> multi("abajo", "below", "en bas", "in basso", "embaixo", "unten")
    }

    // --- Ajustes ---

    fun settingsTitle(): String = multi("AJUSTES", "SETTINGS", "RÉGLAGES", "IMPOSTAZIONI", "AJUSTES", "EINSTELLUNGEN")

    fun settingsSound(): String = multi("Sonido", "Sound", "Son", "Audio", "Som", "Ton")

    fun settingsHaptics(): String = multi("Vibración", "Haptics", "Vibration", "Vibrazione", "Vibração", "Vibration")

    fun settingsOn(): String = multi("Activado", "On", "Activé", "Attivo", "Ativado", "An")

    fun settingsOff(): String = multi("Desactivado", "Off", "Désactivé", "Disattivato", "Desativado", "Aus")

    fun settingsLanguage(): String = multi("Idioma", "Language", "Langue", "Lingua", "Idioma", "Sprache")

    fun settingsClose(): String = multi("CERRAR", "CLOSE", "FERMER", "CHIUDI", "FECHAR", "SCHLIESSEN")

    // --- Desafío diario: cabecera y resultados ---

    fun dailyStreakLabel(streak: Int): String = multi(
        "Racha: $streak días",
        "Streak: $streak days",
        "Série : $streak jours",
        "Serie: $streak giorni",
        "Sequência: $streak dias",
        "Serie: $streak Tage"
    )

    /** Que ocurre al pulsar una ficha jugable (lo anuncia el lector de pantalla). */
    fun tileClickAction(): String = when (currentLanguage) {
        AppLanguage.ES -> "jugar esta ficha"
        AppLanguage.EN -> "play this tile"
        AppLanguage.FR -> "jouer ce domino"
        AppLanguage.IT -> "giocare questa tessera"
        AppLanguage.PT -> "jogar esta peça"
        AppLanguage.DE -> "diesen Stein spielen"
    }

    // --- DESAFÍO DIARIO ---
    /** Nombre del juego para el icono de la pantalla de inicio. Es marca: no se traduce. */
    fun appIconLabel(): String = "Domino Imperial"
    fun dailyChallengeTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "🎯 DESAFÍO DIARIO"
        AppLanguage.EN -> "🎯 DAILY CHALLENGE"
        AppLanguage.FR -> "🎯 DÉFI QUOTIDIEN"
        AppLanguage.IT -> "🎯 SFIDA GIORNALIERA"
        AppLanguage.PT -> "🎯 DESAFIO DIÁRIO"
        AppLanguage.DE -> "🎯 TÄGLICHE HERAUSFORDERUNG"
    }

    /** Detalle del reto de hoy: numero de reto, modalidad y objetivo. */
    fun dailyChallengeSub(label: String, modeTitle: String, target: Int): String =
        when (currentLanguage) {
            AppLanguage.ES -> "$label • $modeTitle a $target puntos"
            AppLanguage.EN -> "$label • $modeTitle to $target points"
            AppLanguage.FR -> "$label • $modeTitle à $target points"
            AppLanguage.IT -> "$label • $modeTitle a $target punti"
            AppLanguage.PT -> "$label • $modeTitle até $target pontos"
            AppLanguage.DE -> "$label • $modeTitle bis $target Punkte"
        }

    fun dailyPlayBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "JUGAR EL RETO DE HOY"
        AppLanguage.EN -> "PLAY TODAY'S CHALLENGE"
        AppLanguage.FR -> "JOUER LE DÉFI DU JOUR"
        AppLanguage.IT -> "GIOCA LA SFIDA DI OGGI"
        AppLanguage.PT -> "JOGAR O DESAFIO DE HOJE"
        AppLanguage.DE -> "HEUTIGE HERAUSFORDERUNG SPIELEN"
    }

    fun dailyAlreadyPlayed(): String = when (currentLanguage) {
        AppLanguage.ES -> "Ya has jugado el reto de hoy"
        AppLanguage.EN -> "You already played today's challenge"
        AppLanguage.FR -> "Vous avez déjà joué le défi du jour"
        AppLanguage.IT -> "Hai già giocato la sfida di oggi"
        AppLanguage.PT -> "Você já jogou o desafio de hoje"
        AppLanguage.DE -> "Du hast die heutige Herausforderung schon gespielt"
    }

    /** Resultado de hoy: ganado o perdido, puntos y racha. */
    fun dailyResultSummary(won: Boolean, points: Int, streak: Int): String = when (currentLanguage) {
        AppLanguage.ES -> if (won) "¡Ganado! $points pts • Racha: $streak" else "Perdido • Racha rota"
        AppLanguage.EN -> if (won) "Won! $points pts • Streak: $streak" else "Lost • Streak broken"
        AppLanguage.FR -> if (won) "Gagné ! $points pts • Série : $streak" else "Perdu • Série brisée"
        AppLanguage.IT -> if (won) "Vinto! $points pt • Striscia: $streak" else "Perso • Striscia interrotta"
        AppLanguage.PT -> if (won) "Ganhou! $points pts • Sequência: $streak" else "Perdeu • Sequência quebrada"
        AppLanguage.DE -> if (won) "Gewonnen! $points Pkt • Serie: $streak" else "Verloren • Serie gerissen"
    }

    fun dailyComeBackTomorrow(): String = when (currentLanguage) {
        AppLanguage.ES -> "Vuelve mañana para un reto nuevo"
        AppLanguage.EN -> "Come back tomorrow for a new challenge"
        AppLanguage.FR -> "Revenez demain pour un nouveau défi"
        AppLanguage.IT -> "Torna domani per una nuova sfida"
        AppLanguage.PT -> "Volte amanhã para um novo desafio"
        AppLanguage.DE -> "Komm morgen für eine neue Herausforderung wieder"
    }

    // --- ECONOMÍA Y QUIEBRA ---
    /** Aviso en el boton de viajar cuando no se puede pagar la entrada. */
    fun insufficientFunds(required: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Fondos insuficientes (requiere \$$required)"
        AppLanguage.EN -> "Not enough funds (requires \$$required)"
        AppLanguage.FR -> "Fonds insuffisants (requiert \$$required)"
        AppLanguage.IT -> "Fondi insufficienti (richiede \$$required)"
        AppLanguage.PT -> "Fundos insuficientes (requer \$$required)"
        AppLanguage.DE -> "Nicht genug Guthaben (erfordert \$$required)"
    }

    fun bankruptcyTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "PRÉSTAMO DE HONOR"
        AppLanguage.EN -> "LOAN OF HONOUR"
        AppLanguage.FR -> "PRÊT D'HONNEUR"
        AppLanguage.IT -> "PRESTITO D'ONORE"
        AppLanguage.PT -> "EMPRÉSTIMO DE HONRA"
        AppLanguage.DE -> "EHRENDARLEHEN"
    }

    fun bankruptcyBody(amount: Int): String = when (currentLanguage) {
        AppLanguage.ES ->
            "El Club te concede un préstamo de honor de \$$amount para volver al ruedo. " +
                "Es la única vez que puede ayudarte: a partir de ahora tendrás que recuperarte " +
                "en las sedes más baratas."
        AppLanguage.EN ->
            "The Club grants you a \$$amount loan of honour to get back in the game. " +
                "This is the only time it can help you: from now on you will have to recover " +
                "in the cheaper venues."
        AppLanguage.FR ->
            "Le Club vous accorde un prêt d'honneur de \$$amount pour revenir dans la partie. " +
                "C'est la seule fois qu'il peut vous aider : désormais vous devrez vous refaire " +
                "dans les villes moins chères."
        AppLanguage.IT ->
            "Il Club ti concede un prestito d'onore di \$$amount per tornare in gioco. " +
                "È l'unica volta che può aiutarti: d'ora in poi dovrai rifarti nelle sedi più " +
                "economiche."
        AppLanguage.PT ->
            "O Clube concede-te um empréstimo de honra de \$$amount para voltares ao jogo. " +
                "É a única vez que te pode ajudar: a partir de agora terás de recuperar-te nas " +
                "cidades mais baratas."
        AppLanguage.DE ->
            "Der Club gewährt dir ein Ehrendarlehen von \$$amount, um wieder ins Spiel zu " +
                "kommen. Das ist das einzige Mal, dass er dir helfen kann: ab jetzt musst du " +
                "dich in den günstigeren Städten erholen."
    }

    // --- VITRINA DE LOGROS ---
    fun achievementsTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "🏆 LOGROS"
        AppLanguage.EN -> "🏆 ACHIEVEMENTS"
        AppLanguage.FR -> "🏆 SUCCÈS"
        AppLanguage.IT -> "🏆 OBIETTIVI"
        AppLanguage.PT -> "🏆 CONQUISTAS"
        AppLanguage.DE -> "🏆 ERFOLGE"
    }

    /** Cabecera de progreso: "Logros: 4 / 15 desbloqueados (27%)". */
    fun achievementsProgress(unlocked: Int, total: Int, percent: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Logros: $unlocked / $total desbloqueados ($percent%)"
        AppLanguage.EN -> "Achievements: $unlocked / $total unlocked ($percent%)"
        AppLanguage.FR -> "Succès : $unlocked / $total débloqués ($percent%)"
        AppLanguage.IT -> "Obiettivi: $unlocked / $total sbloccati ($percent%)"
        AppLanguage.PT -> "Conquistas: $unlocked / $total desbloqueadas ($percent%)"
        AppLanguage.DE -> "Erfolge: $unlocked / $total freigeschaltet ($percent%)"
    }

    fun achievementUnlockedBadge(): String = when (currentLanguage) {
        AppLanguage.ES -> "✓ CONSEGUIDO"
        AppLanguage.EN -> "✓ UNLOCKED"
        AppLanguage.FR -> "✓ OBTENU"
        AppLanguage.IT -> "✓ OTTENUTO"
        AppLanguage.PT -> "✓ CONQUISTADA"
        AppLanguage.DE -> "✓ ERREICHT"
    }

    fun achievementLockedBadge(): String = when (currentLanguage) {
        AppLanguage.ES -> "🔒 BLOQUEADO"
        AppLanguage.EN -> "🔒 LOCKED"
        AppLanguage.FR -> "🔒 VERROUILLÉ"
        AppLanguage.IT -> "🔒 BLOCCATO"
        AppLanguage.PT -> "🔒 BLOQUEADA"
        AppLanguage.DE -> "🔒 GESPERRT"
    }

    /** Aviso emergente al conseguir un logro. */
    fun achievementUnlockedToast(name: String): String = when (currentLanguage) {
        AppLanguage.ES -> "¡LOGRO DESBLOQUEADO!  $name"
        AppLanguage.EN -> "ACHIEVEMENT UNLOCKED!  $name"
        AppLanguage.FR -> "SUCCÈS DÉBLOQUÉ !  $name"
        AppLanguage.IT -> "OBIETTIVO SBLOCCATO!  $name"
        AppLanguage.PT -> "CONQUISTA DESBLOQUEADA!  $name"
        AppLanguage.DE -> "ERFOLG FREIGESCHALTET!  $name"
    }

    fun missionCompletedToast(name: String): String = when (currentLanguage) {
        AppLanguage.ES -> "✓ MISIÓN CUMPLIDA  $name"
        AppLanguage.EN -> "✓ MISSION COMPLETE  $name"
        AppLanguage.FR -> "✓ MISSION ACCOMPLIE  $name"
        AppLanguage.IT -> "✓ MISSIONE COMPIUTA  $name"
        AppLanguage.PT -> "✓ MISSÃO CUMPRIDA  $name"
        AppLanguage.DE -> "✓ MISSION ERFÜLLT  $name"
    }

    fun achievementsBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "LOGROS"
        AppLanguage.EN -> "ACHIEVEMENTS"
        AppLanguage.FR -> "SUCCÈS"
        AppLanguage.IT -> "OBIETTIVI"
        AppLanguage.PT -> "CONQUISTAS"
        AppLanguage.DE -> "ERFOLGE"
    }

    /** Misiones de una sede en su tarjeta del mapa. */
    fun venueMissionsHeader(completed: Int, total: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "MISIONES ($completed/$total)"
        AppLanguage.EN -> "MISSIONS ($completed/$total)"
        AppLanguage.FR -> "MISSIONS ($completed/$total)"
        AppLanguage.IT -> "MISSIONI ($completed/$total)"
        AppLanguage.PT -> "MISSÕES ($completed/$total)"
        AppLanguage.DE -> "MISSIONEN ($completed/$total)"
    }

    // --- PASS-AND-PLAY LOCAL ---
    fun passDeviceTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "PASA EL MÓVIL"
        AppLanguage.EN -> "PASS THE PHONE"
        AppLanguage.FR -> "PASSEZ LE TÉLÉPHONE"
        AppLanguage.IT -> "PASSA IL TELEFONO"
        AppLanguage.PT -> "PASSE O TELEFONE"
        AppLanguage.DE -> "GIB DAS HANDY WEITER"
    }

    fun passDeviceTo(name: String): String = when (currentLanguage) {
        AppLanguage.ES -> "Le toca a $name"
        AppLanguage.EN -> "$name's turn"
        AppLanguage.FR -> "Au tour de $name"
        AppLanguage.IT -> "Tocca a $name"
        AppLanguage.PT -> "Vez de $name"
        AppLanguage.DE -> "$name ist dran"
    }

    fun passDeviceHint(): String = when (currentLanguage) {
        AppLanguage.ES -> "Entrega el dispositivo y toca cuando estés listo. La mano queda oculta hasta entonces."
        AppLanguage.EN -> "Hand over the device and tap when ready. The hand stays hidden until then."
        AppLanguage.FR -> "Passez l'appareil et touchez quand vous êtes prêt. La main reste cachée jusque-là."
        AppLanguage.IT -> "Consegna il dispositivo e tocca quando sei pronto. La mano resta nascosta fino ad allora."
        AppLanguage.PT -> "Entregue o aparelho e toque quando estiver pronto. A mão fica oculta até então."
        AppLanguage.DE -> "Gib das Gerät weiter und tippe, wenn du bereit bist. Bis dahin bleibt die Hand verdeckt."
    }

    fun readyBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "LISTO, VER MI MANO"
        AppLanguage.EN -> "READY, SHOW MY HAND"
        AppLanguage.FR -> "PRÊT, VOIR MA MAIN"
        AppLanguage.IT -> "PRONTO, MOSTRA LA MIA MANO"
        AppLanguage.PT -> "PRONTO, VER MINHA MÃO"
        AppLanguage.DE -> "BEREIT, HAND ANZEIGEN"
    }

    fun localMatchBtnTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "👥 PARTIDA LOCAL (MISMO MÓVIL)"
        AppLanguage.EN -> "👥 LOCAL MATCH (SAME DEVICE)"
        AppLanguage.FR -> "👥 PARTIE LOCALE (MÊME APPAREIL)"
        AppLanguage.IT -> "👥 PARTITA LOCALE (STESSO DISPOSITIVO)"
        AppLanguage.PT -> "👥 PARTIDA LOCAL (MESMO APARELHO)"
        AppLanguage.DE -> "👥 LOKALES SPIEL (SELBES GERÄT)"
    }

    fun localMatchBtnSub(): String = when (currentLanguage) {
        AppLanguage.ES -> "Dos jugadores por turnos en el mismo teléfono"
        AppLanguage.EN -> "Two players taking turns on the same phone"
        AppLanguage.FR -> "Deux joueurs à tour de rôle sur le même téléphone"
        AppLanguage.IT -> "Due giocatori a turni sullo stesso telefono"
        AppLanguage.PT -> "Dois jogadores por turnos no mesmo telefone"
        AppLanguage.DE -> "Zwei Spieler abwechselnd am selben Handy"
    }

    fun localPlayerName(seat: Int): String = when (currentLanguage) {
        AppLanguage.ES -> "Jugador ${seat + 1}"
        AppLanguage.EN -> "Player ${seat + 1}"
        AppLanguage.FR -> "Joueur ${seat + 1}"
        AppLanguage.IT -> "Giocatore ${seat + 1}"
        AppLanguage.PT -> "Jogador ${seat + 1}"
        AppLanguage.DE -> "Spieler ${seat + 1}"
    }

    // --- PARTIDA EN CURSO GUARDADA ---
    fun resumeMatchTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "TIENES UNA PARTIDA SIN TERMINAR"
        AppLanguage.EN -> "YOU HAVE AN UNFINISHED MATCH"
        AppLanguage.FR -> "VOUS AVEZ UNE PARTIE INACHEVÉE"
        AppLanguage.IT -> "HAI UNA PARTITA IN CORSO"
        AppLanguage.PT -> "VOCÊ TEM UMA PARTIDA INACABADA"
        AppLanguage.DE -> "DU HAST EIN OFFENES SPIEL"
    }

    fun resumeMatchSub(cityName: String): String = when (currentLanguage) {
        AppLanguage.ES -> "Se quedó a medias en $cityName"
        AppLanguage.EN -> "Left unfinished in $cityName"
        AppLanguage.FR -> "Inachevée à $cityName"
        AppLanguage.IT -> "Rimasta in sospeso a $cityName"
        AppLanguage.PT -> "Ficou pela metade em $cityName"
        AppLanguage.DE -> "Unbeendet in $cityName"
    }

    fun resumeMatchBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "CONTINUAR PARTIDA"
        AppLanguage.EN -> "CONTINUE MATCH"
        AppLanguage.FR -> "REPRENDRE LA PARTIE"
        AppLanguage.IT -> "CONTINUA PARTITA"
        AppLanguage.PT -> "CONTINUAR PARTIDA"
        AppLanguage.DE -> "SPIEL FORTSETZEN"
    }

    fun discardMatchBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "Descartar"
        AppLanguage.EN -> "Discard"
        AppLanguage.FR -> "Abandonner"
        AppLanguage.IT -> "Scarta"
        AppLanguage.PT -> "Descartar"
        AppLanguage.DE -> "Verwerfen"
    }

    fun modeName(mode: GameMode): String = when (currentLanguage) {
        AppLanguage.ES -> when (mode) {
            GameMode.DRAW -> "Dominó Clásico (Con Robo)"
            GameMode.BLOCK -> "Dominó Trancado (Sin Robo)"
            GameMode.ALL_FIVES -> "Dominó de Cinco (All Fives)"
            GameMode.ALL_THREES -> "Múltiplos de 3 (All Threes)"
            GameMode.FIVES_AND_THREES -> "Fives & Threes (Cinco y Tres)"
            GameMode.MATADOR -> "Dominó Matador (Suma 7)"
            GameMode.DRAW_SPECIAL -> "Rama Propia (Especial)"
            GameMode.BLOCK_HUNGARIAN -> "Dominó Húngaro (Cadenas)"
        }
        AppLanguage.EN -> when (mode) {
            GameMode.DRAW -> "Draw Domino"
            GameMode.BLOCK -> "Block Domino"
            GameMode.ALL_FIVES -> "All Fives (Muggins)"
            GameMode.ALL_THREES -> "All Threes"
            GameMode.FIVES_AND_THREES -> "Fives & Threes"
            GameMode.MATADOR -> "Matador (Sum to 7)"
            GameMode.DRAW_SPECIAL -> "Own Branch (Special)"
            GameMode.BLOCK_HUNGARIAN -> "Hungarian Block"
        }
        else -> mode.title
    }

    fun selectModeTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "Selecciona Modalidad"
        AppLanguage.EN -> "Select Game Mode"
        AppLanguage.FR -> "Choisir le mode"
        AppLanguage.IT -> "Seleziona modalità"
        AppLanguage.PT -> "Selecionar modalidade"
        AppLanguage.DE -> "Spielmodus wählen"
    }

    fun playSelectedModeBtn(): String = when (currentLanguage) {
        AppLanguage.ES -> "¡Jugar Ahora!"
        AppLanguage.EN -> "Play Now!"
        AppLanguage.FR -> "Jouer !"
        AppLanguage.IT -> "Gioca ora!"
        AppLanguage.PT -> "Jogar agora!"
        AppLanguage.DE -> "Jetzt spielen!"
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
                    musicTitle = "Garibaldi Cumbia (Mexico City)",
                    cityLore = "Beneath colonial stone arches decorated with festive papel picado overlooking the Cathedral, All Fives matches demand sharp calculation.",
                    rivalIntro = "In Mexico City, multiples of five sing like mariachi! Score fast or get left behind."
                )
                AppLanguage.FR -> venue.copy(
                    name = "Mexico",
                    region = "Mexique • Terrasse Royale du Zócalo",
                    trophyTitle = "Médaille Aztèque d'Argent",
                    musicTitle = "Cumbia de Garibaldi (Mexico)",
                    cityLore = "Sous les arches coloniales décorées de papel picado face à la Cathédrale, les parties de multiples de 5 exigent une précision absolue.",
                    rivalIntro = "À Mexico, les multiples de cinq chantent comme les mariachis ! Comptez vite ou restez derrière."
                )
                AppLanguage.IT -> venue.copy(
                    name = "Città del Messico",
                    region = "Messico • Terrazza Reale dello Zócalo",
                    trophyTitle = "Medaglia Azteca d'Argento",
                    musicTitle = "Cumbia di Garibaldi (Città del Messico)",
                    cityLore = "Sotto gli archi coloniali illuminati di fronte alla Cattedrale, le sfide sui multipli di 5 richiedono calcolo millimetrico.",
                    rivalIntro = "A Città del Messico i multipli di cinque cantano come mariachi! Calcola veloce o resterai indietro."
                )
                AppLanguage.PT -> venue.copy(
                    name = "Cidade do México",
                    region = "México • Terraço Real do Zócalo",
                    trophyTitle = "Medalha Asteca de Prata",
                    musicTitle = "Cumbia de Garibaldi (Cidade do México)",
                    cityLore = "Sob os arcos coloniais iluminados em frente à Catedral, as partidas de múltiplos de 5 exigem cálculo rápido.",
                    rivalIntro = "Na Cidade do México os múltiplos de cinco cantam como mariachi! Some rápido para vencer."
                )
                AppLanguage.DE -> venue.copy(
                    name = "Mexiko-Stadt",
                    region = "Mexiko • Königliche Zócalo-Terrasse",
                    trophyTitle = "Silberne Azteken-Medaille",
                    musicTitle = "Garibaldi-Cumbia (Mexiko-Stadt)",
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
            "miami" -> when (currentLanguage) {
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

    // --- CRÉDITOS & LICENCIAS ---
    fun creditsBtnLabel(): String = when (currentLanguage) {
        AppLanguage.ES -> "Créditos"
        AppLanguage.EN -> "Credits"
        AppLanguage.FR -> "Crédits"
        AppLanguage.IT -> "Crediti"
        AppLanguage.PT -> "Créditos"
        AppLanguage.DE -> "Credits"
    }

    fun creditsDialogTitle(): String = when (currentLanguage) {
        AppLanguage.ES -> "Créditos y Licencias de Audio"
        AppLanguage.EN -> "Audio Credits & Licenses"
        AppLanguage.FR -> "Crédits et Licences Audio"
        AppLanguage.IT -> "Crediti e Licenze Audio"
        AppLanguage.PT -> "Créditos e Licenças de Áudio"
        AppLanguage.DE -> "Audio-Credits & Lizenzen"
    }

    fun musicCreditsSection(): String = when (currentLanguage) {
        AppLanguage.ES -> "Música y Banda Sonora (CC-BY 4.0)"
        AppLanguage.EN -> "Soundtrack & Music (CC-BY 4.0)"
        AppLanguage.FR -> "Bande Sonore & Musique (CC-BY 4.0)"
        AppLanguage.IT -> "Colonna Sonora & Musica (CC-BY 4.0)"
        AppLanguage.PT -> "Trilha Sonora & Música (CC-BY 4.0)"
        AppLanguage.DE -> "Soundtrack & Musik (CC-BY 4.0)"
    }

    fun proceduralMusicSection(): String = when (currentLanguage) {
        AppLanguage.ES -> "Música Procedural y Efectos (Dominio Público)"
        AppLanguage.EN -> "Procedural Music & Sound FX (Public Domain)"
        AppLanguage.FR -> "Musique Procédurale & Bruitages (Domaine Public)"
        AppLanguage.IT -> "Musica Procedurale ed Effetti (Pubblico Dominio)"
        AppLanguage.PT -> "Música Procedural e Efeitos (Domínio Público)"
        AppLanguage.DE -> "Prozedurale Musik & Soundeffekte (Gemeinfrei)"
    }

    fun proceduralMusicBody(): String = when (currentLanguage) {
        AppLanguage.ES -> "Fanfarrias y jingles generados proceduralmente con síntesis de ondas. Efectos de sonido de impacto de fichas de baquelita y acrílico (Dominio Público CC0)."
        AppLanguage.EN -> "Jingles and victory fanfares procedurally synthesized. Acrylic and bakelite domino tile impact sounds (Public Domain CC0)."
        AppLanguage.FR -> "Jingles et fanfares synthétisés de manière procédurale. Bruits d'impact de dominos en bakélite (Domaine Public CC0)."
        AppLanguage.IT -> "Jingle e fanfare sintetizzati proceduralmente. Suoni di tessere in bachelite (Pubblico Dominio CC0)."
        AppLanguage.PT -> "Jingles e fanfarras sintetizados proceduralmente. Sons de impacto de dominós (Domínio Público CC0)."
        AppLanguage.DE -> "Jingles und Fanfaren prozedural synthetisiert. Acryl- und Bakelit-Dominogeräusche (Gemeinfrei CC0)."
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

