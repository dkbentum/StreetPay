package com.example.streetpay.sms

import android.content.Context
import android.provider.Telephony
import com.example.streetpay.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsScanner {
    suspend fun scanInbox(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE),
            null,
            null,
            "${Telephony.Sms.Inbox.DATE} DESC"
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
                    if (!db.transactionDao().exists(transaction.transactionId)) {
                        db.transactionDao().insert(transaction)
                    }
                }
            }
        }
    }
}
