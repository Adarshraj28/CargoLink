package com.cargolink.app.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.R
import kotlinx.coroutines.delay

import androidx.compose.ui.text.TextStyle
import com.cargolink.app.ui.theme.BlueGradient

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = OvershootInterpolator().toEasing()
        ),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        onAnimationFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val circleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "circleScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background circles
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF0066FF).copy(alpha = 0.03f),
                radius = 400.dp.toPx() * circleScale,
                center = center.copy(x = size.width * 0.9f, y = size.height * 0.1f)
            )
            drawCircle(
                color = Color(0xFF0066FF).copy(alpha = 0.02f),
                radius = 300.dp.toPx() * circleScale,
                center = center.copy(x = size.width * 0.1f, y = size.height * 0.9f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.cargolink_truck),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "CARGOLINK",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(brush = BlueGradient),
                letterSpacing = 4.sp
            )
            
            Text(
                text = "Move • Track • Deliver",
                fontSize = 14.sp,
                color = Color.Gray,
                letterSpacing = 2.sp
            )
        }
    }
}

// Helper to use OvershootInterpolator in Compose
private class OvershootInterpolator {
    fun toEasing(): Easing = Easing { x ->
        val tension = 2.0f
        val t = x - 1.0f
        t * t * ((tension + 1) * t + tension) + 1.0f
    }
}
