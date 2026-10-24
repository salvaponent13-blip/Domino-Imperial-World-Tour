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

## 7. IA: pendiente, con los defectos conocidos

La auditoría de la v2.4.0 dejó tres cosas sin arreglar, aplazadas a una sesión propia:

1. `MediumStrategy` no mira el palo que deja abierto (solo los pips que suelta).
2. STANDARD y TOUGH solo escalan constantes; en los 5 modos sin puntos juegan igual salvo cuando
   la jugada tranca. RELAXED sí se nota (estrategia aleatoria).
3. `simulate` usa las manos reales: la IA sabe cuándo su jugada tranca la mesa.

**Plan:** quitar/estimar el dato oculto → añadir afinidad de palo → **volver a medir** con
`MediumStrategyBenchmarkTest` aceptando que el número baje al principio. No se toca sin medir: la
referencia medida (MEDIUM 55,81 % vs LEGACY) es la fuente de verdad del proyecto.
