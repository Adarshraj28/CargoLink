package com.cargolink.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.android.libraries.places.api.Places
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.screens.*
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import android.content.Context
import android.content.SharedPreferences
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.activity.result.contract.ActivityResultContracts
import com.cargolink.app.navigation.Screen
import com.cargolink.app.viewmodel.AuthViewModel
import androidx.activity.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultListener {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var sharedPreferences: SharedPreferences
    private var pendingTopupAmount: Double = 0.0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val deniedPermissions = results.filter { !it.value }.keys
        if (deniedPermissions.isNotEmpty()) {
            if (deniedPermissions.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
                Toast.makeText(this, "These permissions are required for core features.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Please enable permissions in app settings to use all features.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("CargoLink", "MainActivity: onCreate")

        // Listen for real-time notifications from Firestore
        lifecycleScope.launch(Dispatchers.Main) {
            authViewModel.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    AuthManager.getCurrentUserEmail()?.let { email ->
                        FirestoreManager.getNotifications(email) { notifications ->
                            // Show the latest notification if it's new (within last 10 seconds)
                            val latest = notifications.firstOrNull()
                            val timestamp = latest?.get("timestamp") as? Long ?: 0
                            if (System.currentTimeMillis() - timestamp < 10000) {
                                val title = latest?.get("title") as? String ?: "CargoLink"
                                val message = latest?.get("message") as? String ?: ""
                                showLocalNotification(title, message)
                            }
                        }
                    }
                }
            }
        }
        
        val startTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition { 
            val checked = authViewModel.isAuthChecked.value
            val elapsedTime = System.currentTimeMillis() - startTime
            if (!checked) android.util.Log.d("CargoLink", "Waiting for auth check... ($elapsedTime ms)")
            
            // Terminal condition: checked OR 5s passed
            !checked && elapsedTime < 5000
        }

        sharedPreferences = getSharedPreferences("cargolink_prefs", MODE_PRIVATE)
        
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (BuildConfig.GOOGLE_MAPS_KEY.isNotEmpty()) {
                    Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_KEY)
                    android.util.Log.d("CargoLink", "Places initialized")
                }
            } catch (e: Exception) {
                android.util.Log.e("CargoLink", "Places Init Error: ${e.message}")
            }

            try {
                Checkout.preload(applicationContext)
                android.util.Log.d("CargoLink", "Razorpay preloaded")
            } catch (e: Exception) {
                android.util.Log.e("CargoLink", "Razorpay Preload Error: ${e.message}")
            }
            
            try {
                if (BuildConfig.STRIPE_KEY.isNotEmpty()) {
                    PaymentConfiguration.init(this@MainActivity, BuildConfig.STRIPE_KEY)
                    android.util.Log.d("CargoLink", "Stripe initialized")
                }
            } catch (e: Exception) {
                android.util.Log.e("CargoLink", "Stripe Init Error: ${e.message}")
            }
        }
        
        paymentSheet = PaymentSheet(this) { onPaymentSheetResult(it) }

        requestPermissions()

        setContent {
            var isDarkTheme by remember { 
                mutableStateOf(sharedPreferences.getBoolean("dark_mode", false)) 
            }
            
            com.cargolink.app.ui.theme.CargoLinkTheme(darkTheme = isDarkTheme) {
                val context = LocalContext.current
                Surface(color = MaterialTheme.colorScheme.background) {
                    CargoLinkApp(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { 
                            isDarkTheme = !isDarkTheme
                            sharedPreferences.edit().putBoolean("dark_mode", isDarkTheme).apply()
                        },
                        authViewModel = authViewModel,
                        sharedPreferences = sharedPreferences
                    )
                }
            }
        }
    }

    fun startRazorpayPayment(amount: Int) {
        pendingTopupAmount = amount.toDouble()
        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY)
        try {
            val options = org.json.JSONObject()
            options.put("name", "CargoLink Logistics")
            options.put("description", "Wallet Refill")
            options.put("currency", "INR")
            options.put("amount", amount * 100) // amount in paisa
            options.put("prefill.email", AuthManager.getCurrentUserEmail() ?: "")
            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in Razorpay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Razorpay Success: $razorpayPaymentId", Toast.LENGTH_LONG).show()
        AuthManager.getCurrentUserEmail()?.let { email ->
            FirestoreManager.topupWallet(email, pendingTopupAmount, "Razorpay") {
                Toast.makeText(this, "Wallet Updated! ₹$pendingTopupAmount added.", Toast.LENGTH_SHORT).show()
                pendingTopupAmount = 0.0
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Razorpay Failed: $response", Toast.LENGTH_LONG).show()
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(this, "Payment Canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(this, "Payment Failed: ${paymentSheetResult.error.message}", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Completed -> {
                AuthManager.getCurrentUserEmail()?.let { email ->
                    FirestoreManager.topupWallet(email, pendingTopupAmount, "Stripe") {
                        Toast.makeText(this, "Stripe Payment Success! ₹$pendingTopupAmount added.", Toast.LENGTH_SHORT).show()
                        pendingTopupAmount = 0.0
                    }
                }
            }
        }
    }

    private fun showLocalNotification(title: String, message: String) {
        val channelId = "cargolink_local_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "CargoLink Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun startStripePayment(amount: Double, clientSecret: String) {
        pendingTopupAmount = amount
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "CargoLink Logistics"
        )
        paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun CargoLinkApp(
    isDarkTheme: Boolean, 
    onThemeToggle: () -> Unit,
    authViewModel: AuthViewModel,
    sharedPreferences: SharedPreferences
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val userRole by authViewModel.userRole.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isPhoneVerified by authViewModel.isPhoneVerified.collectAsStateWithLifecycle()
    val isAuthChecked by authViewModel.isAuthChecked.collectAsStateWithLifecycle()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    var selectedShipmentId by rememberSaveable { mutableStateOf("") }

    var splashFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isAuthChecked, isLoading, isLoggedIn, isPhoneVerified, userRole, navBackStackEntry, splashFinished) {
        android.util.Log.d("CargoLink", "CargoLinkApp: isAuthChecked=$isAuthChecked, isLoading=$isLoading, splashFinished=$splashFinished")
        if (!isAuthChecked || isLoading || !splashFinished) return@LaunchedEffect

        val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route
        val isOnboardingCompleted = sharedPreferences.getBoolean("onboarding_completed", false)
        val normalizedRole = userRole?.trim() ?: ""

        android.util.Log.d("CargoLink", "Nav check: loggedIn=$isLoggedIn, verified=$isPhoneVerified, role=$normalizedRole, route=$currentRoute")

        if (!isOnboardingCompleted) {
            if (currentRoute != Screen.Onboarding.route) {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            return@LaunchedEffect
        }

        if (isLoggedIn) {
            val isBasicAuthScreen = currentRoute == Screen.Splash.route || 
                             currentRoute.startsWith(Screen.Login.route) || 
                             currentRoute.startsWith(Screen.Signup.route) || 
                             currentRoute == Screen.Onboarding.route || 
                             currentRoute == Screen.RoleSelection.route
            
            val isVerifiedOnPhoneScreen = currentRoute == Screen.PhoneLogin.route && isPhoneVerified

            if (normalizedRole.isNotEmpty()) {
                if (normalizedRole == "Guest") {
                    if (isBasicAuthScreen || isVerifiedOnPhoneScreen) {
                        // For Guest, we can go to Home, but we'll show completion prompts
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else if (!normalizedRole.contains("Error", ignoreCase = true) &&
                    !normalizedRole.equals("ProfileMissing", ignoreCase = true)) {
                    
                    if (isBasicAuthScreen || isVerifiedOnPhoneScreen) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else {
                    // Logged in but explicitly invalid role found
                    authViewModel.logout()
                }
            } else {
                // Logged in but role is still null/empty (still loading or network error)
                // Fallback: If we are on an auth screen and logged in, move to Home
                if (isBasicAuthScreen || isVerifiedOnPhoneScreen) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        } else {
            // Not logged in
            val isAuthScreen = currentRoute == Screen.Splash.route || 
                             currentRoute == Screen.Onboarding.route || 
                             currentRoute == Screen.Home.route
            
            val isLoginOrSignup = currentRoute.startsWith(Screen.Login.route) || 
                                currentRoute.startsWith(Screen.Signup.route)

            if (isAuthScreen && !isLoginOrSignup && currentRoute != Screen.RoleSelection.route) {
                navController.navigate(Screen.RoleSelection.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { 
            SplashScreen(onAnimationFinished = { splashFinished = true }) 
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    sharedPreferences.edit().putBoolean("onboarding_completed", true).apply()
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    navController.navigate(Screen.Login.route + "?role=$role")
                }
            )
        }
        composable(
            route = Screen.Login.route + "?role={role}",
            arguments = listOf(androidx.navigation.navArgument("role") { nullable = true })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role")
            LoginScreen(
                onSignupClick = { selectedRole ->
                    navController.navigate(Screen.Signup.route + "?role=$selectedRole") 
                },
                onPhoneLoginClick = {
                    navController.navigate(Screen.PhoneLogin.route)
                },
                authViewModel = authViewModel,
                role = role
            )
        }
        composable(Screen.PhoneLogin.route) {
            PhoneLoginScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Signup.route + "?role={role}",
            arguments = listOf(androidx.navigation.navArgument("role") { defaultValue = "Vendor" })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "Vendor"
            SignupScreen(
                onBack = { navController.popBackStack() },
                authViewModel = authViewModel,
                initialRole = role
            )
        }
        composable(Screen.Home.route) {
            val userRole by authViewModel.userRole.collectAsStateWithLifecycle()
            val isGuest = userRole == "Guest"
            
            if (userRole?.equals("Vendor", ignoreCase = true) == true) {
                HomeDashboard(
                    onCreateClick = { navController.navigate(Screen.Create.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onFleetClick = { navController.navigate(Screen.FleetHeatmap.route) },
                    onRoutesClick = { navController.navigate(Screen.Routes.route) },
                    onPaymentsClick = { navController.navigate(Screen.Payments.route) },
                    onOrdersClick = { navController.navigate(Screen.Orders.route) },
                    onHistoryClick = { navController.navigate(Screen.History.route) },
                    onChatbotClick = { navController.navigate(Screen.Chatbot.route) },
                    onTrackClick = { id ->
                        selectedShipmentId = id
                        navController.navigate(Screen.Tracking.route)
                    },
                    onVerifyClick = { navController.navigate(Screen.Verification.route) },
                    onQrClick = { id, status ->
                        selectedShipmentId = id
                        navController.navigate(Screen.QrShow.route)
                    },
                    onDriversClick = { navController.navigate(Screen.Drivers.route) },
                    onReferClick = { navController.navigate(Screen.ReferEarn.route) },
                    viewModel = hiltViewModel(),
                    isGuest = false,
                    onCompleteProfile = { navController.navigate(Screen.EditProfile.route) },
                    authViewModel = authViewModel
                )
            } else if (userRole?.equals("Driver", ignoreCase = true) == true) {
                DriverDashboard(
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onPaymentsClick = { navController.navigate(Screen.Payments.route) },
                    onOrdersClick = { navController.navigate(Screen.Orders.route) },
                    onHistoryClick = { navController.navigate(Screen.History.route) },
                    onChatbotClick = { navController.navigate(Screen.Chatbot.route) },
                    onTrackClick = { id ->
                        selectedShipmentId = id
                        navController.navigate(Screen.Tracking.route)
                    },
                    onVerifyClick = { navController.navigate(Screen.Verification.route) },
                    onQrClick = { id, status ->
                        selectedShipmentId = id
                        if (status == "In Transit") navController.navigate(Screen.OtpVerify.route)
                        else navController.navigate(Screen.QrShow.route)
                    },
                    onLoadClick = { id ->
                        selectedShipmentId = id
                        navController.navigate(Screen.LoadDetails.route)
                    },
                    onProfileClick = { navController.navigate(Screen.Profile.route) },
                    onReferClick = { navController.navigate(Screen.ReferEarn.route) },
                    onExpenseClick = { navController.navigate(Screen.ExpenseTracker.route) },
                    viewModel = hiltViewModel(),
                    returnViewModel = hiltViewModel(),
                    isGuest = false,
                    onCompleteProfile = { navController.navigate(Screen.EditProfile.route) },
                    authViewModel = authViewModel
                )
            } else {
                // Guest mode - show Vendor dashboard with restricted features
                HomeDashboard(
                    onCreateClick = { navController.navigate(Screen.Create.route) },
                    onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onFleetClick = { navController.navigate(Screen.FleetHeatmap.route) },
                    onRoutesClick = { navController.navigate(Screen.Routes.route) },
                    onPaymentsClick = { navController.navigate(Screen.Payments.route) },
                    onOrdersClick = { navController.navigate(Screen.Orders.route) },
                    onHistoryClick = { navController.navigate(Screen.History.route) },
                    onChatbotClick = { navController.navigate(Screen.Chatbot.route) },
                    onTrackClick = { id ->
                        selectedShipmentId = id
                        navController.navigate(Screen.Tracking.route)
                    },
                    onVerifyClick = { navController.navigate(Screen.Verification.route) },
                    onQrClick = { id, status ->
                        selectedShipmentId = id
                        navController.navigate(Screen.QrShow.route)
                    },
                    onDriversClick = { navController.navigate(Screen.Drivers.route) },
                    onReferClick = { navController.navigate(Screen.ReferEarn.route) },
                    viewModel = hiltViewModel(),
                    isGuest = true,
                    onCompleteProfile = { navController.navigate(Screen.EditProfile.route) },
                    authViewModel = authViewModel
                )
            }
        }
        composable(Screen.FleetHeatmap.route) { FleetHeatmapScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Notifications.route) { NotificationScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Payments.route) { PaymentScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Drivers.route) { DriversScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Routes.route) { RoutesScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Search.route) {
            if (userRole == "Driver") {
                FindLoadScreen(onBack = { navController.popBackStack() })
            } else {
                SearchScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(Screen.Orders.route) {
            CurrentOrderScreen(
                onBack = { navController.popBackStack() },
                userRole = userRole ?: "Vendor",
                onTrackClick = { id ->
                    selectedShipmentId = id
                    navController.navigate(Screen.Tracking.route)
                },
                onQrClick = { id, status ->
                    selectedShipmentId = id
                    if (userRole == "Driver") {
                        if (status == "In Transit") navController.navigate(Screen.OtpVerify.route)
                        else navController.navigate(Screen.QrScan.route)
                    } else navController.navigate(Screen.QrShow.route)
                }
            )
        }
        composable(Screen.History.route) { HistoryScreen(onBack = { navController.popBackStack() }, userRole = userRole ?: "Vendor") }
        composable(Screen.Chatbot.route) { 
            ChatbotScreen(
                onBack = { navController.popBackStack() }, 
                userRole = userRole ?: "Vendor" 
            ) 
        }
        composable(Screen.LoadDetails.route) {
            LoadDetailsScreen(
                shipmentId = selectedShipmentId,
                onBack = { navController.popBackStack() },
                onAccepted = { navController.navigate(Screen.Orders.route) }
            )
        }
        composable(Screen.Verification.route) { DriverVerificationScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.QrShow.route) { QRShowScreen(shipmentId = selectedShipmentId, onBack = { navController.popBackStack() }) }
        composable(Screen.QrScan.route) { QRScanScreen(shipmentId = selectedShipmentId, onBack = { navController.popBackStack() }) }
        composable(Screen.OtpVerify.route) { OTPVerifyScreen(shipmentId = selectedShipmentId, onBack = { navController.popBackStack() }) }
        composable(Screen.Tracking.route) {
            LiveTrackingScreen(
                shipmentId = selectedShipmentId,
                onBack = { navController.popBackStack() },
                userRole = userRole ?: "Vendor",
                onChatClick = { navController.navigate(Screen.ShipmentChat.route) },
                onVerifyClick = { navController.navigate(Screen.OtpVerify.route) },
                onAvailableRedirect = { id ->
                    selectedShipmentId = id
                    navController.navigate(Screen.Confirming.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        composable(Screen.ShipmentChat.route) {
            ShipmentChatScreen(
                shipmentId = selectedShipmentId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ExpenseTracker.route) {
            ExpenseTrackerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            val context = LocalContext.current
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onSecurityClick = { 
                    Toast.makeText(context, "Security Settings coming soon!", Toast.LENGTH_SHORT).show()
                },
                onTermsClick = { navController.navigate(Screen.Terms.route) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onPersonalInfoClick = { navController.navigate(Screen.EditProfile.route) },
                onDocumentsClick = { navController.navigate(Screen.Verification.route) },
                onPayoutsClick = { navController.navigate(Screen.Payments.route) },
                onSupportClick = { navController.navigate(Screen.Support.route) },
                onReferClick = { navController.navigate(Screen.ReferEarn.route) }
            )
        }
        composable(Screen.Create.route) { 
            CreateShipmentScreen(
                onBack = { navController.popBackStack() },
                onPostSuccess = { id ->
                    selectedShipmentId = id
                    navController.navigate(Screen.Confirming.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            ) 
        }
        composable(Screen.Confirming.route) {
            ShipmentConfirmingScreen(
                shipmentId = selectedShipmentId,
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onConfirmed = {
                    navController.navigate(Screen.Tracking.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        composable(Screen.Matching.route + "/{shipmentId}") { backStackEntry ->
            val shipmentId = backStackEntry.arguments?.getString("shipmentId") ?: ""
            MatchingScreen(shipmentId = shipmentId, onBack = { 
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            })
        }
        composable(Screen.EditProfile.route) { EditProfileScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Terms.route) { TermsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.AnimationLab.route) {
            AnimationLabScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Support.route) {
            SupportScreen(
                onBack = { navController.popBackStack() },
                onChatSupportClick = { navController.navigate(Screen.Chatbot.route) },
                onHistoryClick = { /* Navigate to support tickets history if implemented */ }
            )
        }
        composable(Screen.ReferEarn.route) {
            ReferEarnScreen(onBack = { navController.popBackStack() })
        }
    }
}
