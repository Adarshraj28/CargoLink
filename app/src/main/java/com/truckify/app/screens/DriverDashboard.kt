package com.truckify.app.screens

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
    val loads by viewModel.availableLoads.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val rating by viewModel.rating.collectAsState()
    val truckNumber by viewModel.truckNumber.collectAsState()
    val todayEarnings by viewModel.todayEarnings.collectAsState()
    val weekEarnings by viewModel.weekEarnings.collectAsState()
    val verificationStatus by viewModel.verificationStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val incomingLoad by viewModel.incomingLoad.collectAsState()

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
                    BottomItem(icon = Icons.Default.Home, selected = true)
                    Box(modifier = Modifier.clickable { onOrdersClick() }) { 
                        BottomItem(icon = Icons.Default.DirectionsTransit, selected = false) 
                    }
                    
                    // New Center Quick Action Button for Drivers
                    Box(
                        modifier = Modifier
                            .offset(y = (-10).dp)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                            .clickable { onSearchClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Find Loads", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    Box(modifier = Modifier.clickable { onPaymentsClick() }) { 
                        BottomItem(icon = Icons.Default.AccountBalanceWallet, selected = false) 
                    }
                    Box(modifier = Modifier.clickable { onProfileClick() }) { 
                        BottomItem(icon = Icons.Default.Person, selected = false) 
                    }
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Good Morning,", color = TextGray, fontSize = 16.sp)
                        Text(text = userName, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isOnline) "Online" else "Offline", color = if (isOnline) SuccessGreen else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { viewModel.toggleOnlineStatus() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                Text(text = String.format(Locale.getDefault(), " %.1f", rating), color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = onNotificationClick) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Black)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Beige.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Truck $truckNumber", color = Color.Black, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                if (verificationStatus != "Verified") {
                    Card(
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().clickable { onVerifyClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (verificationStatus == "Pending") Color(0xFFFFF3E0) else Color(0xFFFBE9E7))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (verificationStatus == "Pending") Icons.Default.History else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (verificationStatus == "Pending") WarningOrange else ErrorRed
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (verificationStatus == "Pending") "Verification Pending" else "Verify Identity",
                                    fontWeight = FontWeight.Bold,
                                    color = if (verificationStatus == "Pending") WarningOrange else ErrorRed
                                )
                                Text(
                                    text = if (verificationStatus == "Pending") "Admin is reviewing your docs." else "Upload your license to start taking loads.",
                                    fontSize = 12.sp,
                                    color = Color.Black.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                if (activeTrip != null) {
                    ActiveTripCard(
                        shipmentId = activeTrip!!.id,
                        origin = activeTrip!!.pickupAddress.split(",").first(),
                        destination = activeTrip!!.destinationAddress.split(",").first(),
                        status = activeTrip!!.status,
                        onTrackClick = { onTrackClick(it) }
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("No active trips. Find a load below!", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "AI Load Suggestions", onSeeAll = onSearchClick)
                if (isLoading) {
                    LoadingAnimation()
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(loads) { shipment ->
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

            item {
                SectionHeader(title = "Quick Actions")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionBtn("Find Load", Icons.Default.Search, PrimaryBlue, onSearchClick)
                    QuickActionBtn("Expenses", Icons.Default.Receipt, Color(0xFF6366F1), onExpenseClick)
                    QuickActionBtn("SOS", Icons.Default.Warning, ErrorRed, {
                        Toast.makeText(context, "Emergency SOS triggered! Help is on the way.", Toast.LENGTH_LONG).show()
                    })
                    QuickActionBtn("Wallet", Icons.Default.AccountBalanceWallet, SuccessGreen, onPaymentsClick)
                }
            }

            item {
                SectionHeader(title = "Earnings Overview")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "Today", color = TextGray, fontSize = 12.sp)
                                Text(text = String.format(Locale.getDefault(), "₹%.0f", todayEarnings), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            MiniEarningsChart()
                        }
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(TextGray.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "This Week", color = TextGray, fontSize = 12.sp)
                            Text(text = String.format(Locale.getDefault(), "₹%.0f", weekEarnings), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
        
        // Chat FAB
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
    val maxOffset = 500f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
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
                            if (offsetX > 150f) {
                                onAccept()
                            } else if (offsetX < -150f) {
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.padding(15.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("New Load Available!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.Black)
                Text("Swipe to take action", color = Color.Gray, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("FROM", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(shipment.pickupAddress.split(",").first(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(shipment.destinationAddress.split(",").first(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PAYMENT", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = shipment.price, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("WEIGHT", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = shipment.weight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Text(" Decline", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Accept ", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
