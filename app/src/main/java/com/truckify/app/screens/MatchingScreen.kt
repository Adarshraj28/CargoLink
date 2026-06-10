package com.truckify.app.screens

import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.TruckifyTopAppBar
import com.truckify.app.models.Driver
import com.truckify.app.ui.theme.*
import com.truckify.app.viewmodel.MatchingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingScreen(shipmentId: String, onBack: () -> Unit, viewModel: MatchingViewModel = viewModel()) {
    val shipment by viewModel.shipment.collectAsState()
    val drivers by viewModel.recommendedDrivers.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(shipmentId) {
        viewModel.findMatches(shipmentId)
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TruckifyTopAppBar(
                title = "Smart Matching",
                onBack = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && drivers.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI is analyzing drivers...", color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        shipment?.let { s ->
                            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(text = "${s.pickupAddress.split(",").first()} → ${s.destinationAddress.split(",").first()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "${s.weight} • ${s.truckType}", color = TextGray, fontSize = 12.sp)
                                    Text(text = "${drivers.size} Smart Matches Found", color = TextGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (aiAnalysis != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AI ENGINE RECOMMENDATION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = aiAnalysis!!, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    items(drivers) { driver ->
                        BidCard(
                            driver = driver, 
                            onSelect = {
                                viewModel.assignDriver(
                                    driver.email,
                                    onSuccess = { onBack() },
                                    onError = { /* Handle error */ }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BidCard(driver: Driver, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(BackgroundDark), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = driver.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Text(text = String.format(Locale.getDefault(), " %.1f", driver.rating), color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(text = "${driver.truckType} • ${driver.experienceYears}y Exp", color = TextGray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${driver.trustScore.toInt()}% Trust",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { /* View Profile */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
                ) {
                    Text(text = "View Profile", color = PrimaryBlue)
                }
                Button(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(text = "Assign Driver")
                }
            }
        }
    }
}
