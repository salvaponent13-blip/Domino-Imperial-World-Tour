# Notas de versión — v2.5.3 (versionCode 19)

> Integración oficial de publicidad no invasiva Google AdMob (Banner e Intersticial entre partidas) con política de privacidad actualizada para la fase de prueba cerrada antes del acceso a producción.

## Para pegar en Play Console (≤ 500 caracteres por idioma)

```text
<es-ES>
• Integración y optimización de servicio para la prueba cerrada.
• Banner de pie adaptable en el menú principal.
• Intersticial al finalizar partidas con intervalo de seguridad.
• Mejoras de estabilidad y rendimiento general.
</es-ES>
<en-US>
• Service optimization and integration for closed testing.
• Adaptive banner on the main menu.
• Post-match interstitial with cooldown protection.
• General performance and stability improvements.
</en-US>
```

---

## Cambios técnicos en v2.5.3:
- SDK `com.google.android.gms:play-services-ads:23.6.0` añadido.
- Permisos `INTERNET`, `ACCESS_NETWORK_STATE` y `AD_ID` declarados en el manifiesto.
- Meta-data oficial `APPLICATION_ID` inyectado mediante `manifestPlaceholders`.
- Arquitectura desacoplada en paquete `com.example.dominograndprix.ads`:
  - `AdConfig`: IDs centralizados y validación de prefijos.
  - `AdManager`: Precarga en background con `applicationContext` y cooldown de 60s.
  - `AdBanner`: Banner adaptativo Compose con destrucción de vista (`onRelease`).
  - `AdConfigTest`: Tests unitarios de validación de formato e integridad de IDs.
- Política de privacidad sincronizada y actualizada en Google Sites y documentación oficial.
