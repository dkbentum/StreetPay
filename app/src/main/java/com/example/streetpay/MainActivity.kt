package com.example.streetpay

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.streetpay.sms.SmsScanner
import com.example.streetpay.ui.screens.AnalyticsScreen
import com.example.streetpay.ui.screens.SettingsScreen
import com.example.streetpay.ui.screens.TransactionListScreen
import com.example.streetpay.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.READ_SMS] == true) {
                // Initial scan after permission granted
                lifecycleScope.launch {
                    SmsScanner.scanInbox(this@MainActivity)
                }
            }
        }

        setContent {
            val navController = rememberNavController()
            val viewModel: TransactionViewModel = viewModel()

            LaunchedEffect(Unit) {
                requestPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                )
                // Also scan on startup in case permissions were already granted
                SmsScanner.scanInbox(this@MainActivity)
                viewModel.connectUsb()
            }

            NavHost(navController = navController, startDestination = "transactions") {
                composable("transactions") {
                    TransactionListScreen(
                        viewModel = viewModel,
                        onNavigateToAnalytics = { navController.navigate("analytics") },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("analytics") {
                    AnalyticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
