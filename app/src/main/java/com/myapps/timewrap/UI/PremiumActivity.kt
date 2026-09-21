package com.myapps.timewrap.UI

import android.R
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.myapps.timewrap.databinding.ActivityPaywallBinding
import com.myapps.timewrap.splashAds.FirstPageMainActivity
import com.myapps.timewrap.splashAds.RemoteConfigManager

class PremiumActivity : BaseActivity() {

    private val LOG_TAG = "PremiumActivity"

    private var interstitialAd: InterstitialAd? = null
    private var plans = emptyList<PlanUiModel>()

    private val handler = Handler(Looper.getMainLooper())
    private var isAdShownOnClose = false
    private var retryCount = 0
    private val MAX_RETRIES = 5
    var resolutionX: Int = 480
    var resolutionY: Int = 640
    var lineResolution: Int = 5

    private lateinit var binding: ActivityPaywallBinding
    private lateinit var remoteConfigManager: RemoteConfigManager
    private var isPremium = false
    private var adsEnabled = true

    companion object {
        private const val PRODUCT_ID = "weekly_timewarp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔴 GUARANTEED-VISIBLE LOG
        Log.e("CHECK", "=== PremiumActivity onCreate STARTED ===")

        if (Build.VERSION.SDK_INT > 29) {
            this.resolutionX = 720
            this.resolutionY = 1280
            this.lineResolution = 7
        }
        getWindow().setFlags(1024, 1024)

        enableEdgeToEdge()
        binding = ActivityPaywallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        // ✅ Remote Config
        remoteConfigManager = RemoteConfigManager.getInstance()

        // ✅ Fetch config FIRST, then read values
        remoteConfigManager.fetchRemoteConfigSync {

            // 🔴 GUARANTEED-VISIBLE LOG
            Log.e(LOG_TAG, "=== Remote Config READY ===")
            Log.e(LOG_TAG, "Ads Enabled       = " + remoteConfigManager.isAdsEnabled)
            Log.e(LOG_TAG, "Banner Enabled    = " + remoteConfigManager.isBannerEnabled)
            Log.e(LOG_TAG, "Interstitial En.  = " + remoteConfigManager.isInterstitialEnabled)
            Log.e(LOG_TAG, "Native Enabled    = " + remoteConfigManager.isNativeEnabled)
            Log.e(LOG_TAG, "App Open Enabled  = " + remoteConfigManager.isAppOpenEnabled)
            Log.e(LOG_TAG, "Premium Enabled   = " + remoteConfigManager.isPremiumEnabled)
            Log.e(LOG_TAG, "Interstitial ID   = [" + remoteConfigManager.interstitialAdId + "]")
            Log.e(LOG_TAG, "Banner Ad ID      = [" + remoteConfigManager.bannerAdId + "]")
            Log.e(LOG_TAG, "Native Ad ID      = [" + remoteConfigManager.nativeAdId + "]")
            Log.e(LOG_TAG, "App Open Ad ID    = [" + remoteConfigManager.appOpenAdId + "]")
            Log.e(LOG_TAG, "===========================")

            // ✅ Now safely read values
            isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled
            adsEnabled = remoteConfigManager.isAdsEnabled

            Log.e(LOG_TAG, "User is premium: $isPremium")
            Log.e(LOG_TAG, "Ads enabled: $adsEnabled")

            if (isPremium) {
                Log.e(LOG_TAG, "Premium user - redirecting")
                startActivity(Intent(this, FirstPageMainActivity::class.java))
                finish()
                return@fetchRemoteConfigSync
            }

            initViews()
            setupClicks()
            showLoadingState()

            // Start billing initialization
            startBilling()

            // ✅ Only load interstitial if NOT premium AND ads enabled AND interstitial enabled
            if (!isPremium && adsEnabled && remoteConfigManager.isInterstitialEnabled) {
                Log.e(LOG_TAG, "Loading interstitial ad")
                loadAd()
            } else {
                Log.e(LOG_TAG, "Skipping ad load - Premium: $isPremium"
                        + ", Ads Enabled: $adsEnabled"
                        + ", Interstitial Enabled: " + remoteConfigManager.isInterstitialEnabled)
            }

            showCloseButtonAfterDelay()
        }
    }

    private fun startBilling() {
        Log.e(LOG_TAG, "🚀 Starting billing initialization...")

        BillingRepository.init(
            this,
            onReady = {
                Log.e(LOG_TAG, "✅ Billing onReady callback triggered!")
                handleBillingReady()
            },
            onPremiumUnlocked = {
                Log.e(LOG_TAG, "🎉 Premium unlocked callback!")
                setResult(RESULT_OK)
                startActivity(Intent(this, FirstPageMainActivity::class.java))
                finish()
            }
        )
    }

    private fun handleBillingReady() {
        plans = BillingRepository.getCachedPlans()
        Log.e(LOG_TAG, "📦 Retrieved ${plans.size} plans from cache")

        runOnUiThread {
            if (plans.isNotEmpty()) {
                bindPlans()
                logPlansStatus()
            } else {
                Log.e(LOG_TAG, "⚠️ Plans empty, starting polling...")
                BillingRepository.fetchPlans()
                startPollingForPlans()
            }
        }
    }

    private fun startPollingForPlans() {
        var pollCount = 0
        val maxPolls = 15

        handler.post(object : Runnable {
            override fun run() {
                pollCount++
                plans = BillingRepository.getCachedPlans()
                Log.e(LOG_TAG, "🔄 Poll $pollCount: ${plans.size} plans found")

                if (plans.isNotEmpty()) {
                    Log.e(LOG_TAG, "✅ Plans found on poll $pollCount!")
                    runOnUiThread {
                        bindPlans()
                        logPlansStatus()
                    }
                } else if (pollCount < maxPolls) {
                    handler.postDelayed(this, 1000)
                } else {
                    Log.e(LOG_TAG, "❌ No plans found after $maxPolls polls")
                    runOnUiThread {
                        showErrorState("Failed to load plans")
                    }
                }
            }
        })
    }

    private fun initViews() {
        binding.btnClose.visibility = View.INVISIBLE
        binding.btnStartTrial.text = "Loading..."
        binding.btnStartTrial.isEnabled = false
        binding.tvPrice?.text = "Loading price..."
        binding.tvPrice?.visibility = View.VISIBLE
    }

    private fun showCloseButtonAfterDelay() {
        handler.postDelayed({
            binding.btnClose.visibility = View.VISIBLE
        }, 3000)
    }

    private fun bindPlans() {
        Log.e(LOG_TAG, "🔍 bindPlans() - Plans size: ${plans.size}")

        if (plans.isEmpty()) {
            if (retryCount < MAX_RETRIES) {
                retryCount++
                Log.e(LOG_TAG, "🔄 Retry $retryCount/$MAX_RETRIES")
                handler.postDelayed({
                    BillingRepository.fetchPlans()
                    plans = BillingRepository.getCachedPlans()
                    bindPlans()
                }, (1500 * retryCount).toLong())
            } else {
                showErrorState("No plans available")
            }
            return
        }

        val weeklyPlan = plans.find { it.id == PRODUCT_ID }
        Log.e(LOG_TAG, "🔍 Looking for '$PRODUCT_ID'...")

        if (weeklyPlan == null) {
            Log.e(LOG_TAG, "❌ '$PRODUCT_ID' NOT FOUND!")
            Log.e(LOG_TAG, "Available IDs: ${plans.map { it.id }}")
            showErrorState("Plan not found")
            return
        }

        Log.e(LOG_TAG, "✅ Found plan: ${weeklyPlan.id} - ${weeklyPlan.price}")

        runOnUiThread {
            showPlanState(weeklyPlan)
        }
    }

    private fun showLoadingState() {
        binding.btnStartTrial.text = "Loading..."
        binding.btnStartTrial.isEnabled = false
        binding.tvPrice?.text = "Loading price..."
        binding.tvPrice?.visibility = View.VISIBLE
    }

    private fun showErrorState(message: String) {
        Log.e(LOG_TAG, "❌ Error: $message")
        binding.btnStartTrial.text = "Retry"
        binding.btnStartTrial.isEnabled = true
        binding.tvPrice?.text = message
        binding.tvPrice?.visibility = View.VISIBLE

        binding.btnStartTrial.setOnClickListener {
            retryCount = 0
            showLoadingState()
            BillingRepository.fetchPlans()
            handler.postDelayed({
                plans = BillingRepository.getCachedPlans()
                bindPlans()
            }, 1000)
        }
    }

    private fun showPlanState(plan: PlanUiModel) {
        Log.e(LOG_TAG, "📱 Updating UI with plan: ${plan.price}")

        binding.btnStartTrial.text = if (plan.hasFreeTrial) "START FREE TRIAL" else "SUBSCRIBE NOW"
        binding.btnStartTrial.isEnabled = true
        binding.btnStartTrial.visibility = View.VISIBLE

        binding.tvPrice?.text = plan.price + "/week"
        binding.tvPrice?.visibility = View.VISIBLE

        binding.btnStartTrial.post {
            binding.btnStartTrial.requestLayout()
            binding.btnStartTrial.invalidate()
        }
        binding.tvPrice?.post {
            binding.tvPrice?.requestLayout()
            binding.tvPrice?.invalidate()
        }

        retryCount = 0

        binding.btnStartTrial.setOnClickListener {
            Log.e(LOG_TAG, "🛒 Purchasing: ${plan.id}")
            BillingRepository.launchPurchase(this, plan)
        }

        Log.e(LOG_TAG, "✅ UI Updated - Button: ${binding.btnStartTrial.text}, Price: ${binding.tvPrice?.text}")
    }

    private fun logPlansStatus() {
        Log.e(LOG_TAG, "========= PLANS DEBUG =========")
        if (plans.isEmpty()) {
            Log.e(LOG_TAG, "❌ No plans available!")
        } else {
            plans.forEachIndexed { index, plan ->
                Log.e(LOG_TAG, "[$index] ID: ${plan.id}, Price: ${plan.price}, Trial: ${plan.hasFreeTrial}")
            }
        }
        Log.e(LOG_TAG, "================================")
    }

    private fun setupClicks() {
        binding.btnClose.setOnClickListener {
            if (PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@setOnClickListener
            }

            if (!isAdShownOnClose) {
                isAdShownOnClose = true
                showAdOnClose()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun loadAd() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled) {
            Log.e(LOG_TAG, "Skipping ad load - conditions not met")
            return
        }

        val interstitialAdId = remoteConfigManager.interstitialAdId

        // 🔴 GUARANTEED-VISIBLE LOG — right before ad request
        Log.e(LOG_TAG, "Interstitial Ad ID = [$interstitialAdId]")

        if (interstitialAdId.isNullOrEmpty()) {
            Log.e(LOG_TAG, "❌ Interstitial Ad ID is EMPTY — skipping load")
            return
        }

        InterstitialAd.load(
            this,
            interstitialAdId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.e(LOG_TAG, "✅ Interstitial ad loaded")
                    ad.setOnPaidEventListener { adValue ->
                        val revenue = adValue.valueMicros / 1_000_000.0
                        Log.e(LOG_TAG, "💰 Revenue: $revenue ${adValue.currencyCode}")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    Log.e(LOG_TAG, "❌ Ad failed: ${adError.message}"
                            + " | code=${adError.code}"
                            + " | domain=${adError.domain}")
                }
            }
        )
    }

    private fun showAdOnClose() {
        val shouldShowAds = !PremiumManager.isPremium(this) &&
                remoteConfigManager.isPremiumEnabled &&
                adsEnabled &&
                remoteConfigManager.isInterstitialEnabled

        if (!shouldShowAds || interstitialAd == null) {
            Log.e(LOG_TAG, "Skipping ad on close - conditions not met")
            navigateToMain()
            return
        }

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                navigateToMain()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(LOG_TAG, "❌ Failed to show ad: ${adError.message}")
                navigateToMain()
            }

            override fun onAdShowedFullScreenContent() {
                Log.e(LOG_TAG, "✅ Ad showed")
            }
        }

        interstitialAd?.show(this) ?: navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()

        // ✅ Only refresh if config was already fetched at least once
        if (!remoteConfigManager.isConfigReady) {
            return
        }

        remoteConfigManager.refresh()

        remoteConfigManager.fetchRemoteConfigSync {

            val currentPremium = PremiumManager.isPremium(this)
                    && remoteConfigManager.isPremiumEnabled
            val currentAdsEnabled = remoteConfigManager.isAdsEnabled
            val currentInterstitialEnabled = remoteConfigManager.isInterstitialEnabled

            val statusChanged = (currentPremium != isPremium)
                    || (currentAdsEnabled != adsEnabled)

            if (statusChanged) {
                isPremium = currentPremium
                adsEnabled = currentAdsEnabled
                Log.e(LOG_TAG, "Status changed - Premium: $isPremium"
                        + ", Ads Enabled: $adsEnabled"
                        + ", Interstitial Enabled: $currentInterstitialEnabled")

                if (!isPremium && adsEnabled && currentInterstitialEnabled
                    && interstitialAd == null) {
                    loadAd()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        interstitialAd = null
    }

    override fun onBackPressed() {
        // Disabled
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.content),
            OnApplyWindowInsetsListener { view: View?, insets: WindowInsetsCompat? ->
                val statusBarHeight = insets!!.getInsets(WindowInsetsCompat.Type.statusBars()).top
                val navigationBarHeight =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

                view!!.setPadding(0, statusBarHeight, 0, navigationBarHeight)
                insets
            })
    }
}