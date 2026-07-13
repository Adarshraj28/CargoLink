package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.cargolink.app.components.*
import com.cargolink.app.viewmodel.ShipmentViewModel
import androidx.hilt.navigation.compose.hiltViewModel

data class TruckType(
    val id: String,
    val name: String,
    val capacity: String,
    val dimensions: String,
    val basePrice: String,
    val icon: ImageVector,
    val isRecommended: Boolean = false,
    val availability: String = "Available"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckSelectionScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    val trucks = listOf(
        TruckType("1", "Mini Truck", "1.5 Tons", "7 x 4 x 5 ft", "₹1,200", Icons.Default.LocalShipping),
        TruckType("2", "Pickup", "3.0 Tons", "8 x 5 x 6 ft", "₹2,500", Icons.Default.LocalShipping, isRecommended = true),
        TruckType("3", "LCV", "5.0 Tons", "12 x 6 x 7 ft", "₹4,800", Icons.Default.LocalShipping),
        TruckType("4", "14 Feet", "7.0 Tons", "14 x 7 x 7 ft", "₹8,500", Icons.Default.LocalShipping),
        TruckType("5", "17 Feet", "10.0 Tons", "17 x 7 x 8 ft", "₹12,000", Icons.Default.LocalShipping),
        TruckType("6", "22 Feet", "15.0 Tons", "22 x 8 x 8 ft", "₹18,000", Icons.Default.LocalShipping),
        TruckType("7", "Trailer", "25+ Tons", "40 x 8 x 8 ft", "₹35,000", Icons.Default.LocalShipping)
    )

    val selectedTruck = trucks.find { it.name == viewModel.truckType } ?: trucks[1]

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
                            Icon(Icons.Default.HelpOutline, null, tint = PrimaryBlue)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                StepProgressBar(currentStep = 4, totalSteps = 8)
            }
        },
        bottomBar = {
            TruckSummaryCard(truck = selectedTruck, onContinue = onContinue)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
        ) {
            Text(
                text = "Select Vehicle",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "Choose the best truck for your cargo weight.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp)
            )

            // HORIZONTAL TRUCK CARDS
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(trucks) { truck ->
                    TruckCard(
                        truck = truck,
                        isSelected = viewModel.truckType == truck.name,
                        onClick = { viewModel.truckType = truck.name }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // VEHICLE SPECS COMPARISON PREVIEW
            Text(
                text = "Vehicle Capabilities",
                fontWeight = FontWeight.Bold,
                color = DarkBlue,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = PrimaryBlue.copy(alpha = 0.1f),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, null, tint = PrimaryBlue, modifier = Modifier.padding(10.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(selectedTruck.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DarkBlue)
                        }
                        
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                        ) {
                            Text("Compare", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CapabilityItem(label = "Payload", value = selectedTruck.capacity, icon = Icons.Default.Scale)
                        CapabilityItem(label = "Interior", value = selectedTruck.dimensions, icon = Icons.Default.OpenInFull)
                        CapabilityItem(label = "Status", value = selectedTruck.availability, icon = Icons.Default.EventAvailable)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // AI RECOMMENDATION BOX (Beige)
                    if (selectedTruck.isRecommended) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Beige.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .border(1.dp, Beige.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "AI recommends this truck based on your ${viewModel.weightInput} ton load and ${viewModel.estimatedDistance} route optimization.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF5D4037),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun TruckCard(
    truck: TruckType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")
    val elevation by animateDpAsState(if (isSelected) 16.dp else 2.dp, label = "elevation")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(160.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .scale(scale),
            shape = RoundedCornerShape(32.dp),
            color = if (isSelected) PrimaryBlue else Color.White,
            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.3f)),
            shadowElevation = elevation
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (truck.isRecommended) {
                    Surface(
                        color = WarningOrange,
                        shape = RoundedCornerShape(bottomEnd = 16.dp, topStart = 32.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            "BEST VALUE", 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.2f) else Background,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            truck.icon, 
                            null, 
                            tint = if (isSelected) Color.White else PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        truck.name, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isSelected) Color.White else DarkBlue,
                        fontSize = 16.sp
                    )
                    Text(
                        truck.capacity, 
                        fontSize = 12.sp, 
                        color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        truck.basePrice, 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 18.sp,
                        color = if (isSelected) Color.White else PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun CapabilityItem(label: String, value: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkBlue)
    }
}

@Composable
fun TruckSummaryCard(truck: TruckType, onContinue: () -> Unit) {
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
                    Text("Selected Vehicle", color = Color.Gray, fontSize = 12.sp)
                    Text("${truck.name} • ${truck.basePrice}", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp)
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
