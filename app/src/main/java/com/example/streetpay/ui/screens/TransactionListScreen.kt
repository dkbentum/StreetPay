package com.example.streetpay.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import com.example.streetpay.data.TransactionEntity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontWeight
import com.example.streetpay.ui.viewmodel.TransactionViewModel
import com.example.streetpay.ui.screens.SwipeableTransactionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val selectedId by viewModel.selectedTransactionId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StreetPay", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Analytics")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            // Navigation bar removed as it's now Cash In only
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(transactions, key = { it.id }) { transaction ->
                SwipeableTransactionCard(
                    transaction = transaction,
                    isSelected = transaction.transactionId == selectedId,
                    onMarkSeen = { viewModel.toggleSeen(it) },
                    onDoubleClick = { viewModel.toggleSeen(transaction) }
                )
            }
        }
    }
}
