package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import com.truckify.app.models.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DriverViewModel : ViewModel() {

    private val _availableLoads = MutableStateFlow<List<Shipment>>(emptyList())
    val availableLoads: StateFlow<List<Shipment>> = _availableLoads

    private val _activeTrip = MutableStateFlow<Shipment?>(null)
    val activeTrip: StateFlow<Shipment?> = _activeTrip

    private val _userName = MutableStateFlow("Driver")
    val userName: StateFlow<String> = _userName

    private val _rating = MutableStateFlow(4.9)
    val rating: StateFlow<Double> = _rating

    private val _truckNumber = MutableStateFlow("RJ14 AB 2211")
    val truckNumber: StateFlow<String> = _truckNumber

    private val _verificationStatus = MutableStateFlow<String?>("Not Started")
    val verificationStatus: StateFlow<String?> = _verificationStatus

    private val _todayEarnings = MutableStateFlow(0.0)
    val todayEarnings: StateFlow<Double> = _todayEarnings

    private val _weekEarnings = MutableStateFlow(0.0)
    val weekEarnings: StateFlow<Double> = _weekEarnings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _incomingLoad = MutableStateFlow<Shipment?>(null)
    val incomingLoad: StateFlow<Shipment?> = _incomingLoad

    private var loadsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var txnListener: ListenerRegistration? = null
    private var incomingLoadListener: ListenerRegistration? = null
    private var currentLoadStatusListener: ListenerRegistration? = null

    fun loadDashboardData() {
        val email = AuthManager.getCurrentUserEmail() ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            // User Data
            FirestoreManager.getUserData(email) { data ->
                _userName.value = data?.get("name") as? String ?: "Driver"
                _rating.value = (data?.get("rating") as? Number)?.toDouble() ?: 4.9
                _truckNumber.value = data?.get("truckNumber") as? String ?: "RJ14 AB 2211"
                _verificationStatus.value = data?.get("verificationStatus") as? String ?: "Not Started"
                _isOnline.value = data?.get("isAvailable") as? Boolean ?: false
                
                if (_isOnline.value) {
                    startListeningForIncomingLoads()
                }
            }

            // Available Loads
            loadsListener?.remove()
            loadsListener = FirestoreManager.getAvailableLoads { loads ->
                _availableLoads.value = loads
                _isLoading.value = false
            }

            // Active Trip
            historyListener?.remove()
            historyListener = FirestoreManager.getShipmentHistory(email, "Driver") { shipments ->
                _activeTrip.value = shipments.find { it.status == "In Transit" }
            }

            // Earnings
            txnListener?.remove()
            txnListener = FirestoreManager.getTransactionHistory(email) { txns ->
                calculateEarnings(txns)
            }
        }
    }

    private fun calculateEarnings(txns: List<Transaction>) {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val oneWeekMillis = 7 * oneDayMillis

        val today = txns.filter { it.type == "Payout" && it.status == "Success" && (now - it.timestamp < oneDayMillis) }.sumOf { it.amount }
        val week = txns.filter { it.type == "Payout" && it.status == "Success" && (now - it.timestamp < oneWeekMillis) }.sumOf { it.amount }

        _todayEarnings.value = today
        _weekEarnings.value = week
    }

    fun toggleOnlineStatus() {
        val email = AuthManager.getCurrentUserEmail() ?: return
        val newStatus = !_isOnline.value
        _isOnline.value = newStatus
        FirestoreManager.updateDriverOnlineStatus(email, newStatus)
        
        if (newStatus) {
            startListeningForIncomingLoads()
        } else {
            stopListeningForIncomingLoads()
        }
    }

    private fun startListeningForIncomingLoads() {
        incomingLoadListener?.remove()
        // Use a 5-second buffer to catch loads posted just as the driver goes online
        val startTime = System.currentTimeMillis() - 5000
        incomingLoadListener = FirestoreManager.listenForNewLoads(startTime) { shipment ->
            if (_activeTrip.value == null && _incomingLoad.value?.id != shipment.id) {
                _incomingLoad.value = shipment
                
                // Monitor this specific load - if someone else takes it, dismiss the popup
                currentLoadStatusListener?.remove()
                currentLoadStatusListener = FirestoreManager.listenToShipment(shipment.id) { updated ->
                    if (updated.status != "Available") {
                        _incomingLoad.value = null
                        currentLoadStatusListener?.remove()
                    }
                }
            }
        }
    }

    private fun stopListeningForIncomingLoads() {
        incomingLoadListener?.remove()
        incomingLoadListener = null
        currentLoadStatusListener?.remove()
        currentLoadStatusListener = null
        _incomingLoad.value = null
    }

    fun declineIncomingLoad() {
        _incomingLoad.value = null
        currentLoadStatusListener?.remove()
    }

    fun acceptLoad(shipmentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val email = AuthManager.getCurrentUserEmail() ?: return
        FirestoreManager.acceptShipment(shipmentId, email, {
            _incomingLoad.value = null
            onSuccess()
        }, onError)
    }

    override fun onCleared() {
        super.onCleared()
        loadsListener?.remove()
        historyListener?.remove()
        txnListener?.remove()
        incomingLoadListener?.remove()
        currentLoadStatusListener?.remove()
    }
}
