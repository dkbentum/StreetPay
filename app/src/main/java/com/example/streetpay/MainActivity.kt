package com.example.streetpay

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.streetpay.gesture.GestureRecognizerHelper
import com.example.streetpay.sms.SmsScanner
import com.example.streetpay.util.SoundManager
import com.example.streetpay.ui.screens.AnalyticsScreen
import com.example.streetpay.ui.screens.SettingsScreen
import com.example.streetpay.ui.screens.TransactionListScreen
import com.example.streetpay.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), GestureRecognizerHelper.GestureRecognizerListener {
    private var smsObserver: ContentObserver? = null
    private lateinit var cameraExecutor: ExecutorService
    private var gestureHelper: GestureRecognizerHelper? = null
    private var viewModel: TransactionViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Create an observer to watch the SMS inbox for changes while app is open
        smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                lifecycleScope.launch {
                    // Quick scan of last 5 messages when a change is detected
                    val newFound = SmsScanner.scanInbox(this@MainActivity, limit = 5)
                    if (newFound && viewModel?.isSoundEnabled?.value == true) {
                        SoundManager.playNotificationSound(
                            this@MainActivity, 
                            viewModel?.notificationSoundUri?.value
                        )
                    }
                }
            }
        }

        // Register the observer
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver!!
        )

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.READ_SMS] == true) {
                lifecycleScope.launch {
                    SmsScanner.scanInbox(this@MainActivity)
                }
            }
            if (permissions[Manifest.permission.CAMERA] == true) {
                startCamera()
            }
        }

        setContent {
            val navController = rememberNavController()
            val vm: TransactionViewModel = viewModel()
            viewModel = vm

            val isGestureEnabled by vm.isGestureEnabled.collectAsState()

            LaunchedEffect(isGestureEnabled) {
                if (isGestureEnabled) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        startCamera()
                    }
                } else {
                    stopCamera()
                }
            }

            LaunchedEffect(Unit) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS, 
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.CAMERA
                    )
                )
                // Also scan on startup in case permissions were already granted
                val found = SmsScanner.scanInbox(this@MainActivity)
                if (found && vm.isSoundEnabled.value) {
                    SoundManager.playNotificationSound(this@MainActivity, vm.notificationSoundUri.value)
                }
                vm.connectUsb()
            }

            NavHost(navController = navController, startDestination = "transactions") {
                composable("transactions") {
                    TransactionListScreen(
                        viewModel = vm,
                        onNavigateToAnalytics = { navController.navigate("analytics") },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("analytics") {
                    AnalyticsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        // Stop watching for SMS changes when app is closed to save battery
        smsObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    override fun onError(error: String) {
        Log.e("MainActivity", "Gesture Error: $error")
    }

    override fun onResults(action: String) {
        if (viewModel?.isGestureEnabled?.value == true) {
            viewModel?.handleExternalCommand(action)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            if (viewModel?.isGestureEnabled?.value == false) return@addListener // Safety check

            if (gestureHelper == null) {
                gestureHelper = GestureRecognizerHelper(this, this)
            }

            val cameraProvider = cameraProviderFuture.get()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("MainActivity", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            Log.d("MainActivity", "Camera stopped to save battery")
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        // Front camera image is often mirrored, and we might need to rotate it
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        
        gestureHelper?.recognizeLiveStream(rotatedBitmap)
        imageProxy.close()
    }
}
