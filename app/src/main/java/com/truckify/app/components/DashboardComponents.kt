package com.truckify.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.models.Shipment
import com.truckify.app.ui.theme.*

@Composable
fun KPICard(title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .width(150.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )
            Text(
                text = title, 
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionBtn(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            color = color.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAll != null) {
            Text(
                text = "View All",
                modifier = Modifier.clickable { onSeeAll() },
                color = PrimaryBlue,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActiveTripCard(
    shipmentId: String = "",
    origin: String = "Delhi",
    destination: String = "Jaipur",
    pickupLat: Double = 0.0,
    pickupLng: Double = 0.0,
    destLat: Double = 0.0,
    destLng: Double = 0.0,
    eta: String = "2h 15m",
    status: String = "Accepted", // Accepted, Arrived, In Transit
    vendorEmail: String = "",
    onTrackClick: (String) -> Unit,
    onQrClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.background(BlueGradient).padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Trip", 
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LocationText(
                            lat = pickupLat,
                            lng = pickupLng,
                            defaultAddress = origin,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward, 
                            contentDescription = null, 
                            tint = Color.White.copy(alpha = 0.6f), 
                            modifier = Modifier.padding(horizontal = 8.dp).size(16.dp)
                        )
                        LocationText(
                            lat = destLat,
                            lng = destLng,
                            defaultAddress = destination,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val targetLat = if (status == "Accepted" || status == "Arrived") pickupLat else destLat
                            val targetLng = if (status == "Accepted" || status == "Arrived") pickupLng else destLng
                            val gmmIntentUri = Uri.parse("google.navigation:q=$targetLat,$targetLng")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Maps not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Navigation, 
                            contentDescription = "Navigate", 
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { onQrClick(shipmentId) },
                        modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.QrCode, 
                            contentDescription = "Show QR", 
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (status == "Accepted") "ON THE WAY TO PICKUP" else if (status == "Arrived") "READY FOR PICKUP" else "IN TRANSIT", 
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = eta, 
                        color = Color.White, 
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onTrackClick(shipmentId) },
                        modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Track", tint = Color.White)
                    }
                    
                    when (status) {
                        "Accepted" -> {
                            Button(
                                onClick = {
                                    com.truckify.app.firebase.FirestoreManager.markArrivedAtPickup(shipmentId, vendorEmail) {
                                        Toast.makeText(context, "Marked as Arrived!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("I've Arrived", fontWeight = FontWeight.Bold)
                            }
                        }
                        "Arrived" -> {
                            Button(
                                onClick = { onQrClick(shipmentId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("Verify OTP", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Button(
                                onClick = { onTrackClick(shipmentId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("Current Order", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomItem(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(26.dp)
    )
}

@Composable
fun AILoadSuggestionCard(
    shipment: com.truckify.app.models.Shipment, 
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.1f), 
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI MATCHED", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(shipment.price, color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LocationText(lat = shipment.pickupLat, lng = shipment.pickupLng, defaultAddress = shipment.pickupAddress, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp).padding(vertical = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            LocationText(lat = shipment.destLat, lng = shipment.destLng, defaultAddress = shipment.destinationAddress, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("Skip", color = Color.Gray, fontSize = 12.sp)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1.5f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MiniEarningsChart() {
    Canvas(modifier = Modifier.size(60.dp, 30.dp)) {
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(size.width * 0.2f, size.height * 0.7f)
            lineTo(size.width * 0.4f, size.height * 0.85f)
            lineTo(size.width * 0.6f, size.height * 0.3f)
            lineTo(size.width * 0.8f, size.height * 0.5f)
            lineTo(size.width, 0f)
        }
        drawPath(
            path = path, 
            color = SuccessGreen, 
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun NotificationCard(title: String, message: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = PrimaryBlue.copy(alpha = 0.1f), 
                shape = CircleShape, 
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
