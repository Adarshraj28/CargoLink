package com.cargolink.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cargolink.app.models.Shipment
import com.cargolink.app.models.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchItem>>(emptyList())
    val searchResults: StateFlow<List<SearchItem>> = _searchResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    fun onQueryChange(query: String, userRole: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            updateSuggestions(query, userRole)
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun performSearch(query: String, userRole: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            addToHistory(query)
            
            try {
                val results = mutableListOf<SearchItem>()
                
                // 1. Search Shipments
                val shipments = searchShipments(query, userRole)
                results.addAll(shipments.map { SearchItem.ShipmentItem(it) })

                // 2. Search Users (Admins and Vendors can see drivers)
                if (userRole == "Admin" || userRole == "Vendor") {
                    val users = searchUsers(query)
                    results.addAll(users)
                }

                // 3. Search Transactions (Admins and relevant roles)
                val txns = searchTransactions(query, userRole)
                results.addAll(txns.map { SearchItem.TransactionItem(it) })

                _searchResults.value = results
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun searchShipments(query: String, userRole: String): List<Shipment> {
        val allShipments = firestore.collection("shipments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .get().await().toObjects(Shipment::class.java)
        return allShipments.filter { shipment ->
            shipment.id.contains(query, ignoreCase = true) ||
            shipment.pickupAddress.contains(query, ignoreCase = true) ||
            shipment.destinationAddress.contains(query, ignoreCase = true) ||
            shipment.status.contains(query, ignoreCase = true) ||
            shipment.loadType.contains(query, ignoreCase = true) ||
            shipment.truckType.contains(query, ignoreCase = true)
        }
    }

    private suspend fun searchUsers(query: String): List<SearchItem.UserItem> {
        val drivers = firestore.collection("users")
            .whereEqualTo("role", "Driver")
            .get().await()
        
        return drivers.documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: ""
            val email = doc.getString("email") ?: ""
            if (name.contains(query, ignoreCase = true) || email.contains(query, ignoreCase = true)) {
                SearchItem.UserItem(name, email, "Driver")
            } else null
        }
    }

    private suspend fun searchTransactions(query: String, userRole: String): List<Transaction> {
        // Implement transaction search based on role
        return emptyList() 
    }

    private fun updateSuggestions(query: String, userRole: String) {
        viewModelScope.launch {
            // Static common suggestions + Dynamic from results
            val common = listOf("Delhi", "Mumbai", "Delivered", "Mini Truck", "In Transit")
            _suggestions.value = common.filter { it.contains(query, ignoreCase = true) }
        }
    }

    private fun addToHistory(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        _searchHistory.value = current.take(5)
    }

    fun clearHistory() {
        _searchHistory.value = emptyList()
    }
}

sealed class SearchItem {
    data class ShipmentItem(val shipment: Shipment) : SearchItem()
    data class UserItem(val name: String, val email: String, val role: String) : SearchItem()
    data class TransactionItem(val transaction: Transaction) : SearchItem()
    data class SuggestionItem(val text: String) : SearchItem()
}
