package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _activeShipments = MutableStateFlow<List<Shipment>>(emptyList())
    val activeShipments: StateFlow<List<Shipment>> = _activeShipments

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName

    private val _verificationStatus = MutableStateFlow<String?>("Not Started")
    val verificationStatus: StateFlow<String?> = _verificationStatus

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var shipmentsListener: ListenerRegistration? = null

    private val _monthlySpend = MutableStateFlow(0.0)
    val monthlySpend: StateFlow<Double> = _monthlySpend

    private val _onTimeRate = MutableStateFlow(96)
    val onTimeRate: StateFlow<Int> = _onTimeRate

    private val _costSaved = MutableStateFlow(0.0)
    val costSaved: StateFlow<Double> = _costSaved

    private var txnsListener: ListenerRegistration? = null

    fun loadDashboardData() {
        val email = AuthManager.getCurrentUserEmail() ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            // Safety timeout
            launch {
                kotlinx.coroutines.delay(2500)
                if (_isLoading.value) {
                    _isLoading.value = false
                }
            }

            // Get user data
            FirestoreManager.getUserData(email) { data ->
                _userName.value = data?.get("name") as? String ?: "User"
                _verificationStatus.value = data?.get("verificationStatus") as? String ?: "Not Started"
                _onTimeRate.value = (data?.get("onTimeDeliveries") as? Number)?.toInt()?.let { 
                    if (it > 0) 98 else 96 
                } ?: 96
            }

            // Listen for active shipments
            shipmentsListener?.remove()
            shipmentsListener = FirestoreManager.getActiveShipments(email) { shipments ->
                _activeShipments.value = shipments
                _isLoading.value = false
                calculateCostSaved(shipments)
            }

            // Monthly Spend
            txnsListener?.remove()
            txnsListener = FirestoreManager.getTransactionHistory(email) { txns ->
                val monthMillis = 30 * 24 * 60 * 60 * 1000L
                val now = System.currentTimeMillis()
                _monthlySpend.value = txns.filter { it.type == "Escrow Payment" && (now - it.timestamp < monthMillis) }.sumOf { it.amount }
            }
        }
    }

    private fun calculateCostSaved(shipments: List<Shipment>) {
        // Mock logic: 15% of total shipment value if shared pooling was used
        val total = shipments.sumOf { it.price.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0 }
        _costSaved.value = total * 0.15
    }

    override fun onCleared() {
        super.onCleared()
        shipmentsListener?.remove()
        txnsListener?.remove()
    }
}
