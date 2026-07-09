package com.cargolink.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.MainActivity
import com.cargolink.app.components.CargoLinkTopAppBar
import com.cargolink.app.ui.theme.*
import com.cargolink.app.viewmodel.PaymentViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(onBack: () -> Unit, viewModel: PaymentViewModel = hiltViewModel()) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val tripEarnings by viewModel.tripEarnings.collectAsStateWithLifecycle()
    val extraCharges by viewModel.extraCharges.collectAsStateWithLifecycle()
    val otherCharges by viewModel.otherCharges.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    var showTopupDialog by remember { mutableStateOf(false) }
    var topupAmount by remember { mutableStateOf("1000") }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CargoLinkTopAppBar(
                title = "Wallet & Earnings",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        if (isLoading && history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "₹${String.format(Locale.getDefault(), "%,.0f", balance)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 42.sp)
                        Text(text = "Current Balance", color = TextGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { /* Withdraw Logic */ },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Withdraw")
                            }
                            Button(
                                onClick = { showTopupDialog = true },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Top-up")
                            }
                        }
                    }
                }

                item {
                    Text(text = "Earnings Analytics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                (listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")).forEachIndexed { index, day ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val height = (30..100).random().dp
                                        Box(
                                            modifier = Modifier
                                                .width(8.dp)
                                                .height(100.dp),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(8.dp)
                                                    .height(height)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (index == 6) AccentGreen else PrimaryBlue)
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
                    Text(text = "Transaction History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                items(history) { txn ->
                    TransactionItem(txn)
                }
                
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }

        if (showTopupDialog) {
            AlertDialog(
                onDismissRequest = { showTopupDialog = false },
                title = { Text("Top-up Wallet") },
                text = {
                    Column {
                        Text("Enter amount to add to your wallet:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = topupAmount,
                            onValueChange = { if (it.all { char -> char.isDigit() }) topupAmount = it },
                            label = { Text("Amount (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = topupAmount.toIntOrNull() ?: 0
                            if (amount > 0) {
                                (context as? MainActivity)?.startRazorpayPayment(amount)
                                showTopupDialog = false
                            }
                        }
                    ) {
                        Text("Pay Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTopupDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionItem(txn: com.cargolink.app.models.Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when(txn.type) {
                            "Payout", "Topup", "Incentive" -> AccentGreen.copy(alpha = 0.1f)
                            else -> Color.Red.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(txn.type) {
                        "Topup" -> Icons.Default.AddCard
                        "Payout" -> Icons.Default.Payments
                        "Escrow Payment" -> Icons.Default.HistoryEdu
                        else -> Icons.AutoMirrored.Filled.ReceiptLong
                    },
                    contentDescription = null,
                    tint = if (txn.type in listOf("Payout", "Topup", "Incentive")) AccentGreen else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = txn.type, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = if (txn.shipmentId.isNotEmpty()) "Shipment #${txn.shipmentId.takeLast(4)}" else txn.gateway,
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
            Text(
                text = (if (txn.type in listOf("Payout", "Topup", "Incentive")) "+" else "-") + "₹${String.format(Locale.getDefault(), "%.0f", txn.amount)}",
                color = if (txn.type in listOf("Payout", "Topup", "Incentive")) AccentGreen else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
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
