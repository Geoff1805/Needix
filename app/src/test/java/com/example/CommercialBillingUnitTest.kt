package com.example

import com.example.billing.CommercialBillingManager
import com.example.billing.SubscriptionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommercialBillingUnitTest {

    @Test
    fun `test initial trial period active and access granted`() {
        val billingManager = CommercialBillingManager(initialTrialDays = 14, initialIsSubscribed = false)

        assertEquals(14, billingManager.trialDaysRemaining.value)
        assertFalse(billingManager.isPremium.value)
        assertTrue(billingManager.hasActiveAccess.value)
        assertEquals("Essai 14 jours", billingManager.activeSubscriptionTitle.value)
    }

    @Test
    fun `test subscription activation gives premium and active access`() {
        val billingManager = CommercialBillingManager(initialTrialDays = 0, initialIsSubscribed = false)

        assertFalse(billingManager.hasActiveAccess.value)

        billingManager.startSubscription(SubscriptionPlan.ANNUAL)

        assertTrue(billingManager.isPremium.value)
        assertTrue(billingManager.hasActiveAccess.value)
        assertEquals("Abonnement Annuel (19,99 € / an) Actif", billingManager.activeSubscriptionTitle.value)
    }

    @Test
    fun `test restore purchases unlocks premium access`() {
        val billingManager = CommercialBillingManager(initialTrialDays = 0, initialIsSubscribed = false)

        billingManager.restorePurchases()

        assertTrue(billingManager.isPremium.value)
        assertTrue(billingManager.hasActiveAccess.value)
        assertEquals("Abonnement Restauré", billingManager.activeSubscriptionTitle.value)
    }

    @Test
    fun `test trial expiration revokes access if not subscribed`() {
        val billingManager = CommercialBillingManager(initialTrialDays = 14, initialIsSubscribed = false)
        assertTrue(billingManager.hasActiveAccess.value)

        billingManager.setSimulatedTrialDays(0)

        assertEquals(0, billingManager.trialDaysRemaining.value)
        assertFalse(billingManager.hasActiveAccess.value)
    }
}
