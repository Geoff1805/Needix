package com.example.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DistributionFlavor {
    FOSS,
    COMMERCIAL
}

object BillingRepository {
    private val fossManager = FossBillingManager()
    private val commercialManager = CommercialBillingManager(initialTrialDays = 14)

    private val _currentFlavor = MutableStateFlow(DistributionFlavor.FOSS)
    val currentFlavor: StateFlow<DistributionFlavor> = _currentFlavor.asStateFlow()

    private val _activeManager = MutableStateFlow<BillingManager>(fossManager)
    val activeManager: StateFlow<BillingManager> = _activeManager.asStateFlow()

    fun setFlavor(flavor: DistributionFlavor) {
        _currentFlavor.value = flavor
        _activeManager.value = when (flavor) {
            DistributionFlavor.FOSS -> fossManager
            DistributionFlavor.COMMERCIAL -> commercialManager
        }
    }
}
