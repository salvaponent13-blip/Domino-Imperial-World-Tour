# Notas de versión — v2.3.1 (versionCode 16)

## Español (Google Play, ≤ 500 caracteres)

```
v2.3.1 · Correcciones de la v2.3.0

• Tablero: la fila de vuelta de las cadenas largas ya no se cruza con los brazos del spinner en All Fives (era el único caso en que dos fichas se solapaban).
• El tablero queda centrado desde la ficha de salida.
• Marcador corregido en partidas individuales de 3 y 4 jugadores: antes solo contaba a un rival.
• Resumen de ronda con los puntos reales que suma el motor (adiós a los «×2 / ×3» de capicúa que no se aplicaban).
• La partida rápida ya no borra el progreso del torneo ni altera la dificultad de la carrera.
• Nuevos textos traducidos a los 6 idiomas del juego.
```

## English (Google Play, ≤ 500 characters)

```
v2.3.1 · Fixes for v2.3.0

• Board: the return row of long chains no longer crosses the spinner arms in All Fives (the only case where two tiles overlapped).
• The board is now centred from the opening tile.
• Fixed the scoreboard in 3-4 player individual games: it only counted one rival.
• Round summary now shows the points the engine actually awards (no more "×2 / ×3" capicua bonuses that were never applied).
• Quick Play no longer wipes tournament progress or changes career difficulty.
• New UI strings translated into all 6 game languages.
```

## Detalle técnico (interno, no va a la ficha de tienda)

- `DominoBoardLayout`: franja central reservada (`crossMode`) para los brazos ARRIBA/ABAJO en
  ALL_FIVES; las serpentinas giran dentro de su propio lado. Barrido de 36.504 combinaciones de
  longitudes de rama sin un solo solape (`DominoBoardLayoutTest`).
- `computeBounds`: el mínimo de 8x6 celdas se reparte simétrico (tablero centrado).
- `DominoEngine`: se elimina la comprobación de colisiones muerta (`slotCollidesWithOtherTiles`),
  se añaden `lastRoundPointsAwarded` y `roundPointsByPlayer`, las fallas se limpian al jugar y el
  layout se recalcula después de restaurar el spinner.
- `DominoRoundSummaryDialog`: usa el delta real del motor; las bonificaciones ya no anuncian
  multiplicadores inexistentes y no se narra "dominó" cuando la ronda cerró por puntos.
- `DominoPipCounterDialog`: scroll, ancho propio y recuento del pozo separado.
- `Navigation`: la dificultad se fija al entrar en cada partida; el guardado de la carrera no se
  toca desde una partida libre y el camino de Reglas limpia el snapshot.
- `DominoTableScreen`: la ranura de previsualización usa la ficha candidata (dobles incluidos),
  sin `selectedTile!!`, HUD con scroll horizontal y textos vía `DominoStrings`.
