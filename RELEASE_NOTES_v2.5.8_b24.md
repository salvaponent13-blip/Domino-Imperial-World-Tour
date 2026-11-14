# Notas de versión — v2.5.8 (versionCode 24)

> Arregla el final del torneo: al ganar el último reto de la última sede, la app se quedaba
> diciendo "continuar torneo · Reto 5 de 5" y volvía a jugar la misma final para siempre.
> Ahora el menú ofrece **volver a jugar el torneo** y reinicia la gira conservando trofeos y logros.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```text
<es-ES>
• Al terminar las 7 sedes, el menú ya no te devuelve a la misma final: ahora dice "VOLVER A JUGAR EL TORNEO" y empieza de nuevo en México.
• Repetir la gira conserva trofeos, logros y estadísticas; "Nuevo torneo" sigue borrando todo.
• La pantalla de la última sede muestra "TORNEO COMPLETADO" con el botón para repetirlo.
</es-ES>
<en-US>
• After finishing the 7 cities, the menu no longer loops back to the same final: it now says "PLAY THE TOURNAMENT AGAIN" and restarts in Mexico.
• Replaying keeps trophies, achievements and stats; "New tournament" still wipes everything.
• The last venue screen shows "TOURNAMENT COMPLETED" with the replay button.
</en-US>
```

---

## Qué incluye

### Final del torneo

- **Nuevo estado `tournamentFinished`** en el progreso de la carrera (los guardados anteriores
  cargan sin tocar nada: el campo nuevo entra con valor por defecto).
- Al ganar el **reto 5 de Tokio** la gira queda marcada como completada. Antes el progreso se
  quedaba en "Reto 5 de 5" para siempre: el menú seguía ofreciendo *continuar torneo* y te devolvía
  a la misma final sin forma de empezar de nuevo.
- **Menú principal**: el botón dorado pasa a **"VOLVER A JUGAR EL TORNEO ➔"** con el subtítulo
  "Torneo completado • 7 sedes y 35 retos".
- **Ese botón reinicia la gira en México (Reto 1)** y **conserva trofeos, logros y estadísticas**.
  "Nuevo torneo" sigue siendo el que empieza de cero (con su confirmación).
- **Pantalla de sede**: en la última sede terminada la insignia dice "🏆 TORNEO COMPLETADO" y el
  botón principal repite la gira en vez de "JUGAR RETO 5".
- Textos nuevos en los 6 idiomas (es, en, fr, it, pt, de).

### Verificación

- Las transiciones de la carrera viven en `CareerFlow` con sus propios tests: ganar un reto,
  ganar el 5º de una sede (avanza y desbloquea la siguiente) y ganar el 5º de la última sede.
- **249 tests unitarios en verde** (6 nuevos de `CareerFlowTest`).

### Sin cambios respecto a la b23

- Tanteo de mesa, resumen de ronda, reglas de All Fives y Fives & Threes: igual que en la v2.5.7.
- R8 sigue desactivado en el release (no se toca sin una prueba en dispositivo).
