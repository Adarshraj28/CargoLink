package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOTPScreen(
    onBack: () -> Unit,
    onVerify: (String) -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Verify Delivery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DarkBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // VERIFICATION HEADER
            Text(
                "Secure Delivery OTP",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue
            )
            Text(
                "Please enter the 4-digit code provided by the receiver to complete the shipment.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            // DRIVER & RECEIVER AVATARS (Premium UI)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(label = "Driver", name = "Ramesh K.", color = PrimaryBlue)
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Icon(Icons.Default.SyncAlt, null, tint = Color.LightGray)
                }
                UserAvatar(label = "Receiver", name = "Adarsh S.", color = SuccessGreen)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // OTP INPUT BOXES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                repeat(4) { index ->
                    val char = otpValue.getOrNull(index)?.toString() ?: ""
                    val isFocused = otpValue.length == index
                    
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(if (isFocused) 8.dp else 2.dp, RoundedCornerShape(16.dp))
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) PrimaryBlue else Color.LightGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkBlue
                        )
                    }
                }
            }
            
            // Invisible TextField for input logic
            TextField(
                value = otpValue,
                onValueChange = { if (it.length <= 4) otpValue = it },
                modifier = Modifier.height(0.dp).width(0.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // SCAN QR OPTION (Premium Glassmorphism)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).background(PrimaryBlue.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan QR Code", fontWeight = FontWeight.Bold, color = DarkBlue)
                        Text("Faster verification via receiver's app", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ACTIONS
            Button(
                onClick = { if (otpValue.length == 4) onVerify(otpValue) },
                enabled = otpValue.length == 4,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                Text("Verify & Complete Delivery", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = {}) {
                Text("Resend Code (Available in 0:45)", color = Color.Gray, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun UserAvatar(label: String, name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(12.dp, CircleShape)
                .background(Color.White, CircleShape)
                .padding(4.dp)
                .border(2.dp, color.copy(alpha = 0.2f), CircleShape)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = color, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
    }
}
