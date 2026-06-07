package com.truckify.app.screens

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.*
import com.truckify.app.ui.theme.*
import com.truckify.app.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

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
    val activeShipments by viewModel.activeShipments.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val verificationStatus by viewModel.verificationStatus.collectAsState()
    val monthlySpend by viewModel.monthlySpend.collectAsState()
    val onTimeRate by viewModel.onTimeRate.collectAsState()
    val costSaved by viewModel.costSaved.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    androidx.activity.compose.BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundWhite,
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.clickable { /* Home */ }) { BottomItem(icon = Icons.Default.Home, selected = true) }
                    Box(modifier = Modifier.clickable { onOrdersClick() }) { BottomItem(icon = Icons.AutoMirrored.Filled.List, selected = false) }
                    
                    // New Center Create Button
                    Box(
                        modifier = Modifier
                            .offset(y = (-10).dp)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                            .clickable { onCreateClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Shipment", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    Box(modifier = Modifier.clickable { onHistoryClick() }) { BottomItem(icon = Icons.Default.History, selected = false) }
                    Box(modifier = Modifier.clickable { onSettingsClick() }) { BottomItem(icon = Icons.Default.Settings, selected = false) }
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = userName, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Black)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Verified Business", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        }
                    }
                    IconButton(onClick = onNotificationClick) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Black)
                    }
                }
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item { KPICard("Active Shipments", activeShipments.size.toString(), Icons.Default.LocalShipping, PrimaryBlue) }
                    item { KPICard("Monthly Spend", String.format(Locale.getDefault(), "₹%.1fL", monthlySpend / 100000.0), Icons.Default.Payments, SuccessGreen) }
                    item { KPICard("On-Time Del.", "$onTimeRate%", Icons.Default.Timer, WarningOrange) }
                    item { KPICard("Cost Saved", String.format(Locale.getDefault(), "₹%.1fL", costSaved / 100000.0), Icons.Default.Savings, Color(0xFF6366F1)) }
                }
            }

            item {
                SectionHeader(title = "Shipment Overview")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val inTransit = activeShipments.count { it.status == "In Transit" }
                    val available = activeShipments.count { it.status == "Available" }
                    
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShipmentDonutChart() // Still shows static visual but stats are dynamic
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendItem("In Transit", inTransit.toString(), PrimaryBlue)
                            LegendItem("Available", available.toString(), SuccessGreen)
                            LegendItem("Delayed", "0", WarningOrange)
                            LegendItem("Total", activeShipments.size.toString(), Color.Black)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Quick Actions")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionBtn("Post Load", Icons.Default.AddBox, PrimaryBlue, onCreateClick)
                    QuickActionBtn("Search", Icons.Default.Search, Color(0xFF6366F1), onSearchClick)
                    QuickActionBtn("Fleet", Icons.Default.LocalShipping, SuccessGreen, onFleetClick)
                    QuickActionBtn("Wallet", Icons.Default.AccountBalanceWallet, WarningOrange, onPaymentsClick)
                }
            }

            item {
                SectionHeader(title = "Live Shipment Map")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { onFleetClick() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
                ) {
                    val inTransit = activeShipments.count { it.status == "In Transit" }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                            Text("View $inTransit trucks in transit", color = TextGray, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Recommendation", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Use shared cargo pooling to reduce cost by 18%",
                                color = Color.Black.copy(alpha = 0.7f), fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Recent Shipments", onSeeAll = onOrdersClick)
            }

            if (isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { LoadingAnimation() } }
            } else {
                items(activeShipments) { shipment ->
                    ActiveShipmentCard(
                        shipment = shipment,
                        onTrackClick = onTrackClick,
                        onQrClick = { id -> onQrClick(id, shipment.status) },
                        userRole = "Vendor"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp, end = 20.dp), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = onChatbotClick,
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        }
    }
}
