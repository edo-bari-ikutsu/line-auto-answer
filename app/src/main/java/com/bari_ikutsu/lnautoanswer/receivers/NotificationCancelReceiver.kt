package com.bari_ikutsu.lnautoanswer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class NotificationCancelReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.bari_ikutsu.lnautoanswer.NOTIFICATION_CANCEL"
        /**
         * Create an intent to cancel all notifications
         */
        fun createIntent(context: Context): Intent {
            val intent = Intent(ACTION)
            intent.setPackage(context.packageName)
            return intent
        }
    }

    /**
     * Cancel all notifications
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}