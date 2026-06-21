package com.example.streetpay

import com.example.streetpay.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmsParserTest {
    @Test
    fun testMtnNewFormatCashIn() {
        val sms = "Payment received for GHS 1.00 from MARTIN AFFUL  Current Balance: GHS 180.53 . Available Balance: GHS 180.53. Reference: A. Transaction ID: 83782557326. TRANSACTION FEE: 0.00"
        val transaction = SmsParser.parse(sms, 123456789L, "Momo")
        
        assertNotNull(transaction)
        assertEquals("83782557326", transaction?.transactionId)
        assertEquals(1.0, transaction?.amount ?: 0.0, 0.0)
        assertEquals("MARTIN AFFUL", transaction?.name)
        assertEquals("A", transaction?.reference)
        assertEquals(180.53, transaction?.balance ?: 0.0, 0.0)
        assertEquals("CASH_IN", transaction?.transactionType)
        assertEquals("MTN", transaction?.network)
    }

    @Test
    fun testMtnLegacyCashIn() {
        val sms = "Payment received for GHS 101.00 from PRISCILLA DEBRAH ASARE ABOAH. Reference: miniproiotgroup. Transaction ID: 83724298138."
        val transaction = SmsParser.parse(sms, 123456789L, "Momo")
        
        assertNotNull(transaction)
        assertEquals("83724298138", transaction?.transactionId)
        assertEquals(101.0, transaction?.amount ?: 0.0, 0.0)
        assertEquals("PRISCILLA DEBRAH ASARE ABOAH", transaction?.name)
        assertEquals("miniproiotgroup", transaction?.reference)
    }

    @Test
    fun testTelecelCashIn() {
        val sms = "0000013444988342 Confirmed. You have received GHS1.00 from MTN MOBILE MONEY with transaction reference: Transfer From: 233596524221-DANDELON BENTUM  on 2026-06-21 at 11:20:28. Your Telecel Cash balance is GHS2.39. Ref: day."
        val transaction = SmsParser.parse(sms, 123456789L, "T-CASH")
        
        assertNotNull(transaction)
        assertEquals("0000013444988342", transaction?.transactionId)
        assertEquals(1.0, transaction?.amount ?: 0.0, 0.0)
        assertEquals("DANDELON BENTUM", transaction?.name)
        assertEquals("day", transaction?.reference)
        assertEquals(2.39, transaction?.balance ?: 0.0, 0.0)
        assertEquals("Telecel", transaction?.network)
    }
}
