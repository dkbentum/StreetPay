package com.example.streetpay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.streetpay.data.TransactionEntity
import com.example.streetpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    isSelected: Boolean,
    onDoubleClick: () -> Unit
) {
    val networkColor = when (transaction.network) {
        "MTN" -> MtnYellow
        "Telecel" -> TelecelRed
        "AirtelTigo" -> AirtelTigoBlue
        else -> Color.Gray
    }

    val typeColor = if (transaction.transactionType == "CASH_IN") CashInGreen else CashOutOrange
    val backgroundColor = if (transaction.isSeen) Color(0xFFE0E0E0) else Color(0xFFE8F5E9)
    val contentColor = Color.Black
    val borderColor = if (isSelected) Color.Blue else if (transaction.isSeen) Color.Gray else typeColor
    val borderStroke = if (isSelected) 3.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .combinedClickable(
                onClick = { /* Do nothing on single tap */ },
                onDoubleClick = onDoubleClick
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderStroke, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Line 0: Network Indicator Bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(8.dp)
                    .background(
                        color = networkColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Line 1: Full Name
            Text(
                text = transaction.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = contentColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Line 2: Amount (Right Aligned)
            Text(
                text = "GHS ${String.format("%.2f", transaction.amount)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = if (transaction.isSeen) Color.Gray else typeColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Line 3: ID and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${transaction.transactionId}",
                    fontSize = 12.sp,
                    color = if (transaction.isSeen) Color.DarkGray else Color.Gray
                )
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp)),
                    fontSize = 12.sp,
                    color = if (transaction.isSeen) Color.DarkGray else Color.Gray
                )
            }
        }
    }
}
