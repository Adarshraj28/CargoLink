package com.cargolink.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.cargolink.app.components.CargoLinkTopAppBar
import com.cargolink.app.firebase.GeminiManager
import com.cargolink.app.ui.theme.*
import kotlinx.coroutines.launch

import android.speech.RecognizerIntent
import android.content.Intent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(onBack: () -> Unit, userRole: String) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(
        Pair("AI", "Hello Ashok! 👋\nHow can I help you today?")
    ) }
    val scope = rememberCoroutineScope()
    var isTyping by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            if (spokenText.isNotBlank()) {
                messageText = spokenText
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CargoLinkTopAppBar(
                title = "AI Assistant",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(CardDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                items(messages) { msg ->
                    val isAI = msg.first == "AI"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isAI) CardDark else PrimaryBlue
                        ) {
                            Text(
                                text = msg.second,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                if (messages.size == 1) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SuggestionItem("Find best loads on my route", Icons.Default.Search) { messageText = "Find best loads on my route" }
                            SuggestionItem("Show return load suggestions", Icons.Default.Repeat) { messageText = "Show return load suggestions" }
                            SuggestionItem("Help with trip & route", Icons.Default.Map) { messageText = "Help with trip & route" }
                        }
                    }
                }
                
                if (isTyping) {
                    item {
                        Text("AI is typing...", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // Input Area
            Surface(
                color = CardDark,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask anything...", color = TextGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = PrimaryBlue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        enabled = !isTyping
                    )
                    IconButton(onClick = { 
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                        }
                        speechLauncher.launch(intent)
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = PrimaryBlue)
                    }
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val userMsg = messageText
                                messages.add(Pair("User", userMsg))
                                messageText = ""
                                scope.launch {
                                    isTyping = true
                                    val reply = GeminiManager.getChatResponse(userMsg, userRole)
                                    messages.add(Pair("AI", reply))
                                    isTyping = false
                                }
                            }
                        },
                        enabled = !isTyping && messageText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = if (messageText.isNotBlank()) PrimaryBlue else TextGray)
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = CardDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, color = Color.White, fontSize = 14.sp)
        }
    }
}
