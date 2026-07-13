package com.cargolink.app.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.cargolink.app.models.Shipment
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ShipmentViewModel @Inject constructor() : ViewModel() {
    // Shared State for multi-step shipment creation
    var pickupAddress by mutableStateOf("")
    var pickupLat by mutableDoubleStateOf(19.0760)
    var pickupLng by mutableDoubleStateOf(72.8777)
    var pickupContactName by mutableStateOf("")
    var pickupContactPhone by mutableStateOf("")
    var selectedWarehouse by mutableStateOf("Main Warehouse")
    var pickupDate by mutableStateOf("Oct 24, 2024")
    var pickupTime by mutableStateOf("10:30 AM")

    var dropAddress by mutableStateOf("")
    var dropLat by mutableDoubleStateOf(28.6139)
    var dropLng by mutableDoubleStateOf(77.2090)
    var receiverName by mutableStateOf("")
    var receiverPhone by mutableStateOf("")
    var deliveryWindow by mutableStateOf("Flexible (8 AM - 8 PM)")
    var specialInstructions by mutableStateOf("")

    var weightInput by mutableStateOf("1.0")
    var loadCategory by mutableStateOf("Industrial Goods")
    var isFragile by mutableStateOf(false)
    var numPackages by mutableIntStateOf(1)

    var truckType by mutableStateOf("Mini Truck")
    var bodyType by mutableStateOf("Open Body")

    var totalPrice by mutableStateOf("₹0")
    var isInsured by mutableStateOf(false)

    var estimatedDistance by mutableStateOf("120 km")
    var estimatedDuration by mutableStateOf("3h 45m")

    fun updateEstimates() {
        val dist = com.cargolink.app.utils.calculateDistance(pickupLat, pickupLng, dropLat, dropLng)
        estimatedDistance = String.format(java.util.Locale.getDefault(), "%.1f km", dist)
        
        // Assume average speed of 40 km/h for trucks
        val hours = (dist / 40).toInt()
        val minutes = ((dist / 40 - hours) * 60).toInt()
        estimatedDuration = "${hours}h ${minutes}m"
    }

    var selectedDriverId by mutableStateOf<String?>(null)
    var selectedDriverName by mutableStateOf("")

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting

    fun validatePickup(): String? {
        if (pickupAddress.isBlank()) return "Pickup address is required"
        if (pickupContactName.isBlank()) return "Contact name is required"
        if (pickupContactPhone.length < 10) return "Valid phone number is required"
        return null
    }

    fun validateDrop(): String? {
        if (dropAddress.isBlank()) return "Destination address is required"
        if (receiverName.isBlank()) return "Receiver name is required"
        if (receiverPhone.length < 10) return "Valid receiver phone is required"
        return null
    }

    fun validateLoad(): String? {
        if ((weightInput.toDoubleOrNull() ?: 0.0) <= 0) return "Invalid weight"
        return null
    }

    fun postShipment(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val userEmail = AuthManager.getCurrentUserEmail() ?: return onError("User not logged in")
        _isPosting.value = true
        
        val shipment = Shipment(
            vendorEmail = userEmail,
            pickupAddress = pickupAddress,
            destinationAddress = dropAddress,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            destLat = dropLat,
            destLng = dropLng,
            price = totalPrice,
            weight = "$weightInput Tons",
            truckType = truckType,
            loadType = loadCategory,
            isFragile = isFragile,
            numPackages = numPackages,
            pickupContactName = pickupContactName,
            pickupContactNumber = pickupContactPhone,
            receiverName = receiverName,
            receiverPhone = receiverPhone,
            additionalInfo = specialInstructions,
            isInsured = isInsured,
            status = "Available",
            timestamp = System.currentTimeMillis()
        )

        FirestoreManager.postShipment(shipment, 
            onSuccess = { 
                _isPosting.value = false
                onSuccess(it) 
            },
            onError = { 
                _isPosting.value = false
                onError(it) 
            }
        )
    }

    fun reset() {
        pickupAddress = ""
        pickupLat = 19.0760
        pickupLng = 72.8777
        pickupContactName = ""
        pickupContactPhone = ""
        dropAddress = ""
        dropLat = 28.6139
        dropLng = 77.2090
        receiverName = ""
        receiverPhone = ""
        weightInput = "1.0"
        totalPrice = "₹0"
        selectedDriverId = null
    }
}
