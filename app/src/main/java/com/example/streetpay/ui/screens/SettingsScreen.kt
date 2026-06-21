package com.example.streetpay.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.streetpay.ui.theme.*
import com.example.streetpay.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedNetworks by viewModel.selectedNetworks.collectAsState()
    
    // Permission States
    var isSmsReadGranted by remember { 
        mutableStateOf(hasPermission(context, Manifest.permission.READ_SMS)) 
    }
    var isSmsReceiveGranted by remember { 
        mutableStateOf(hasPermission(context, Manifest.permission.RECEIVE_SMS)) 
    }

    LaunchedEffect(Unit) {
        isSmsReadGranted = hasPermission(context, Manifest.permission.READ_SMS)
        isSmsReceiveGranted = hasPermission(context, Manifest.permission.RECEIVE_SMS)
    }

    // Date Picker State
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.setDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Filters", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Transaction Date", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Network", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val networks = listOf("MTN", "Telecel", "AirtelTigo")
                        networks.forEach { network ->
                            val chipColor = when (network) {
                                "MTN" -> MtnYellow
                                "Telecel" -> TelecelRed
                                "AirtelTigo" -> AirtelTigoBlue
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }

                            FilterChip(
                                selected = selectedNetworks.contains(network),
                                onClick = { viewModel.toggleNetwork(network) },
                                label = { Text(network) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            Text(
                "Enable these permissions in system settings for full functionality.", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            PermissionToggle(
                label = "Read SMS (History)",
                isGranted = isSmsReadGranted,
                onToggle = { openAppSettings(context) }
            )

            PermissionToggle(
                label = "Receive SMS (Real-time)",
                isGranted = isSmsReceiveGranted,
                onToggle = { openAppSettings(context) }
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Data Management", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { /* TODO: Implement clear data */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Transactions")
            }
        }
    }
}

@Composable
fun PermissionToggle(
    label: String,
    isGranted: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = isGranted,
            onCheckedChange = { onToggle() }
        )
    }
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
