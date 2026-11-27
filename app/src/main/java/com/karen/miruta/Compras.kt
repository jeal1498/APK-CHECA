package com.karen.miruta

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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

/**
 * Suscripción de Google Play (Fase 7, solo canal play): producto `checa_acceso` con tres planes
 * básicos (mensual, semestral y anual). La app solo inicia la compra; quien da la licencia es
 * `play-verificar` en Supabase, que consulta a Google, confirma la compra y firma la licencia.
 */
object Compras {
    const val PRODUCTO = "checa_acceso"

    /** Paquete de Checa (1m, 6m, 12m) → plan básico de Google Play. */
    private val BASES = mapOf("1m" to "mensual", "6m" to "semestral", "12m" to "anual")

    private val principal = Handler(Looper.getMainLooper())
    private var cliente: BillingClient? = null
    private var detalles: ProductDetails? = null
    private var contexto: Context? = null
    private var alResultado: ((String) -> Unit)? = null
    private var ultimaRestauracion = 0L

    /** ¿Hay planes activos en Play? Con los planes en borrador no hay nada que ofrecer. */
    @Volatile var disponible = false
        private set

    private val escuchar = PurchasesUpdatedListener { r, compras ->
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> compras?.forEach { procesar(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> avisar("")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                avisar("Ya tienes esta suscripción. Revisando tu licencia…")
                restaurar(force = true)
            }
            else -> avisar("No se pudo completar la compra. Inténtalo más tarde.")
        }
    }

    private fun avisar(mensaje: String) { principal.post { alResultado?.invoke(mensaje) } }

    private fun conectar(listo: (BillingClient?) -> Unit) {
        val actual = cliente
        if (actual != null && actual.isReady) return listo(actual)
        val ctx = contexto ?: return listo(null)
        val nuevo = BillingClient.newBuilder(ctx)
            .setListener(escuchar)
            .enableAutoServiceReconnection()
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        cliente = nuevo
        nuevo.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                principal.post { listo(if (r.responseCode == BillingClient.BillingResponseCode.OK) nuevo else null) }
            }
            override fun onBillingServiceDisconnected() { cliente = null }
        })
    }

    /**
     * Prepara la conexión con Google Play y busca los planes. [resultado] recibe los avisos de cada
     * compra (texto vacío = no hay nada que decir); se llama en el hilo principal.
     */
    fun iniciar(ctx: Context, resultado: (String) -> Unit, listo: (Boolean) -> Unit) {
        contexto = ctx.applicationContext
        alResultado = resultado
        if (detalles != null) return listo(disponible)
        conectar { c ->
            if (c == null) { disponible = false; return@conectar listo(false) }
            val producto = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCTO).setProductType(BillingClient.ProductType.SUBS).build()
            c.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(producto)).build()) { r, resultado ->
                detalles = if (r.responseCode == BillingClient.BillingResponseCode.OK) resultado.productDetailsList.firstOrNull() else null
                // Solo se ofrece si hay al menos un plan básico activo.
                disponible = BASES.values.any { base -> oferta(base) != null }
                principal.post { listo(disponible) }
            }
        }
    }

    /** La oferta sin descuentos especiales del plan básico [base]. */
    private fun oferta(base: String): ProductDetails.SubscriptionOfferDetails? =
        detalles?.subscriptionOfferDetails?.firstOrNull { it.basePlanId == base && it.offerId == null }

    /**
     * Abre la hoja de pago de Google Play para el paquete [plan] (1m, 6m o 12m). Con [fundador] usa el
     * plan básico `-fundador` (−40 %); si no existe, no se cobra el normal sin avisar.
     */
    fun comprar(act: Activity, plan: String, fundador: Boolean = false) {
        val pd = detalles
        val of = BASES[plan]?.let { oferta(it + if (fundador) "-fundador" else "") }
        if (pd == null || of == null) return avisar("Este plan aún no está disponible. Inténtalo más tarde.")
        conectar { c ->
            if (c == null) return@conectar avisar("Google Play no responde. Inténtalo más tarde.")
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(pd).setOfferToken(of.offerToken).build()
                ))
                .build()
            val r = c.launchBillingFlow(act, params)
            if (r.responseCode != BillingClient.BillingResponseCode.OK) {
                avisar("No se pudo abrir Google Play. Inténtalo más tarde.")
            }
        }
    }

    /** Revisa las compras de esta cuenta de Google Play y las vuelve a verificar (restaurar). */
    fun restaurar(force: Boolean = false) {
        val ahora = System.currentTimeMillis()
        if (!force && ahora - ultimaRestauracion < 10 * 60_000L) return
        ultimaRestauracion = ahora
        conectar { c ->
            c?.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { r, lista ->
                if (r.responseCode == BillingClient.BillingResponseCode.OK) lista.forEach { procesar(it, silencioso = true) }
            }
        }
    }

    private fun procesar(compra: Purchase, silencioso: Boolean = false) {
        if (PRODUCTO !in compra.products) return
        if (compra.purchaseState == Purchase.PurchaseState.PENDING) {
            if (!silencioso) avisar("Tu pago está pendiente. Cuando se confirme, tu licencia se activa sola.")
            return
        }
        if (compra.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val ctx = contexto ?: return
        if (!Cuenta.conectada(ctx)) {
            if (!silencioso) avisar("Entra con tu cuenta de Google para activar tu licencia.")
            return
        }
        Cuenta.verificarCompra(ctx, compra.purchaseToken) { ok, mensaje ->
            if (ok || !silencioso) avisar(mensaje)
        }
    }
}
