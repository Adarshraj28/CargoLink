package com.cargolink.app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.components.CargoLinkButton
import com.cargolink.app.components.CargoLinkLogoIcon
import com.cargolink.app.components.CargoLinkTextField
import com.cargolink.app.ui.theme.*
import com.cargolink.app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onSignupClick: (String) -> Unit,
    onPhoneLoginClick: () -> Unit,
    authViewModel: AuthViewModel,
    role: String? = null
) {
    val context = LocalContext.current
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige.copy(alpha = 0.05f))
    ) {
        // Background Header
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-50).dp, y = 50.dp)
                        .alpha(0.05f),
                    tint = PrimaryBlue
                )
                
                Column(
                    modifier = Modifier.padding(32.dp)
                ) {
                    CargoLinkLogoIcon()
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "MOVE", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(text = " • ", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "CONNECT", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(text = " • ", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "DELIVER", color = Beige, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
            }
        }

        // Login Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Beige.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = if (role != null) "$role Login" else "Welcome Back",
                        style = MaterialTheme.typography.displayMedium,
                        color = DarkBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (role == "Driver") "Access your loads and earnings" else "Login to your logistics dashboard",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    CargoLinkTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        label = "Email or Phone",
                        leadingIcon = Icons.Default.Person
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    CargoLinkTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "Forgot Password?",
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    CargoLinkButton(
                        text = "Login",
                        onClick = {
                            if (emailOrPhone.trim().isEmpty() || password.trim().isEmpty()) {
                                Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                                return@CargoLinkButton
                            }
                            authViewModel.login(emailOrPhone.trim(), password.trim())
                        },
                        isLoading = isLoading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onPhoneLoginClick,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Login with Phone", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Don't have an account? ", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            "Sign up",
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onSignupClick(role ?: "Vendor") }
                        )
                    }
                }
            }
        }
    }
}
