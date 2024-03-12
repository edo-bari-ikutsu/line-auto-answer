package com.bari_ikutsu.lineautoanswer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.bari_ikutsu.lineautoanswer.data.PrefStore
import com.bari_ikutsu.lineautoanswer.ui.theme.LINEAutoAnswerTheme
import com.bari_ikutsu.lineautoanswer.utils.Consts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AutoAnswerMode(val value: Int) {
    OFF(0),
    ALWAYS(1),
    WHEN_HEADSET(2)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LINEAutoAnswerTheme {
                Page(
                    tryToGetPermission = { tryToGetPermission() },
                    versionName = packageManager.getPackageInfo(packageName, 0).versionName
                )
            }
        }

        // permissions to request
        var permissionsToRequest = arrayOf<String>()
        // add BLUETOOTH_CONNECT permission if needed for Android 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest += android.Manifest.permission.BLUETOOTH_CONNECT
        }
        // add POST_NOTIFICATIONS permission if needed for Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest += android.Manifest.permission.POST_NOTIFICATIONS
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, 0)
        }

        // Prepare notification channels
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannel(
                Consts.NOTIFICATION_CHANNEL_ID,
                Consts.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val enabled =
            NotificationManagerCompat.getEnabledListenerPackages(this@MainActivity).contains(
                packageName
            )

        val prefStore = PrefStore(this)
        CoroutineScope(Dispatchers.IO).launch {
            prefStore.saveNotificationAccess(enabled)
        }
    }

    private fun tryToGetPermission() {
        val enabled =
            NotificationManagerCompat.getEnabledListenerPackages(this@MainActivity).contains(
                packageName
            )

        if (!enabled) {
            ActivityCompat.startActivityForResult(
                this,
                Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS),
                0,
                null
            )
        }
    }
}

@Composable
fun Page(tryToGetPermission: () -> Unit, versionName: String) {
    val prefStore = PrefStore(LocalContext.current)
    val enableTextToSpeech = prefStore.getEnableTextToSpeech.collectAsState(initial = true)
    val showIncomingNotification = prefStore.getShowIncomingNotification.collectAsState(initial = true)
    val showOngoingNotification = prefStore.getShowOngoingNotification.collectAsState(initial = true)

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TitleBar(title = stringResource(id = R.string.app_name))
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Card (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.system_settings))
                    NotificationReadPermitted(tryToGetPermission)
                }

//                FunctionHeader(title = stringResource(id = R.string.system_settings))
//                NotificationReadPermitted(tryToGetPermission)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.basic_settings))
                    SelectAutoAnswerMode()
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.delay_of_auto_answer))
                    CallTimeOut()
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.advanced_settings))
                    SwitchSetting(checked = enableTextToSpeech,
                        text = stringResource(id = R.string.enable_text_to_speech),
                        onCheckedChange = {
                            CoroutineScope(Dispatchers.IO).launch {
                                prefStore.saveEnableTextToSpeech(it)
                            }
                        }
                    )
                    SwitchSetting(
                        checked = showIncomingNotification,
                        text = stringResource(id = R.string.show_incoming_notification),
                        onCheckedChange = {
                            CoroutineScope(Dispatchers.IO).launch {
                                prefStore.saveShowIncomingNotification(it)
                            }
                        }
                    )
                    SwitchSetting(
                        checked = showOngoingNotification,
                        text = stringResource(id = R.string.show_ongoing_notification),
                        onCheckedChange = {
                            CoroutineScope(Dispatchers.IO).launch {
                                prefStore.saveShowOngoingNotification(it)
                            }
                        }
                    )
                    Text(
                        text = stringResource(id = R.string.incoming_notification_note),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 12.dp)
                            .align(Alignment.End)

                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    FunctionHeader(title = stringResource(id = R.string.about_this_app))
                    VersionAndCopyright(versionName = versionName)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(title: String) {
    Surface(color = Color.Red) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = contentColorFor(MaterialTheme.colorScheme.primary)
            ),
            title = {
                Text(
                    text = title
                )
            }
        )
    }
}

@Composable
fun FunctionHeader(title: String) {
    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun NotificationReadPermitted(tryToGetPermission: () -> Unit) {
    val prefStore = PrefStore(LocalContext.current)
    val isPermitted = prefStore.getNotificationAccess.collectAsState(initial = false)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Switch(
            checked = isPermitted.value,
            onCheckedChange = {
                tryToGetPermission()
            }
        )
        Text(
            stringResource(id = R.string.notification_read_permitted),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun SelectAutoAnswerMode() {
    val prefStore = PrefStore(LocalContext.current)
    val selectValue =
        prefStore.getAutoAnswerMode.collectAsState(initial = AutoAnswerMode.ALWAYS.value)
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        SelectAutoAnswerModeElem(
            selected = selectValue.value == AutoAnswerMode.ALWAYS.value,
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    prefStore.saveAutoAnswerMode(AutoAnswerMode.ALWAYS.value)
                }
            },
            text = stringResource(id = R.string.auto_answer_always)
        )
        SelectAutoAnswerModeElem(
            selected = selectValue.value == AutoAnswerMode.WHEN_HEADSET.value,
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    prefStore.saveAutoAnswerMode(AutoAnswerMode.WHEN_HEADSET.value)
                }
            },
            text = stringResource(id = R.string.auto_answer_headset)
        )
        SelectAutoAnswerModeElem(
            selected = selectValue.value == AutoAnswerMode.OFF.value,
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    prefStore.saveAutoAnswerMode(AutoAnswerMode.OFF.value)
                }
            },
            text = stringResource(id = R.string.auto_answer_disabled)
        )
    }
}

@Composable
fun SelectAutoAnswerModeElem(selected: Boolean, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text)
    }
}

@Composable
fun CallTimeOut() {
    val prefStore = PrefStore(LocalContext.current)
    val sliderValue = prefStore.getCallTimeout.collectAsState(initial = 3.0f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp)
    ) {
        Slider(
            value = sliderValue.value,
            onValueChange = {
                CoroutineScope(Dispatchers.IO).launch {
                    prefStore.saveCallTimeout(it)
                }
            },
            valueRange = 0f..10f
        )
        Text(
            "%.1f ".format(sliderValue.value) + stringResource(id = R.string.seconds)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SwitchSetting(checked: State<Boolean>, text: String, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Switch(
            checked = checked.value,
            onCheckedChange = onCheckedChange
        )
        Text(
        text,
            modifier = Modifier.padding(start = 8.dp, end = 12.dp)
        )
    }
}

@Composable
fun VersionAndCopyright(versionName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, bottom = 8.dp)
    ) {
        Text("Version $versionName")
        Text(stringResource(id = R.string.copyright))
    }
}

@Preview(showBackground = true)
@Composable
fun PagePreview() {
    LINEAutoAnswerTheme {
        Page(
            tryToGetPermission = {},
            versionName = "0.1"
        )
    }
}
