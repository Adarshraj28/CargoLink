package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import java.util.Locale
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.components.*
import com.cargolink.app.ui.theme.*
import com.cargolink.app.viewmodel.HomeViewModel
import com.cargolink.app.firebase.AuthManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeDashboard(
    onCreateClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFleetClick: () -> Unit,
    onRoutesClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onChatbotClick: () -> Unit,
    onTrackClick: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onQrClick: (String, String) -> Unit,
    onDriversClick: () -> Unit,
    onReferClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    isGuest: Boolean = false,
    onCompleteProfile: () -> Unit = {},
    authViewModel: com.cargolink.app.viewmodel.AuthViewModel? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeShipments by viewModel.activeShipments.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val verificationStatus by viewModel.verificationStatus.collectAsStateWithLifecycle()
    val monthlySpend by viewModel.monthlySpend.collectAsStateWithLifecycle()
    val onTimeRate by viewModel.onTimeRate.collectAsStateWithLifecycle()
    val costSaved by viewModel.costSaved.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    var showGuestDialog by remember { mutableStateOf(false) }

    val handleAction = { action: () -> Unit ->
        if (isGuest) {
            showGuestDialog = true
        } else {
            action()
        }
    }

    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = { Text("Operation Restricted") },
            text = { 
                Column {
                    Text("This operation cannot be performed. Please complete your profile with all necessary details to unlock this feature.")
                    if (authViewModel != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("--- Developer Mode ---", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Button(
                            onClick = { 
                                authViewModel.updateRole("Vendor")
                                showGuestDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
                        ) {
                            Text("DEV: BYPASS GUEST MODE")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showGuestDialog = false
                    onCompleteProfile() 
                }) {
                    Text("Complete Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    val activeTrip = activeShipments.find { 
        it.status == "Accepted" || it.status == "Arrived" || it.status == "In Transit" 
    }
    
    val recentShipments = activeShipments.filter { it.status == "Available" }.take(3)

    LaunchedEffect(Unit) {
        if (!isGuest) {
            viewModel.loadDashboardData()
        }
    }

    androidx.activity.compose.BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        modifier = Modifier.fillMaxSize().designerBackground(),
        containerColor = Color.Transparent,
        bottomBar = {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(800)) { it }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                        .height(72.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(40.dp),
                            spotColor = PrimaryBlue.copy(alpha = 0.5f),
                            ambientColor = PrimaryBlue.copy(alpha = 0.3f)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(40.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                            ),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { }) { BottomItem(icon = Icons.Default.Home, selected = true) }
                        IconButton(onClick = { handleAction(onOrdersClick) }) { BottomItem(icon = Icons.AutoMirrored.Filled.List, selected = false) }
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BlueGradient)
                                .clickable { handleAction(onCreateClick) },
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "scale"
                            )
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Create", 
                                tint = Color.White, 
                                modifier = Modifier.size(30.dp).scale(scale)
                            )
                        }

                        IconButton(onClick = { handleAction(onHistoryClick) }) { BottomItem(icon = Icons.Default.History, selected = false) }
                        IconButton(onClick = onSettingsClick) { BottomItem(icon = Icons.Default.Settings, selected = false) }
                    }
                }
            }
        }
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000)) + expandVertically(tween(1000))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isGuest) "Welcome to CargoLink" else "Hello, $userName",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                                if (isGuest && authViewModel != null) {
                                    IconButton(
                                        onClick = { authViewModel.updateRole("Vendor") },
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .size(32.dp)
                                            .background(Color.Magenta.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Code,
                                            contentDescription = "Dev Bypass",
                                            tint = Color.Magenta,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (isGuest) {
                                Text(
                                    text = "Complete profile to start booking",
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.clickable { onCompleteProfile() }
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verified Business Account", 
                                        color = SuccessGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { handleAction(onNotificationClick) }) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Active Ships",
                            value = activeShipments.size.toString(),
                            icon = Icons.Default.LocalShipping,
                            color = PrimaryBlue
                        )
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Monthly Spend",
                            value = String.format(Locale.getDefault(), "₹%.1fL", monthlySpend / 100000.0),
                            icon = Icons.Default.AccountBalanceWallet,
                            color = SuccessGreen
                        )
                    }
                }

                item {
                    SectionHeader(title = "Quick Actions")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionBtn("Post Load", Icons.Default.AddBox, PrimaryBlue) { handleAction(onCreateClick) }
                        QuickActionBtn("My Drivers", Icons.Default.Groups, InfoBlue) { handleAction(onDriversClick) }
                        QuickActionBtn("Heatmap", Icons.Default.Map, SuccessGreen) { handleAction(onFleetClick) }
                        QuickActionBtn("Payments", Icons.Default.AccountBalanceWallet, WarningOrange) { handleAction(onPaymentsClick) }
                    }
                }

                if (activeTrip != null) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically { -it } + fadeIn()
                        ) {
                            Column {
                                SectionHeader(title = "Ongoing Shipment")
                                ActiveTripCard(
                                    shipmentId = activeTrip.id,
                                    origin = activeTrip.pickupAddress,
                                    destination = activeTrip.destinationAddress,
                                    pickupLat = activeTrip.pickupLat,
                                    pickupLng = activeTrip.pickupLng,
                                    destLat = activeTrip.destLat,
                                    destLng = activeTrip.destLng,
                                    status = activeTrip.status,
                                    vendorEmail = activeTrip.vendorEmail,
                                    onTrackClick = onTrackClick,
                                    onQrClick = { id -> onQrClick(id, activeTrip.status) }
                                )
                            }
                        }
                    }
                } else if (!isGuest) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Active Shipments", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Start by posting a new load request", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { handleAction(onCreateClick) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Book Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (isGuest) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Complete Your Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Unlock premium fleet tracking and verified driver matching by completing your profile.", fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onCompleteProfile, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                                    Text("Get Started")
                                }
                            }
                        }
                    }
                }

                item {
                    val beigeBackground = Color(0xFFFFF8E1)
                    val deepBrown = Color(0xFF5D4037)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onReferClick() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = beigeBackground),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                                val infiniteTransition = rememberInfiniteTransition(label = "referPulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.15f,
                                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                    label = "scale"
                                )
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = WarningOrange, modifier = Modifier.padding(12.dp).scale(scale))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Refer & Earn ₹2,000", fontWeight = FontWeight.Black, color = deepBrown, fontSize = 16.sp)
                                Text("Invite other businesses", fontSize = 12.sp, color = deepBrown.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = deepBrown)
                        }
                    }
                }

                item {
                    SectionHeader(title = "Recent Shipments", onSeeAll = { handleAction(onOrdersClick) })
                }

                if (isLoading) {
                    items(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shimmerEffect()
                        )
                    }
                } else if (recentShipments.isEmpty()) {
                    item {
                        Text(
                            text = "No recent shipments found",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(recentShipments, key = { it.id }) { shipment ->
                        ActiveShipmentCard(
                            shipment = shipment,
                            onTrackClick = onTrackClick,
                            onQrClick = { id -> onQrClick(id, shipment.status) },
                            onRepostClick = { item ->
                                viewModel.repostShipment(
                                    item,
                                    onSuccess = { Toast.makeText(context, "Load Re-posted!", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show() }
                                )
                            },
                            userRole = "Vendor"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        // Chat FAB
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 110.dp, end = 20.dp), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = onChatbotClick,
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        }
    }
}

@Composable
fun DashboardMetricCard(modifier: Modifier = Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = DarkBlue, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
        }
    }
}
