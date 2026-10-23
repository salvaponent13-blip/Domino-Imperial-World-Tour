# Capturas v2.5.0 — set corregido

Cinco capturas reales, **a sangre** (sin barras ni marcos) y a **1080x1920 (16:9 nativo)**, sacadas
del APK release en un emulador configurado a esa resolución. Cumplen el límite de Play (máximo
2:1) sin recortes.

| # | Archivo | Qué se ve | Por qué está |
|---|---|---|---|
| 1 | `01_mesa_con_pista.png` | Mesa en juego, cadena en serpentina, HUD con dos rivales, botones flotantes de deshacer y pista, y la pista activa: "Prueba [3\|3] a la izquierda" | Es la que vende: se entiende en dos segundos y enseña una función que el jugador usará |
| 2 | `02_titulo.png` | Portada con el logo limpio, torneo, partida rápida y el desafío diario con la racha | Ancla la marca y avisa de que hay contenido diario |
| 3 | `03_sede_mexico.png` | Sede de Ciudad de México: reto, modalidad, meta y rival | Explica la estructura de la gira sin monedas ni apuestas |
| 4 | `04_partida_rapida.png` | Configurador con modalidad, objetivo, jugadores, formato, dificultad y **"2 humanos (pasa el móvil)"** | Enseña la profundidad y el pass-and-play |
| 5 | `05_ocho_modalidades.png` | Guía de las 8 modalidades con la estrategia de cada una | El argumento contra los clones de un solo modo |

## Lo que se descartó y por qué

- **Capturas con barras laterales** y resoluciones 2:1 recortadas: el contenido no llegaba al
  borde y se leía como un fallo de exportación.
- **Tablets**: la app es retrato; meter un móvil en un marco dentro de un lienzo 16:9 no
  representa la experiencia de tablet. Play permite no subir capturas de tablet.
- **Logros y vitrina**: salen vacíos en una instalación nueva ("0/14 desbloqueados", vitrina sin
  trofeos) y no venden nada. Mejor cuando el jugador tenga progreso real.

## Qué falta (y por qué no lo hice aquí)

- **All Fives con spinner de 4 ramas** y **resumen de ronda con desglose**: requieren jugar una
  partida de puntos hasta el final. En el emulador se puede, pero es lento y ciego; en tu móvil
  lo sacas en dos minutos y con la partida ya jugada de verdad.
- **Racha de varios días** en el desafío diario: necesita días reales de juego.

Cuando las tengas, ponlas en la posición 1 y 3: son las dos que más pesan.

## `_anteriores_compuestas/`

El set anterior (8 móvil + 8 con fondo + 16 de tablet) se guardó aquí como referencia. Tiene
barras laterales y las pantallas de móvil dentro de marcos de tablet, así que no lo subiría.
