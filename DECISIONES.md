# Decisiones de diseño pendientes de cerrar

Documento corto para que las tres reglas "raras" no se queden en el limbo: hoy el código hace
una cosa y la interfaz insinúa otra. Ninguna es un fallo de implementación, son decisiones de
producto que conviene tomar con criterio y dejarlas escritas.

## 1. La capicúa se anuncia pero no puntúa

**Qué pasa.** `DominoEngine.finishRound` suma los pips de los rivales sin aplicar ningún
multiplicador; `lastRoundBonusType` (CAPICUA / DOBLE_CAPICUA / DOUBLE) solo se usa como etiqueta
en el log y en el resumen de ronda. La v2.3.1 quitó el "×2 / ×3" inventado del resumen, así que
hoy la bonificación es decorativa.

**Opciones.**
- A. **Dejarla decorativa** (estado actual). Coste 0. Riesgo: el jugador lee "¡CAPICÚA!" y no ve
  puntos; conviene un texto que lo deje claro ("¡Capicúa! — sin bonificación en este modo").
- B. **Implementar bonus real**: `totalOpponentPips * 2` (capicúa) / `* 3` (doble capicúa) en
  `finishRound`, con test de reglas y aviso en el resumen. Coste ~2 h + decisión de balance.

Recomendación: B solo si se quiere profundidad competitiva; si no, A y quitar la palabra
"bonificación" de cualquier texto.

## 2. La partida puede cerrarse a mitad de ronda en los modos de puntos

**Qué pasa.** En All Fives / All Threes / Fives & Threes, `evaluatePointScoring` cierra la
partida en cuanto un equipo alcanza el objetivo, aunque la mano siga abierta
(`finishMatchByPoints`). El resumen ya no lo narra como "dominó" (v2.3.1), pero el jugador puede
ver cómo se le corta una mano que iba ganando.

**Opciones.**
- A. **Mantener** (es la regla de los modos de tanteo en muchos clubs: se juega a puntos, no a
  manos).
- B. **Terminar la mano** y cerrar al final de la ronda.

Recomendación: A, documentado en la pantalla de reglas del modo.

## 3. `matchPointsScored` ya cuenta la ronda, pero las estadísticas antiguas no

**Qué pasa.** Hasta la v2.3.1 las estadísticas de carrera sumaban `lastPointsEarned`, que son
los puntos de **una jugada**, no de la ronda; en los modos de tanteo eso dejaba el contador casi
a cero. Desde la v2.4.0 se suman los puntos reales de la ronda (`roundPointsForTeam`).

**Consecuencia.** Los progresos guardados antes de la v2.4.0 tienen ese contador infravalorado.
No se migra: es una estadística histórica, no una moneda. Los logros que dependen de ella se
desbloquean jugando partidas nuevas.

## 4. El "deshacer" revierte también la respuesta de la IA

Es deliberado: el jugador se arrepiente de **su** jugada, no de la del rival, y volver a un
estado donde la IA ya movió dejaría la mesa sin sentido. Solo se ofrece en partidas de un único
humano (en pass-and-play sería una trampa contra el otro jugador).
