package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.firebase.GeminiManager
import com.truckify.app.models.Driver
import com.truckify.app.models.Shipment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchingViewModel : ViewModel() {

    private val _shipment = MutableStateFlow<Shipment?>(null)
    val shipment: StateFlow<Shipment?> = _shipment

    private val _recommendedDrivers = MutableStateFlow<List<Driver>>(emptyList())
    val recommendedDrivers: StateFlow<List<Driver>> = _recommendedDrivers

    private val _aiAnalysis = MutableStateFlow<String?>(null)
    val aiAnalysis: StateFlow<String?> = _aiAnalysis

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var shipmentListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun findMatches(shipmentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            shipmentListener?.remove()
            shipmentListener = FirestoreManager.listenToShipment(shipmentId) { s ->
                _shipment.value = s
                
                // Once we have the shipment, find matches
                FirestoreManager.getSmartMatches(shipmentId) { candidates ->
                    _recommendedDrivers.value = candidates
                    
                    viewModelScope.launch {
                        _aiAnalysis.value = GeminiManager.getRecommendedDriver(s, candidates)
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun assignDriver(driverEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val s = _shipment.value ?: return
        FirestoreManager.assignDriverToShipment(s.id, driverEmail, onSuccess, onError)
    }

    override fun onCleared() {
        super.onCleared()
        shipmentListener?.remove()
    }
}
