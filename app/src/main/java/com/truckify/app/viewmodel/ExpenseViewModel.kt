package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ExpenseViewModel : ViewModel() {
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private var expenseListener: ListenerRegistration? = null

    fun loadExpenses() {
        val email = AuthManager.getCurrentUserEmail() ?: return
        expenseListener?.remove()
        expenseListener = FirestoreManager.getExpenseHistory(email) {
            _expenses.value = it
        }
    }

    fun addExpense(amount: Double, type: String, description: String) {
        val email = AuthManager.getCurrentUserEmail() ?: return
        val expense = Expense(
            driverEmail = email,
            amount = amount,
            type = type,
            description = description
        )
        FirestoreManager.addExpense(expense) {}
    }

    override fun onCleared() {
        super.onCleared()
        expenseListener?.remove()
    }
}
