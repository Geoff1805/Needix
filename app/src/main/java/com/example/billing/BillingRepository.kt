package com.example.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BillingRepository {
    private val commercialManager = CommercialBillingManager(initialTrialDays = 14)

    private val _activeManager = MutableStateFlow<BillingManager>(commercialManager)
    val activeManager: StateFlow<BillingManager> = _activeManager.asStateFlow()
}
