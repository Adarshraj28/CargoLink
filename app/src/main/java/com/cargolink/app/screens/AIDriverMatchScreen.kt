package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
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

data class DriverMatch(
    val id: String,
    val name: String,
    val rating: String,
    val trips: String,
    val eta: String,
    val trustScore: Int,
    val matchPercentage: Int,
    val isVerified: Boolean = true,
    val isPreferred: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIDriverMatchScreen(
    onBack: () -> Unit,
    onContinue: (DriverMatch) -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    var isSearching by remember { mutableStateOf(true) }

    val drivers = listOf(
        DriverMatch("1", "Ramesh Kumar", "4.9", "1,240", "12 mins", 98, 99, isPreferred = true),
        DriverMatch("2", "Suresh Singh", "4.8", "850", "15 mins", 95, 94),
        DriverMatch("3", "Vikram Rathore", "4.7", "2,100", "8 mins", 92, 88)
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        isSearching = false
    }

    Scaffold(
        containerColor = Beige.copy(alpha = 0.05f),
        topBar = {
            Column(modifier = Modifier.background(Beige.copy(alpha = 0.1f))) {
                CenterAlignedTopAppBar(
                    title = { Text("AI Driver Match", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                StepProgressBar(currentStep = 6, totalSteps = 8)
            }
        },
        bottomBar = {
            val driver = drivers.find { it.id == viewModel.selectedDriverId }
            AcceptDriverBottomBar(
                selectedDriver = driver,
                onAccept = { 
                    driver?.let { 
                        viewModel.selectedDriverName = it.name
                        onContinue(it) 
                    } 
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isSearching) {
                SearchingAnimation()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Text(
                            text = "Top Matches",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkBlue
                        )
                        Text(
                            text = "AI has analyzed 42 nearby drivers for your route.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }

                    items(drivers) { driver ->
                        DriverMatchCard(
                            driver = driver,
                            isSelected = viewModel.selectedDriverId == driver.id,
                            onClick = { viewModel.selectedDriverId = driver.id }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "searching")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Pulse backgrounds
            repeat(3) { index ->
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 2.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, delayMillis = index * 500),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, delayMillis = index * 500),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(SoftPurple.copy(alpha = alpha))
                )
            }

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .shadow(16.dp, CircleShape, spotColor = SoftPurple.copy(alpha = 0.4f))
                    .background(Color.White, CircleShape)
                    .border(2.dp, SoftPurple.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SoftPurple,
                    modifier = Modifier.size(52.dp).rotate(rotation)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Analyzing Optimal Drivers...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DarkBlue
        )
        Text(
            "Checking trust scores, ratings and live traffic ETA.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun DriverMatchCard(
    driver: DriverMatch,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(if (isSelected) 16.dp else 4.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryBlue else Beige.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp).background(if (isSelected) PrimaryBlue.copy(alpha = 0.03f) else Color.Transparent)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // DRIVER IMAGE PLACEHOLDER
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Beige.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF5D4037), modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkBlue)
                        if (driver.isVerified) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.padding(start = 4.dp).size(14.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = WarningOrange, modifier = Modifier.size(14.dp))
                        Text("${driver.rating} • ${driver.trips} trips", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                
                // MATCH PERCENTAGE (Apple Style Badge)
                Surface(
                    color = if (driver.matchPercentage > 95) TealGreen.copy(alpha = 0.12f) else SoftPurple.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${driver.matchPercentage}% Match",
                        color = if (driver.matchPercentage > 95) TealGreen else SoftPurple,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DriverSpecItem("ETA", driver.eta, Icons.Default.Timer)
                DriverSpecItem("Trust Score", "${driver.trustScore}%", Icons.Default.VerifiedUser)
                DriverSpecItem("Vehicle", "Verified", Icons.Default.LocalShipping)
            }

            if (driver.isPreferred) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Beige.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preferred Driver: You have worked with Ramesh before.", fontSize = 11.sp, color = Color(0xFF5D4037))
                    }
                }
            }
        }
    }
}

@Composable
fun DriverSpecItem(label: String, value: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkBlue)
    }
}

@Composable
fun AcceptDriverBottomBar(selectedDriver: DriverMatch?, onAccept: () -> Unit) {
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
                    Text("Selected Match", color = Color.Gray, fontSize = 12.sp)
                    Text(selectedDriver?.name ?: "No Driver Selected", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp)
                }
                Button(
                    onClick = onAccept,
                    enabled = selectedDriver != null,
                    modifier = Modifier.height(56.dp).width(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Accept Driver", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}
