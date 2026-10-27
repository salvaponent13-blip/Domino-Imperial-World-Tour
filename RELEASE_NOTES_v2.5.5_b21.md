# Notas de versión — v2.5.5 (versionCode 21)

> Versión de entrega para pedir el acceso a producción: incluye las mejoras de IA medidas
> (61,06 % frente a LEGACY, sin usar las manos ocultas), el banner anclado abajo, los botones de
> Valorar/Compartir y la actualización del SDK `androidx.fragment` que avisaba Play Console.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```text
<es-ES>
• Nuevo: valora la app y compártela desde Ajustes.
• El banner del menú ya queda fijo abajo, siempre visible.
• IA más justa (ya no ve tus fichas) y con niveles que se notan de verdad.
• Consentimiento de privacidad antes de los anuncios y opción de quitarlos.
• SDK actualizado y textos completos en los 6 idiomas.
</es-ES>
<en-US>
• New: rate and share the app from Settings.
• The menu banner now stays pinned at the bottom, always visible.
• Fairer AI (it no longer sees your tiles) with difficulty levels you can feel.
• Privacy consent before ads and an option to remove them.
• Updated SDK and complete text in all 6 languages.
</en-US>
```

---

## Cambios técnicos v2.5.5

- **IA:** `simulate()` estima los pips rivales por los 168 puntos del juego y deduce la tranca
  solo con información pública (`isBoardBlockedFairly`). Nuevos factores `suitAffinity` y
  `passExploit`. Benchmark incremental separado: factor 4 (puntuación) +0,38 y factor 5
  (afinidad de palo y fallas) +2,31 hasta el 61,06 % final.
- **Escritorio/portada:** `AdBanner` sale del `Column` con scroll y queda anclado al pie.
- **Ajustes:** botones Valorar (`market://details?id=…` con reserva web) y Compartir
  (`ACTION_SEND` con chooser).
- **Consentimiento:** `ConsentManager.canRequestAds` arranca en falso y se habilita también si el
  formulario UMP falla, para no bloquear los anuncios ni el arranque.
- **Facturación:** `BillingManager.init` antes de pedir anuncios; si el producto `remove_ads`
  todavía no existe en Play, el aviso es traducido y no deja botones bloqueados.
- **Dependencias:** `androidx.fragment:fragment-ktx:1.8.5` para quitar el aviso de Play Console
  sobre la versión obsoleta.
- **Tests:** 235 en verde, incluido el benchmark de fuerza y el test de juego limpio.

## Antes de subir

- `versionCode = 21` porque el 20 ya está usado en la pista alpha.
- Un solo artefacto en `PLAY_STORE_RELEASE/`: `DominoGrandPrix_v2.5.5_b21.aab` (y su APK).
