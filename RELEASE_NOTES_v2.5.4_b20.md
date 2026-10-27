# Notas de versión — v2.5.4 (versionCode 20)

> Añade la compra **remove_ads** (quitar anuncios para siempre) sobre la publicidad de la v2.5.3.
> Es la versión que se promociona a producción.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```text
<es-ES>
• Nuevo: quita los anuncios para siempre con una compra única desde Ajustes.
• Restauración de compra incluida si cambias de móvil o reinstalas.
• Banner solo en el menú y un intersticial al cerrar la partida, nunca sobre la mesa.
• Consentimiento de privacidad (RGPD) antes de mostrar anuncios personalizados.
• Mejoras de estabilidad y rendimiento.
</es-ES>
<en-US>
• New: remove ads for good with a one-time purchase from Settings.
• Purchase restore included if you change phone or reinstall.
• Banner only in the menu and one interstitial after a match, never over the table.
• GDPR privacy consent before personalised ads.
• Stability and performance improvements.
</en-US>
```

---

## Cambios técnicos en v2.5.4

- Compra integrada `remove_ads` (producto único no consumible) con Google Play Billing:
  `BillingManager` con conexión, consulta de compras, `acknowledge` y **restauración** al abrir.
- El estado Pro solo se activa con la compra confirmada por Play; se guarda en `SettingsStore`
  y desactiva banner, intersticial y precarga.
- Consentimiento RGPD con **User Messaging Platform** (`ConsentManager`): sin consentimiento no
  se piden anuncios personalizados.
- IDs de AdMob: reales en release, de **prueba** en debug (`BuildConfig.DEBUG`) para no generar
  tráfico inválido durante el desarrollo.
- Ajustes: entrada "Quitar anuncios" con precio de referencia y estado de la compra.
- Tests unitarios de `AdConfig` y `BillingConfig`.

## Pendiente en Play Console (no se puede hacer desde el código)

- Monetización → Productos dentro de la app → crear `remove_ads` (2,49 €) y **activarlo**.
- Contenido de la aplicación → **Anuncios: Sí** y **Compras dentro de la app: Sí**.
- Seguridad de los datos → ID de publicidad y diagnóstico (AdMob).
- Clasificación IARC → repetir el cuestionario (ha cambiado por anuncios y compras).
- AdMob → Privacidad y mensajería → publicar el mensaje de consentimiento (RGPD/UMP).
- Probar la compra con una cuenta de licencia de prueba antes de producción.
