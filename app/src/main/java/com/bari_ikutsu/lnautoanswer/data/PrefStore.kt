package com.bari_ikutsu.lnautoanswer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrefStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("userToken")
        private val NOTIFICATION_ACCESS_KEY = booleanPreferencesKey("notificationAccess")
        private val AUTO_ANSWER_MODE_KEY = intPreferencesKey("autoAnswerMode")
        private val CALL_TIMEOUT_KEY = floatPreferencesKey("callTimeout")
        private val ENABLE_TEXT_TO_SPEECH_KEY = booleanPreferencesKey("enableTextToSpeech")
        private val SHOW_INCOMING_NOTIFICATIONS_KEY =
            booleanPreferencesKey("showIncomingNotifications")
        private val MERGE_INCOMING_NOTIFICATIONS_KEY =
            booleanPreferencesKey("mergeIncomingNotifications")
        private val SHOW_ONGOING_NOTIFICATION_KEY = booleanPreferencesKey("showOngoingNotification")
    }

    val getNotificationAccess: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_ACCESS_KEY] ?: false
    }
    val getAutoAnswerMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AUTO_ANSWER_MODE_KEY] ?: 0
    }
    val getCallTimeout: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[CALL_TIMEOUT_KEY] ?: 3.0f
    }
    val getEnableTextToSpeech: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_TEXT_TO_SPEECH_KEY] ?: true
    }
    val getShowIncomingNotifications: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_INCOMING_NOTIFICATIONS_KEY] ?: true
    }
    val getMergeIncomingNotifications: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MERGE_INCOMING_NOTIFICATIONS_KEY] ?: true
    }
    val getShowOngoingNotification: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_ONGOING_NOTIFICATION_KEY] ?: true
    }

    suspend fun saveNotificationAccess(notificationAccess: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ACCESS_KEY] = notificationAccess
        }
    }

    suspend fun saveAutoAnswerMode(answerMode: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_ANSWER_MODE_KEY] = answerMode
        }
    }

    suspend fun saveCallTimeout(callTimeout: Float) {
        context.dataStore.edit { preferences ->
            preferences[CALL_TIMEOUT_KEY] = callTimeout
        }
    }

    suspend fun saveEnableTextToSpeech(enableTextToSpeech: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_TEXT_TO_SPEECH_KEY] = enableTextToSpeech
        }
    }

    suspend fun saveShowIncomingNotifications(showIncomingNotification: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_INCOMING_NOTIFICATIONS_KEY] = showIncomingNotification
        }
    }

    suspend fun saveMergeIncomingNotifications(mergeIncomingNotification: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MERGE_INCOMING_NOTIFICATIONS_KEY] = mergeIncomingNotification
        }
    }

    suspend fun saveShowOngoingNotification(showOngoingNotification: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ONGOING_NOTIFICATION_KEY] = showOngoingNotification
        }
    }
}