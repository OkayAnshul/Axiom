package com.cosmiclaboratory.axiom.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AxiomNotifications {
    /**
     * One channel, because there is only one kind of notification: the
     * companion saying something. Check-ins and the weekly look back are the
     * same voice, and splitting them would ask the user to reason about
     * plumbing rather than about who is talking to them.
     */
    const val CHANNEL_COMPANION = "axiom.companion"
    const val CHANNEL_COMPANION_NAME = "Messages from your companion"
    const val CHANNEL_COMPANION_DESCRIPTION =
        "Check-ins and weekly reflections, at most one a day."

    /** Pre-Phase-10 channel, superseded by [CHANNEL_COMPANION]. */
    private const val LEGACY_CHANNEL_REFLECTION = "axiom.reflection"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_COMPANION) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_COMPANION,
                    CHANNEL_COMPANION_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_COMPANION_DESCRIPTION
                }
            )
        }
        // Leaving it behind would show users a dead toggle in system settings.
        if (manager.getNotificationChannel(LEGACY_CHANNEL_REFLECTION) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_REFLECTION)
        }
    }
}
