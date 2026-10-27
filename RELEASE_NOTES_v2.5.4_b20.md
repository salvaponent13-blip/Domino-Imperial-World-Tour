# Notas de versión — v2.5.5 (versionCode 21)

> Versión definitiva para Producción (`DominoGrandPrix_v2.5.5_b21.aab`).
> Incluye la compra **remove_ads** (2,50 €), consentimiento RGPD (UMP), botones de Valorar y Compartir en Ajustes, IA mejorada sin información oculta (61,06 % frente a Legacy) y actualización del SDK `androidx.fragment` (1.8.5).

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```text
<es-ES>
• Nuevo: opción PRO para quitar anuncios desde Ajustes y botón de restaurar compra.
• Añadidos botones de Valorar y Compartir aplicación en Ajustes.
• IA táctica mejorada: mayor dominio de palo y respuesta a fallas sin mirar manos ocultas.
• Traducción completa revisada en 6 idiomas (ES, EN, FR, IT, PT, DE).
• Actualización de librerías AndroidX (fragment 1.8.5) y mejoras de estabilidad.
</es-ES>
<en-US>
• New: one-time PRO purchase to remove ads from Settings, plus purchase restore.
• Added Rate App and Share App buttons in Settings.
• Improved tactical AI: smarter suit continuity and pass tracking with 100% fair play.
• Complete localization across 6 languages (ES, EN, FR, IT, PT, DE).
• Updated AndroidX libraries (fragment 1.8.5) and stability improvements.
</en-US>
```

---

## Cambios técnicos en v2.5.5 (build 21)

- **SDK actualizado (`androidx.fragment:fragment-ktx:1.8.5`):** elimina el aviso de Google Play Console sobre `androidx.fragment:fragment:1.1.0` obsoleto detectado en la versión 20.
- **Facturación y anuncios blindados:** `BillingManager.init` se ejecuta antes de `ConsentManager` y `AdManager`; el `AdBanner` queda anclado fijo en el pie fuera del scroll, nace en `View.GONE` mientras no haya inventario y reacciona en vivo a `BillingManager.pro`.
- **IA limpia y más fuerte (61,06 % winrate vs Legacy):** `DominoEngine.simulate` estima los puntos rivales sin espiar manos ocultas y `MediumStrategy` / `StrategyCatalog` incorporan afinidad de palo (`suitAffinity`) y explotación de fallas (`passExploit`), diferenciando `STANDARD` y `TOUGH` en los 8 modos.
- **Feedback de testers aplicado:** botones de **Valorar app** y **Compartir** en `SettingsDialog` y textos traducidos a los 6 idiomas en `DominoStrings`.
