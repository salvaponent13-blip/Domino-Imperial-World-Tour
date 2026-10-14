# Checklist de QA en dispositivo — Release Candidate v2.1.1

Sesión de comprobación física en teléfono. Todo lo que hay aquí **no se puede verificar con
tests unitarios**: o necesita hardware (vibración, pantalla, altavoz), o necesita el árbol de
accesibilidad real de Android, o necesita ojos humanos.

## Build bajo prueba

| Dato | Valor |
|---|---|
| Versión | `versionName 2.1.1` / `versionCode 5` |
| APK release | `app/build/outputs/apk/release/app-release.apk` — **17,18 MB** |
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` — **21,11 MB** |
| Firma | APK Signature Scheme **V2**, certificado `CN=Salvador Barroso Montero` |
| Suite al construir | **188 tests, 0 fallos** |

Para instalar por cable:

```
adb install -r app/build/outputs/apk/release/app-release.apk
```

El APK de release es el que se prueba a mano. El de debug solo si hace falta ver el log:

```
adb logcat | Select-String "DominoImperial"
```

---

## 1. TalkBack y accesibilidad

**Por qué no lo cubren los tests:** la semántica la resuelve el árbol de Compose en tiempo de
ejecución. El test `TileSemanticsInstrumentedTest` está escrito y compila, pero exige un
dispositivo.

| # | Comprobación | Cómo | Resultado |
|---|---|---|---|
| 1.1 | Activar TalkBack | Ajustes → Accesibilidad → TalkBack | ☐ |
| 1.2 | Las fichas se anuncian | Recorrer la mano con un dedo: debe decir *"Ficha seis tres, jugable"* | ☐ |
| 1.3 | Los dobles se distinguen | Una ficha `[4\|4]` debe decir *"Ficha doble cuatro"* | ☐ |
| 1.4 | El cero se dice "blanco" | Una ficha `[0\|5]` debe decir *"Ficha blanco cinco"* | ☐ |
| 1.5 | Se puede jugar a ciegas | Doble toque sobre una ficha jugable: debe colocarse | ☐ |
| 1.6 | Las no jugables no responden | Doble toque sobre una ficha apagada: **no** debe hacer nada | ☐ |
| 1.7 | El contorno discontinuo se ve | Mirar la mano: las no jugables llevan borde punteado (señal de forma, no de color) | ☐ |
| 1.8 | Botones del menú con nombre | Recorrer el menú principal: ningún botón debe anunciarse como "botón" sin más | ☐ |

**Fallo típico:** si una ficha se anuncia como "botón" o sin nombre, la semántica no llegó al
árbol. Es el caso que el test de instrumentación detectaría.

---

## 2. Pass-and-Play y cortina de privacidad

**Por qué no lo cubren los tests:** `PrivacyCurtainRulesTest` fija *cuándo* debe aparecer
(9 tests), pero que el overlay tape de verdad la mano **solo se ve ejecutándolo**.

| # | Comprobación | Cómo | Resultado |
|---|---|---|---|
| 2.1 | Entrar en partida local | Menú → 👥 PARTIDA LOCAL | ☐ |
| 2.2 | La cortina aparece al pasar | Jugar el turno del Jugador 1: al ceder el turno debe salir "PASA EL MÓVIL" | ☐ |
| 2.3 | **No se filtra la mano** | Con la cortina puesta, mirar los cuatro bordes y las esquinas: no debe verse ninguna ficha | ☐ |
| 2.4 | La cortina se traga los toques | Tocar la pantalla por debajo: no debe colocarse nada | ☐ |
| 2.5 | El botón la levanta | "LISTO, VER MI MANO" debe mostrar la mano del nuevo jugador | ☐ |
| 2.6 | Vuelve a salir en cada traspaso | Jugar varios turnos: debe salir en **cada** cambio de persona | ☐ |
| 2.7 | No sale contra la IA | Comprobar que al pasar el turno a la máquina no aparece la cortina | ☐ |

**Fallo típico:** un borde sin cubrir por el que se ve la mano un instante. Es el objetivo del
sprint y lo único que no se puede razonar sin verlo.

---

## 3. Tacto, animación y háptica

| # | Comprobación | Cómo | Resultado |
|---|---|---|---|
| 3.1 | Animación de colocación | Colocar una ficha: debe entrar con un rebote suave, no aparecer de golpe | ☐ |
| 3.2 | Las fichas ya puestas no saltan | Jugar por la **izquierda** (la cadena se reordena): las fichas anteriores no deben reanimarse | ☐ |
| 3.3 | Cámara suavizada | Cuando la cadena crece, el reencuadre debe deslizarse, no dar un salto seco | ☐ |
| 3.4 | El arrastre manual responde | Arrastrar el tablero con el dedo: debe seguir al dedo al instante, sin retardo | ☐ |
| 3.5 | Háptica seca al colocar | Vibración corta y seca | ☐ |
| 3.6 | Háptica rotunda al cerrar ronda | Vibración más contundente al cerrar la ronda | ☐ |
| 3.7 | Respeta el modo silencio | Poner el móvil en silencio: **no** debe vibrar | ☐ |
| 3.8 | Marcador con destello | Cuando alguien anota, el marcador debe latir y destellar | ☐ |

**Fallo típico:** que la vibración suene con el móvil en silencio. Si ocurre, es que se está
usando una vía que no respeta los ajustes del sistema.

---

## 4. Vitrina de logros y misiones

| # | Comprobación | Cómo | Resultado |
|---|---|---|---|
| 4.1 | Se abre desde el menú | Botón 🏆 del menú principal | ☐ |
| 4.2 | Se abre desde el mapa | Botón 🏆 de la barra del World Tour | ☐ |
| 4.3 | Cabecera de progreso | Debe decir "Logros: X / 15 desbloqueados (Y%)" y la barra debe ir acorde | ☐ |
| 4.4 | Los bloqueados muestran cómo conseguirlos | Cada logro apagado debe llevar su descripción | ☐ |
| 4.5 | Conseguido y bloqueado se distinguen | Dorado con "✓ CONSEGUIDO" frente a gris con "🔒 BLOQUEADO" | ☐ |
| 4.6 | **Aviso emergente al conseguir uno** | Ganar una partida que desbloquee un logro: debe salir el banner arriba con vibración | ☐ |
| 4.7 | El aviso se va solo | No debe quedarse pegado tapando la pantalla | ☐ |
| 4.8 | Misiones en la tarjeta de sede | En el mapa, cada ciudad debe mostrar "MISIONES (X/2)" con sus dos líneas | ☐ |
| 4.9 | La marca de misión cumplida | Completar una: debe pasar de ○ a ✓ con el texto tachado | ☐ |
| 4.10 | El conteo sube al instante | Desbloquear un logro y volver a la vitrina: el número debe haber subido | ☐ |

---

## 5. Economía

| # | Comprobación | Cómo | Resultado |
|---|---|---|---|
| 5.1 | Se cobra la entrada al entrar | Anotar la caja, entrar en La Habana ($100): debe bajar al instante | ☐ |
| 5.2 | Al ganar se cobra el premio | Ganar: debe ingresarse el doble de la apuesta | ☐ |
| 5.3 | **Al perder no se devuelve nada** | Perder: la caja debe quedarse como estaba tras pagar la entrada | ☐ |
| 5.4 | Bloqueo por fondos insuficientes | Con poca caja, una sede cara debe tener el botón apagado | ☐ |
| 5.5 | El aviso explica cuánto falta | Debe leerse "Fondos insuficientes (requiere $N)" | ☐ |
| 5.6 | Se puede farmear en sedes baratas | Con la caja justa, La Habana debe seguir siendo jugable | ☐ |
| 5.7 | Préstamo de honor una sola vez | Gastar hasta quedar por debajo de $100 y perder: debe salir el aviso del préstamo | ☐ |
| 5.8 | **El préstamo NO se repite** | Volver a quedar sin dinero y perder: **no** debe volver a concederse | ☐ |
| 5.9 | La caja sobrevive al cierre | Cerrar la app por completo y reabrir: la caja debe ser la misma | ☐ |

**Fallo típico en 5.8:** si el préstamo se repite, el dinero vuelve a ser infinito y la economía
pierde el sentido. Es la comprobación más importante de esta sección.

---

## 6. Música y audio

| # | Comprobación | Cómo | Resultado |
|---|---|---|---|
| 6.1 | *Cumbia de Garibaldi* en México | Viajar a Ciudad de México: debe sonar la cumbia nueva | ☐ |
| 6.2 | La atribución de licencia es correcta | Abrir Créditos: México debe figurar como *No Frills Cumbia*, Kevin MacLeod, CC-BY 4.0 | ☐ |
| 6.3 | Los bucles no tienen costura | Escuchar una pista más de 90 s: la repetición no debe oírse como un tropiezo | ☐ |
| 6.4 | El botón de silencio funciona | Silenciar y comprobar que calla; volver a activar | ☐ |
| 6.5 | El peso del audio se nota bien | La app debe instalarse rápido y pesar ~17 MB, no 46 MB | ☐ |

---

## 7. Comprobaciones de cierre

| # | Comprobación | Resultado |
|---|---|---|
| 7.1 | El nombre de la app sale en el idioma del móvil (probar cambiando el idioma del sistema a inglés y francés) | ☐ |
| 7.2 | La app no se cierra en ningún punto del recorrido completo | ☐ |
| 7.3 | Guardado y reanudación de partida en curso | ☐ |
| 7.4 | Desafío diario: se juega una vez y no deja reanudar | ☐ |
| 7.5 | Una partida completa de principio a fin sin cierres forzados | ☐ |

---

## Lo que queda pendiente tras esta sesión

Estos puntos **no** se cubren aquí y siguen abiertos:

- **R8 / minificación** (`isMinifyEnabled = false`): las reglas de `proguard-rules.pro` están
  escritas, pero activarlas sin poder ejecutar el APK minificado es un riesgo a ciegas. Se
  activa cuando haya un dispositivo para probar el APK ofuscado.
- **Calibración medida de la dificultad por sede** (punto 6.2): hace falta medir la tasa de
  victoria real contra cada nivel, como se hizo con las estrategias de IA.

## Cómo registrar el resultado

Anotar en cada casilla `OK` o el fallo concreto. Para los fallos visuales, una captura de
pantalla vale más que una descripción. Para los de accesibilidad, lo más útil es **lo que
TalkBack dijo literalmente**.
