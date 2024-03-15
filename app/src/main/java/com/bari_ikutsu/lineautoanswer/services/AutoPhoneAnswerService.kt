package com.bari_ikutsu.lineautoanswer.services

import android.app.Notification
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import androidx.core.os.LocaleListCompat
import com.bari_ikutsu.lineautoanswer.AutoAnswerMode
import com.bari_ikutsu.lineautoanswer.R
import com.bari_ikutsu.lineautoanswer.data.PrefStore
import com.bari_ikutsu.lineautoanswer.receivers.NotificationActionReceiver
import com.bari_ikutsu.lineautoanswer.receivers.NotificationCancelReceiver
import com.bari_ikutsu.lineautoanswer.utils.Consts
import com.bari_ikutsu.lineautoanswer.utils.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Random

class AutoPhoneAnswerService : NotificationListenerService() {
    private val TAG = "LineAutoAnswer:AutoPhoneAnswerService"

    private lateinit var tts: TextToSpeech
    private lateinit var prefStore: PrefStore

    private var isHeadsetPlugged = false

    /**
     * BroadcastReceiver for detecting headphone plug/unplug events
     */
    inner class HeadphonePlugBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                if (state == 0) {
                    Log.d(TAG, "Headset is unplugged")
                    isHeadsetPlugged = false
                } else if (state == 1) {
                    Log.d(TAG, "Headset is plugged")
                    isHeadsetPlugged = true
                }
            }
        }
    }

    private val headphonePlugBroadcastReceiver = HeadphonePlugBroadcastReceiver()

    private var isBluetoothHeadsetConnected = false

    /**
     * BroadcastReceiver for detecting Bluetooth headset connection/disconnection events
     */
    inner class BluetoothHeadsetConnectStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED") {
                val state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1)
                if (state == 0) {
                    Log.d(TAG, "Bluetooth headset is disconnected")
                    isBluetoothHeadsetConnected = false
                } else if (state == 2) {
                    Log.d(TAG, "Bluetooth headset is connected")
                    isBluetoothHeadsetConnected = true
                }
            }
        }
    }

    private val bluetoothHeadsetConnectStateReceiver = BluetoothHeadsetConnectStateReceiver()

    /**
     * BluetoothHeadsetProfileServiceListener for detecting Bluetooth headset connection status
     */
    inner class BluetoothHeadsetProfileServiceListener : BluetoothProfile.ServiceListener {
        private var mBluetoothHeadset: BluetoothHeadset? = null
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = proxy as BluetoothHeadset
                val devices = mBluetoothHeadset?.connectedDevices
                if (devices != null && devices.isNotEmpty()) {
                    isBluetoothHeadsetConnected = true
                }
                Log.d(TAG, "Bluetooth headset connected status: $isBluetoothHeadsetConnected")
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null
            }
        }
    }

    private val bluetoothHeadsetProfileServiceListener = BluetoothHeadsetProfileServiceListener()

    private val notificationActionReceiver = NotificationActionReceiver()
    private val notificationCancelReceiver = NotificationCancelReceiver()

    override fun onCreate() {
        super.onCreate()
        // Register headphone plug/unplug receiver
        ContextCompat.registerReceiver(
            applicationContext,
            headphonePlugBroadcastReceiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG),
            ContextCompat.RECEIVER_EXPORTED
        )
        // Register Bluetooth headset connection state receiver
        ContextCompat.registerReceiver(
            applicationContext,
            bluetoothHeadsetConnectStateReceiver,
            IntentFilter("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"),
            ContextCompat.RECEIVER_EXPORTED
        )
        // Register notification action receiver
        ContextCompat.registerReceiver(
            applicationContext,
            notificationActionReceiver,
            IntentFilter(NotificationActionReceiver.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Register notification cancel receiver
        ContextCompat.registerReceiver(
            applicationContext,
            notificationCancelReceiver,
            IntentFilter(NotificationCancelReceiver.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Register Bluetooth headset profile service listener
        // to retrieve initial connection status of Bluetooth headset
        val bluetoothManager =
            ContextCompat.getSystemService(applicationContext, BluetoothManager::class.java)
        bluetoothManager?.getAdapter()?.getProfileProxy(
            applicationContext,
            bluetoothHeadsetProfileServiceListener,
            BluetoothProfile.HEADSET
        )

        prefStore = PrefStore(applicationContext)
        tts = TextToSpeech(applicationContext) { status: Int ->
            if (status != TextToSpeech.ERROR) {
                val locales = LocaleListCompat.getDefault()
                for (i in 0 until locales.size()) {
                    val locale = locales[i]
                    if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        tts.language = locale
                        return@TextToSpeech
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        applicationContext.unregisterReceiver(headphonePlugBroadcastReceiver)
        applicationContext.unregisterReceiver(bluetoothHeadsetConnectStateReceiver)
        applicationContext.unregisterReceiver(notificationActionReceiver)
        applicationContext.unregisterReceiver(notificationCancelReceiver)
        tts.shutdown()
        super.onDestroy()
    }

    /**
     * Called when a new notification is posted
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        // If the notification is not from LINE, do nothing
        if (sbn?.packageName != Consts.LINE_PACKAGENAME) {
            return
        }

        // Retrieve settings from DataStore
        var autoAnswerMode: Int
        var callTimeout: Float
        var enableTextToSpeech: Boolean
        var showIncomingNotification: Boolean
        var showongoingNotification: Boolean
        runBlocking(Dispatchers.IO) {
            autoAnswerMode = prefStore.getAutoAnswerMode.first()
            callTimeout = prefStore.getCallTimeout.first()
            enableTextToSpeech = prefStore.getEnableTextToSpeech.first()
            showIncomingNotification = prefStore.getShowIncomingNotification.first()
            showongoingNotification = prefStore.getShowOngoingNotification.first()
        }

        val notification = sbn.notification
        val bundle = notification.extras
        val title = bundle.getCharSequence(Notification.EXTRA_TITLE, "").toString()
        var text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT, "").toString()
        if ("" == text) {
            text = bundle.getCharSequence(Notification.EXTRA_TEXT, "").toString()
        }

        // If the notification is from LINE and it is a VoIP call, answer the call
        if (notification.channelId == Consts.LINE_VOIP_CHANNEL_ID_INCOMING) {

            // If the notification does not have any action, do nothing
            if (notification.actions == null || notification.actions.isEmpty()) {
                Log.d(TAG, "No action in notification")
                return
            }

            // Process for incoming call notification
            if (showIncomingNotification && autoAnswerMode == AutoAnswerMode.OFF.value) {
                NotificationManagerCompat.from(applicationContext).cancelAll()

                // Send two notifications, one for answering the call and one for declining the call
                run {
                    val notificationIcon =
                        NotificationUtil.getNotificationIconBitmap(
                            applicationContext,
                            notification,
                            R.drawable.ic_notification_phone_enabled
                        )
                    NotificationUtil.sendNotification(
                        applicationContext,
                        title,
                        "$text ${getString(R.string.tap_to_answer)}",
                        R.drawable.ic_notification_phone_enabled,
                        notificationIcon,
                        getColor(R.color.ic_notification_default),
                        R.drawable.ic_notification_phone_enabled,
                        getString(R.string.answer),
                        Random().nextInt(100000),
                        notification.actions[0].actionIntent,
                        Consts.NOTIFICATION_CHANNEL_ID
                    )
                }
                if (notification.actions.size >= 2) {
                    val notificationIcon =
                        NotificationUtil.getNotificationIconBitmap(
                            applicationContext,
                            notification,
                            R.drawable.ic_notification_phone_disabled
                        )
                    NotificationUtil.sendNotification(
                        applicationContext,
                        title,
                        "$text ${getString(R.string.tap_to_decline)}",
                        R.drawable.ic_notification_phone_disabled,
                        notificationIcon,
                        getColor(R.color.ic_notification_phone_disabled),
                        R.drawable.ic_notification_phone_disabled,
                        getString(R.string.decline),
                        Random().nextInt(100000),
                        notification.actions[1].actionIntent,
                        Consts.NOTIFICATION_CHANNEL_ID
                    )
                }
            }

            // If auto answer mode is off, do nothing
            if (autoAnswerMode == AutoAnswerMode.OFF.value) {
                return
            }

            // If auto answer mode is set to "When headset is plugged" and headset is not plugged, do nothing
            if (autoAnswerMode == AutoAnswerMode.WHEN_HEADSET.value
                && (!isHeadsetPlugged && !isBluetoothHeadsetConnected)
            ) {
                return
            }

            // Below is the process of answering the call
            val handler = HandlerCompat.createAsync(mainLooper)
            handler.postDelayed({
                // Assumes that first action of notification is action of answering call
                notification.actions[0].actionIntent.send()

                // Announce the sender of the call by speech analysis
                if (enableTextToSpeech) {
                    val params = Bundle()
                    params.putInt(
                        TextToSpeech.Engine.KEY_PARAM_STREAM,
                        AudioManager.STREAM_VOICE_CALL
                    )
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                    handler.postDelayed({
                        tts.speak(
                            getString(R.string.announce_call, title),
                            TextToSpeech.QUEUE_FLUSH,
                            params,
                            null
                        )
                    }, 1000)
                }
            }, (callTimeout * 1000).toLong())
        } else if (notification.channelId == Consts.LINE_VOIP_CHANNEL_ID_ONGOING) {
            // If the notification text contains keyword that represents making call, do nothing
            // Only process for ongoing call notification
            if (text.contains(getString(R.string.keyword_making_call))) {
                return
            }

            // Process for ongoing call notification
            if (showongoingNotification) {
                NotificationManagerCompat.from(applicationContext).cancelAll()
                val notificationIcon =
                    NotificationUtil.getNotificationIconBitmap(
                        applicationContext,
                        notification,
                        R.drawable.ic_notification_phone
                    )
                NotificationUtil.sendNotification(
                    applicationContext,
                    title,
                    "$text ${getString(R.string.tap_to_end_call)}",
                    R.drawable.ic_notification_phone,
                    notificationIcon,
                    getColor(R.color.ic_notification_default),
                    R.drawable.ic_notification_phone_disabled,
                    getString(R.string.end_call),
                    Random().nextInt(100000),
                    notification.actions[0].actionIntent,
                    Consts.NOTIFICATION_CHANNEL_ID
                )
            }
        }
    }
}
