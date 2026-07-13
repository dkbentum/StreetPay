package com.example.streetpay.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

object SoundManager {
    fun playNotificationSound(context: Context, soundUri: String? = null) {
        try {
            val uri = if (soundUri != null) Uri.parse(soundUri) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, uri)
            r.play()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound", e)
        }
    }
}
