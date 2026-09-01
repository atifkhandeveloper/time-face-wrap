package com.myapps.timewrap.UI

import android.content.Context
import android.util.Log

object PremiumManager {

    private const val PREF = "premium_prefs"
    private const val KEY = "is_premium"
    private const val KEY_PURCHASE_TOKEN = "purchase_token"
    private const val KEY_PREMIUM_SINCE = "premium_since"

    // ---------------- SAVE PREMIUM ----------------
    @JvmStatic
    fun setPremium(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY, value).commit()

        if (value) {
            prefs.edit()
                .putLong(KEY_PREMIUM_SINCE, System.currentTimeMillis())
                .commit()
        }
    }

    // ---------------- CHECK PREMIUM ----------------
    @JvmStatic
    fun isPremium(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY, false) // ✅ Default is FALSE
    }

    // ---------------- ADS HELPER ----------------
    @JvmStatic
    fun shouldShowAds(context: Context): Boolean {
        return !isPremium(context)
    }

    // ---------------- GET PREMIUM SINCE ----------------
    @JvmStatic
    fun getPremiumSince(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_PREMIUM_SINCE, 0)
    }

    // ---------------- SAVE PURCHASE TOKEN ----------------
    @JvmStatic
    fun setPurchaseToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PURCHASE_TOKEN, token).commit()
    }

    @JvmStatic
    fun getPurchaseToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PURCHASE_TOKEN, null)
    }

    // ---------------- RESET (TEST ONLY) ----------------
    @JvmStatic
    fun reset(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit()
            .clear()
            .apply()
    }

    // ---------------- FORCE RESET TO DEFAULT ----------------
    @JvmStatic
    fun resetToDefault(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY, false)
            .putString(KEY_PURCHASE_TOKEN, null)
            .putLong(KEY_PREMIUM_SINCE, 0)
            .commit()
        Log.d("PremiumManager", "✅ Reset to default (not premium)")
    }

    // ---------------- REFRESH PREMIUM ----------------
    @JvmStatic
    fun refreshPremium(context: Context): Boolean {
        val value = isPremium(context)

        if (value) {
            val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY, true).apply()
        }

        return value
    }
}