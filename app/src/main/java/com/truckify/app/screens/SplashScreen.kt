package com.truckify.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.truckify.app.ui.theme.BackgroundDark
import com.truckify.app.ui.theme.PrimaryBlue

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp),
                tint = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = PrimaryBlue)
        }
    }
}




