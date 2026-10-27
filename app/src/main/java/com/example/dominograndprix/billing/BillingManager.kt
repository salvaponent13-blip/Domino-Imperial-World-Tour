package com.example.dominograndprix.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.dominograndprix.ads.AdManager
import com.example.dominograndprix.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Resultado de una acción de compra o restauración. */
enum class BillingNotice { PURCHASED, CANCELED, FAILED, RESTORED, NOTHING, NOT_READY }

/**
 * Gestor de la compra integrada "remove_ads" (Google Play Billing 8.0.0).
 *
 * Características:
 * - Carga el estado PRO offline desde [SettingsStore] al iniciar para silenciar anuncios de inmediato.
 * - Sincroniza y valida la compra con los servidores de Google Play al conectar.
 * - Si el producto aún no está activo en Google Play Console o no hay red, responde de forma
 *   controlada con [BillingNotice.NOT_READY] sin bloquear la interfaz.
 * - Desactiva y limpia la caché de [AdManager] en cuanto se confirma el estado PRO.
 */
object BillingManager {
    private const val TAG = "BillingManager"

    private var client: BillingClient? = null
    private var appContext: Context? = null

    @Volatile
    private var product: ProductDetails? = null

    private val _pro = MutableStateFlow(false)
    val pro: StateFlow<Boolean> = _pro.asStateFlow()

    private val _price = MutableStateFlow(BillingConfig.REFERENCE_PRICE_EUR)
    val price: StateFlow<String> = _price.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _notice = MutableStateFlow<BillingNotice?>(null)
    val notice: StateFlow<BillingNotice?> = _notice.asStateFlow()

    fun isPro(): Boolean = _pro.value

    fun consumeNotice() {
        _notice.value = null
    }

    /** Inicializa el cliente de facturación de Google Play. */
    fun init(context: Context) {
        val app = context.applicationContext
        this.appContext = app

        // Estado inicial desde caché local (sin depender de red para ocultar anuncios al arrancar)
        if (SettingsStore.isPro(app)) {
            _pro.value = true
            AdManager.adsDisabled = true
            AdManager.clear()
        }

        if (client != null) return

        val listener = PurchasesUpdatedListener { result, purchases ->
            onPurchaseUpdate(result, purchases.orEmpty())
        }
        client = BillingClient.newBuilder(app)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()

        connect()
    }

    private fun connect(onResult: ((Boolean) -> Unit)? = null) {
        val c = client
        if (c == null) {
            onResult?.invoke(false)
            return
        }
        if (c.isReady) {
            onConnected()
            onResult?.invoke(true)
            return
        }
        try {
            c.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (ok) {
                        onConnected()
                    } else {
                        Log.w(TAG, "Billing setup no completado: ${result.debugMessage}")
                    }
                    onResult?.invoke(ok)
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Servicio de Billing desconectado; se reconectará al reintentar.")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Error al conectar con Google Play Billing: ${e.message}")
            onResult?.invoke(false)
        }
    }

    private fun onConnected() {
        queryProduct()
        queryPurchases(silent = true)
    }

    private fun queryProduct(onDone: ((ProductDetails?) -> Unit)? = null) {
        val c = client
        if (c == null || !c.isReady) {
            onDone?.invoke(null)
            return
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConfig.PRODUCT_REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        c.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryResult.productDetailsList.firstOrNull()
                if (details != null) {
                    product = details
                    val formatted = details.oneTimePurchaseOfferDetails?.formattedPrice
                    if (!formatted.isNullOrBlank()) {
                        _price.value = formatted
                    }
                }
                onDone?.invoke(details)
            } else {
                Log.w(TAG, "queryProductDetails falló: ${result.debugMessage}")
                onDone?.invoke(null)
            }
        }
    }

    private fun queryPurchases(silent: Boolean, onResult: ((Boolean) -> Unit)? = null) {
        val c = client
        if (c == null || !c.isReady) {
            onResult?.invoke(false)
            if (!silent) _notice.value = BillingNotice.NOT_READY
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        c.queryPurchasesAsync(params) { result, purchases ->
            val owns = result.responseCode == BillingClient.BillingResponseCode.OK &&
                applyOwnership(purchases)
            if (!silent) _notice.value = if (owns) BillingNotice.RESTORED else BillingNotice.NOTHING
            onResult?.invoke(owns)
        }
    }

    private fun applyOwnership(purchases: List<Purchase>): Boolean {
        val owned = purchases.filter {
            it.products.contains(BillingConfig.PRODUCT_REMOVE_ADS) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (owned.isEmpty()) return false
        owned.filter { !it.isAcknowledged }.forEach(::acknowledge)
        setPro(true)
        return true
    }

    private fun onPurchaseUpdate(result: BillingResult, purchases: List<Purchase>) {
        _busy.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (applyOwnership(purchases)) {
                    _notice.value = BillingNotice.PURCHASED
                } else {
                    _notice.value = BillingNotice.FAILED
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                if (applyOwnership(purchases)) {
                    _notice.value = BillingNotice.PURCHASED
                } else {
                    // Cuando Google Play responde ITEM_ALREADY_OWNED, la lista del callback puede
                    // venir vacía: consultamos las compras activas para activar PRO en firme.
                    queryPurchases(silent = false)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _notice.value = BillingNotice.CANCELED
            }
            else -> {
                _notice.value = BillingNotice.FAILED
            }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val c = client ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        c.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "acknowledge falló: ${result.debugMessage}")
            }
        }
    }

    /** Inicia el flujo de pago de Google Play. Si aún no está activo el producto, avisa sin colgarse. */
    fun buy(activity: Activity) {
        val existingDetails = product
        val c = client
        if (c != null && c.isReady && existingDetails != null) {
            launchFlow(activity, c, existingDetails)
            return
        }

        _busy.value = true
        connect { connected ->
            if (!connected) {
                _busy.value = false
                _notice.value = BillingNotice.NOT_READY
                return@connect
            }
            queryProduct { fetchedDetails ->
                val readyClient = client
                if (fetchedDetails != null && readyClient != null && readyClient.isReady && !activity.isFinishing) {
                    launchFlow(activity, readyClient, fetchedDetails)
                } else {
                    _busy.value = false
                    _notice.value = BillingNotice.NOT_READY
                }
            }
        }
    }

    private fun launchFlow(activity: Activity, c: BillingClient, details: ProductDetails) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        _busy.value = true
        val code = c.launchBillingFlow(activity, params).responseCode
        if (code != BillingClient.BillingResponseCode.OK) {
            _busy.value = false
            _notice.value = BillingNotice.FAILED
        }
    }

    /** Restaura compras anteriores esperando a reconectar si el servicio estaba desconectado. */
    fun restore() {
        _busy.value = true
        connect { connected ->
            if (connected) {
                queryPurchases(silent = false) { _busy.value = false }
            } else {
                _busy.value = false
                _notice.value = BillingNotice.NOT_READY
            }
        }
    }

    private fun setPro(value: Boolean) {
        if (value) {
            AdManager.adsDisabled = true
            AdManager.clear()
        }
        _pro.value = value
        appContext?.let { SettingsStore.setPro(it, value) }
    }
}
