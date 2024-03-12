package com.bari_ikutsu.lineautoanswer.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationUtil {
    companion object {

        /**
         * Send a notification
         */
        @SuppressLint("MissingPermission")
        fun sendNotification(
            context: Context,
            title: String,
            message: String,
            smallIcon: Int,
            largeIcon: Bitmap,
            color: Int,
            conversationId: Int,
            contentIntent: PendingIntent,
            notificationChannelId: String
        ) {
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(context, notificationChannelId)
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setColor(color)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)

            NotificationManagerCompat.from(context).notify(conversationId, builder.build())
        }

        /**
         * Get the best notification icon (large, small, default) and return it as bitmap
         */
        fun getNotificationIconBitmap(
            context: Context,
            notification: Notification,
            defaultIcon: Int
        ): Bitmap {
            var bmp: Bitmap? = null
            var icon = notification.getLargeIcon()
            if (icon == null) {
                icon = notification.smallIcon
            }
            if (icon != null) {
                bmp = drawableToBitMap(icon.loadDrawable(context))
            }
            if (bmp == null) {
                bmp = BitmapFactory.decodeResource(context.resources, defaultIcon)
            }
            return bmp as Bitmap
        }

        /**
         * Convert a drawable to a bitmap
         */
        private fun drawableToBitMap(drawable: Drawable?): Bitmap? {
            if (drawable == null) {
                return null
            }
            return if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                drawable.draw(canvas)
                bitmap
            }
        }
    }
}
