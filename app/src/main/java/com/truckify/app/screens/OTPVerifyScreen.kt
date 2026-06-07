package com.truckify.app.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.ui.theme.DarkBlue
import com.truckify.app.components.DeliveredAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerifyScreen(shipmentId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var otpValue by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onBack,
            confirmButton = {
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { 
                    Text("Done") 
                }
            },
            title = { Text("Delivery Verified", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DeliveredAnimation()
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Delivery OTP", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter the 6-digit OTP received by the customer to complete delivery.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = otpValue,
                onValueChange = { if (it.length <= 6) otpValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("6-Digit OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (otpValue.length != 6) {
                        Toast.makeText(context, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    loading = true
                    
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            FirestoreManager.verifyDeliveryOtp(shipmentId, otpValue, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0,
                                onSuccess = { loading = false; showSuccess = true },
                                onError = { loading = false; Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    } else {
                        FirestoreManager.verifyDeliveryOtp(shipmentId, otpValue, 0.0, 0.0,
                            onSuccess = { loading = false; showSuccess = true },
                            onError = { loading = false; Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Verify & Complete Delivery", fontWeight = FontWeight.Bold)
            }
        }
    }
}
