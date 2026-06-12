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
import kotlinx.coroutines.Job
import javax.inject.Inject
import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private val _isPhoneVerified = MutableStateFlow(false)
    val isPhoneVerified: StateFlow<Boolean> = _isPhoneVerified

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isAuthChecked = MutableStateFlow(false)
    val isAuthChecked: StateFlow<Boolean> = _isAuthChecked

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Phone Auth States
    private val _verificationId = MutableStateFlow<String?>(null)
    val verificationId: StateFlow<String?> = _verificationId

    private val _codeSent = MutableStateFlow(false)
    val codeSent: StateFlow<Boolean> = _codeSent

    private var authCheckJob: Job? = null

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        authCheckJob?.cancel()
        authCheckJob = viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("Truckify", "Checking Auth Status...")
            
            // Safety timeout to prevent Splash hang
            val timeoutJob = launch {
                delay(5000)
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
                    if (role != null && role != "ProfileMissing" && role != "NoRoleField") {
                        _userRole.value = role
                        _isPhoneVerified.value = repository.isPhoneVerified()
                        _isLoggedIn.value = true
                        repository.getCurrentUserEmail()?.let {
                            FirestoreManager.fetchAndSaveFcmToken(it)
                        }
                    } else if (role == "ProfileMissing" || role == "NoRoleField") {
                        logout()
                    } else {
                        // role is null (likely network), don't logout, stay logged in
                        _isLoggedIn.value = true
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

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _isLoading.value = true
        repository.verifyPhoneNumber(phoneNumber, activity, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    // This can sometimes happen automatically if the phone number is already verified
                    _isLoading.value = false
                    // Handle auto-verification if needed
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _isLoading.value = false
                _error.value = e.message
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _isLoading.value = false
                _verificationId.value = verificationId
                _codeSent.value = true
            }
        })
    }

    fun verifyOtp(code: String) {
        val vId = _verificationId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.signInWithPhone(vId, code)
            result.onSuccess {
                val markResult = repository.markPhoneAsVerified()
                if (markResult.isSuccess) {
                    _isPhoneVerified.value = true
                    checkAuthStatus()
                } else {
                    _error.value = markResult.exceptionOrNull()?.message ?: "Verification failed"
                }
            }.onFailure {
                _error.value = it.message ?: "Invalid OTP"
                _isLoading.value = false
            }
        }
    }

    fun resetPhoneAuthState() {
        _codeSent.value = false
        _verificationId.value = null
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
        _isPhoneVerified.value = false
        _userRole.value = null
        _isLoading.value = false
        _isAuthChecked.value = true
    }

    fun updateRole(role: String) {
        _userRole.value = role
        _isLoggedIn.value = true
    }
}
