# Notas de versión — v2.5.2 (versionCode 18)

> Cambio de nombre: la app pasa a llamarse **Dominó Grand Prix** (coincide con el logo del juego). Mismo paquete `com.horts.dominoimperial`, así que la pista de prueba y el enlace de los testers no cambian.
> Incluye todo lo de la v2.5.0 y la v2.5.1: fuera la economía, portada limpia, pista y deshacer flotantes.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```n<es-ES>
• La app pasa a llamarse Dominó Grand Prix: mismo icono, mismo juego.
• El reto es la gira: completa las 7 sedes y sus 35 retos.
• Fuera monedas y apuestas: no hay economía que gestionar ni nada que comprar.
• Los trofeos son el recuerdo de cada sede completada, no dinero.
• Pista y deshacer ahora flotan sobre la mesa, y los logros se abren desde el título.
</es-ES>
<en-US>
• The app is now called Domino Grand Prix: same icon, same game.
• The challenge is the tour: complete all 7 cities and their 35 challenges.
• No more coins or bets: no economy to manage and nothing to buy.
• Trophies are the memory of each completed city, not money.
• Hint and undo now float over the table, and achievements open from the title.
</en-US>```

---

## Qué ha cambiado en esta versión (interno)

**Simplificación: el juego ya no tiene moneda.**

- `JourneyProgress` pierde `cash`, `wrp`, `soldTrophyIds` y `bankruptcyRescueClaimed`.
  Los guardados antiguos siguen cargando porque el JSON ahora usa `ignoreUnknownKeys`.
- Se elimina `CareerEconomy` completo (cobro de entrada, liquidación, rescate por bancarrota).
- `VenueChallenge` pierde `entryBet`, `rewardCash`, `rewardWrp` y `trophyValue`;
  `CityVenue` pierde `entryBet`, `wrpReward` y `trophyValue`; `LuxuryItem` pierde `value`
  e `isSold`.
- `MatchReport` pierde `cashDelta` y `wrpDelta`; `CareerReporter` ya no recibe premio ni apuesta.
- La vitrina de trofeos pasa a ser una colección de recuerdos (sin vender, sin valor).
- La pantalla de sede pierde la caja y el premio en metálico; la mesa pierde el importe de la
  apuesta.
- El logro `high_roller` desaparece (dependía de tener 20.000 en caja).
- Se borran dos pantallas huérfanas que no estaban conectadas a la navegación
  (`WorldTourMapScreen`, `RivalShowdownScreen`).

**Corregido de paso (riesgo nulo, sin cambiar lo que ve el jugador):**

- `CareerProgressStore.snapshot` guarda **en unión**: antes reemplazaba `completedVenueIds` y
  `unlockedVenueIds` con lo que dijera el catálogo en memoria (que nunca se actualizaba), así que
  el progreso de sedes se perdía en cada guardado y el logro `perfect_tour` era imposible.
- Test nuevo con un JSON real de la v2.4.0 (con `cash`/`wrp`) que verifica que el progreso se
  conserva al cargar.



