package com.truckify.app.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.models.Shipment
import com.truckify.app.ui.theme.DarkBlue
import com.truckify.app.ui.theme.LightBlue
import com.truckify.app.ui.theme.Beige
import com.truckify.app.ui.theme.Background
import java.util.Locale

@Composable
fun StatCard(title: String, value: String, subValue: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = subValue, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActiveShipmentCard(
    shipment: Shipment = Shipment(),
    onTrackClick: (String) -> Unit = {},
    onQrClick: (String) -> Unit = {},
    userRole: String = "Vendor"
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = LightBlue, modifier = Modifier.size(40.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (shipment.id.isEmpty()) "Delhi → Mumbai" else "${shipment.pickupAddress.split(",").first()} → ${shipment.destinationAddress.split(",").first()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Text(
                            text = "Shipment #${if (shipment.id.isEmpty()) "TRK902" else shipment.id.takeLast(4).uppercase()}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        when (shipment.status) {
                                            "Available" -> Beige
                                            "In Transit" -> Color(0xFFE3F2FD)
                                            else -> Color(0xFFE8F5E9)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = shipment.status,
                                    color = when (shipment.status) {
                                        "Available" -> DarkBlue
                                        "In Transit" -> Color(0xFF1976D2)
                                        else -> Color(0xFF2E7D32)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = shipment.price.ifEmpty { "₹12,500" }, fontWeight = FontWeight.ExtraBold, color = LightBlue)
                        }
                    }
                    
                    IconButton(onClick = { onTrackClick(shipment.id) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
                
                if (userRole == "Driver") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onQrClick(shipment.id) },
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (shipment.status == "In Transit") Color(0xFF4CAF50) else DarkBlue
                        )
                    ) {
                        Icon(
                            if (shipment.status == "In Transit") Icons.Default.LockOpen else Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (shipment.status == "In Transit") "Verify Delivery (OTP)" else "Scan Pickup QR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverLoadCard(shipment: Shipment = Shipment(), onClick: () -> Unit = {}) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (shipment.id.isEmpty()) "Delhi → Chandigarh" else "${shipment.pickupAddress.split(",").first()} → ${shipment.destinationAddress.split(",").first()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Weight: ${shipment.weight.ifEmpty { "3 Tons" }}",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = shipment.price.ifEmpty { "₹12,500" },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = LightBlue
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip("FastPay")
                    InfoChip("Verified")
                    InfoChip("Fuel Inc.")
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Review Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun NewLoadPopup(shipment: Shipment, onDismiss: () -> Unit, onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightBlue)
            ) {
                Text("Accept Ride", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Swipe to Reject", color = Color.Gray)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Beige),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = DarkBlue)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(shipment.vendorEmail.split("@").first().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Text(" 4.8", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        },
        text = {
            Column {
                Text("${shipment.pickupAddress.split(",").first()} → ${shipment.destinationAddress.split(",").first()}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Price: ${shipment.price}", color = LightBlue, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun NotificationCard(title: String, message: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title.uppercase(), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = message, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = time, color = Color.Gray)
            }
        }
    }
}

@Composable
fun LoadCard() {
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "₹38,500", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = DarkBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Delhi → Mumbai", color = Color.Gray)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Beige).padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(text = "Available", color = DarkBlue, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                InfoChip("2 Tons")
                InfoChip("4h ETA")
                InfoChip("AC Truck")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                Text(text = "View Details", fontSize = 16.sp)
            }
        }
    }
}
