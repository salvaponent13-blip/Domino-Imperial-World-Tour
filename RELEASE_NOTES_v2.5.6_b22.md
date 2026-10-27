# Notas de versión — v2.5.6 (versionCode 22)

> Añade el tutorial de la primera partida y la tarjeta que explica cada modalidad. Es la versión
> que sustituye a la b21 para la entrega.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```text
<es-ES>
• Nuevo tutorial de la primera partida: tres pasos sobre la mesa y opción de saltar.
• Cada modalidad se explica sola la primera vez que la juegas.
• Puedes volver a ver el tutorial cuando quieras desde Ajustes.
• Corrección del aviso de SDK obsoleto en Play Console.
</es-ES>
<en-US>
• New first-match tutorial: three steps on the table, skippable at any time.
• Each game mode explains itself the first time you play it.
• You can replay the tutorial whenever you want from Settings.
• Fixed the outdated SDK warning in Play Console.
</en-US>
```

---

## Qué incluye

- **Tutorial de primera partida** (`ui/tutorial/TutorialOverlay.kt`): tres pasos, se cierra con
  *Saltar*, y solo sale la primera vez. El paso de la mano se pinta arriba para no tapar las
  fichas; los del tablero, abajo.
- **Tarjeta por modalidad**: la primera vez que se juega un modo sale una tarjeta con su regla
  esencial (una línea por modo, en los 6 idiomas). Al terminar el tutorial se marca ese modo como
  explicado para no encadenar dos avisos seguidos.
- **Ajustes → "Ver tutorial otra vez"**: vuelve a activarlo para la próxima partida y avisa con un
  texto en el propio diálogo.
- **Persistencia** (`SettingsStore`): `tutorial_seen` y `mode_hints_seen` (conjunto de modos).
- **Textos** nuevos en `i18n/TutorialStrings.kt` (pasos, tarjeta y botones) en ES/EN/FR/IT/PT/DE.

## Verificado en emulador (1080x1920, APK release)

- Primera partida: sale el paso 1/3 arriba, con las fichas visibles.
- 2/3 y 3/3 avanzan y el botón cambia a *Entendido*; al pulsarlo desaparece.
- **Segunda partida: no vuelve a salir.**
- 235 tests en verde.

## Entrega

- `versionCode = 22`, `versionName = 2.5.6`.
- Un único artefacto: `PLAY_STORE_RELEASE/DominoGrandPrix_v2.5.6_b22.aab` (y su APK).
- Sustituye a la b21: si la 21 ya estaba en la pista, sube la 22.
