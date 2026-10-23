# Plan de mejoras — Domino Imperial: World Tour

> **Documento histórico (23/10/2026).** Este plan se escribió antes de la v2.0 y describe el
> estado de entonces: habla de economía, de apuestas y de una IA sin heurística. Parte de eso ya
> no existe (el juego no tiene monedas desde la v2.5.0) y parte ya está hecho (dificultad por
> sede, desafío diario, pass-and-play, pista/deshacer, ajustes persistentes).
> Lo que sigue vigente —decisiones del juego y lo que falta de verdad— está en `DECISIONES.md`.
Documento de trabajo. Cada punto incluye **qué falta**, **por dónde se toca** (con archivos y
líneas reales del repositorio), **cuánto cuesta**, **qué riesgo tiene** y **cómo se comprueba
que está bien hecho**. Lo que no se puede medir, no entra.

Los esfuerzos son estimaciones de orden de magnitud, no compromisos.

---

## 0. Diagnóstico de partida

**Fortalezas que no hay que tocar** (son la ventaja competitiva y sería un error diluirlas):

- Fichas 3D dibujadas a mano con `Canvas` en `DominoTileView.kt`. Nadie en gratis lo iguala.
- Plantillas de serpentina (5 recorridos) con encaje geométrico real de las fichas.
- 8 modalidades con reglas auténticas: Matador, Húngaro, Draw Special (clave 3-2, tumbao).
- Banda sonora original, loops alineados al compás, manifiesto de licencias y test que lo blinda.
- 32 tests, CI en verde, 6 idiomas, 0 permisos, 0 red, 0 anuncios.
- Carrera persistente con cierre tolerante a datos corruptos.

**Los tres agujeros que impiden competir en Play Store**:

1. **La IA no juega**: `executeAIMove()` elige la primera ficha jugable
   ([DominoEngine.kt:482](app/src/main/java/com/example/dominograndprix/engine/DominoEngine.kt#L482)).
   Es el problema número uno del juego.
2. **No hay juego con humanos**: ni online ni en el mismo dispositivo.
3. **No hay razón para volver mañana**: sin logros, sin desafíos, sin progresión de dificultad.

---

## 1. Prioridad 1 — Que la IA juegue de verdad

**Problema.** Los tres rivales (Carlos, Mateo, Rodrigo) comparten la misma lógica trivial:
primera ficha jugable, y si la personalidad es `AGGRESSIVE`, la de más pips. `Personality` es
una etiqueta decorativa ([DominoModels.kt](app/src/main/java/com/example/dominograndprix/model/DominoModels.kt)).
Un jugador con dos dedos de frente gana siempre y se aburre en la tercera partida.

**Prerrequisito técnico (bloquea todo lo demás).** `DominoEngine` usa
`Random(System.currentTimeMillis())` para barajar ([línea 86](app/src/main/java/com/example/dominograndprix/engine/DominoEngine.kt#L86)).
Sin poder inyectar una semilla no hay benchmark reproducible ni desafío diario.

```
1.1  Inyectar RNG                        DominoEngine.kt                 ~2 h    Riesgo: bajo
     constructor(..., random: Random = Random.Default)
     startNewRound() usa deck.shuffle(random)

1.2  Extraer la decisión de la IA        engine/ai/ (paquete nuevo)      ~1 día  Riesgo: bajo
     interface DominoStrategy { fun decide(view: PlayerView): AiMove }
     sealed class AiMove { Play(tile, side), Draw, Pass }
     PlayerView: mano, extremos abiertos, cadena visible, boneyard, asiento,
                 rama propia, número de turno
     El motor solo construye la vista y ejecuta la jugada devuelta.

1.3  Nivel FÁCIL                          ai/EasyStrategy.kt              ~2 h    Riesgo: nulo
     El comportamiento actual + elección aleatoria entre las jugables.
     Objetivo: que pierda y que parezca humano, no que sea tonto de forma obvia.

1.4  Nivel MEDIO (heurística)             ai/MediumStrategy.kt            ~2-3 d  Riesgo: medio
     Puntuar cada jugada legal con una función de evaluación:
       + irse de mano (ganar la ronda)                 peso altísimo
       + vaciar pips de la mano                        proporcional a pips
       + dejar extremos múltiplos de 5/3 si el modo puntúa
       - abrir el 6 si el rival tiene muchos turnos por delante
       - dejar un extremo que el rival pueda cerrar
       + guardar los dobles para el final
       + en Matador, conservar los 4 comodines
       + en Draw Special, no invadir la rama ajena salvo ventaja clara
     Elegir la jugada de mayor puntuación.

1.5  Nivel DIFÍCIL (memoria y conteo)     ai/HardStrategy.kt             ~3-5 d  Riesgo: medio
     Medio + modelo del rival:
       - fichas vistas (tablero + las que él ha soltado) => qué pips quedan vivos
       - probabilidad de que el rival tenga cada pip, dado el pozo restante
       - decidir bloquear cuando va por delante en pips
       - jugar "a la mano": forzar al rival a robar o pasar
```

**Cómo se comprueba (esto es lo que convierte la mejora en verificable):**

```
1.6  Benchmark A/B enfrentado              test/ai/AiStrengthBenchmarkTest.kt
     Con semilla fija, MEDIO contra FÁCIL jugando las MISMAS manos y alternando
     asientos (diseño por parejas). Criterio de aceptación: MEDIO gana >= 55%
     de 500 parejas. Y DIFÍCIL >= 55% contra MEDIO.
     Sin este test, "mejorar la IA" es una opinión.

1.7  Invariantes de la IA                  test/ai/AiInvariantsTest.kt
     Nunca devuelve una jugada ilegal en 50.000 decisiones sobre estados generados.
     En Matador, nunca juega fuera de regla. En Draw Special, nunca invade sin pase previo.
```

**Impacto en producto:** es el mayor salto de calidad por hora invertida de todo el plan. Pasa
de "no tiene juego" a "tiene un juego al que quieres ganar".

---

## 2. Prioridad 1 — Guardar la partida en curso

**Problema.** `DominoTableScreen` guarda el motor en `remember { mutableStateOf(DominoEngine(...)) }`.
Si el sistema mata el proceso a mitad de un match, se pierde la partida y las apuestas. Es el
tipo de cosa que genera reseñas de 1 estrella.

```
2.1  Snapshot del motor                    data/MatchSnapshot.kt           ~2-3 d  Riesgo: alto
     @Serializable con: modo, ciudad, apuesta, patrón, semilla del RNG, índice de
     jugador actual, número de turno, cadena colocada, extremos abiertos, pozo y
     las cuatro manos. Reutiliza el patrón de CareerProgressStore.

2.2  save/restore en el motor              DominoEngine.kt                 ~1 d    Riesgo: alto
     Constructor secundario que reconstruye el estado completo, y toSnapshot().
     El punto delicado: estados intermedios (encadenamiento húngaro a medias,
     ventana de invasión de Draw Special). Hay que decidir si se serializan o si
     se guarda solo al cerrar ronda.

     RECOMENDACIÓN: empezar por guardar SOLO al terminar cada ronda
     (isRoundOver == true). Es el 80% del valor con el 20% del riesgo.

2.3  Ciclo de vida                          ui/game/, MainActivity.kt       ~3 h    Riesgo: bajo
     Guardar en onStop; ofrecer "Continuar partida" en el mapa si hay snapshot vivo.

2.4  Versionado del esquema                (dentro de MatchSnapshot)       ~2 h    Riesgo: bajo
     Campo schemaVersion. Si no coincide, se descarta el snapshot y se avisa.
     Decisión consciente: ante la duda, perder la partida antes que crashear.
```

**Verificación:** test de instrumentación que guarda, mata el proceso, relanza y comprueba que
la partida se restaura. Más un test unitario de ida y vuelta del snapshot.

---

## 3. Prioridad 2 — Juego con humanos: pass-and-play local

**Problema.** No existe ninguna forma de jugar con otra persona. Online es caro (backend,
cuentas, matchmaking); **pass-and-play no necesita nada de eso** y cubre la mitad de la
necesidad social.

```
3.1  Parametrizar los asientos humanos     DominoEngine.kt, DominoModels.kt  ~1 d  Riesgo: medio
     Hoy hay un `0` hardcodeado por todas partes (el motor asume que el humano es el
     asiento 0). Introducir humanPlayerIndices: Set<Int> y sustituir las comprobaciones
     por currentPlayerIndex in humanPlayerIndices.
     Puntos a tocar: el dispatch de executeAIMove, la condición de turno en la UI y
     los badges de jugador.

3.2  Pantalla de "pasa el móvil"           ui/game/PassDeviceScreen.kt      ~4 h  Riesgo: bajo
     Entre turnos de distintos humanos: cortina a pantalla completa con "Pasa el móvil
     a Mateo" y botón "Listo". Imprescindible para no destapar la mano.

3.3  Modo 2 jugadores 1v1                  configuración de partida         ~1 d  Riesgo: bajo
     Sin equipos, con las mismas 8 modalidades. También 2v2 en el mismo dispositivo
     (4 humanos) sale casi gratis una vez hecho 3.1.

3.4  Verificación                          test + instrumentación           ~3 h  Riesgo: bajo
     Una partida de 4 humanos se juega entera sin que la IA mueva ficha.
     Y una de 1 humano + 3 IA sigue comportándose igual que antes (no romper lo actual).
```

**Impacto:** añade el eje social sin coste de servidores. Es la mejora más rentable después de la IA.

---

## 4. Prioridad 2 — Accesibilidad (hoy es cero)

**Problema.** `DominoTileView` es un `Canvas`: **TalkBack no puede leerlo**. No hay
`contentDescription`, ni `stateDescription`, ni escalado de texto, ni modo daltónico.

```
4.1  Semántica de las fichas               DominoTileView.kt               ~4 h  Riesgo: bajo
     semantics { contentDescription = "[6|3]", stateDescription = "jugable" }
     Los botones y badges de la mesa necesitan lo mismo.

4.2  Escalado de texto                     toda la UI                      ~4 h  Riesgo: bajo
     Hay muchos `fontSize = 11.sp` fijos. Revisar que nada se rompa con
     "tamaño de fuente grande" del sistema. Usar unidades escalables y no dp fijos.

4.3  Modo daltónico + contraste            theme/, DominoTileView.kt       ~4 h  Riesgo: bajo
     El dorado sobre verde oscuro y los estados "jugable / no jugable" (hoy solo
     atenuados por color) necesitan un segundo indicador no cromático.

4.4  Verificación                          instrumentación                 ~4 h  Riesgo: bajo
     Un test que recorra la mesa y compruebe que cada ficha expone su descripción.
```

**Por qué importa más de lo que parece:** es requisito de facto para colecciones destacadas y
para el sello de accesibilidad; y en un juego de mesa, excluir a alguien por no ver bien es
especialmente absurdo.

---

## 5. Prioridad 2 — Retención: desafío diario y logros

**Problema.** La carrera se recorre una vez y se acaba. Cero motivos para volver mañana.

```
5.1  Desafío diario                        data/, engine/                  ~1-2 d  Riesgo: bajo
     Ya está resuelto el prerrequisito con la semilla inyectable (1.1):
     seed = hash(fecha) => misma mano y mismos rivales para todos ese día.
     Un intento al día, con racha de días consecutivos.
     Barato de construir y es el mayor generador de retorno diario que existe.

5.2  Logros                                data/CareerProgressStore.kt     ~1-2 d  Riesgo: medio
     Extender JourneyProgress con unlockedAchievements: Set<String> y contadores
     (partidas ganadas, dominós, cadenas húngaras, ciudades completadas).
     Catálogo de ~20 logros en un objeto de datos.
     OJO: el JSON de JourneyProgress es estricto a propósito; al añadir campos hay que
     decidir si se sube schemaVersion (y se pierde progreso) o se relaja el decoder.
     Hoy el decoder es tolerante con claves que SOBRAN, pero no con claves que FALTAN
     en el esquema (tienen valor por defecto, así que en la práctica sí se puede
     añadir campos sin romper). Verificar con un test antes de tocar.

5.3  Misiones por ciudad                   data/WorldTourRepository.kt     ~1 d    Riesgo: bajo
     Objetivos concretos por sede ("gana sin robar del pozo", "cierra con dominó")
     que den un motivo para rejugar una ciudad ya completada.
```

**Verificación:** test de migración del JSON de progreso (guardado de la versión anterior se
carga sin perder nada), y test de que dos llamadas con la misma fecha producen la misma partida.

---

## 6. Prioridad 3 — Economía, dificultad y tensión

**Problemas concretos.**

- No se puede perder dinero: si pierdes, no pagas nada más, y si te arruinas te recargan
  (`Navigation.kt`, bloque `else if (playerCash < 100)`). No hay tensión.
- Los rivales no escalan: solo cambia el modo y la apuesta.
- El jugador inicial se decide siempre por el doble más alto de la ronda, sin arrastrar
  ventaja del ganador anterior.
- `isMatchOver` puede activarse a mitad de ronda en los modos de puntuación y la partida
  continúa hasta que alguien cierra la mano con dominó. Es discutible, no un fallo.

```
6.1  Reglas económicas reales              Navigation.kt, modelo            ~1 d  Riesgo: bajo
     Apuesta de entrada que se pierde de verdad, bote que crece, y bloqueo de
     inscripción si no puedes pagarla (hoy se entra igual y se cobra sin fondos).
     Decidir si el "rescate" de 300 se queda como red de seguridad o se elimina.

6.2  Dificultad por sede                    ai/, WorldTourRepository.kt     ~4 h  Riesgo: bajo
     Cada ciudad con su Strategy: La Habana FÁCIL, Las Vegas DIFÍCIL. Ahora mismo
     las 7 sedes se sienten igual.

6.3  Arrastre de turno inicial               DominoEngine.kt                ~3 h  Riesgo: medio
     startNewRound() siempre recalcula por doble más alto; valorar que el ganador de
     la ronda anterior abra la siguiente (convención habitual en club).

6.4  Revisar el momento de fin de partida    DominoEngine.kt                ~3 h  Riesgo: medio
     Decidir si al alcanzar targetScore en un modo de puntuación se cierra ya el match
     o se termina la ronda. Documentar la decisión en el propio código.
```

---

## 7. Prioridad 3 — Tacto y presentación

**Problema.** Colocar una ficha no produce ninguna respuesta: la ficha aparece de golpe. En un
juego de mesa digital, el tacto es parte del producto.

```
7.1  Animación de colocación               DominoTableScreen.kt            ~4-6 h  Riesgo: bajo
     Aparición con escala desde 0.85 y asentamiento (spring), 120-180 ms.
     La cámara ya tiene desplazamiento vertical (verticalCameraVirtY): animar su
     transición en lugar de saltar de golpe.

7.2  Háptica                                DominoTableScreen.kt           ~2 h   Riesgo: bajo
     Vibración corta al colocar, distinta al cerrar con dominó.
     Usar HapticFeedbackType y respetar el ajuste del sistema.

7.3  Feedback de turno                      UI                             ~3 h   Riesgo: bajo
     Indicar con claridad cuándo el turno pasa al rival, y animar el marcador cuando
     alguien puntúa (hoy solo cambia un número).
```

---

## 8. Prioridad 3 — Publicación y calidad técnica

```
8.1  Activar R8                             build.gradle.kts               ~2 h   Riesgo: medio
     Las reglas ya están escritas. Requiere verificación en dispositivo:
     ./gradlew :app:connectedDebugAndroidTest (CareerProgressInstrumentedTest).
     Solo activar si pasa. Criterio ya documentado en el propio build.gradle.kts.

8.2  Reducir peso                           resources                      ~3 h   Riesgo: bajo
     14,7 MB de APK, ~1,7 MB de música. Valorar si las 15 pistas caben todas o si
     algunas ciudades pueden compartir tema.

8.3  Recursos de sistema localizados        res/values-*/                  ~2 h   Riesgo: bajo
     strings.xml tiene 3 líneas y no hay values-en/fr/it/pt/de. La ficha de Play sí
     está traducida, pero app_name y textos de sistema no. Incoherente con los 6 idiomas
     que sí soporta la app.

8.4  Reseñas y ficha                        Play Console                   ~1 d   Riesgo: nulo
     La ficha describe 8 modalidades y música original; conviene añadir capturas de
     Matador y del modo húngaro encadenando, que es donde el juego se diferencia.
```

---

## 9. Lo que NO hay que hacer (y por qué)

| Tentación | Por qué no |
|---|---|
| **Multijugador online** | Backend, cuentas, matchmaking, reconexión, anti-abandono, moderación y coste mensual. Es otro proyecto, no una tarea. Y sin retención demostrada, no se justifica. |
| **Reescribir las fichas con un motor 3D** | El render actual es la ventaja competitiva y funciona. Un motor añade peso, dependencias y licencias. |
| **Portar a iOS ahora** | Compose Multiplatform no es un port directo: es una reescritura de la capa de UI. Primero validar tracción en Android. |
| **Meter anuncios o IAP** | Hoy "0 anuncios, 0 permisos, 0 datos" es un argumento de venta real. No diluirlo antes de tener retención. |
| **Más modalidades de juego** | Ya hay 8, más que la competencia. El cuello de botella no es cantidad de reglas, es la IA y la progresión. |
| **Refactor del motor a ViewModel** | Tentador técnicamente, pero no aporta al jugador. Anotarlo y hacerlo solo si el guardado de partida (punto 2) lo exige. |

---

## 10. Orden recomendado

**Fase 1 (1-4 semanas) — Que el juego sea un juego**
1. RNG inyectable (1.1) — bloquea lo demás.
2. IA: extracción + MEDIO + benchmark A/B (1.2, 1.4, 1.6).
3. Guardado de partida al cerrar ronda (2.1, 2.2 versión simple).
4. Háptica y animación de colocación (7.1, 7.2).

**Fase 2 (5-8 semanas) — Que se pueda jugar con alguien**
5. Asientos humanos parametrizados + pass-and-play (3.1-3.3).
6. Accesibilidad (4.1-4.3).
7. Desafío diario (5.1).

**Fase 3 (9-12 semanas) — Motivos para volver**
8. Logros y misiones por ciudad (5.2, 5.3).
9. Economía y dificultad por sede (6.1, 6.2).
10. R8, recursos localizados y peso (8.1-8.3).

**Más adelante, solo si hay tracción:** IA DIFÍCIL con conteo (1.5), guardado en cada jugada,
online.

---

## 11. Cómo saber si está funcionando

Sin métricas, esto es una lista de deseos.

| Señal | Cómo se mide | Objetivo |
|---|---|---|
| La IA es un rival | Benchmark A/B con semilla | MEDIO ≥55% contra FÁCIL |
| No hay jugadas ilegales | Test de invariantes | 0 fallos en 50.000 decisiones |
| Se puede retomar | Test de instrumentación | Partida restaurada tras matar el proceso |
| Accesibilidad | Test de semántica | 100% de fichas con descripción |
| Retención D1 | Play Console / Firebase (si se añade) | >35% a las 6 semanas de lanzar |
| Estabilidad | Play Console | >99,5% sesiones sin fallo |

---

## 12. Convenciones del proyecto (recordatorio)

- **Antes de tocar reglas del motor, primero el test.** Es lo que ha salvado este proyecto.
- Un modo nuevo sin test de reglas no está terminado.
- Nuevo audio => entrada en `AUDIO_LICENSES.md` o el CI se pone en rojo (`AudioLicenseManifestTest`).
- Regenerar música con `tools/make_music.py --out <dir>` si el juego usa pistas de terceros.
- Comentarios y mensajes de commit en español, nombres de código en inglés.
- No commitear binarios (`*.apk`, `*.aab` ya están en `.gitignore`).


