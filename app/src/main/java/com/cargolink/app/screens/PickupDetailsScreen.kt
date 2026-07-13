package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.cargolink.app.components.*
import com.cargolink.app.viewmodel.ShipmentViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupDetailsScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(viewModel.pickupLat, viewModel.pickupLng), 15f)
    }

    Scaffold(
        containerColor = Beige.copy(alpha = 0.05f),
        topBar = {
            Column(modifier = Modifier.background(Beige.copy(alpha = 0.1f))) {
                CenterAlignedTopAppBar(
                    title = { Text("New Shipment", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DarkBlue)
                        }
                    },
                    actions = {
                        TextButton(onClick = { /* Help */ }) {
                            Text("Help", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                StepProgressBar(currentStep = 1, totalSteps = 8)
            }
        },
        bottomBar = {
            PickupSummaryCard(
                viewModel = viewModel,
                onContinue = {
                    val error = viewModel.validatePickup()
                    if (error == null) onContinue()
                    else android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = "Pickup Details",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue
            )
            Text(
                text = "Where should we collect the cargo from?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // LOCATION SEARCH (Glassmorphism inspired)
            OutlinedTextField(
                value = viewModel.pickupAddress,
                onValueChange = { viewModel.pickupAddress = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                placeholder = { Text("Search pickup location...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryBlue) },
                trailingIcon = { 
                    IconButton(onClick = {}) { 
                        Icon(Icons.Default.MyLocation, null, tint = PrimaryBlue) 
                    } 
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // MAP PREVIEW CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                    onMapClick = { latLng ->
                        viewModel.pickupLat = latLng.latitude
                        viewModel.pickupLng = latLng.longitude
                        viewModel.updateEstimates()
                        coroutineScope.launch {
                            val address = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.cargolink.app.utils.getFriendlyAddress(context, latLng.latitude, latLng.longitude)
                            }
                            viewModel.pickupAddress = address
                        }
                    }
                ) {
                    Marker(state = MarkerState(position = LatLng(viewModel.pickupLat, viewModel.pickupLng)))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // WAREHOUSE SELECTION
            Text("Select Warehouse", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val warehouses = listOf("Main Warehouse", "North Hub", "Dock Side", "Factory A")
                items(warehouses) { warehouse ->
                    WarehouseChip(
                        name = warehouse,
                        isSelected = viewModel.selectedWarehouse == warehouse,
                        onClick = { viewModel.selectedWarehouse = warehouse }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CONTACT DETAILS CARD
            Text("Contact Information", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PremiumInput(label = "Contact Person", value = viewModel.pickupContactName, onValueChange = { viewModel.pickupContactName = it }, icon = Icons.Default.Person)
                    PremiumInput(label = "Phone Number", value = viewModel.pickupContactPhone, onValueChange = { viewModel.pickupContactPhone = it }, icon = Icons.Default.Phone)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // DATE & TIME (Apple Style)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pickup Date", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppleSelector(
                        text = viewModel.pickupDate, 
                        icon = Icons.Default.CalendarToday,
                        onClick = {
                            val calendar = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                                    viewModel.pickupDate = "${months[month]} $dayOfMonth, $year"
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pickup Time", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppleSelector(
                        text = viewModel.pickupTime, 
                        icon = Icons.Default.AccessTime,
                        onClick = {
                            val calendar = java.util.Calendar.getInstance()
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val amPm = if (hourOfDay < 12) "AM" else "PM"
                                    val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                    viewModel.pickupTime = String.format(java.util.Locale.getDefault(), "%d:%02d %s", hour, minute, amPm)
                                },
                                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                                calendar.get(java.util.Calendar.MINUTE),
                                false
                            ).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun WarehouseChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PrimaryBlue else Color.White,
        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.3f)),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Storefront, 
                null, 
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, color = if (isSelected) Color.White else DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun PickupSummaryCard(onContinue: () -> Unit, viewModel: ShipmentViewModel = hiltViewModel()) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(32.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Pickup Scheduled", color = Color.Gray, fontSize = 12.sp)
                    Text("${viewModel.pickupDate} • ${viewModel.pickupTime}", fontWeight = FontWeight.Bold, color = DarkBlue)
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.height(56.dp).width(160.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}
