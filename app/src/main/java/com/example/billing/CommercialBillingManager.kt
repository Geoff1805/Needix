package com.example.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implémentation Commerciale (Google Play / Amazon) :
 * Propose une période d'essai gratuit de 14 jours, puis gère l'abonnement
 * mensuel ou annuel via l'architecture RevenueCat.
 */
class CommercialBillingManager(
    initialTrialDays: Int = 14,
    initialIsSubscribed: Boolean = false
) : BillingManager {
    override val isFoss: Boolean = false

    private val _trialDaysRemaining = MutableStateFlow(initialTrialDays)
    override val trialDaysRemaining: StateFlow<Int> = _trialDaysRemaining.asStateFlow()

    private val _isPremium = MutableStateFlow(initialIsSubscribed)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _hasActiveAccess = MutableStateFlow(initialTrialDays > 0 || initialIsSubscribed)
    override val hasActiveAccess: StateFlow<Boolean> = _hasActiveAccess.asStateFlow()

    private val _activeSubscriptionTitle = MutableStateFlow<String?>(
        if (initialIsSubscribed) "Abonnement Actif" else "Essai 14 jours"
    )
    override val activeSubscriptionTitle: StateFlow<String?> = _activeSubscriptionTitle.asStateFlow()

    override fun startSubscription(planType: SubscriptionPlan) {
        // Simulation / Intégration de l'achat RevenueCat
        _isPremium.value = true
        _hasActiveAccess.value = true
        _activeSubscriptionTitle.value = "Abonnement ${planType.title} Actif"
    }

    override fun restorePurchases() {
        // Restauration des achats Store / RevenueCat
        _isPremium.value = true
        _hasActiveAccess.value = true
        _activeSubscriptionTitle.value = "Abonnement Restauré"
    }

    fun setSimulatedTrialDays(days: Int) {
        _trialDaysRemaining.value = days
        _hasActiveAccess.value = days > 0 || _isPremium.value
    }
}
