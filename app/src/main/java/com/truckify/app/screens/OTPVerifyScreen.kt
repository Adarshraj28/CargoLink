package com.truckify.app.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.ui.theme.*
import com.truckify.app.components.DeliveredAnimation
import com.truckify.app.components.TruckifyButton
import com.truckify.app.components.TruckifyTopAppBar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerifyScreen(shipmentId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var otpValue by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    // Real-time Timer State
    var timeLeft by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(key1 = timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else {
            canResend = true
        }
    }

    fun handleVerify() {
        if (otpValue.length != 6) return
        
        loading = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                FirestoreManager.verifyDeliveryOtp(shipmentId, otpValue, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0,
                    onSuccess = { loading = false; showSuccess = true },
                    onError = { 
                        loading = false
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        if (it.contains("expired", ignoreCase = true)) {
                            otpValue = ""
                        }
                    }
                )
            }
        } else {
            FirestoreManager.verifyDeliveryOtp(shipmentId, otpValue, 0.0, 0.0,
                onSuccess = { loading = false; showSuccess = true },
                onError = { 
                    loading = false
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    if (it.contains("expired", ignoreCase = true)) {
                        otpValue = ""
                    }
                }
            )
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onBack,
            confirmButton = {
                TextButton(onClick = onBack) { 
                    Text("DONE", fontWeight = FontWeight.Bold, color = SuccessGreen) 
                }
            },
            title = { 
                Text(
                    "Delivery Successful!", 
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    DeliveredAnimation()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "The shipment has been marked as delivered and payment is released.", 
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, 
                        color = TextSecondary
                    )
                }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = DarkSurface
        )
    }

    Scaffold(
        topBar = {
            TruckifyTopAppBar(
                title = "Verify Delivery",
                onBack = onBack
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "Enter Delivery OTP", 
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Ask the customer for the 6-digit code shown on their screen to complete the verification.", 
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary, 
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Custom OTP Input UI
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = otpValue,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            otpValue = it
                            if (it.length == 6) handleVerify()
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.focusRequester(focusRequester).size(1.dp),
                    decorationBox = { it() }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (0..5).forEach { index ->
                        val char = if (index < otpValue.length) otpValue[index].toString() else ""
                        val isFocused = otpValue.length == index
                        
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isFocused) PrimaryBlue.copy(alpha = 0.1f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) PrimaryBlue else DarkBorder
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(60.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (timeLeft > 0) "Resend code in ${timeLeft}s" else "Didn't receive the code?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canResend) PrimaryBlue else TextSecondary
                )
                if (canResend) {
                    TextButton(onClick = {
                        FirestoreManager.resendDeliveryOtp(shipmentId,
                            onSuccess = { 
                                timeLeft = 60
                                canResend = false
                                Toast.makeText(context, "New OTP sent to customer!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }) {
                        Text("RESEND OTP", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TruckifyButton(
                text = "Confirm Delivery",
                onClick = { handleVerify() },
                isLoading = loading,
                enabled = otpValue.length == 6
            )
            
            if (loading) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Verifying location and OTP...", 
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue
                )
            }
        }
    }
}
