package com.truckify.app.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.*
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import com.truckify.app.ui.theme.*
import java.util.Locale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.truckify.app.viewmodel.DriverViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
    onExpenseClick: () -> Unit = {},
    viewModel: DriverViewModel = viewModel()
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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shadowElevation = 16.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) { BottomItem(icon = Icons.Default.Home, selected = true) }
                    IconButton(onClick = onOrdersClick) { BottomItem(icon = Icons.Default.DirectionsTransit, selected = false) }
                    
                    Box(
                        modifier = Modifier
                            .offset(y = (-10).dp)
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(BlueGradient)
                            .clickable { onSearchClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Find Loads", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(onClick = onPaymentsClick) { BottomItem(icon = Icons.Default.AccountBalanceWallet, selected = false) }
                    IconButton(onClick = onProfileClick) { BottomItem(icon = Icons.Default.Person, selected = false) }
                }
            }
        }
    ) { paddingValues ->
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
                        Text(text = "Good Morning,", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                        Text(text = userName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                onCheckedChange = { viewModel.toggleOnlineStatus() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                Text(text = String.format(Locale.getDefault(), " %.1f", rating), color = PrimaryBlue, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onNotificationClick) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = PrimaryBlue, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Active Vehicle", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(text = truckNumber, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            item {
                if (verificationStatus != "Verified") {
                    Card(
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().clickable { onVerifyClick() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (verificationStatus == "Pending") WarningOrange.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, if (verificationStatus == "Pending") WarningOrange.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (verificationStatus == "Pending") Icons.Default.History else Icons.Default.GppMaybe,
                                contentDescription = null,
                                tint = if (verificationStatus == "Pending") WarningOrange else ErrorRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Text(
                                    text = if (verificationStatus == "Pending") "Verification in Progress" else "Account Unverified",
                                    fontWeight = FontWeight.Bold,
                                    color = if (verificationStatus == "Pending") WarningOrange else ErrorRed,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (verificationStatus == "Pending") "Your documents are being reviewed." else "Upload your documents to start earning.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            if (isOnline && activeTrip != null) {
                item {
                    SectionHeader(title = "Current Trip")
                    ActiveTripCard(
                        shipmentId = activeTrip!!.id,
                        origin = activeTrip!!.pickupAddress,
                        destination = activeTrip!!.destinationAddress,
                        pickupLat = activeTrip!!.pickupLat,
                        pickupLng = activeTrip!!.pickupLng,
                        destLat = activeTrip!!.destLat,
                        destLng = activeTrip!!.destLng,
                        status = activeTrip!!.status,
                        onTrackClick = { onTrackClick(it) }
                    )
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
                                mapStyleOptions = MapStyleOptions("[{\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#242f3e\"}]},{\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"#746855\"}]},{\"elementType\":\"labels.text.stroke\",\"stylers\":[{\"color\":\"#242f3e\"}]},{\"featureType\":\"administrative.locality\",\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"#d59563\"}]},{\"featureType\":\"road\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#38414e\"}]},{\"featureType\":\"road\",\"elementType\":\"geometry.stroke\",\"stylers\":[{\"color\":\"#212a37\"}]},{\"featureType\":\"water\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#17263c\"}]}]")
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
                item {
                    if (isOnline) {
                        Card(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(140.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Searching for nearby loads...", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("We'll notify you as soon as a match is found", color = PrimaryBlue.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(160.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("You are currently Offline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("Go online to start receiving new load requests", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                item {
                    if (isOnline) {
                        SectionHeader(title = "Smart Suggestions", onSeeAll = onSearchClick)
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
                            }
                        } else {
                            if (loads.isEmpty()) {
                                Text("No loads matching your truck currently.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 30.dp), textAlign = TextAlign.Center)
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(loads, key = { it.id }) { shipment ->
                                        AILoadSuggestionCard(shipment = shipment, onClick = {
                                            viewModel.acceptLoad(shipment.id, 
                                                onSuccess = { Toast.makeText(context, "Load Accepted!", Toast.LENGTH_SHORT).show() },
                                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                            )
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Earnings Overview")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "TODAY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(text = String.format(Locale.getDefault(), "₹%.0f", todayEarnings), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            MiniEarningsChart()
                        }
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "THIS WEEK", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(text = String.format(Locale.getDefault(), "₹%.0f", weekEarnings), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = SuccessGreen)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
        
        // Chat FAB
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp, end = 20.dp), contentAlignment = Alignment.BottomEnd) {
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

    if (incomingLoad != null) {
        SwipeableLoadRequestPopup(
            shipment = incomingLoad!!,
            onAccept = {
                viewModel.acceptLoad(incomingLoad!!.id,
                    onSuccess = { Toast.makeText(context, "Load Accepted!", Toast.LENGTH_SHORT).show() },
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
fun SwipeableLoadRequestPopup(
    shipment: Shipment,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
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
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
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
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("New Load Found!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Swipe right to Accept • Left to Decline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PICKUP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        LocationText(
                            lat = shipment.pickupLat,
                            lng = shipment.pickupLng,
                            defaultAddress = shipment.pickupAddress,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.padding(horizontal = 12.dp).size(20.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("DROP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        LocationText(
                            lat = shipment.destLat,
                            lng = shipment.destLng,
                            defaultAddress = shipment.destinationAddress,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("OFFERED PRICE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(text = shipment.price, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("WEIGHT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(text = shipment.weight, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
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
                        Text("Decline", fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Accept Load", fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                LinearProgressIndicator(
                    progress = { (offsetX.coerceIn(-200f, 200f) + 200f) / 400f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = if (offsetX > 0) SuccessGreen else if (offsetX < 0) ErrorRed else PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            }
        }
    }
}
