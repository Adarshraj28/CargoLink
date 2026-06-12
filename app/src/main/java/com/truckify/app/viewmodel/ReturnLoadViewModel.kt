package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckify.app.data.repository.ReturnLoadRepository
import com.truckify.app.models.Shipment
import com.truckify.app.firebase.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReturnLoadViewModel @Inject constructor(
    private val repository: ReturnLoadRepository
) : ViewModel() {

    private val _returnLoads = MutableStateFlow<List<Shipment>>(emptyList())
    val returnLoads: StateFlow<List<Shipment>> = _returnLoads

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isReturning = MutableStateFlow(false)
    val isReturning: StateFlow<Boolean> = _isReturning

    fun setReturningStatus(isReturning: Boolean) {
        val email = AuthManager.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _isReturning.value = isReturning
                if (isReturning) {
                    repository.updateDriverStatus(email, "Returning")
                    fetchReturnLoads()
                } else {
                    repository.updateDriverStatus(email, "Available")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update status"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchReturnLoads() {
        val email = AuthManager.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val driver = repository.getDriverProfile(email)
                if (driver != null) {
                    val loads = repository.getPotentialReturnLoads(driver)
                    _returnLoads.value = loads
                } else {
                    _error.value = "Driver profile not found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch return loads"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptReturnLoad(shipment: Shipment, onSuccess: () -> Unit) {
        val email = AuthManager.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.acceptReturnLoad(email, shipment.id, shipment.pickupAddress, shipment.destinationAddress)
            _isLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure {
                _error.value = it.message ?: "Failed to accept load"
            }
        }
    }
}
