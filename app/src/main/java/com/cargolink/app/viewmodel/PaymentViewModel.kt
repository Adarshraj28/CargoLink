package com.cargolink.app.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor() : ViewModel() {

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance

    private val _history = MutableStateFlow<List<Transaction>>(emptyList())
    val history: StateFlow<List<Transaction>> = _history

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _tripEarnings = MutableStateFlow(0.0)
    val tripEarnings: StateFlow<Double> = _tripEarnings

    private val _extraCharges = MutableStateFlow(0.0)
    val extraCharges: StateFlow<Double> = _extraCharges

    private val _otherCharges = MutableStateFlow(0.0)
    val otherCharges: StateFlow<Double> = _otherCharges

    private var balanceListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    fun loadData() {
        val email = AuthManager.getCurrentUserEmail() ?: return
        
        _isLoading.value = true
        balanceListener?.remove()
        balanceListener = FirestoreManager.getWalletBalance(email) { 
            _balance.value = it
        }

        historyListener?.remove()
        historyListener = FirestoreManager.getTransactionHistory(email) { txns ->
            _history.value = txns
            _tripEarnings.value = txns.filter { it.type == "Payout" || it.type == "COD Collection" }.sumOf { it.amount }
            _extraCharges.value = txns.filter { it.type == "Incentive" }.sumOf { it.amount }
            _otherCharges.value = txns.filter { it.type == "Toll" || it.type == "Penalty" }.sumOf { it.amount }
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        balanceListener?.remove()
        historyListener?.remove()
    }
}
