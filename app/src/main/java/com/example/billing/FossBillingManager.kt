package com.example.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implémentation FOSS (F-Droid) :
 * 100% débloquée et gratuite, aucun SDK propriétaire, zéro suivi.
 */
class FossBillingManager : BillingManager {
    override val isFoss: Boolean = true

    private val _isPremium = MutableStateFlow(true)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _hasActiveAccess = MutableStateFlow(true)
    override val hasActiveAccess: StateFlow<Boolean> = _hasActiveAccess.asStateFlow()

    private val _trialDaysRemaining = MutableStateFlow(Int.MAX_VALUE)
    override val trialDaysRemaining: StateFlow<Int> = _trialDaysRemaining.asStateFlow()

    private val _activeSubscriptionTitle = MutableStateFlow<String?>("FOSS - Édition Libre & Ouverte")
    override val activeSubscriptionTitle: StateFlow<String?> = _activeSubscriptionTitle.asStateFlow()

    override fun startSubscription(planType: SubscriptionPlan) {
        // En FOSS, l'accès est déjà illimité. Les dons sont gérés via l'écran SupportProjectScreen.
    }

    override fun restorePurchases() {
        // Rien à restaurer en mode FOSS
    }
}
