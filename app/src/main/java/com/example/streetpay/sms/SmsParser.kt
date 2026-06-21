package com.example.streetpay.sms

import com.example.streetpay.data.TransactionEntity
import java.util.regex.Pattern

object SmsParser {
    /**
     * MTN Ghana Cash In Pattern (New format):
     * "Payment received for GHS 1.00 from MARTIN AFFUL  Current Balance: GHS 180.53 . Available Balance: GHS 180.53. Reference: A. Transaction ID: 83782557326. TRANSACTION FEE: 0.00"
     */
    private val cashInPatternNew = Pattern.compile(
        "Payment\\s+received\\s+for\\s+GHS\\s+([\\d,.]+)\\s+from\\s+(.+?)\\s+Current\\s+Balance:\\s+GHS\\s+([\\d,.]+)\\s*\\.\\s+Available\\s+Balance:.*?Reference:\\s*(.*?)\\.\\s+Transaction\\s+ID:\\s+(\\d+)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * MTN Ghana Cash In Pattern (Alternative format):
     * "Payment received for GHS 101.00 from PRISCILLA DEBRAH ASARE ABOAH. Reference: miniproiotgroup. Transaction ID: 83724298138."
     */
    private val cashInPatternLegacy = Pattern.compile(
        "Payment\\s+received\\s+for\\s+GHS\\s+([\\d,.]+)\\s+from\\s+([^.]+?)(?:\\.\\s+Reference:|\\s+Reference:)\\s+(.*?)\\.\\s+Transaction\\s+ID:\\s+(\\d+)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Telecel (T-CASH) Pattern:
     * "0000013444988342 Confirmed. You have received GHS1.00 from MTN MOBILE MONEY with transaction reference: Transfer From: 233596524221-DANDELON BENTUM  on 2026-06-21 at 11:20:28. Your Telecel Cash balance is GHS2.39. Ref: day."
     */
    private val telecelPattern = Pattern.compile(
        "(\\d+)\\s+Confirmed\\.\\s+You\\s+have\\s+received\\s+GHS([\\d,.]+)\\s+from\\s+(.+?)\\s+with\\s+transaction\\s+reference:\\s+(.+?)\\s+on\\s+(\\d{4}-\\d{2}-\\d{2})\\s+at\\s+(\\d{2}:\\d{2}:\\d{2})\\.\\s+Your\\s+Telecel\\s+Cash\\s+balance\\s+is\\s+GHS([\\d,.]+)\\.\\s+Ref:\\s+(.*?)\\.",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(smsBody: String, timestamp: Long, address: String): TransactionEntity? {
        val network = detectNetwork(address, smsBody) ?: "MTN"
        
        // Try Telecel Pattern if network is Telecel or if it matches
        val telecelMatcher = telecelPattern.matcher(smsBody)
        if (telecelMatcher.find()) {
            var name = telecelMatcher.group(3)?.trim() ?: "Unknown"
            val reference = telecelMatcher.group(4)?.trim()
            
            // If it's a cross-network transfer, the real name might be in the reference
            if (name.contains("MTN MOBILE MONEY", ignoreCase = true) && reference?.contains("-") == true) {
                name = reference.substringAfterLast("-").trim()
            }

            return TransactionEntity(
                transactionId = telecelMatcher.group(1) ?: "",
                name = name,
                amount = telecelMatcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0,
                reference = telecelMatcher.group(8)?.trim() ?: reference,
                transactionType = "CASH_IN",
                network = "Telecel",
                balance = telecelMatcher.group(7)?.replace(",", "")?.toDoubleOrNull(),
                smsBody = smsBody,
                timestamp = timestamp
            )
        }

        // Try New Format first (more detailed)
        val newMatcher = cashInPatternNew.matcher(smsBody)
        if (newMatcher.find()) {
            return TransactionEntity(
                transactionId = newMatcher.group(5) ?: "",
                name = newMatcher.group(2)?.trim() ?: "Unknown",
                amount = newMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0,
                reference = newMatcher.group(4)?.trim(),
                transactionType = "CASH_IN",
                network = network ?: "MTN",
                balance = newMatcher.group(3)?.replace(",", "")?.toDoubleOrNull(),
                smsBody = smsBody,
                timestamp = timestamp
            )
        }

        // Fallback to Legacy Format
        val legacyMatcher = cashInPatternLegacy.matcher(smsBody)
        if (legacyMatcher.find()) {
            return TransactionEntity(
                transactionId = legacyMatcher.group(4) ?: "",
                name = legacyMatcher.group(2)?.trim() ?: "Unknown",
                amount = legacyMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0,
                reference = legacyMatcher.group(3)?.trim(),
                transactionType = "CASH_IN",
                network = network ?: "MTN",
                balance = null,
                smsBody = smsBody,
                timestamp = timestamp
            )
        }

        return null
    }

    private fun detectNetwork(address: String, body: String): String? {
        val addr = address.uppercase()
        if (addr == "MOBILEMONEY") return "MTN"
        if (addr == "T-CASH" || addr == "T_CASH") return "Telecel"
        
        val content = (address + body).lowercase()
        return when {
            content.contains("momo") || content.contains("mtn") -> "MTN"
            content.contains("telecel") || content.contains("vodafone") || content.contains("t-cash") || content.contains("t_cash") -> "Telecel"
            content.contains("airteltigo") || content.contains("tigo") || content.contains("airtel") -> "AirtelTigo"
            else -> null
        }
    }
}
