package com.cosmiclaboratory.axiom.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AxiomNotifications {
    const val CHANNEL_REFLECTION = "axiom.reflection"
    const val CHANNEL_REFLECTION_NAME = "Reflection reminders"
    const val CHANNEL_REFLECTION_DESCRIPTION =
        "Gentle nudge at your chosen time when you haven't written today."

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_REFLECTION) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REFLECTION,
                    CHANNEL_REFLECTION_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_REFLECTION_DESCRIPTION
                }
            )
        }
    }
}
