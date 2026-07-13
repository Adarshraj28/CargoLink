package com.cargolink.app.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*

@Composable
fun KPICard(title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.padding(vertical = 8.dp).width(150.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumSectionHeader(title: String, subtitle: String? = null, onSeeAll: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = DarkBlue)
            if (onSeeAll != null) {
                Surface(
                    onClick = onSeeAll,
                    color = Beige.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
                ) {
                    Text(text = "View All", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun PremiumQuickAction(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(110.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Beige.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).background(color, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, color = DarkBlue, fontSize = 15.sp)
                Text(description, fontSize = 12.sp, color = Color(0xFF5D4037).copy(alpha = 0.7f), lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, trend: String? = null, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    val animatedScale by animateFloatAsState(if (isVisible) 1f else 0.8f, tween(600, easing = FastOutSlowInEasing), label = "scale")

    Card(
        modifier = modifier.height(140.dp).scale(animatedScale),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
                if (trend != null) {
                    val trendColor = if (trend.startsWith("+")) SuccessGreen else ErrorRed
                    Surface(color = trendColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(trend, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = trendColor)
                    }
                }
            }
            Column {
                Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(value, fontWeight = FontWeight.ExtraBold, color = DarkBlue, fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun GlassIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White.copy(alpha = 0.8f), border = BorderStroke(1.dp, Color.White), shadowElevation = 4.dp) {
        Icon(icon, null, modifier = Modifier.padding(10.dp).size(22.dp), tint = DarkBlue)
    }
}

@Composable
fun ActiveShipmentPremiumCard(shipment: Shipment, onTrackClick: () -> Unit) {
    Card(
        modifier = Modifier.width(300.dp).clickable { onTrackClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp).background(Brush.verticalGradient(listOf(Color.White, Beige.copy(alpha = 0.05f))))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(SuccessGreen, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(shipment.status.uppercase(), color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("ETA: 2h 45m", color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(shipment.destinationAddress, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkBlue, maxLines = 1)
            Text("From: ${shipment.pickupAddress}", fontSize = 14.sp, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(progress = { 0.6f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = PrimaryBlue, trackColor = PrimaryBlue.copy(alpha = 0.1f))
        }
    }
}

@Composable
fun AISuggestionsSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = SoftPurple.copy(alpha = 0.2f))
                .border(1.dp, SoftPurple.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = SoftPurple.copy(alpha = 0.1f), shape = CircleShape) {
                        Icon(Icons.Default.AutoAwesome, null, tint = SoftPurple, modifier = Modifier.padding(8.dp).size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Optimization", fontWeight = FontWeight.ExtraBold, color = SoftPurple, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Consolidate 3 shipments to save ₹4,200 today based on real-time route overlaps.", fontSize = 14.sp, color = DarkBlue.copy(alpha = 0.7f), lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {}, 
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPurple), 
                    shape = RoundedCornerShape(16.dp), 
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Apply Smart Route", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MarketTrendChart() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.8f)
            cubicTo(size.width * 0.2f, size.height * 0.7f, size.width * 0.4f, size.height * 0.9f, size.width * 0.6f, size.height * 0.4f)
            cubicTo(size.width * 0.8f, size.height * 0.1f, size.width * 0.9f, size.height * 0.3f, size.width, size.height * 0.2f)
        }
        drawPath(path = path, color = PrimaryBlue.copy(alpha = 0.3f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = PrimaryBlue, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f))
    }
}

@Composable
fun DocumentCard(title: String, type: String, date: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(Beige.copy(alpha = 0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, null, tint = Color(0xFF5D4037))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 14.sp)
                Text("$type • $date", fontSize = 11.sp, color = Color.Gray)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.FileDownload, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun WalletOverviewCard(balance: Double, monthlySpend: Double) {
    var isUnlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(200.dp)
            .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = PrimaryBlue.copy(alpha = 0.3f))
            .background(brush = PremiumGradient, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .clickable {
                authenticateUser(
                    context = context,
                    onSuccess = { isUnlocked = true },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = 350f, center = center.copy(x = size.width, y = 0f))
            drawCircle(Color.White.copy(alpha = 0.04f), radius = 250f, center = center.copy(x = 0f, y = size.height))
        }
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Cargolink Premium Wallet", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isUnlocked) "₹${String.format("%,.0f", balance)}" else "₹ • • • •", 
                        color = Color.White, 
                        fontSize = 34.sp, 
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(
                    if (isUnlocked) Icons.Default.AccountBalanceWallet else Icons.Default.Lock, 
                    null, 
                    tint = Color.White.copy(alpha = 0.3f), 
                    modifier = Modifier.size(44.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(14.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TrendingUp, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Monthly Spending: ₹${String.format("%,.0f", monthlySpend)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) PrimaryBlue else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(26.dp))
    }
}

@Composable
fun TrustedPartnerTicker() {
    val partners = listOf("Uber Freight", "Stripe", "Apple", "Volvo", "DHL", "FedEx")
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
        items(partners) { partner ->
            Text(partner, fontWeight = FontWeight.Black, color = Color.LightGray.copy(alpha = 0.4f), fontSize = 22.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun ActiveTripCard(
    shipmentId: String = "", origin: String = "Delhi", destination: String = "Jaipur",
    pickupLat: Double = 0.0, pickupLng: Double = 0.0, destLat: Double = 0.0, destLng: Double = 0.0,
    eta: String = "2h 15m", status: String = "Accepted", vendorEmail: String = "",
    onTrackClick: (String) -> Unit, onQrClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentStep = when (status) {
        "Accepted" -> 0; "Arrived" -> 1; "In Transit" -> 2; "Arrived at Destination" -> 3; "Completed" -> 4; else -> 0
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier.background(BlueGradient).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Active Shipment", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    Text("#${shipmentId.takeLast(6).uppercase()}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
                Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when(status) { "In Transit" -> Color.Cyan; "Arrived", "Arrived at Destination" -> WarningOrange; else -> SuccessGreen }
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(status.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            ShipmentStepIndicator(currentStep = currentStep)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PICKUP", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    LocationText(lat = pickupLat, lng = pickupLng, defaultAddress = origin, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 12.dp).size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("DELIVERY", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    LocationText(lat = destLat, lng = destLng, defaultAddress = destination, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ESTIMATED ARRIVAL", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text(eta, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = {
                        val targetLat = if (currentStep < 2) pickupLat else destLat
                        val targetLng = if (currentStep < 2) pickupLng else destLng
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$targetLat,$targetLng")).apply { setPackage("com.google.android.apps.maps") })
                    }, modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))) {
                        Icon(Icons.Default.Navigation, null, tint = Color.White)
                    }
                    val actionText = when (status) { "Accepted" -> "Mark Arrived"; "Arrived" -> "Verify Pickup"; "In Transit" -> "Mark Delivered"; "Arrived at Destination" -> "Verify Delivery"; else -> "View Details" }
                    val actionColor = when(status) { "Arrived", "Arrived at Destination" -> Color.White; else -> SuccessGreen }
                    val contentColor = if (actionColor == Color.White) PrimaryBlue else Color.White
                    Button(onClick = {
                        when (status) {
                            "Accepted" -> com.cargolink.app.firebase.FirestoreManager.markArrivedAtPickup(shipmentId, vendorEmail) {}
                            "Arrived" -> onQrClick(shipmentId)
                            "In Transit" -> com.cargolink.app.firebase.FirestoreManager.markArrivedAtDestination(shipmentId, vendorEmail) {}
                            "Arrived at Destination" -> onQrClick(shipmentId)
                            else -> onTrackClick(shipmentId)
                        }
                    }, modifier = Modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = actionColor, contentColor = contentColor), shape = RoundedCornerShape(14.dp)) {
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, title ->
            val isActive = index <= currentStep; val isCurrent = index == currentStep
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(if (isCurrent) 24.dp else 16.dp).background(if (isActive) Color.White else Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    if (index < currentStep) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = PrimaryBlue)
                    else if (isCurrent) Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
            }
            if (index < steps.size - 1) Box(modifier = Modifier.weight(1f).height(2.dp).padding(horizontal = 4.dp).background(if (index < currentStep) Color.White else Color.White.copy(alpha = 0.2f)))
        }
    }
}

@Composable
fun QuickActionBtn(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(20.dp))
                .background(color)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = DarkBlue, textAlign = TextAlign.Center)
    }
}

@Composable
fun NotificationCard(title: String, message: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(PrimaryBlue.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 15.sp)
                Text(message, color = Color.Gray, fontSize = 13.sp, maxLines = 2)
                Text(time, color = Color.Gray.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun BottomItem(icon: ImageVector, selected: Boolean) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
        modifier = Modifier.size(26.dp)
    )
}

@Composable
fun AILoadSuggestionCard(shipment: Shipment, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = SuccessGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("98% Match", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(shipment.price, fontWeight = FontWeight.ExtraBold, color = PrimaryBlue, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(shipment.destinationAddress, fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp, maxLines = 1)
            Text("From: ${shipment.pickupAddress}", color = Color.Gray, fontSize = 13.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)) {
                    Text("Accept", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text("Ignore", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MiniEarningsChart() {
    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.7f)
                lineTo(size.width * 0.2f, size.height * 0.4f)
                lineTo(size.width * 0.4f, size.height * 0.6f)
                lineTo(size.width * 0.6f, size.height * 0.2f)
                lineTo(size.width * 0.8f, size.height * 0.5f)
                lineTo(size.width, size.height * 0.1f)
            }
            drawPath(path = path, color = SuccessGreen, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
