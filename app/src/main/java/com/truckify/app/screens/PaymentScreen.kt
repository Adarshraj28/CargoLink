package com.truckify.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.truckify.app.components.TruckifyTopAppBar
import com.truckify.app.ui.theme.*
import com.truckify.app.viewmodel.PaymentViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(onBack: () -> Unit, viewModel: PaymentViewModel = viewModel()) {
    val balance by viewModel.balance.collectAsState()
    val tripEarnings by viewModel.tripEarnings.collectAsState()
    val extraCharges by viewModel.extraCharges.collectAsState()
    val otherCharges by viewModel.otherCharges.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TruckifyTopAppBar(
                title = "Earnings",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "₹${String.format(Locale.getDefault(), "%,.0f", balance)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                        Text(text = "Total Balance", color = TextGray, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Text(text = " +12.5% from last week", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                (listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")).forEachIndexed { index, day ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val height = (50..120).random().dp
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height(120.dp),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(12.dp)
                                                    .height(height)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (index == 5) PrimaryBlue else PrimaryBlue.copy(alpha = 0.3f))
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = day, color = TextGray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(text = "Breakdown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    BreakdownItem("Trip Earnings", "₹${String.format(Locale.getDefault(), "%,.0f", tripEarnings)}")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                    BreakdownItem("Extra Charges", "₹${String.format(Locale.getDefault(), "%,.0f", extraCharges)}")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                    BreakdownItem("Toll & Others", "-₹${String.format(Locale.getDefault(), "%,.0f", otherCharges)}", isNegative = true)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Total Earnings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "₹${String.format(Locale.getDefault(), "%,.0f", tripEarnings + extraCharges - otherCharges)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun BreakdownItem(label: String, value: String, isNegative: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = TextGray, fontSize = 14.sp)
        Text(text = value, color = if (isNegative) Color.Red else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
