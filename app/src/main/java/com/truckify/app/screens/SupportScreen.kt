package com.truckify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.TruckifyTopAppBar
import com.truckify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit,
    onChatSupportClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TruckifyTopAppBar(
                title = "Support",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            item {
                Text(
                    text = "How can we help?",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Support Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SupportQuickCard(
                        title = "Chat with us",
                        subtitle = "Average wait: 2m",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onChatSupportClick
                    )
                    SupportQuickCard(
                        title = "Your Tickets",
                        subtitle = "View history",
                        icon = Icons.Default.ConfirmationNumber,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onHistoryClick
                    )
                }
            }

            item {
                Text(
                    text = "Common Issues",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val commonIssues = listOf(
                SupportIssue("Payment & Refund", Icons.Default.Payments),
                SupportIssue("Route & GPS Issues", Icons.Default.Map),
                SupportIssue("Safety & Accidents", Icons.Default.HealthAndSafety),
                SupportIssue("Account & Verification", Icons.Default.VerifiedUser),
                SupportIssue("App Performance", Icons.Default.SettingsSuggest)
            )

            items(commonIssues) { issue ->
                SupportIssueItem(issue) {
                    // Navigate to specific issue sub-category or chat
                    onChatSupportClick()
                }
            }

            item {
                Text(
                    text = "FAQs",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val faqs = listOf(
                "How to track my shipment?",
                "How to change my vehicle details?",
                "What is the cancellation policy?",
                "When do I receive my payout?"
            )

            items(faqs) { faq ->
                FAQItem(faq)
            }
        }
    }
}

@Composable
fun SupportQuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = subtitle, color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SupportIssueItem(issue: SupportIssue, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = CardDark,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(issue.icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = issue.title, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
        }
    }
}

@Composable
fun FAQItem(question: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = question, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextGray
                )
            }
            if (expanded) {
                Text(
                    text = "You can find detailed information about this in our full user guide available on our website.",
                    color = TextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(top = 12.dp))
        }
    }
}

data class SupportIssue(val title: String, val icon: ImageVector)
