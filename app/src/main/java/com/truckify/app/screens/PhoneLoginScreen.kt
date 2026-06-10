package com.truckify.app.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.TruckifyButton
import com.truckify.app.components.TruckifyTextField
import com.truckify.app.ui.theme.*
import com.truckify.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    
    val isLoading by authViewModel.isLoading.collectAsState()
    val codeSent by authViewModel.codeSent.collectAsState()
    val error by authViewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Login", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        authViewModel.resetPhoneAuthState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (!codeSent) "Verify Your Number" else "Enter OTP",
                style = MaterialTheme.typography.headlineLarge,
                color = DarkBlue,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (!codeSent) 
                    "We will send you a 6-digit verification code" 
                    else "Code sent to $phoneNumber",
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            if (!codeSent) {
                TruckifyTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "Phone Number (with +91)",
                    leadingIcon = Icons.Default.Phone
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TruckifyButton(
                    text = "Get OTP",
                    onClick = {
                        if (phoneNumber.length < 10) {
                            Toast.makeText(context, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                        } else {
                            authViewModel.sendOtp(phoneNumber, activity)
                        }
                    },
                    isLoading = isLoading
                )
            } else {
                TruckifyTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    label = "6-Digit OTP",
                    leadingIcon = Icons.Default.Phone
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TruckifyButton(
                    text = "Verify & Login",
                    onClick = {
                        if (otpCode.length == 6) {
                            authViewModel.verifyOtp(otpCode)
                        } else {
                            Toast.makeText(context, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isLoading = isLoading
                )
                
                TextButton(onClick = { authViewModel.resetPhoneAuthState() }) {
                    Text("Change Phone Number", color = PrimaryBlue)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = {
                authViewModel.logout()
                onBack()
            }) {
                Text("Logout and try another account", color = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}
