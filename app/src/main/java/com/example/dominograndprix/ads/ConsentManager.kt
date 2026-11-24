package com.example.dominograndprix.ads

import android.app.Activity
import android.util.Log
import com.example.dominograndprix.billing.BillingManager
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consentimiento RGPD mediante Google UMP (User Messaging Platform).
 *
 * Google exige un CMP certificado para mostrar anuncios a usuarios del EEE y del
 * Reino Unido: sin él las impresiones pueden considerarse no conformes y la cuenta
 * de AdMob puede recibir una restricción. Aquí se actualiza el consentimiento y se
 * muestra el formulario solo cuando la normativa lo exige.
 *
 * No bloquea el arranque: si el formulario aún no está activo en AdMob, no hay red
 * o el usuario tarda en decidir, la app sigue funcionando sin errores y habilita
 * el SDK de anuncios una vez resuelta la consulta.
 */
object ConsentManager {
    private const val TAG = "ConsentManager"

    @Volatile
    private var requested = false

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /** Pide/actualiza el consentimiento y ejecuta [onAdsReady] cuando se pueden solicitar anuncios. */
    fun ensureConsent(activity: Activity, onAdsReady: () -> Unit = {}) {
        if (BillingManager.isPro() || AdManager.adsDisabled) return

        val notified = AtomicBoolean(false)
        fun notifyReady() {
            _canRequestAds.value = true
            if (notified.compareAndSet(false, true)) {
                onAdsReady()
            }
        }

        if (requested) {
            if (_canRequestAds.value) notifyReady()
            return
        }
        requested = true

        var consentInfo: ConsentInformation? = null
        try {
            val params = ConsentRequestParameters.Builder().build()
            val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
            consentInfo = info

            // Si ya existe consentimiento válido de una sesión previa, permitimos cargar sin esperar a la red.
            if (info.canRequestAds()) {
                notifyReady()
            }

            info.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "Formulario de consentimiento no disponible aún: ${formError.message}")
                        }
                        if (info.canRequestAds()) {
                            notifyReady()
                        } else {
                            Log.i(TAG, "Sin consentimiento válido (canRequestAds=false): no se inicializa AdMob.")
                        }
                    }
                },
                { requestError ->
                    Log.w(TAG, "No se pudo actualizar el consentimiento (${requestError.MessageSafe}).")
                    if (info.canRequestAds()) {
                        notifyReady()
                    } else {
                        Log.i(TAG, "Sin consentimiento previo tras error de actualización (canRequestAds=false): no se inicializa AdMob.")
                    }
                },
            )
        } catch (e: Exception) {
            // Nunca debe tumbar el arranque de la app.
            Log.w(TAG, "Error inicializando UMP: ${e.message}")
            if (consentInfo?.canRequestAds() == true) {
                notifyReady()
            } else {
                Log.i(TAG, "Excepción UMP sin consentimiento previo (canRequestAds=false): no se inicializa AdMob.")
            }
        }
    }

    private val com.google.android.ump.FormError.MessageSafe: String
        get() = "${errorCode}: ${message}"
}
