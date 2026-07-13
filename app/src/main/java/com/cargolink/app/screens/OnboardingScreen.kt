package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.DarkBackground
import com.cargolink.app.ui.theme.PrimaryBlue
import com.cargolink.app.ui.theme.TextPrimary
import com.cargolink.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

import com.cargolink.app.ui.theme.*

data class OnboardingData(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val onboardingPages = listOf(
        OnboardingData(
            "India's Smart Logistics Platform",
            "Connecting vendors and drivers seamlessly for efficient transport across the nation.",
            Icons.Default.LocalShipping
        ),
        OnboardingData(
            "Track shipments in real time",
            "Get live location updates and precise ETA for every single delivery at your fingertips.",
            Icons.Default.Timeline
        ),
        OnboardingData(
            "Verified drivers and secure payments",
            "Trust is our priority with fully verified partners and multi-layered secure payment gateways.",
            Icons.Default.Security
        )
    )

    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPage(onboardingPages[page])
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(onboardingPages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) PrimaryBlue else Beige.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == iteration) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Next Button
                IconButton(
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                        .shadow(8.dp, CircleShape, spotColor = PrimaryBlue.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Skip Button
        TextButton(
            onClick = onFinish,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(
                text = "Skip",
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OnboardingPage(data: OnboardingData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(Color.White)
                .shadow(16.dp, RoundedCornerShape(48.dp), spotColor = Beige.copy(alpha = 0.3f))
                .border(1.dp, Beige.copy(alpha = 0.4f), RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = data.title,
            color = DarkBlue,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = data.description,
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
