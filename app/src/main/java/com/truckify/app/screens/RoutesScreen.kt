package com.truckify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.firebase.GeminiManager
import com.truckify.app.ui.theme.DarkBlue
import com.truckify.app.ui.theme.LightBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var optimizationResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val deliveryPoints = listOf(
        "Warehouse A, Okhla",
        "Retailer B, Noida",
        "Factory C, Gurgaon",
        "Hub D, Ghaziabad"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Route Optimizer", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Select waypoints to optimize your delivery sequence.", color = Color.Gray)
            }

            items(deliveryPoints) { point ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = LightBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(point, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            val start = deliveryPoints.first()
                            val end = deliveryPoints.last()
                            val stops = deliveryPoints.subList(1, deliveryPoints.size - 1)
                            optimizationResult = GeminiManager.getOptimizedRoute(start, stops, end)
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimize with AI")
                    }
                }
            }

            if (optimizationResult != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = LightBlue.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Optimized Sequence", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(optimizationResult!!, color = Color.DarkGray, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
