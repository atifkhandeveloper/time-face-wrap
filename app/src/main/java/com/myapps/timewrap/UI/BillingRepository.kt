package com.myapps.timewrap.UI

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

object BillingRepository : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private lateinit var appContext: Context

    private var isProductsReady = false
    private var cachedPlans = emptyList<PlanUiModel>()
    private var premiumCallback: (() -> Unit)? = null
    private var plansCallback: (() -> Unit)? = null

    private const val PRODUCT_ID = "weekly_timewarp"

    fun isReady(): Boolean = isProductsReady

    fun init(
        context: Context,
        onReady: (() -> Unit)? = null,
        onPremiumUnlocked: (() -> Unit)? = null
    ) {
        appContext = context.applicationContext
        premiumCallback = onPremiumUnlocked
        plansCallback = onReady

        Log.d("BILLING", "🚀 Initializing billing...")

        billingClient = BillingClient.newBuilder(appContext)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .setListener(this)
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BILLING", "✅ Connected to Google Play")
                    loadAllProducts()
                    restorePurchases()
                } else {
                    Log.e("BILLING", "❌ Setup failed: ${result.debugMessage}")
                    // Retry after 2 seconds
                    handler.postDelayed({
                        retryConnection()
                    }, 2000)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("BILLING", "❌ Disconnected from Google Play")
            }
        })
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var retryCount = 0

    private fun retryConnection() {
        if (retryCount < 3) {
            retryCount++
            Log.d("BILLING", "🔄 Retrying connection $retryCount/3")
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("BILLING", "✅ Reconnected successfully")
                        loadAllProducts()
                        restorePurchases()
                    } else {
                        Log.e("BILLING", "❌ Retry failed: ${result.debugMessage}")
                    }
                }

                override fun onBillingServiceDisconnected() {}
            })
        }
    }

    private fun loadAllProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        Log.d("BILLING", "📦 Querying product: $PRODUCT_ID")

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
        ) { result, data ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d("BILLING", "✅ Query successful!")
                val mappedPlans = data.productDetailsList.mapNotNull { it.toPlan() }
                Log.d("BILLING", "📦 Found ${mappedPlans.size} products")

                if (mappedPlans.isNotEmpty()) {
                    cachedPlans = mappedPlans
                    isProductsReady = true

                    cachedPlans.forEach {
                        Log.d("BILLING", "   - ${it.id}: ${it.price}")
                    }

                    // ✅ CRITICAL: Call the callback
                    Log.d("BILLING", "📢 Calling plansCallback...")
                    plansCallback?.invoke()
                } else {
                    Log.e("BILLING", "❌ No products found for ID: $PRODUCT_ID")
                    Log.e("BILLING", "   Check if product exists in Google Play Console")
                    Log.e("BILLING", "   And it's published/submitted for testing")
                }
            } else {
                Log.e("BILLING", "❌ Query failed: ${result.debugMessage}")
                Log.e("BILLING", "   Response code: ${result.responseCode}")
            }
        }
    }

    private fun ProductDetails.toPlan(): PlanUiModel {
        val hasFreeTrial = subscriptionOfferDetails?.any { offer ->
            offer.pricingPhases.pricingPhaseList.any {
                it.priceAmountMicros == 0L
            }
        } == true

        val price = subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
            ?.formattedPrice ?: "Loading..."

        return PlanUiModel(
            id = productId,
            title = "Weekly Plan",
            price = price,
            hasFreeTrial = hasFreeTrial,
            isBestValue = false,
            product = this
        )
    }

    fun launchPurchase(activity: Activity, plan: PlanUiModel) {
        val product = plan.product
        val offers = product.subscriptionOfferDetails ?: run {
            Log.e("BILLING", "❌ No offers available")
            return
        }

        val selectedOffer = offers.firstOrNull()
        if (selectedOffer == null) {
            Log.e("BILLING", "❌ No valid offer")
            return
        }

        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(selectedOffer.offerToken)
            .build()

        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(params))
                .build()
        )
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            ) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    unlockPremium()
                }
            }
        } else {
            unlockPremium()
        }
    }

    private fun unlockPremium() {
        PremiumManager.setPremium(appContext, true)
        premiumCallback?.invoke()
    }

    private fun restorePurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasPremium) {
                    unlockPremium()
                }
            }
        }
    }

    fun getCachedPlans(): List<PlanUiModel> = cachedPlans

    fun fetchPlans() {
        Log.d("BILLING", "🔄 Manual fetch requested")
        cachedPlans = emptyList()
        isProductsReady = false
        loadAllProducts()
    }

    fun destroy() {
        if (::billingClient.isInitialized) billingClient.endConnection()
    }
}