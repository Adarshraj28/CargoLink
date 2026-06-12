package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import com.truckify.app.models.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class DriverViewModel @Inject constructor() : ViewModel() {

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

    private val _emptyKmSaved = MutableStateFlow(0.0)
    val emptyKmSaved: StateFlow<Double> = _emptyKmSaved

    private val _additionalEarnings = MutableStateFlow(0.0)
    val additionalEarnings: StateFlow<Double> = _additionalEarnings

    private val _returnMatchesCount = MutableStateFlow(0)
    val returnMatchesCount: StateFlow<Int> = _returnMatchesCount

    private val _co2Reduced = MutableStateFlow(0.0)
    val co2Reduced: StateFlow<Double> = _co2Reduced

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

    private var activeTripListener: ListenerRegistration? = null
    private var loadsListener: ListenerRegistration? = null
    private var txnListener: ListenerRegistration? = null
    private var incomingLoadListener: ListenerRegistration? = null
    private var currentLoadStatusListener: ListenerRegistration? = null

    init {
        // Periodic refresh to remove old loads from the UI state even if Firestore doesn't update
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000) // Every minute
                val now = System.currentTimeMillis()
                val freshLoads = _availableLoads.value.filter { 
                    now - it.timestamp < (24 * 60 * 60 * 1000L) 
                }
                if (freshLoads.size != _availableLoads.value.size) {
                    _availableLoads.value = freshLoads
                }
            }
        }
    }

    fun loadDashboardData() {
        val email = AuthManager.getCurrentUserEmail() ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            // Safety timeout to prevent screen lock/hang
            launch {
                delay(3000)
                if (_isLoading.value) {
                    _isLoading.value = false
                }
            }
            
            // User Data
            FirestoreManager.getUserData(email) { data ->
                _userName.value = data?.get("name") as? String ?: "Driver"
                _rating.value = (data?.get("rating") as? Number)?.toDouble() ?: 4.9
                _truckNumber.value = data?.get("truckNumber") as? String ?: "RJ14 AB 2211"
                _verificationStatus.value = data?.get("verificationStatus") as? String ?: "Not Started"
                _isOnline.value = data?.get("isAvailable") as? Boolean ?: false
                
                _emptyKmSaved.value = (data?.get("emptyKmSaved") as? Number)?.toDouble() ?: 0.0
                _additionalEarnings.value = (data?.get("additionalEarnings") as? Number)?.toDouble() ?: 0.0
                _returnMatchesCount.value = (data?.get("returnMatchesCount") as? Number)?.toInt() ?: 0
                _co2Reduced.value = (data?.get("co2Reduced") as? Number)?.toDouble() ?: 0.0
                
                if (_isOnline.value) {
                    startListeningForIncomingLoads()
                }
            }

            // Available Loads
            if (loadsListener == null) {
                loadsListener = FirestoreManager.getAvailableLoads { loads ->
                    _availableLoads.value = loads
                    _isLoading.value = false
                }
            }

            // Active Trip - Optimized listener
            if (activeTripListener == null) {
                activeTripListener = FirestoreManager.listenToActiveShipments(email, "Driver") { shipments ->
                    val trip = shipments.firstOrNull()
                    // Stability check: Only update if the trip data actually changed
                    if (_activeTrip.value != trip) {
                        _activeTrip.value = trip
                    }
                }
            }

            // Earnings
            if (txnListener == null) {
                txnListener = FirestoreManager.getTransactionHistory(email) { txns ->
                    calculateEarnings(txns)
                }
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

    fun toggleOnlineStatus(onError: (String) -> Unit = {}) {
        val email = AuthManager.getCurrentUserEmail() ?: return
        val currentStatus = _isOnline.value
        val newStatus = !currentStatus
        
        // If driver is trying to go offline while on a trip, block it
        if (currentStatus && !newStatus && _activeTrip.value != null) {
            onError("Cannot go offline during an active trip. Complete your delivery first.")
            return
        }

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
        val startTime = System.currentTimeMillis() - 30000
        
        incomingLoadListener = FirestoreManager.listenForNewLoads(startTime) { shipment ->
            val email = AuthManager.getCurrentUserEmail() ?: return@listenForNewLoads
            FirestoreManager.getUserData(email) { userData ->
                val capStr = userData?.get("capacity") as? String ?: "5 Tons"
                val capacity = capStr.split(" ").firstOrNull()?.toDoubleOrNull() ?: 0.0
                val loadWeightStr = shipment.weight.split(" ").firstOrNull() ?: "0"
                val loadWeight = loadWeightStr.toDoubleOrNull() ?: 0.0

                if (_activeTrip.value == null && 
                    _incomingLoad.value?.id != shipment.id && 
                    loadWeight <= capacity) {
                    
                    _incomingLoad.value = shipment
                    
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
        activeTripListener?.remove()
        txnListener?.remove()
        incomingLoadListener?.remove()
        currentLoadStatusListener?.remove()
    }
}
