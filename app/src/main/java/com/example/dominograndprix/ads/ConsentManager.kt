package com.example.dominograndprix.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Consentimiento RGPD mediante Google UMP (User Messaging Platform).
 *
 * Google exige un CMP certificado para mostrar anuncios a usuarios del EEE y del
 * Reino Unido: sin él las impresiones pueden considerarse no conformes y la cuenta
 * de AdMob puede recibir una restricción. Aquí se actualiza el consentimiento y se
 * muestra el formulario solo cuando la normativa lo exige.
 *
 * No bloquea el arranque: si no hay red o el usuario tarda en decidir, la app sigue
 * funcionando y el SDK de anuncios aplica el estado que UMP tenga guardado (anuncios
 * limitados cuando no hay consentimiento).
 */
object ConsentManager {
    private const val TAG = "ConsentManager"

    @Volatile
    private var requested = false

    /** Pide/actualiza el consentimiento. Seguro de llamar en cada onCreate. */
    fun ensureConsent(activity: Activity) {
        if (requested) return
        requested = true

        try {
            val params = ConsentRequestParameters.Builder().build()
            val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

            info.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "Formulario de consentimiento: ${formError.message}")
                        }
                        if (!info.canRequestAds()) {
                            Log.i(TAG, "Sin consentimiento: solo anuncios limitados.")
                        }
                    }
                },
                { requestError ->
                    Log.w(TAG, "No se pudo actualizar el consentimiento: ${requestError.message}")
                },
            )
        } catch (e: Exception) {
            // Nunca debe tumbar el arranque de la app.
            Log.w(TAG, "Error inicializando UMP: ${e.message}")
        }
    }
}
