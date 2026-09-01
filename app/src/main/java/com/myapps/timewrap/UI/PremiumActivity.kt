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

class PremiumActivity : BaseActivity() {

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

    companion object {
        private const val PRODUCT_ID = "weekly_timewarp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        if (PremiumManager.isPremium(this)) {
            startActivity(Intent(this, FirstPageMainActivity::class.java))
            finish()
            return
        }

        initViews()
        setupClicks()
        showLoadingState()

        // Start billing initialization
        startBilling()

        if (PremiumManager.shouldShowAds(this)) {
            loadAd()
        }

        showCloseButtonAfterDelay()
    }

    private fun startBilling() {
        Log.d("PremiumActivity", "🚀 Starting billing initialization...")

        BillingRepository.init(
            this,
            onReady = {
                Log.d("PremiumActivity", "✅ Billing onReady callback triggered!")
                handleBillingReady()
            },
            onPremiumUnlocked = {
                Log.d("PremiumActivity", "🎉 Premium unlocked callback!")
                setResult(RESULT_OK)
                startActivity(Intent(this, FirstPageMainActivity::class.java))
                finish()
            }
        )
    }

    private fun handleBillingReady() {
        // Get plans from repository
        plans = BillingRepository.getCachedPlans()
        Log.d("PremiumActivity", "📦 Retrieved ${plans.size} plans from cache")

        // ⭐ IMPORTANT: Run on UI thread
        runOnUiThread {
            if (plans.isNotEmpty()) {
                bindPlans()
                logPlansStatus()
            } else {
                Log.d("PremiumActivity", "⚠️ Plans empty, starting polling...")
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
                Log.d("PremiumActivity", "🔄 Poll $pollCount: ${plans.size} plans found")

                if (plans.isNotEmpty()) {
                    Log.d("PremiumActivity", "✅ Plans found on poll $pollCount!")
                    runOnUiThread {
                        bindPlans()
                        logPlansStatus()
                    }
                } else if (pollCount < maxPolls) {
                    handler.postDelayed(this, 1000)
                } else {
                    Log.e("PremiumActivity", "❌ No plans found after $maxPolls polls")
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
        Log.d("PremiumActivity", "🔍 bindPlans() - Plans size: ${plans.size}")

        if (plans.isEmpty()) {
            if (retryCount < MAX_RETRIES) {
                retryCount++
                Log.d("PremiumActivity", "🔄 Retry $retryCount/$MAX_RETRIES")
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
        Log.d("PremiumActivity", "🔍 Looking for '$PRODUCT_ID'...")

        if (weeklyPlan == null) {
            Log.e("PremiumActivity", "❌ '$PRODUCT_ID' NOT FOUND!")
            Log.d("PremiumActivity", "Available IDs: ${plans.map { it.id }}")
            showErrorState("Plan not found")
            return
        }

        Log.d("PremiumActivity", "✅ Found plan: ${weeklyPlan.id} - ${weeklyPlan.price}")

        // ⭐ CRITICAL: Update UI on main thread
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
        Log.e("PremiumActivity", "❌ Error: $message")
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
        Log.d("PremiumActivity", "📱 Updating UI with plan: ${plan.price}")

        // ⭐ Update button
        binding.btnStartTrial.text = if (plan.hasFreeTrial) "START FREE TRIAL" else "SUBSCRIBE NOW"
        binding.btnStartTrial.isEnabled = true
        binding.btnStartTrial.visibility = View.VISIBLE

        // ⭐ Update price
        binding.tvPrice?.text = plan.price+"/week"
        binding.tvPrice?.visibility = View.VISIBLE

        // ⭐ Force refresh
        binding.btnStartTrial.post {
            binding.btnStartTrial.requestLayout()
            binding.btnStartTrial.invalidate()
        }
        binding.tvPrice?.post {
            binding.tvPrice?.requestLayout()
            binding.tvPrice?.invalidate()
        }

        retryCount = 0

        // ⭐ Set click listener
        binding.btnStartTrial.setOnClickListener {
            Log.d("PremiumActivity", "🛒 Purchasing: ${plan.id}")
            BillingRepository.launchPurchase(this, plan)
        }

        Log.d("PremiumActivity", "✅ UI Updated - Button: ${binding.btnStartTrial.text}, Price: ${binding.tvPrice?.text}")
    }

    private fun logPlansStatus() {
        Log.d("PremiumActivity", "========= PLANS DEBUG =========")
        if (plans.isEmpty()) {
            Log.e("PremiumActivity", "❌ No plans available!")
        } else {
            plans.forEachIndexed { index, plan ->
                Log.d("PremiumActivity", "[$index] ID: ${plan.id}, Price: ${plan.price}, Trial: ${plan.hasFreeTrial}")
            }
        }
        Log.d("PremiumActivity", "================================")
    }

    private fun setupClicks() {
        binding.btnClose.setOnClickListener {
            if (PremiumManager.isPremium(this)) {
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
        val interstitialAdId = "ca-app-pub-5969006643846426/8974610806"

        InterstitialAd.load(
            this,
            interstitialAdId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("AdManager", "✅ Interstitial ad loaded")
                    ad.setOnPaidEventListener { adValue ->
                        val revenue = adValue.valueMicros / 1_000_000.0
                        Log.d("AdManager", "💰 Revenue: $revenue ${adValue.currencyCode}")
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    Log.e("AdManager", "❌ Ad failed: ${adError.message}")
                }
            }
        )
    }

    private fun showAdOnClose() {
        if (!PremiumManager.shouldShowAds(this) || interstitialAd == null) {
            navigateToMain()
            return
        }

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                navigateToMain()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("AdManager", "❌ Failed to show ad: ${adError.message}")
                navigateToMain()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("AdManager", "✅ Ad showed")
            }
        }

        interstitialAd?.show(this) ?: navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        // Disabled
    }

    private fun applyWindowInsets() {
        // For the root view of your layout
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.content),
            OnApplyWindowInsetsListener { view: View?, insets: WindowInsetsCompat? ->
                // Get insets for system bars
                val statusBarHeight = insets!!.getInsets(WindowInsetsCompat.Type.statusBars()).top
                val navigationBarHeight =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

                // Apply padding to your root layout to avoid overlapping with system bars
                // If you want your content to go under system bars, remove this
                view!!.setPadding(0, statusBarHeight, 0, navigationBarHeight)
                insets
            })
    }
}