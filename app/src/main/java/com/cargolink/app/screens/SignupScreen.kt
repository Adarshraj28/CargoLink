package com.cargolink.app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.components.CargoLinkButton
import com.cargolink.app.components.CargoLinkTextField
import com.cargolink.app.ui.theme.*
import com.cargolink.app.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onBack: () -> Unit, 
    authViewModel: AuthViewModel = hiltViewModel(),
    initialRole: String = "Vendor"
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    var phoneNumber by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(false) }
    
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()
    val codeSent by authViewModel.codeSent.collectAsStateWithLifecycle()
    var otpCode by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (!codeSent) "Sign Up" else "Verify OTP", fontWeight = FontWeight.Bold)
                        if (codeSent) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.size(4.dp).background(PrimaryBlue, androidx.compose.foundation.shape.CircleShape))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (codeSent) authViewModel.resetPhoneAuthState()
                        else onBack()
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = if (!codeSent) "Create Account" else "Verification",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (!codeSent) "Sign up with your phone number to get started" 
                       else "We've sent a code to +91 $phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            if (!codeSent) {
                Text("Phone Number", 
                    style = MaterialTheme.typography.titleSmall, 
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.width(80.dp).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+91", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    CargoLinkTextField(
                        value = phoneNumber,
                        onValueChange = { if (it.length <= 10) phoneNumber = it },
                        label = "Phone Number",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = agreeToTerms,
                        onCheckedChange = { agreeToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "I agree to the Terms of Service and Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { agreeToTerms = !agreeToTerms }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                CargoLinkButton(
                    text = "Get OTP",
                    onClick = {
                        if (phoneNumber.length != 10) {
                            Toast.makeText(context, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                            return@CargoLinkButton
                        }
                        if (!agreeToTerms) {
                            Toast.makeText(context, "Please agree to Terms", Toast.LENGTH_SHORT).show()
                            return@CargoLinkButton
                        }
                        authViewModel.sendOtp("+91$phoneNumber", activity)
                    },
                    isLoading = isLoading
                )
            } else {
                CargoLinkTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    label = "6-Digit OTP",
                    leadingIcon = Icons.Default.Lock,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                CargoLinkButton(
                    text = "Verify & Continue",
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Already have an account? ", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Login",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
