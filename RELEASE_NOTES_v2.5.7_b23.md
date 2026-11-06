# Notas de versión — v2.5.7 (versionCode 23)

> Corrige el tanteo de los modos de puntos y varias reglas de All Fives y Fives & Threes que se
> desviaban del reglamento. Es la versión que sustituye a la b22 para la entrega.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```text
<es-ES>
• El resumen de cada mano ahora suma exactamente los puntos que suben al marcador (mesa + fichas rivales).
• All Fives: al cerrar la mano se reparten los pips rivales (redondeados a 5) y el spinner ya no regala puntos de un brazo sin abrir.
• Cinco y Tres: no se roba del pozo y el marcador anuncia los puntos reales (15 paga 8).
• Tus puntos de una mano perdida cuentan para la carrera y los logros.
</es-ES>
<en-US>
• The hand summary now adds exactly the points that reach the scoreboard (table + opponent pips).
• All Fives: hand end now awards the opponents' pips (rounded to 5) and the spinner no longer scores an unopened arm.
• Fives & Threes: no boneyard draws, and the badge shows the real payout (15 pays 8).
• Points scored in a lost hand now count for career stats and achievements.
</en-US>
```

---

## Qué incluye

### Tanteo de mesa

- **Una sola fuente de verdad** (`openEndsBreakdown`): la suma que se cobra y la que anuncia el HUD
  ya no pueden discrepar.
- **All Fives**: el cuarto brazo del spinner solo cuenta cuando se empieza (antes se sumaba su pip
  desde el tercer brazo: el 24,6 % de las colocaciones de una partida de prueba salían con la suma
  mal). Los brazos de arriba/abajo se abren al cubrir los dos lados del doble.
- **All Fives, cierre de mano**: el ganador suma los pips rivales redondeados al múltiplo de 5 más
  cercano; si la tranca empata a pips, no puntúa nadie.
- **Fives & Threes**: 15 paga 8 (tres cincos y cinco treses), el HUD lo dice así y ya no se roba del
  pozo.

### Resumen de ronda y estadísticas

- El resumen separa **puntos de mesa** y **pips de cierre**, y su total es exactamente lo que sube el
  marcador (antes en All Fives ponía "+0").
- Las filas de manos dicen "N pips en mano" en los modos de tanteo, en vez de "N pts".
- Los puntos anotados en una mano que gana el rival cuentan para `matchPointsScored` y
  `matchBestRoundPoints` (carrera y logros).
- Reto diario: el resultado guarda los puntos del **equipo**, no solo los del asiento 0.

### Estabilidad

- El turno de la IA siempre avanza o cierra la ronda: si no puede aplicar su jugada, roba o pasa.
  Antes podía devolver "nada" y dejar la mesa esperando un turno que ya no llegaba.
- Test nuevo que recorre las 8 modalidades comprobando ese invariante.

### Pendiente conocido

- Fives & Threes sigue repartiendo los pips rivales al cerrar (la regla de pub solo da 1 punto por
  "chip out") y en los tres modos de tanteo se gana al alcanzar la meta, no al clavarla exacta. Es
  una decisión consciente para no dejar las metas de la campaña fuera de escala.
