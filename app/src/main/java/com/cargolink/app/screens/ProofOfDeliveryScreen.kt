package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.cargolink.app.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofOfDeliveryScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()
    var receiverName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Proof of Delivery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DarkBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.shadow(32.dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Complete Shipment", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                "Finalize Delivery",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue
            )
            Text(
                "Please capture the handover details to close the shipment.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // PHOTO CAPTION CARD
            Text("Delivery Photo", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { },
                shape = RoundedCornerShape(28.dp),
                color = Beige.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(64.dp).background(Color.White, CircleShape).shadow(4.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Take a photo of the cargo", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                    Text("Ensure label is visible", fontSize = 12.sp, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // RECEIVER INFO & SIGNATURE
            Text("Recipient Verification", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    PremiumInput(label = "Receiver Name", value = receiverName, onValueChange = { receiverName = it }, icon = Icons.Default.Person)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("E-Signature", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // SIGNATURE PAD PLACEHOLDER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Background, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign Here", color = Color.LightGray, style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = { }, modifier = Modifier.align(Alignment.End)) {
                        Text("Clear Signature", color = ErrorRed, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ADDITIONAL DOCUMENTS & NOTES
            Text("Delivery Notes", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Any remarks about the shipment condition...") },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedBorderColor = PrimaryBlue
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
