package com.truckify.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.truckify.app.ui.theme.DarkBlue
import com.truckify.app.ui.theme.LightBlue

@Composable
fun LottieAnimationComponent(
    url: String,
    modifier: Modifier = Modifier,
    iterations: Int = 1
) {
    // Replaced with a simple loader for performance
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LightBlue)
    }
}

@Composable
fun SuccessAnimation(modifier: Modifier = Modifier.size(250.dp)) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = modifier)
}

@Composable
fun LoadingAnimation(modifier: Modifier = Modifier.size(150.dp)) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LightBlue, strokeWidth = 4.dp)
    }
}

@Composable
fun NoInternetAnimation(modifier: Modifier = Modifier.size(300.dp)) {
    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.Gray, modifier = modifier)
}

@Composable
fun DeliveredAnimation(modifier: Modifier = Modifier.size(250.dp)) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DarkBlue, modifier = modifier)
}

@Composable
fun TruckAnimation(modifier: Modifier = Modifier.size(200.dp)) {
    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = LightBlue, modifier = modifier)
}

@Composable
fun WalletAnimation(modifier: Modifier = Modifier.size(100.dp)) {
    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = DarkBlue, modifier = modifier)
}
