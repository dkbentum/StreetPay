package com.example.streetpay.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val name: String,
    val amount: Double,
    val reference: String?,
    val transactionType: String, // CASH_IN
    val network: String,
    val balance: Double?,
    val smsBody: String,
    val timestamp: Long,
    var isSeen: Boolean = false
)
