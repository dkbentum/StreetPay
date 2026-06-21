package com.example.streetpay.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.streetpay.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                val address = sms.displayOriginatingAddress
                val timestamp = sms.timestampMillis
                
                val transaction = SmsParser.parse(body, timestamp, address)
                if (transaction != null) {
                    scope.launch {
                        val db = AppDatabase.getDatabase(context)
                        if (!db.transactionDao().exists(transaction.transactionId)) {
                            db.transactionDao().insert(transaction)
                            Log.d("SmsReceiver", "Inserted transaction: ${transaction.transactionId}")
                        }
                    }
                }
            }
        }
    }
}
