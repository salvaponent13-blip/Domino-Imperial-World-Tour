# Notas de versión — v2.4.0 (versionCode 16)

> Sustituye a la v2.3.1 (b16), que no llegó a subirse. Mismo versionCode 16: si ya subiste un
> artefacto de prueba con ese código y Play no te deja reutilizarlo, borra el borrador anterior.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```
<es-ES>
• Desafío diario: un reto al día con racha y dos logros nuevos ya disponibles.
• Dificultad por sede: La Habana es asequible, Tokio es duro de verdad.
• Pass-and-play: dos personas en el mismo móvil.
• Pista y deshacer en la mesa, para aprender sin frustrarte.
• Ajustes que se guardan: sonido, vibración, idioma y cámara.
• Tablero 2D/3D centrado y sin solapes, también en cadenas largas.
• Tanteo real, marcador correcto con 3-4 jugadores y la partida rápida ya cuenta para tus logros.
</es-ES>
<en-US>
• Daily Challenge: one challenge a day, a streak, and two new achievements.
• Per-venue difficulty: Havana is friendly, Tokyo is genuinely tough.
• Pass-and-play: two people on the same phone.
• Hint and undo at the table, so you can learn without frustration.
• Settings that stick: sound, haptics, language and camera.
• 2D/3D board centred and overlap-free, even on long chains.
• Real scoring, correct scoreboard with 3-4 players, and Quick Play now counts toward achievements.
</en-US>
```

Longitud: ES 487 caracteres · EN 481 caracteres.

---

## Todo lo que se hizo hoy (resumen completo, no cabe en Play)

**Contenido nuevo**

- Desafío diario: un reto por día, con la misma mano para todo el mundo, racha de días y dos
  logros nuevos (`daily_first`, `daily_streak_7`) que antes eran inalcanzables.
- Dificultad por sede: México y La Habana `RELAXED`, San Juan/Nueva Orleans/Miami `STANDARD`,
  Las Vegas y Tokio `TOUGH`. Antes los 35 retos jugaban todos en `STANDARD`.
- Pass-and-play: 1 o 2 personas en el mismo móvil, con cortina de privacidad entre turnos.
- Pista en la mesa: propone una jugada legal y el extremo, usando la evaluación de la IA media.
- Deshacer: vuelve al momento anterior a tu última jugada (incluida la respuesta de la IA).
  Solo en partidas de un humano.
- Ajustes persistentes: sonido, vibración, idioma, cámara, iluminación y velocidad de la IA.
- La partida rápida ya cuenta para estadísticas y logros.
- Contador de fichas 0-6 con el pozo separado (v15, mejorado).
- Fallas de los rivales en tiempo real (v15, ahora se limpian al volver a jugar).
- Tanteo detallado al cerrar la mano, con desglose real de puntos (v15, corregido).

**Correcciones**

- All Fives: las cadenas largas ya no cruzan los brazos del spinner (era el único caso en que
  dos fichas se solapaban). Franja central reservada y 13.689 tableros verificados sin solapes.
- Tablero centrado desde la ficha de salida (antes se iba 2 celdas a la izquierda).
- Marcador correcto en partidas individuales de 3 y 4 jugadores (antes solo contaba a un rival).
- El resumen de ronda muestra los puntos que suma el motor de verdad, sin los «×2 / ×3» de
  capicúa que nunca se aplicaron, y sin narrar "dominó" cuando la ronda cerró por puntos.
- La partida rápida ya no borra el progreso de la carrera ni contagia la dificultad a los retos.
- Solo la carrera reanuda partida guardada: ninguna partida libre o diaria resucita un guardado
  viejo con otro modo y otro objetivo.
- `matchPointsScored` suma los puntos reales de la ronda, no los de la última jugada.
- Arrastre del tablero convertido de píxeles a dp (se movía de más).
- HUD de rivales con scroll: en pantallas estrechas ya no se recortan los badges.
- El contador de fichas no se queda encima del resumen de ronda.

**Calidad**

- 239 tests en verde (antes 227), con `PlayAssistTest` nuevo y las aserciones de colisión
  actualizadas a la convención real del tablero (centro, 56x28).
- Decisiones ambiguas documentadas en `DECISIONES.md` (capicúa, cierre a mitad de ronda en los
  modos de puntos, estadísticas históricas).

## Detalle técnico (interno)

- `SettingsStore` + `SettingsDialog`; `DominoSoundManager` y `DominoHaptics` recuperan su estado
  al arrancar y lo guardan al cambiar.
- `DominoEngine`: `markUndoPoint`/`undo`/`canUndo`, `suggestedMove`, `lastRoundPointsAwarded`,
  `roundPointsByPlayer` y `roundPointsForTeam` (acumuladores de ronda correctos).
- `VenueChallenge.difficulty` + `difficultyForVenue()`; `Navigation` deja de forzar `STANDARD`.
- `QuickPlayConfig.humanPlayerCount` (1 o 2) → `humanPlayerIndices` reales en la mesa.
- `DailyChallengeManager`/`DailyChallengeStore` conectados a `Navigation` y a
  `AchievementCatalog.applyReport(dailyStreak)`.
- `MatchSnapshot` gana `lastRoundPointsAwarded` (default, compatible con guardados anteriores).
