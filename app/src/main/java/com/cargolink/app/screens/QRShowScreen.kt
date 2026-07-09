package com.cargolink.app.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.DarkBlue
import com.cargolink.app.ui.theme.LightBlue
import com.cargolink.app.utils.QRCodeGenerator
import com.cargolink.app.components.RatingDialog
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRShowScreen(shipmentId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var shipment by remember { mutableStateOf<Shipment?>(null) }
    var deliveryOtp by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showRating by remember { mutableStateOf(false) }

    LaunchedEffect(shipmentId) {
        FirestoreManager.listenToShipment(shipmentId) { updatedShipment ->
            if (shipment?.status != "Delivered" && updatedShipment.status == "Delivered") {
                showRating = true
            }
            shipment = updatedShipment
            qrBitmap = QRCodeGenerator.generate(updatedShipment.podToken)
        }
        
        FirestoreManager.getShipmentOtp(shipmentId) { otp ->
            deliveryOtp = otp
        }
    }

    if (showRating) {
        RatingDialog(
            onDismiss = { 
                showRating = false
                onBack()
            },
            onSubmit = { stars ->
                Toast.makeText(context, "Thank you for rating: $stars stars!", Toast.LENGTH_SHORT).show()
                showRating = false
                onBack()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proof of Delivery", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isInTransit = shipment?.status == "In Transit"
            
            Text(
                text = if (isInTransit) "Delivery Verification OTP" else "Pickup Verification QR", 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp, 
                color = DarkBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isInTransit) "Share this OTP with the driver at the destination." else "Show this QR to the Driver at pickup.", 
                color = Color.Gray, 
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            if (isInTransit) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = deliveryOtp ?: "------",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = LightBlue,
                            letterSpacing = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        Button(
                            onClick = {
                                deliveryOtp?.let {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                                    Toast.makeText(context, "OTP Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightBlue)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy OTP")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.size(300.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        qrBitmap?.let { 
                            Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(250.dp))
                        } ?: CircularProgressIndicator(color = LightBlue)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            shipment?.let { s ->
                Text("Shipment #${s.id.takeLast(4).uppercase()}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = LightBlue)
                Text("${s.pickupAddress.split(",").first()} → ${s.destinationAddress.split(",").first()}", color = Color.Gray)
            }
        }
    }
}
