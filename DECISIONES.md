# Decisiones de diseño

Documento vivo: aquí quedan escritas las decisiones que no son obvias leyendo el código, para que
nadie (ni yo dentro de un mes) las deshaga sin saber por qué estaban.

**Última revisión:** 23 de octubre de 2026 (v2.5.0, versionCode 16).

---

## 1. El juego no tiene moneda (v2.5.0)

**Decisión:** se elimina toda la economía (monedas, WRP, apuestas de entrada, rescate por
bancarrota y venta de trofeos). **El objetivo es completar la gira**: 7 sedes y 35 retos.

**Por qué:**
- Las monedas no tenían sumidero (no había tienda), así que no eran una decisión: eran un número.
- La apuesta de entrada no se cobraba si no había caja, así que tampoco era una consecuencia.
- La vitrina permitía vender los 7 trofeos recién instalado (76.200 $) y dejaba la progresión sin
  sentido. Quitar el dinero elimina el agujero por diseño, no con un parche.

**Qué queda:** los trofeos son recuerdos de las sedes completadas (colección, sin valor). Si algún
día se quiere tensión, la vía limpia son **estrellas por reto** (1-3 según margen o sin fallos), no
volver a introducir una moneda.

**Compatibilidad:** los guardados de la v2.4.0 (con `cash`/`wrp`) siguen cargando porque el JSON
usa `ignoreUnknownKeys`; hay un test con un guardado real que lo verifica.

---

## 2. La capicúa se anuncia, pero no multiplica

`DominoEngine.finishRound` suma los pips de los rivales sin factor. `lastRoundBonusType` es una
etiqueta (dominó con doble / capicúa / doble capicúa) que el resumen muestra sin prometer
multiplicadores.

**Decisión:** se queda como adorno hasta que haya una regla decidida. Si algún día se implementa,
tiene que ir con test de reglas y con el aviso en el resumen; no se anuncia un ×2/×3 que el motor
no aplique.

---

## 3. La partida puede cerrarse a mitad de ronda en los modos de puntos

En All Fives, All Threes y Fives & Threes, `evaluatePointScoring` cierra la partida en cuanto un
equipo alcanza el objetivo, aunque queden fichas en la mano.

**Decisión:** se mantiene (es la regla habitual de los modos de tanteo). El resumen de ronda ya no
lo narra como "dominó": dice que se alcanzó la meta.

---

## 4. Las estadísticas cuentan la ronda, no la última jugada

`matchPointsScored` y `matchBestRoundPoints` suman los puntos reales de la ronda
(`roundPointsForTeam`), no `lastPointsEarned` (que es solo de la última jugada). Los progresos
guardados antes de la v2.4.0 tienen ese contador infravalorado y no se migran: es histórico, no
una moneda.

**Ampliación (resumen de ronda).** El resumen que ve el jugador al cerrar la mano desglosa la
ronda en las dos cifras que calcula el motor: `lastRoundTablePoints` (lo anotado jugando) y
`lastRoundClosingPoints` (los pips del cierre). Su suma es `lastRoundPointsAwarded`, y **ese** es
el total que muestra el diálogo. Antes `lastRoundPointsAwarded` guardaba solo el cierre y el
diálogo lo usaba como total: en All Fives (donde el cierre no reparte pips) el resumen decía
"+0 pts" aunque el jugador acabara de anotar 30 en la mesa, y en Fives & Threes se comía los
puntos de mesa y enseñaba solo los pips. Invariante que hay que respetar si se toca:
`mesa + cierre == lo que sube el marcador del bando ganador` (test: `RoundSummaryPointsTest`).


---

## 5. Deshacer revierte también la respuesta de la IA

El jugador se arrepiente de **su** jugada; volver a un estado donde la IA ya movió dejaría la mesa
sin sentido. Por eso el punto de deshacer se marca justo antes de jugar y el `undo` vuelve a ese
instante. Solo se ofrece en partidas de un humano: en pass-and-play sería una trampa contra el
otro jugador.

---

## 6. Publicidad AdMob (decisión del 24/10/2026)

**Decisión:** se integra Google AdMob (`play-services-ads:23.6.0`) con la misma arquitectura
desacoplada de *Rumbo* y *Butimania*:
- **Banner adaptativo** en el pie del menú principal (`MainTitleScreen`), sin invadir nunca la mesa
  de juego (`DominoTableScreen`).
- **Intersticial** que se precarga en segundo plano y se muestra únicamente al finalizar una partida
  completa (`onMatchFinished`), con un *cooldown* mínimo de 60 segundos para no saturar al jugador.
- **IDs centralizados** en `app/build.gradle.kts` inyectados vía `BuildConfig` y `manifestPlaceholders`.
- **Filtros y seguridad:** si el anuncio falla o el jugador no tiene red, el juego continúa de
  inmediato sin pantallas bloqueadas. Preparado con flag `adsDisabled` para futuras compras integradas
  (quitar publicidad).
- **Declaraciones:** la ficha de Google Play declara "Contiene anuncios", Seguridad de datos declara
  el ID de publicidad (`AD_ID`) y la política de privacidad contempla a Google AdMob.

---

## 7. IA: afinidad de palo, diferenciación STANDARD/TOUGH y simulación sin información oculta (v2.5.5)

En la v2.5.5 (b21) se resolvieron los tres puntos pendientes de la auditoría de IA y se separaron
en el banco de pruebas (`MediumStrategyBenchmarkTest`) los pasos incrementales:

1. **Afinidad de palo (`suitAffinity`) y explotación de fallas (`passExploit`) en `MediumStrategy`:**
   la evaluación valora cuántas fichas conserva el jugador para responder a los extremos que deja
   abiertos (`mySuitSupport`) y bonifica dejar abiertos números en los que los rivales ya han pasado
   públicamente (`opponentPassedEndsMatched`). En `MediumWeights` se mantiene `WITH_SCORING` (paso 4,
   solo puntos de extremos) separado de `ALL` (paso 5, que suma afinidad de palo y fallas y eleva la
   winrate frente a `LegacyStrategy` del **55,81 % al 61,06 %**).
2. **Diferenciación real entre `STANDARD` y `TOUGH` en los 8 modos (`StrategyCatalog`):**
   `STANDARD` mantiene una continuidad básica de palo sin explotar memoria de fallas (`passExploit = 0.0`),
   mientras que `TOUGH` pondera activamente el dominio de palo y el castigo a las fallas rivales
   (`suitAffinity = 1.75`, `passExploit = 2.4`), tomando decisiones tácticas distintas incluso en los
   5 modos que no suman puntos en mesa.
3. **Simulación limpia sin espiar manos ocultas (`DominoEngine.simulate`):**
   `opponentsPips` y `ownTeamPips` se estiman a partir de los 168 puntos totales del juego menos las
   fichas visibles en mesa y la mano propia, repartidos según el tamaño público de cada mano; y
   `isBoardBlockedFairly` deduce la tranca exclusivamente por conteo de fichas vistas y pases cantados.

---

## 8. Tanteo de mesa: una sola fuente de verdad (auditoría de puntuación)

`DominoEngine.openEndsBreakdown()` es **la única** función que decide qué extremos cuentan y cuánto
vale cada uno; de ella salen `calculateOpenEndsSum()` (lo que se cobra) y
`getOpenEndsEvaluationText()` (lo que enseña el HUD), y el pago lo aplica `pointsForOpenEnds()`.
Antes había tres copias de la regla y no decían lo mismo: el HUD anunciaba la suma mientras el motor
pagaba la suma dividida por 5 y por 3 en Fives & Threes (decía "15 pts" y pagaba 8).

Reglas que aplica, según la [regla oficial de All Fives](https://www.pagat.com/domino/cross/all_fives.html)
y la de [Fives and Threes](https://www.pagat.com/domino/line/fives_and_threes.html):

- Un doble en el extremo de un brazo vale sus dos mitades; en medio de la línea, nada.
- El spinner (primer doble) aporta como mucho sus dos mitades, y solo mientras tenga un lado sin
  cubrir. Con los dos lados cubiertos pasa a ser una ficha más de la línea.
- **Un brazo sin empezar no es un extremo abierto.** El cuarto brazo del spinner cuenta cuando se
  empieza el cuarto, no antes: contar el pip del spinner desde el tercer brazo regalaba 6 puntos por
  jugada (el 24,6 % de las colocaciones de una partida IA salían con la suma mal).
- Fives & Threes paga 1 punto por cada 5 y 1 por cada 3 (15 → 8), no la suma. El 15 **sí** es
  alcanzable: [6|6] en un extremo (12) y un 3 en el otro.
- **Los brazos transversales del spinner se abren después de cubrir sus dos lados.** Antes se podía
  jugar arriba/abajo nada más salir el spinner, saltándose el orden que marca la regla
  (`canOpenCrossArms()` en el motor).
- **All Fives reparte al cerrar la mano**: los pips de los rivales redondeados al múltiplo de 5 más
  cercano (22 → 20, 3 → 5, 2 → 0). Si la tranca empata a pips, no puntúa nadie.
- **Fives & Threes no roba del pozo**: las fichas sobrantes quedan fuera (por eso `allowsDraw = false`
  y `handSize = 5` con 3-4 jugadores, que no se derivan uno del otro).

Queda como desviación consciente, porque cambiarla deja las metas de la campaña fuera de escala:
Fives & Threes sigue repartiendo los pips rivales al cerrar (la regla oficial solo da 1 punto por
"chip out"), y en los tres modos de tanteo se gana al *alcanzar* la meta, no al clavarla exacta.


