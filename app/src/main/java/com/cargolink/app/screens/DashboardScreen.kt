package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cargolink.app.components.*
import com.cargolink.app.ui.theme.*
import com.cargolink.app.viewmodel.HomeViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCreateClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFleetClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onChatbotClick: () -> Unit,
    onTrackClick: (String) -> Unit,
    onQrClick: (String, String) -> Unit,
    isGuest: Boolean = false,
    onCompleteProfile: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: com.cargolink.app.viewmodel.AuthViewModel? = null
) {
    val activeShipments by viewModel.activeShipments.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()
    val monthlySpend by viewModel.monthlySpend.collectAsStateWithLifecycle()
    val onTimeRate by viewModel.onTimeRate.collectAsStateWithLifecycle()
    
    var showGuestDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val activeTrip = activeShipments.find { 
        it.status == "Accepted" || it.status == "Arrived" || it.status == "In Transit" 
    }

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
            title = { Text("Complete Your Profile") },
            text = { 
                Column {
                    Text("To access premium features like live tracking and instant booking, please finish setting up your business profile.")
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
                Button(onClick = { showGuestDialog = false; onCompleteProfile() }) {
                    Text("Complete Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        containerColor = Beige.copy(alpha = 0.05f),
        topBar = {
            NewPremiumTopBar(
                userName = userName,
                isGuest = isGuest,
                onNotificationClick = { handleAction(onNotificationClick) },
                onSearchClick = { handleAction(onSearchClick) },
                onProfileClick = onSettingsClick,
                onBypass = { authViewModel?.updateRole("Vendor") }
            )
        },
        bottomBar = {
            NewPremiumBottomNav(
                onHomeClick = {},
                onShipmentsClick = { handleAction(onOrdersClick) },
                onTrackingClick = { handleAction(onFleetClick) },
                onWalletClick = { handleAction(onPaymentsClick) },
                onProfileClick = onSettingsClick,
                onFabClick = { handleAction(onCreateClick) },
                onAIClick = { handleAction(onChatbotClick) }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadDashboardData()
                coroutineScope.launch {
                    kotlinx.coroutines.delay(1500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // HERO WELCOME CARD
                item {
                    WelcomeHeroCard(
                        onBookNow = { handleAction(onCreateClick) },
                        onViewActive = { handleAction(onOrdersClick) },
                        onSmartQuote = { handleAction(onChatbotClick) }
                    )
                }

                // ACTIVE TRIP HIGHLIGHT (Uber Style)
                if (activeTrip != null) {
                    item {
                        PremiumSectionHeader(title = "Live Trip", subtitle = "Your shipment is currently in progress")
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
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
                                onTrackClick = { handleAction { onTrackClick(activeTrip.id) } },
                                onQrClick = { id -> handleAction { onQrClick(id, activeTrip.status) } }
                            )
                        }
                    }
                }

                // QUICK ACTIONS GRID
                item {
                    PremiumSectionHeader(title = "Quick Actions", subtitle = "Fast access to essential tools")
                    QuickActionsRow(
                        onCreate = { handleAction(onCreateClick) },
                        onFleet = { handleAction(onFleetClick) },
                        onPayments = { handleAction(onPaymentsClick) },
                        onOrders = { handleAction(onOrdersClick) },
                        onAI = { handleAction(onChatbotClick) },
                        onHistory = { handleAction(onHistoryClick) },
                        onSettings = onSettingsClick
                    )
                }

                // LIVE MAP PREVIEW
                item {
                    PremiumSectionHeader(title = "Live Fleet Map", onSeeAll = { handleAction(onFleetClick) })
                    LiveMapPreviewCard(onExpand = { handleAction(onFleetClick) })
                }

                // WALLET OVERVIEW
                item {
                    PremiumSectionHeader(title = "Financials", onSeeAll = { handleAction(onPaymentsClick) })
                    WalletOverviewCard(balance = walletBalance, monthlySpend = monthlySpend)
                }

                // RECENT SHIPMENTS
                if (activeShipments.isNotEmpty()) {
                    item {
                        PremiumSectionHeader(title = "Recent Shipments", onSeeAll = { handleAction(onOrdersClick) })
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeShipments.filter { it.id != activeTrip?.id }) { shipment ->
                                ActiveShipmentPremiumCard(
                                    shipment = shipment,
                                    onTrackClick = { handleAction { onTrackClick(shipment.id) } }
                                )
                            }
                        }
                    }
                }

                // STATISTICS
                item {
                    PremiumSectionHeader(title = "Analytics Overview", subtitle = "Performance metrics for this month")
                    var statsVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { statsVisible = true }
                    AnimatedVisibility(visible = statsVisible, enter = fadeIn(tween(800)) + slideInHorizontally(tween(800))) {
                        AnalyticsGrid(activeCount = activeShipments.size, onTimeRate = onTimeRate, savings = viewModel.costSaved.collectAsStateWithLifecycle().value)
                    }
                }

                // RECENT DOCUMENTS
                item {
                    PremiumSectionHeader(title = "Recent Documents", onSeeAll = {})
                    DocumentCard(title = "Invoice_CL9842.pdf", type = "Invoicing", date = "Today")
                    DocumentCard(title = "POD_CL9840.png", type = "Proof of Delivery", date = "Yesterday")
                }

                // AI INSIGHTS
                item {
                    PremiumSectionHeader(title = "AI Insights", subtitle = "Intelligence powered optimizations")
                    AISuggestionsSection()
                }

                // PERFORMANCE GRAPH
                item {
                    PremiumSectionHeader(title = "Efficiency Index")
                    PerformanceGraphCard()
                }

                // RECENT ACTIVITY
                item {
                    PremiumSectionHeader(title = "Recent Activity", onSeeAll = { handleAction(onHistoryClick) })
                    RecentActivityTimeline()
                }

                // TRUSTED PARTNERS
                item {
                    TrustedPartnerTicker()
                }
            }
        }
    }
}

@Composable
fun NewPremiumTopBar(
    userName: String,
    isGuest: Boolean,
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onBypass: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Beige.copy(alpha = 0.1f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Beige.copy(alpha = 0.3f))
                        .border(1.5.dp, PrimaryBlue.copy(alpha = 0.1f), CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = DarkBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hello, $userName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkBlue
                        )
                        if (!isGuest) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text("Delhi • 24°C Sunny", fontSize = 11.sp, color = Color.Gray)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isGuest) {
                    IconButton(
                        onClick = onBypass,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Magenta.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Code, null, tint = Color.Magenta, modifier = Modifier.size(18.dp))
                    }
                }
                GlassIconButton(icon = Icons.Default.Search, onClick = onSearchClick)
                Box {
                    GlassIconButton(icon = Icons.Default.Notifications, onClick = onNotificationClick)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(8.dp)
                            .background(ErrorRed, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeHeroCard(onBookNow: () -> Unit, onViewActive: () -> Unit, onSmartQuote: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(260.dp)
            .shadow(32.dp, RoundedCornerShape(32.dp), spotColor = SoftPurple.copy(alpha = 0.4f))
            .background(
                brush = PremiumGradient,
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
    ) {
        // Abstract illustration background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = 450f, center = center.copy(x = size.width * 0.95f, y = size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.04f), radius = 300f, center = center.copy(x = 0f, y = size.height))
        }

        Column(
            modifier = Modifier.padding(28.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Ready to move your\nnext shipment?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                        onClick = onSmartQuote
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Smart Quote", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onBookNow,
                    modifier = Modifier.weight(1.5f).height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryBlue)
                ) {
                    Text("Book Shipment", fontWeight = FontWeight.ExtraBold)
                }
                Surface(
                    onClick = onViewActive,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Active Loads", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onCreate: () -> Unit,
    onFleet: () -> Unit,
    onPayments: () -> Unit,
    onOrders: () -> Unit,
    onAI: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionBtn("Post Load", Icons.Default.AddBox, PrimaryBlue, onCreate)
            QuickActionBtn("Live Track", Icons.Default.LocationOn, SuccessGreen, onOrders)
            QuickActionBtn("My Fleet", Icons.Default.Groups, InfoBlue, onFleet)
            QuickActionBtn("AI Helper", Icons.Default.AutoAwesome, SoftPurple, onAI)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionBtn("Wallet", Icons.Default.AccountBalanceWallet, Color(0xFF673AB7), onPayments)
            QuickActionBtn("History", Icons.Default.History, Color.Gray, onHistory)
            QuickActionBtn("Support", Icons.Default.HelpOutline, WarningOrange, {})
            QuickActionBtn("Settings", Icons.Default.Settings, Color(0xFF607D8B), onSettings)
        }
    }
}

@Composable
fun AnalyticsGrid(activeCount: Int, onTimeRate: Int, savings: Double) {
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard("ACTIVE", activeCount.toString(), null, Icons.Default.LocalShipping, PrimaryBlue, Modifier.weight(1f))
            MetricCard("ON-TIME", "$onTimeRate%", "+2%", Icons.Default.Speed, SuccessGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard("SAVINGS", "₹${String.format("%.1fK", savings/1000)}", "AI Opt", Icons.Default.Savings, InfoBlue, Modifier.weight(1f))
            MetricCard("REVENUE", "₹12.4L", "+12%", Icons.AutoMirrored.Filled.TrendingUp, WarningOrange, Modifier.weight(1f))
        }
    }
}

@Composable
fun LiveMapPreviewCard(onExpand: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 24.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        onClick = onExpand
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(28.6139, 77.2090), 10f) },
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, scrollGesturesEnabled = false)
            )
            
            // Map Overlay Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))))
            )
            
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = PrimaryBlue, shape = CircleShape) {
                    Box(modifier = Modifier.size(8.dp).padding(2.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("4 Trucks Live Nearby", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            IconButton(
                onClick = onExpand,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(Icons.Default.Fullscreen, null, tint = DarkBlue)
            }
        }
    }
}

@Composable
fun PerformanceGraphCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 24.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Efficiency Index", fontWeight = FontWeight.Bold, color = DarkBlue)
                Text("Month View", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Placeholder for Chart
                MarketTrendChart()
            }
        }
    }
}

@Composable
fun RecentActivityTimeline() {
    val activities = listOf(
        Triple("Shipment Delivered", "Mumbai Hub • #CL9842", "2h ago"),
        Triple("Driver Assigned", "Ramesh K • #CL9850", "5h ago"),
        Triple("Payment Success", "₹4,850 • Wallet", "Yesterday")
    )
    
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        activities.forEachIndexed { index, (title, sub, time) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(12.dp).background(if (index == 0) SuccessGreen else Color.LightGray, CircleShape))
                    if (index < activities.size - 1) {
                        Box(modifier = Modifier.width(2.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkBlue)
                    Text(sub, fontSize = 12.sp, color = Color.Gray)
                }
                Text(time, fontSize = 11.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun NewPremiumBottomNav(
    onHomeClick: () -> Unit,
    onShipmentsClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onWalletClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFabClick: () -> Unit,
    onAIClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        // AI Button above Setting (Profile)
        Surface(
            onClick = onAIClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 28.dp)
                .size(54.dp)
                .shadow(16.dp, CircleShape, spotColor = SoftPurple.copy(alpha = 0.4f)),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.5.dp, SoftPurple.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    "AI", 
                    tint = SoftPurple, 
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(72.dp)
                .shadow(24.dp, RoundedCornerShape(40.dp), spotColor = PrimaryBlue.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(40.dp),
            color = Color.White.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(icon = Icons.Default.Dashboard, isSelected = true, onClick = onHomeClick)
                NavItem(icon = Icons.AutoMirrored.Filled.Assignment, isSelected = false, onClick = onShipmentsClick)
                Spacer(modifier = Modifier.width(56.dp)) // For FAB
                NavItem(icon = Icons.Default.AccountBalanceWallet, isSelected = false, onClick = onWalletClick)
                NavItem(icon = Icons.Default.Person, isSelected = false, onClick = onProfileClick)
            }
        }
        
        FloatingActionButton(
            onClick = onFabClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .size(64.dp)
                .shadow(16.dp, CircleShape, spotColor = PrimaryBlue.copy(alpha = 0.4f)),
            shape = CircleShape,
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(PremiumGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp))
            }
        }
    }
}
