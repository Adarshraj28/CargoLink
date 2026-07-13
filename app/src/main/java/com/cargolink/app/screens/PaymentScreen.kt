package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onPaymentComplete: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedMethod by remember { mutableStateOf("Wallet") }

    Scaffold(
        containerColor = Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DarkBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.shadow(32.dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = onPaymentComplete,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                    ) {
                        Text("Pay ₹4,850 Securely", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PCI-DSS Secure Payment", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // BILLING SUMMARY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Billing Summary", fontWeight = FontWeight.Bold, color = DarkBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    PaymentDetailRow("Shipment ID", "#CL-984210")
                    PaymentDetailRow("Service Type", "Express Freight")
                    PaymentDetailRow("Total Amount", "₹4,850", isBold = true)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Select Payment Method", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // PAYMENT METHODS
            PaymentMethodItem(
                title = "Cargolink Wallet",
                subtitle = "Balance: ₹12,400",
                icon = Icons.Default.AccountBalanceWallet,
                isSelected = selectedMethod == "Wallet",
                onClick = { selectedMethod = "Wallet" }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PaymentMethodItem(
                title = "UPI / Google Pay / PhonePe",
                subtitle = "Pay using your favorite UPI app",
                icon = Icons.Default.QrCode,
                isSelected = selectedMethod == "UPI",
                onClick = { selectedMethod = "UPI" }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PaymentMethodItem(
                title = "Credit / Debit Cards",
                subtitle = "Visa, Mastercard, RuPay",
                icon = Icons.Default.CreditCard,
                isSelected = selectedMethod == "Card",
                onClick = { selectedMethod = "Card" }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PaymentMethodItem(
                title = "Net Banking",
                subtitle = "Over 40+ Indian Banks",
                icon = Icons.Default.AccountBalance,
                isSelected = selectedMethod == "NetBanking",
                onClick = { selectedMethod = "NetBanking" }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PROMO CODE SECTION
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter Promo Code") },
                trailingIcon = { Text("APPLY", color = PrimaryBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PaymentMethodItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.05f) else Color.White,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(if (isSelected) PrimaryBlue else Background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (isSelected) Color.White else PrimaryBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 15.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
            )
        }
    }
}

@Composable
fun PaymentDetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium, color = DarkBlue, fontSize = 14.sp)
    }
}
