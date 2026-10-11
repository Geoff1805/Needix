package com.example.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction de facturation / monétisation commerciale (RevenueCat, Google Play Billing, Amazon).
 */
interface BillingManager {
    val isPremium: StateFlow<Boolean>
    val hasActiveAccess: StateFlow<Boolean>
    val trialDaysRemaining: StateFlow<Int>
    val activeSubscriptionTitle: StateFlow<String?>

    fun startSubscription(planType: SubscriptionPlan)
    fun restorePurchases()
}

enum class SubscriptionPlan(val title: String, val price: String) {
    MONTHLY("Mensuel", "2,99 € / mois"),
    ANNUAL("Annuel (19,99 € / an)", "19,99 € / an")
}
