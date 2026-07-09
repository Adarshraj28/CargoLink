package com.cargolink.app.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*
import androidx.compose.foundation.border

@Composable
fun KPICard(title: String, value: String, icon: ImageVector, color: Color) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically()
    ) {
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
}

@Composable
fun QuickActionBtn(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ),
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
    val currentStep = when (status) {
        "Accepted" -> 0
        "Arrived" -> 1
        "In Transit" -> 2
        "Arrived at Destination" -> 3
        "Completed" -> 4
        else -> 0
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.background(BlueGradient).padding(20.dp)) {
            // Header with progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Shipment", 
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#${shipmentId.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusColor = when(status) {
                            "In Transit" -> Color.Cyan
                            "Arrived", "Arrived at Destination" -> WarningOrange
                            else -> SuccessGreen
                        }
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = status.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            ShipmentStepIndicator(currentStep = currentStep)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Route Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PICKUP", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    LocationText(
                        lat = pickupLat,
                        lng = pickupLng,
                        defaultAddress = origin,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, 
                    contentDescription = null, 
                    tint = Color.White.copy(alpha = 0.4f), 
                    modifier = Modifier.padding(horizontal = 12.dp).size(20.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text("DELIVERY", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    LocationText(
                        lat = destLat,
                        lng = destLng,
                        defaultAddress = destination,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Actions Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ESTIMATED ARRIVAL", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text(text = eta, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = {
                            val targetLat = if (currentStep < 2) pickupLat else destLat
                            val targetLng = if (currentStep < 2) pickupLng else destLng
                            val gmmIntentUri = Uri.parse("google.navigation:q=$targetLat,$targetLng")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Maps not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Navigate", tint = Color.White)
                    }

                    val actionText = when (status) {
                        "Accepted" -> "Mark Arrived"
                        "Arrived" -> "Verify Pickup"
                        "In Transit" -> "Mark Delivered"
                        "Arrived at Destination" -> "Verify Delivery"
                        else -> "View Details"
                    }

                    val actionColor = when(status) {
                        "Arrived", "Arrived at Destination" -> Color.White
                        else -> SuccessGreen
                    }

                    val contentColor = if (actionColor == Color.White) PrimaryBlue else Color.White

                    Button(
                        onClick = {
                            when (status) {
                                "Accepted" -> {
                                    com.cargolink.app.firebase.FirestoreManager.markArrivedAtPickup(shipmentId, vendorEmail) {
                                        Toast.makeText(context, "Arrived at Pickup!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "Arrived" -> onQrClick(shipmentId)
                                "In Transit" -> {
                                    com.cargolink.app.firebase.FirestoreManager.markArrivedAtDestination(shipmentId, vendorEmail) {
                                        Toast.makeText(context, "Arrived at Destination!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "Arrived at Destination" -> onQrClick(shipmentId)
                                else -> onTrackClick(shipmentId)
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = actionColor, contentColor = contentColor),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(actionText, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ShipmentStepIndicator(currentStep: Int) {
    val steps = listOf("Accepted", "Pickup", "Transit", "Delivery")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val isActive = index <= currentStep
            val isCurrent = index == currentStep
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 24.dp else 16.dp)
                        .background(
                            if (isActive) Color.White else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStep) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = PrimaryBlue)
                    } else if (isCurrent) {
                        Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            if (index < currentStep) Color.White else Color.White.copy(alpha = 0.2f)
                        )
                )
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
    shipment: com.cargolink.app.models.Shipment, 
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                    label = "alpha"
                )
                
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.1f), 
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.alpha(alpha)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMART MATCH", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(shipment.price, color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Route
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    LocationText(lat = shipment.pickupLat, lng = shipment.pickupLng, defaultAddress = shipment.pickupAddress, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
                Box(modifier = Modifier.padding(start = 3.5.dp).width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).border(2.dp, SuccessGreen, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    LocationText(lat = shipment.destLat, lng = shipment.destLng, defaultAddress = shipment.destinationAddress, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Specs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SpecItem(Icons.Default.Scale, shipment.weight, "Weight")
                SpecItem(Icons.Default.Inventory2, shipment.loadType, "Type")
                SpecItem(Icons.Default.LocalShipping, shipment.truckType, "Vehicle")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text("Decline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1.5f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Accept Load", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SpecItem(icon: ImageVector, value: String, label: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
