package com.example.streetpay.sms

import android.content.Context
import android.provider.Telephony
import com.example.streetpay.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsScanner {
    /**
     * Scans the inbox and returns true if at least one NEW transaction was found and saved.
     */
    suspend fun scanInbox(context: Context, limit: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val contentResolver = context.contentResolver
        var anyNew = false
        
        // Add a limit for frequent background scans
        val sortOrder = if (limit > 0) {
            "${Telephony.Sms.Inbox.DATE} DESC LIMIT $limit"
        } else {
            "${Telephony.Sms.Inbox.DATE} DESC"
        }

        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE),
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val bodyIdx = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val addrIdx = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val dateIdx = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

            while (it.moveToNext()) {
                val body = it.getString(bodyIdx)
                val address = it.getString(addrIdx)
                val date = it.getLong(dateIdx)

                val transaction = SmsParser.parse(body, date, address)
                if (transaction != null) {
                    // Check for duplicates with different IDs (e.g. truncated IDs)
                    val similar = db.transactionDao().findSimilar(
                        transaction.name,
                        transaction.amount,
                        transaction.timestamp
                    )

                    if (similar != null) {
                        // If we found a match, keep the one with the LONGER ID
                        if (transaction.transactionId.length > similar.transactionId.length) {
                            db.transactionDao().delete(similar)
                            db.transactionDao().insert(transaction)
                            anyNew = true
                        }
                    } else {
                        // Brand new transaction
                        if (!db.transactionDao().exists(transaction.transactionId)) {
                            db.transactionDao().insert(transaction)
                            anyNew = true
                        }
                    }
                }
            }
        }
        return@withContext anyNew
    }
}
