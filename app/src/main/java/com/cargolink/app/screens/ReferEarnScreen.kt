package com.cargolink.app.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.cargolink.app.firebase.AuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferEarnScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val userEmail = AuthManager.getCurrentUserEmail() ?: "USER"
    val referralCode = userEmail.take(6).uppercase()
    
    val shareText = "Hey! Use my referral code $referralCode to join CargoLink and earn up to ₹2000 on your first few shipments! Download now: https://cargolink.app/download"

    val beigeBackground = Color(0xFFFDF5E6) // Soft Beige
    val darkBeige = Color(0xFFD2B48C)
    val deepBrown = Color(0xFF5D4037)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refer & Earn", fontWeight = FontWeight.Bold, color = deepBrown) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = deepBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = beigeBackground
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { it / 2 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Gift Icon
                val infiniteTransition = rememberInfiniteTransition(label = "gift")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(darkBeige.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = deepBrown,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Refer Friends & Family",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = deepBrown,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Invite your friends to CargoLink and earn up to ₹2,000 when they complete their first trip.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = deepBrown.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Referral Code Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOUR UNIQUE CODE",
                            style = MaterialTheme.typography.labelMedium,
                            color = darkBeige,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(beigeBackground)
                                .border(1.dp, darkBeige.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(referralCode))
                                    android.widget.Toast.makeText(context, "Code Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = referralCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = deepBrown,
                                letterSpacing = 6.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = deepBrown, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // How it works with cute nodes
                Text(
                    text = "Three Simple Steps",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = deepBrown
                )

                Spacer(modifier = Modifier.height(20.dp))

                ReferStepBeige(
                    number = "1",
                    title = "Share Link",
                    description = "Send your referral link to friends.",
                    deepBrown = deepBrown
                )
                ReferStepBeige(
                    number = "2",
                    title = "Friend Registers",
                    description = "They join CargoLink using your code.",
                    deepBrown = deepBrown
                )
                ReferStepBeige(
                    number = "3",
                    title = "Receive Rewards",
                    description = "Get ₹500 for each friend, up to ₹2,000!",
                    deepBrown = deepBrown
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = deepBrown)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Share with Friends", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ReferStepBeige(number: String, title: String, description: String, deepBrown: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(deepBrown),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = deepBrown, fontSize = 16.sp)
            Text(text = description, color = deepBrown.copy(alpha = 0.6f), fontSize = 14.sp)
        }
    }
}
