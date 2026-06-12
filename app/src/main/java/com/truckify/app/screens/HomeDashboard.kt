package com.truckify.app.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.*
import com.truckify.app.ui.theme.*
import com.truckify.app.viewmodel.HomeViewModel
import com.truckify.app.firebase.AuthManager
import androidx.lifecycle.viewmodel.compose.viewModel
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
    viewModel: HomeViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeShipments by viewModel.activeShipments.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val verificationStatus by viewModel.verificationStatus.collectAsStateWithLifecycle()
    val monthlySpend by viewModel.monthlySpend.collectAsStateWithLifecycle()
    val onTimeRate by viewModel.onTimeRate.collectAsStateWithLifecycle()
    val costSaved by viewModel.costSaved.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val activeTrip = activeShipments.find { 
        it.status == "Accepted" || it.status == "Arrived" || it.status == "In Transit" 
    }
    
    // CLEANUP: Filter out the active trip from recent shipments to avoid double-showing
    val recentShipments = activeShipments.filter { it.status == "Available" }.take(3)

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    androidx.activity.compose.BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
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
                    IconButton(onClick = { }) { Icon(Icons.Default.Home, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp)) }
                    IconButton(onClick = onOrdersClick) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(28.dp)) }
                    
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BlueGradient)
                            .clickable { onCreateClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White, modifier = Modifier.size(30.dp))
                    }

                    IconButton(onClick = onHistoryClick) { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(28.dp)) }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(28.dp)) }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, $userName", 
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 28.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Verified Business", 
                                style = MaterialTheme.typography.labelMedium,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        }
                    }
                    Surface(
                        onClick = onNotificationClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            Icons.Default.Notifications, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                }
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item { KPICard("Active Ships", activeShipments.size.toString(), Icons.Default.LocalShipping, PrimaryBlue) }
                    item { KPICard("Monthly Spend", String.format(Locale.getDefault(), "₹%.1fL", monthlySpend / 100000.0), Icons.Default.Payments, SuccessGreen) }
                    item { KPICard("On-Time Rate", "$onTimeRate%", Icons.Default.Timer, WarningOrange) }
                    item { KPICard("Savings", String.format(Locale.getDefault(), "₹%.1fL", costSaved / 100000.0), Icons.Default.Savings, InfoBlue) }
                }
            }

            item {
                SectionHeader(title = "Quick Actions")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionBtn("Post Load", Icons.Default.AddBox, PrimaryBlue, onCreateClick)
                    QuickActionBtn("My Drivers", Icons.Default.Groups, InfoBlue, onDriversClick)
                    QuickActionBtn("Heatmap", Icons.Default.Map, SuccessGreen, onFleetClick)
                    QuickActionBtn("Payments", Icons.Default.AccountBalanceWallet, WarningOrange, onPaymentsClick)
                }
            }

            item {
                if (activeTrip != null) {
                    Spacer(modifier = Modifier.height(12.dp))
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
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No active shipments found", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Recent Shipments", onSeeAll = onOrdersClick)
            }

            if (isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp) } }
            } else if (recentShipments.isEmpty()) {
                item {
                    Text(
                        text = "No pending shipments found",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // Chat FAB
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 110.dp, end = 20.dp), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = onChatbotClick,
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        }
    }
}
