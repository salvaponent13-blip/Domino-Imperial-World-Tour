# Notas de versión — v2.4.0 (versionCode 16)

> Reemplaza a la v2.3.1 (b16), que no llegó a subirse. Mismo versionCode 16: si ya subiste un
> artefacto de prueba con ese código y Play no te deja reutilizarlo, sube este como 16 y borra
> el borrador anterior.

## Español (Google Play, ≤ 500 caracteres)

```
v2.4.0 · Contenido nuevo

• DESAFÍO DIARIO: un reto al día, igual para todos, con racha de días y dos logros nuevos que ya se pueden conseguir.
• Cada sede del torneo tiene su nivel: La Habana es asequible y Tokio es un rival duro de verdad.
• Pass-and-play: juega con otra persona en el mismo móvil (1 o 2 humanos) en cualquiera de las 8 modalidades.
• Pista y deshacer en la mesa, para aprender sin frustrarte.
• Ajustes que se guardan: sonido, vibración e idioma. La cámara, la iluminación y la velocidad también se recuerdan.
• La partida rápida ya cuenta para tus estadísticas y logros.
```

## English (Google Play, ≤ 500 characters)

```
v2.4.0 · New content

• DAILY CHALLENGE: one challenge a day, the same for everyone, with a day streak and two new achievements that are finally obtainable.
• Each tour venue now has its own level: Havana is friendly, Tokyo is genuinely tough.
• Pass-and-play: play with a friend on the same phone (1 or 2 humans) in any of the 8 modes.
• Hint and undo at the table, so you can learn without frustration.
• Settings that stick: sound, haptics and language. Camera, lighting and AI speed are remembered too.
• Quick Play now counts toward your stats and achievements.
```

## Detalle técnico (interno, no va a la ficha de tienda)

- **Desafío diario conectado**: `Navigation` construye el reto con `DailyChallengeManager.today()`,
  pasa `dailySeed` a la mesa (que ya no guarda snapshot en modo diario), guarda
  `DailyChallengeResult` con `nextStreak` y alimenta `AchievementCatalog.applyReport(dailyStreak)`,
  de modo que `daily_first` y `daily_streak_7` dejan de ser inalcanzables. Tarjeta en el título
  con modo, objetivo y racha (todas las cadenas ya existían traducidas).
- **Dificultad por sede**: `VenueChallenge.difficulty` + `difficultyForVenue()` (La Habana/México
  RELAXED, San Juan/Nueva Orleans/Miami STANDARD, Las Vegas/Tokio TOUGH); antes los 35 retos
  corrían en STANDARD.
- **Pass-and-play**: `QuickPlayConfig.humanPlayerCount` (1 o 2) y `humanPlayerIndices` reales en
  la mesa. La infraestructura (cortina de privacidad, `HandViewRules`) ya estaba probada.
- **Deshacer**: `markUndoPoint()` / `undo()` / `canUndo` en el motor (usa `snapshot`/`restore`);
  se descarta al empezar ronda y al cerrarla. Solo en partidas de un humano.
- **Pista**: `suggestedMove()` reutiliza `MediumStrategy` + `simulate()` para proponer una jugada
  legal; se anuncia con `hintPlaySuggestion` y el extremo elegido.
- **Ajustes persistentes**: `SettingsStore` (sonido, háptica, cámara, iluminación, velocidad) +
  `SettingsDialog` en el título. El silencio y la háptica ya se recuperan al arrancar.
- **Partida rápida con informe**: aplica `CareerReporter` (venueId nulo, sin premios) y actualiza
  estadísticas y logros.
- **Estadísticas**: `matchPointsScored` / `matchBestRoundPoints` usan los puntos reales de la ronda
  (`roundPointsForTeam`) en vez de los de la última jugada.
- **Reanudación endurecida**: solo la carrera carga snapshot (`activeVenueId != null`), así que
  ninguna partida libre o diaria puede resucitar un guardado viejo.
- **Tests nuevos**: `PlayAssistTest` (deshacer restaura el estado completo, pista legal, pista
  nula sin jugadas, dificultad por sede y en los 35 retos). Total: 239 tests.
