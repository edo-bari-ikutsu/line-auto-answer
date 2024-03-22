package com.bari_ikutsu.lnautoanswer.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.IntentCompat

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "com.bari_ikutsu.lnautoanswer.NOTIFICATION_ACTION"
        private const val EXTRA_PENDING_INTENT = "pendingIntent"

        /**
         * Create an intent to be used as a pending intent for the notification action
         */
        fun createIntent(
            context: Context,
            pendingIntent: PendingIntent
        ): Intent {
            val intent = Intent(ACTION)
            intent.setPackage(context.packageName)
            intent.putExtra(EXTRA_PENDING_INTENT, pendingIntent)
            return intent
        }
    }

    /**
     * When the notification action is clicked, send the content intent and cancel the notification
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) {
            val pendingIntent =
                IntentCompat.getParcelableExtra(intent, EXTRA_PENDING_INTENT, PendingIntent::class.java)
            pendingIntent?.send()
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}