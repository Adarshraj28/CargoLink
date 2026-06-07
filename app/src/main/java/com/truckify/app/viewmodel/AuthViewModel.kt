package com.truckify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckify.app.data.repository.AuthRepository
import com.truckify.app.firebase.FirestoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked: StateFlow<Boolean> = _isAuthChecked

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (_isLoading.value) return 
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("Truckify", "Checking Auth Status...")
            
            // Safety timeout to prevent Splash hang
            val timeoutJob = launch {
                delay(8000)
                if (!_isAuthChecked.value) {
                    android.util.Log.w("Truckify", "Auth check timed out!")
                    _isAuthChecked.value = true
                    _isLoading.value = false
                }
            }

            try {
                val loggedIn = repository.isUserLoggedIn()
                android.util.Log.d("Truckify", "isLoggedIn: $loggedIn")
                if (loggedIn) {
                    val role = repository.getUserRole()
                    android.util.Log.d("Truckify", "userRole: $role")
                    if (role == null || role == "ProfileMissing" || role == "NoRoleField") {
                        logout()
                    } else {
                        _userRole.value = role
                        _isLoggedIn.value = true
                        repository.getCurrentUserEmail()?.let {
                            FirestoreManager.fetchAndSaveFcmToken(it)
                        }
                    }
                } else {
                    _isLoggedIn.value = false
                    _userRole.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("Truckify", "Auth Error: ${e.message}")
                _error.value = "Auth error: ${e.message}"
            } finally {
                timeoutJob.cancel()
                _isLoading.value = false
                _isAuthChecked.value = true
                android.util.Log.d("Truckify", "Auth check complete.")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.login(email, password)
            result.onSuccess {
                checkAuthStatus()
            }.onFailure {
                _error.value = it.message ?: "Login Failed"
                _isLoading.value = false
            }
        }
    }

    fun signup(name: String, email: String, password: String, role: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = repository.signup(name, email, password, role)
            result.onSuccess {
                checkAuthStatus()
            }.onFailure {
                _error.value = it.message ?: "Signup Failed"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun logout() {
        repository.logout()
        _isLoggedIn.value = false
        _userRole.value = null
        _isLoading.value = false
        _isAuthChecked.value = true
    }

    fun updateRole(role: String) {
        _userRole.value = role
        _isLoggedIn.value = true
    }
}
