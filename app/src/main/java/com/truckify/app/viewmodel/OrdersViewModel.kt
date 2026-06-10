package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OrdersViewModel : ViewModel() {

    private val _activeShipments = MutableStateFlow<List<Shipment>>(emptyList())
    val activeShipments: StateFlow<List<Shipment>> = _activeShipments

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var shipmentsListener: ListenerRegistration? = null

    fun observeOrders(userRole: String) {
        val email = AuthManager.getCurrentUserEmail() ?: return
        
        _isLoading.value = true
        shipmentsListener?.remove()
        shipmentsListener = if (userRole == "Vendor") {
            FirestoreManager.getActiveShipments(email) { 
                _activeShipments.value = it
                _isLoading.value = false
            }
        } else {
            FirestoreManager.getActiveShipments(email) { shipments ->
                _activeShipments.value = shipments.filter { it.driverEmail == email }
                _isLoading.value = false
            }
        }
    }

    fun repostShipment(shipment: Shipment, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val newShipment = shipment.copy(
            id = "", // Will be set by Firestore
            status = "Available",
            driverEmail = "",
            timestamp = System.currentTimeMillis(),
            paymentStatus = "Pending",
            escrowTxnId = "",
            pickupTime = 0,
            deliveryTime = 0
        )
        FirestoreManager.postShipment(newShipment, onSuccess, onError)
    }

    override fun onCleared() {
        super.onCleared()
        shipmentsListener?.remove()
    }
}
