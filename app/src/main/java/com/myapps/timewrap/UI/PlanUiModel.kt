package com.myapps.timewrap.UI


import com.android.billingclient.api.ProductDetails

data class PlanUiModel(
    val id: String,           // "weekly_antitheft", "yearly_antitheft", "lifetime_antitheft"
    val title: String,        // "Weekly Plan", "Yearly Plan", "Lifetime Plan"
    val price: String,        // "$4.99", "$49.99", etc.
    val hasFreeTrial: Boolean, // true for weekly, possibly true for yearly if configured
    val isBestValue: Boolean,  // true for yearly
    val product: ProductDetails
)