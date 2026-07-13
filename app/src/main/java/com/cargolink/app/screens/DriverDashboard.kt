package com.cargolink.app.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*
import java.util.Locale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.cargolink.app.viewmodel.DriverViewModel
import com.cargolink.app.viewmodel.ReturnLoadViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun DriverDashboard(
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onChatbotClick: () -> Unit,
    onTrackClick: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onQrClick: (String, String) -> Unit,
    onLoadClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onReferClick: () -> Unit = {},
    onExpenseClick: () -> Unit = {},
    viewModel: DriverViewModel = hiltViewModel(),
    returnViewModel: ReturnLoadViewModel = hiltViewModel(),
    isGuest: Boolean = false,
    onCompleteProfile: () -> Unit = {},
    authViewModel: com.cargolink.app.viewmodel.AuthViewModel? = null,
) {
    val context = LocalContext.current
    val loads by viewModel.availableLoads.collectAsStateWithLifecycle()
    val activeTrip by viewModel.activeTrip.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val rating by viewModel.rating.collectAsStateWithLifecycle()
    val truckNumber by viewModel.truckNumber.collectAsStateWithLifecycle()
    val todayEarnings by viewModel.todayEarnings.collectAsStateWithLifecycle()
    val weekEarnings by viewModel.weekEarnings.collectAsStateWithLifecycle()
    val verificationStatus by viewModel.verificationStatus.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val incomingLoad by viewModel.incomingLoad.collectAsStateWithLifecycle()
    
    var showGuestDialog by remember { mutableStateOf(value = false) }

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
                    Text("This operation cannot be performed. Please complete your profile with all necessary details to start earning.")
                    if (authViewModel != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("--- Developer Mode ---", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Button(
                            onClick = { 
                                authViewModel.updateRole("Driver")
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

    // Return Load States
    val returnLoads by returnViewModel.returnLoads.collectAsStateWithLifecycle()
    val emptyKmSaved by viewModel.emptyKmSaved.collectAsStateWithLifecycle()
    val returnEarnings by viewModel.additionalEarnings.collectAsStateWithLifecycle()
    val co2Reduced by viewModel.co2Reduced.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!isGuest) {
            viewModel.loadDashboardData()
            returnViewModel.fetchReturnLoads()
        }
    }

    androidx.activity.compose.BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        modifier = Modifier.fillMaxSize().designerBackground(),
        containerColor = Beige.copy(alpha = 0.03f),
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
                            spotColor = SoftPurple.copy(alpha = 0.3f),
                            ambientColor = PrimaryBlue.copy(alpha = 0.2f)
                        )
                        .background(
                            color = Color.White.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(40.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.8f), Beige.copy(alpha = 0.4f))
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
                        IconButton(onClick = {
                            handleAction {
                                activeTrip?.id?.let { onTrackClick(it) } ?: onOrdersClick()
                            }
                        }) { BottomItem(icon = Icons.Default.DirectionsTransit, selected = false) }
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PremiumGradient)
                                .shadow(12.dp, CircleShape, spotColor = SoftPurple.copy(alpha = 0.5f))
                                .clickable { handleAction(onSearchClick) },
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "scale"
                            )
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = "Find Loads", 
                                tint = Color.White, 
                                modifier = Modifier.size(28.dp).scale(scale)
                            )
                        }

                        IconButton(onClick = { handleAction(onPaymentsClick) }) { BottomItem(icon = Icons.Default.AccountBalanceWallet, selected = false) }
                        IconButton(onClick = onProfileClick) { BottomItem(icon = Icons.Default.Person, selected = false) }
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000)) + expandVertically(tween(1000))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp)
            ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = if (isGuest) "Welcome to CargoLink" else "Good Morning,", color = TextSecondary, fontSize = 16.sp)
                        Text(text = if (isGuest) "New Driver" else userName, color = DarkBlue, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGuest && authViewModel != null) {
                            IconButton(
                                onClick = { authViewModel.updateRole("Driver") },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                                    .background(Color.Magenta.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = "Dev Bypass",
                                    tint = Color.Magenta,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isOnline) "ONLINE" else "OFFLINE", 
                                color = if (isOnline) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { 
                                    handleAction {
                                        viewModel.toggleOnlineStatus(
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        ) 
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        IconButton(onClick = { handleAction(onNotificationClick) }) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
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
                            Text("Upload your documents and vehicle details to start accepting loads and earning money.", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onCompleteProfile, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                                Text("Get Started")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DriverStatusCard(
                        modifier = Modifier.weight(1f),
                        label = "Active Vehicle",
                        value = if (isGuest) "Not Set" else truckNumber,
                        icon = Icons.Default.LocalShipping,
                        color = PrimaryBlue
                    )
                    DriverStatusCard(
                        modifier = Modifier.weight(0.7f),
                        label = "Rating",
                        value = String.format(Locale.getDefault(), "%.1f", rating),
                        icon = Icons.Default.Star,
                        color = WarningOrange
                    )
                }
            }

            item {
                if (verificationStatus != "Verified") {
                    Card(
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (verificationStatus == "Pending") WarningOrange.copy(alpha = 0.08f) else ErrorRed.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, if (verificationStatus == "Pending") WarningOrange.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (verificationStatus == "Pending") Icons.Default.History else Icons.Default.GppMaybe,
                                contentDescription = null,
                                tint = if (verificationStatus == "Pending") WarningOrange else ErrorRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (verificationStatus == "Pending") "Verification Pending" else "Verify Identity",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (verificationStatus == "Pending") WarningOrange else ErrorRed,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (verificationStatus == "Pending") "Review in progress." else "Upload Docs to start.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Button(
                                onClick = { onVerifyClick() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (verificationStatus == "Pending") WarningOrange else ErrorRed),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text(if (verificationStatus == "Pending") "Status" else "Verify", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Quick Actions")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionBtn("Find Loads", Icons.Default.LocalShipping, PrimaryBlue) { handleAction(onSearchClick) }
                    QuickActionBtn("Earnings", Icons.Default.AccountBalanceWallet, SuccessGreen) { handleAction(onPaymentsClick) }
                    QuickActionBtn("My Orders", Icons.Default.History, InfoBlue) { handleAction(onOrdersClick) }
                    QuickActionBtn("Profile", Icons.Default.Person, WarningOrange) { handleAction(onProfileClick) }
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
                                shipmentId = activeTrip!!.id,
                                origin = activeTrip!!.pickupAddress,
                                destination = activeTrip!!.destinationAddress,
                                pickupLat = activeTrip!!.pickupLat,
                                pickupLng = activeTrip!!.pickupLng,
                                destLat = activeTrip!!.destLat,
                                destLng = activeTrip!!.destLng,
                                status = activeTrip!!.status,
                                vendorEmail = activeTrip!!.vendorEmail,
                                onTrackClick = { onTrackClick(it) },
                                onQrClick = { onQrClick(it, activeTrip!!.vendorEmail) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader(title = "Live Route")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        val cameraPositionState = rememberCameraPositionState()
                        val trip = activeTrip!!
                        val location = if (trip.currentLat != 0.0) LatLng(trip.currentLat, trip.currentLng) else LatLng(trip.pickupLat, trip.pickupLng)
                        
                        LaunchedEffect(location) {
                            cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(location, 14f)
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                isMyLocationEnabled = true,
                                mapStyleOptions = MapStyleOptions(GOOGLE_MAPS_DARK_STYLE)
                            ),
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
                        ) {
                            Marker(
                                state = MarkerState(position = location),
                                title = "Your Location",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                            )
                            Marker(
                                state = MarkerState(position = LatLng(trip.destLat, trip.destLng)),
                                title = "Destination"
                            )
                        }
                    }
                }
            } else {
                // No Active Trip
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn()) {
                        if (isOnline) {
                            if (loads.isEmpty()) {
                                // Large Searching Card
                                Card(
                                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(140.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.1f))
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val infiniteTransition = rememberInfiniteTransition(label = "pulseSearch")
                                            val searchScale by infiniteTransition.animateFloat(
                                                initialValue = 1f,
                                                targetValue = 1.2f,
                                                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                                label = "scale"
                                            )
                                            Icon(Icons.Default.Wifi, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp).scale(searchScale))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Searching for nearby loads...", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Matches will appear here instantly", color = PrimaryBlue.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                // Small Searching Bar with pulsing animation
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "alpha"
                                )

                                Surface(
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = PrimaryBlue.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).alpha(alpha),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Finding more matches...", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.weight(1f))
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                                    }
                                }
                            }
                        } else {
                            // Offline Card
                            Card(
                                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(160.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Currently Offline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Go online to start receiving matches", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { handleAction { viewModel.toggleOnlineStatus() } },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Go Online Now")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    if (isOnline) {
                        SectionHeader(title = "Smart Suggestions", onSeeAll = onSearchClick)
                        if (isLoading) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(2) {
                                    Box(
                                        modifier = Modifier
                                            .width(280.dp)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .shimmerEffect()
                                    )
                                }
                            }
                        } else {
                            if (loads.isEmpty()) {
                                Text("No matches found nearby.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 30.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(loads, key = { it.id }) { shipment ->
                                        AILoadSuggestionCard(
                                            shipment = shipment,
                    onAccept = {
                        val shipmentId = shipment.id
                        viewModel.acceptLoad(
                            shipmentId = shipmentId,
                            onSuccess = { 
                                Toast.makeText(context, "Load Accepted!", Toast.LENGTH_SHORT).show()
                                onTrackClick(shipmentId)
                            },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        )
                    },
                                            onDecline = {
                                                Toast.makeText(context, "Load ignored", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Sustainability & Impact")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ImpactItem(Icons.Default.Route, String.format(Locale.getDefault(), "%.0f km", emptyKmSaved), "Empty Saved")
                        VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ImpactItem(Icons.Default.Co2, String.format(Locale.getDefault(), "%.1f kg", co2Reduced), "CO2 Saved")
                        VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ImpactItem(Icons.Default.AddBusiness, String.format(Locale.getDefault(), "₹%.0f", returnEarnings), "Extra Earned")
                    }
                }
            }

            if (returnLoads.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "Return Matches")
                    Text(
                        "Based on your home city preferences", 
                        fontSize = 12.sp, 
                        color = SuccessGreen, 
                        modifier = Modifier.padding(bottom = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                items(returnLoads) { load ->
                    ReturnLoadCard(
                        shipment = load,
                        onAccept = {
                            returnViewModel.acceptReturnLoad(load) {
                                viewModel.loadDashboardData()
                                Toast.makeText(context, "Return Load Accepted!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                SectionHeader(title = "Earnings Overview")
                var isUnlocked by remember { mutableStateOf(false) }
                val context = LocalContext.current

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isUnlocked) {
                                authenticateUser(
                                    context = context,
                                    onSuccess = { isUnlocked = true },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            }
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "TODAY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    text = if (isUnlocked) String.format(Locale.getDefault(), "₹%.0f", todayEarnings) else "₹ • • • •", 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 28.sp, 
                                    color = DarkBlue
                                )
                            }
                            if (isUnlocked) {
                                MiniEarningsChart()
                            } else {
                                Icon(Icons.Default.Lock, null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        val weeklyGoal = 10000.0
                        val progress = (weekEarnings / weeklyGoal).coerceIn(0.0, 1.0).toFloat()
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Weekly Goal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (isUnlocked) "₹${weekEarnings.toInt()} / ₹${weeklyGoal.toInt()}" else "Locked", 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = PrimaryBlue
                                )
                            }
                            Text(if (isUnlocked) "${(progress * 100).toInt()}%" else "--%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (isUnlocked) progress else 0f },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = PrimaryBlue,
                            trackColor = PrimaryBlue.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            item {
                val beigeBackground = Color(0xFFFFF8E1)
                val deepBrown = Color(0xFF5D4037)
                
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onReferClick()
                    },
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
                            Text("Invite your driver friends", fontSize = 12.sp, color = deepBrown.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = deepBrown)
                    }
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
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        }
    }
}

    if (incomingLoad != null) {
        SwipeableLoadRequestPopup(
            shipment = incomingLoad!!,
            onAccept = {
                val shipmentId = incomingLoad!!.id
                viewModel.acceptLoad(shipmentId,
                    onSuccess = { 
                        Toast.makeText(context, "Load Accepted!", Toast.LENGTH_SHORT).show()
                        onTrackClick(shipmentId)
                    },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            },
            onDecline = {
                viewModel.declineIncomingLoad()
            }
        )
    }
}

@Composable
fun DriverStatusCard(modifier: Modifier = Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(24.dp), spotColor = color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp).background(Brush.linearGradient(listOf(Color.White, Beige.copy(alpha = 0.05f))))) {
            Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(text = value, color = DarkBlue, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1)
        }
    }
}

@Composable
fun ImpactItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(44.dp).background(TealGreen.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = TealGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DarkBlue)
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ReturnLoadCard(shipment: Shipment, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = SuccessGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, null, modifier = Modifier.size(12.dp), tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RETURN MATCH", 
                            color = SuccessGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(text = shipment.price, fontWeight = FontWeight.ExtraBold, color = SuccessGreen, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                LocationText(lat = shipment.pickupLat, lng = shipment.pickupLng, defaultAddress = shipment.pickupAddress, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Box(modifier = Modifier.padding(start = 3.5.dp).width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).border(2.dp, SuccessGreen, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                LocationText(lat = shipment.destLat, lng = shipment.destLng, defaultAddress = shipment.destinationAddress, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(Icons.Default.Handshake, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Accept Return Load", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SwipeableLoadRequestPopup(
    shipment: Shipment,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX > 200f) {
                                onAccept()
                            } else if (offsetX < -200f) {
                                onDecline()
                            }
                            offsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                        }
                    )
                },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.padding(18.dp))
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                Text("New Load Found!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = DarkBlue)
                Text("Swipe right to Accept • Left to Decline", color = Color.Gray, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PICKUP", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        LocationText(
                            lat = shipment.pickupLat,
                            lng = shipment.pickupLng,
                            defaultAddress = shipment.pickupAddress,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkBlue
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.padding(horizontal = 12.dp).size(20.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("DROP", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        LocationText(
                            lat = shipment.destLat,
                            lng = shipment.destLng,
                            defaultAddress = shipment.destinationAddress,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkBlue
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("OFFERED PRICE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(text = shipment.price, fontSize = 28.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("WEIGHT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(text = shipment.weight, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkBlue)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f), contentColor = ErrorRed)
                    ) {
                        Text("Decline", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Accept Load", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                LinearProgressIndicator(
                    progress = { (offsetX.coerceIn(-200f, 200f) + 200f) / 400f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (offsetX > 0) SuccessGreen else if (offsetX < 0) ErrorRed else PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            }
        }
    }
}
