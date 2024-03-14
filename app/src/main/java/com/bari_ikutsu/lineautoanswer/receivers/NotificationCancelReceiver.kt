package com.bari_ikutsu.lineautoanswer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class NotificationCancelReceiver : BroadcastReceiver() {

    companion object {
        /**
         * Create an intent to cancel all notifications
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, NotificationCancelReceiver::class.java)
        }
    }

    /**
     * Cancel all notifications
     */
    override fun onReceive(context: Context, intent: Intent) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}