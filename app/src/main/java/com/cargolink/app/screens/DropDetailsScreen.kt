package com.cargolink.app.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.cargolink.app.components.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

import com.cargolink.app.viewmodel.ShipmentViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDetailsScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(viewModel.dropLat, viewModel.dropLng), 12f)
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
                        IconButton(onClick = { /* Help */ }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = PrimaryBlue)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                StepProgressBar(currentStep = 2, totalSteps = 8)
            }
        },
        bottomBar = {
            DropSummaryCard(distance = viewModel.estimatedDistance, duration = viewModel.estimatedDuration, onContinue = {
                val error = viewModel.validateDrop()
                if (error == null) onContinue()
                else android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            })
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
                text = "Drop Details",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue
            )
            Text(
                text = "Where should the cargo be delivered?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // DESTINATION SEARCH
            OutlinedTextField(
                value = viewModel.dropAddress,
                onValueChange = { viewModel.dropAddress = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                placeholder = { Text("Search destination...") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = ErrorRed) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // MAP PREVIEW
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    onMapClick = { latLng ->
                        viewModel.dropLat = latLng.latitude
                        viewModel.dropLng = latLng.longitude
                        viewModel.updateEstimates()
                        coroutineScope.launch {
                            val address = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.cargolink.app.utils.getFriendlyAddress(context, latLng.latitude, latLng.longitude)
                            }
                            viewModel.dropAddress = address
                        }
                    }
                ) {
                    Marker(
                        state = MarkerState(position = LatLng(viewModel.dropLat, viewModel.dropLng)),
                        title = "Destination"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ESTIMATED INFO (Glassmorphism inspired row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumn(label = "Distance", value = viewModel.estimatedDistance, icon = Icons.Default.Route)
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                InfoColumn(label = "Duration", value = viewModel.estimatedDuration, icon = Icons.Default.Schedule)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // RECEIVER INFO
            Text("Receiver Details", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PremiumInput(label = "Receiver Name", value = viewModel.receiverName, onValueChange = { viewModel.receiverName = it }, icon = Icons.Default.PersonOutline)
                    PremiumInput(label = "Contact Phone", value = viewModel.receiverPhone, onValueChange = { viewModel.receiverPhone = it }, icon = Icons.Default.PhoneIphone)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // DELIVERY TIME WINDOW
            Text("Delivery Window", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            var showWindowDialog by remember { mutableStateOf(false) }
            val windows = listOf("Flexible (8 AM - 8 PM)", "Morning (8 AM - 12 PM)", "Afternoon (12 PM - 4 PM)", "Evening (4 PM - 8 PM)", "Night (8 PM - 12 AM)")
            
            AppleSelector(
                text = viewModel.deliveryWindow, 
                icon = Icons.Default.AccessTimeFilled,
                onClick = { showWindowDialog = true }
            )

            if (showWindowDialog) {
                AlertDialog(
                    onDismissRequest = { showWindowDialog = false },
                    title = { Text("Select Delivery Window", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            windows.forEach { window ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.deliveryWindow = window
                                            showWindowDialog = false 
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = viewModel.deliveryWindow == window,
                                        onClick = { 
                                            viewModel.deliveryWindow = window
                                            showWindowDialog = false 
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(window)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showWindowDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SPECIAL INSTRUCTIONS
            Text("Special Instructions", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = viewModel.specialInstructions,
                onValueChange = { viewModel.specialInstructions = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text("Gate codes, security procedures, etc.") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedBorderColor = PrimaryBlue
                )
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
        Text(value, fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
    }
}

@Composable
fun DropSummaryCard(distance: String, duration: String, onContinue: () -> Unit) {
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
                    Text("Total Estimation", color = Color.Gray, fontSize = 12.sp)
                    Text("$distance • $duration", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp)
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.height(56.dp).width(160.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}
