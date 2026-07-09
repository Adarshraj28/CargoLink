package com.cargolink.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cargolink.app.ui.theme.*

@Composable
fun LottieAnimationComponent(
    url: String,
    modifier: Modifier = Modifier,
    iterations: Int = 1
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryBlue)
    }
}

@Composable
fun SuccessAnimation(modifier: Modifier = Modifier.size(250.dp)) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = modifier)
}

@Composable
fun LoadingAnimation(modifier: Modifier = Modifier.size(150.dp)) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 4.dp)
    }
}

@Composable
fun NoInternetAnimation(modifier: Modifier = Modifier.size(300.dp)) {
    Icon(Icons.Default.WifiOff, contentDescription = null, tint = TextSecondary, modifier = modifier)
}

@Composable
fun DeliveredAnimation(modifier: Modifier = Modifier.size(250.dp)) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = modifier)
}

@Composable
fun TruckAnimation(modifier: Modifier = Modifier.size(200.dp)) {
    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = modifier)
}

@Composable
fun WalletAnimation(modifier: Modifier = Modifier.size(100.dp)) {
    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryBlue, modifier = modifier)
}
