package com.example.ridehailingsearchautomation

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import com.example.ridehailingsearchautomation.ui.theme.RideHailingSearchAutomationTheme
import androidx.core.net.toUri
import com.example.ridehailingsearchautomation.processors.GojekProcessor
import com.example.ridehailingsearchautomation.processors.GrabProcessor
import com.example.ridehailingsearchautomation.processors.TadaProcessor
import com.example.ridehailingsearchautomation.processors.ZigProcessor
import com.example.ridehailingsearchautomation.processors.UniversalRideScraperService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private var TAG = "MainActivityLogs"
private const val grabPackageName = "com.grabtaxi.passenger"
private const val gojekPackageName = "com.gojek.app"
private const val tadaPackageName = "io.mvlchain.tada"
private const val zigPackageName = "com.codigo.comfort"
private var isDualSearchActive = false

class MainActivity : ComponentActivity() {
    private var lastSearchedDestination = mutableStateOf("")

    // Price states
    private var grabPriceState = mutableStateOf("")
    private var gojekPriceState = mutableStateOf("")
    private var tadaPriceState = mutableStateOf("")
    private var zigPriceState = mutableStateOf("")

    // Last updated states
    private var lastUpdatedGrabState = mutableStateOf("")
    private var lastUpdatedGojekState = mutableStateOf("")
    private var lastUpdatedTadaState = mutableStateOf("")
    private var lastUpdatedZigState = mutableStateOf("")

    // Start times
    private var grabStartTime = 0L
    private var gojekStartTime = 0L
    private var tadaStartTime = 0L
    private var zigStartTime = 0L

    // Durations
    private var grabDuration = mutableStateOf("-")
    private var gojekDuration = mutableStateOf("-")
    private var tadaDuration = mutableStateOf("-")
    private var zigDuration = mutableStateOf("-")

    @SuppressLint("DefaultLocale")
    private fun calculateDuration(startTime: Long): String {
        if (startTime == 0L) return "-"
        val durationMs = System.currentTimeMillis() - startTime
        val seconds = durationMs / 1000.0
        return String.format("%.1fs", seconds)
    }

    private fun startTimerSequence() {
        grabDuration.value = "-"
        gojekDuration.value = "-"
        tadaDuration.value = "-"
        zigDuration.value = "-"
        grabStartTime = System.currentTimeMillis()
    }

    private val grabPriceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val price = intent?.getStringExtra("price_value")
            Log.d(TAG, "MainActivity received Grab price broadcast: $price")
            if (price != null) {
                grabDuration.value = calculateDuration(grabStartTime)
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                lastUpdatedGrabState.value = "$currentTime"
                grabPriceState.value = price

                if (isDualSearchActive) {
                    Log.d(TAG, "Grab done, launching Gojek")
                    gojekStartTime = System.currentTimeMillis()
                    openGojek(context!!, lastSearchedDestination.value)
                }
            }
        }
    }

    private val gojekPriceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val price = intent?.getStringExtra("price_value")
            Log.d(TAG, "MainActivity received Gojek price broadcast: $price")
            if (price != null) {
                gojekDuration.value = calculateDuration(gojekStartTime)
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                lastUpdatedGojekState.value = "$currentTime"
                gojekPriceState.value = price

                if (isDualSearchActive) {
                    Log.d(TAG, "Gojek done, launching Tada")
                    tadaStartTime = System.currentTimeMillis()
                    openTada(context!!, lastSearchedDestination.value)
                }
            }
        }
    }

    private val tadaPriceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val price = intent?.getStringExtra("price_value")
            Log.d(TAG, "MainActivity received Tada price broadcast: $price")
            if (price != null) {
                tadaDuration.value = calculateDuration(tadaStartTime)
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                lastUpdatedTadaState.value = "$currentTime"
                tadaPriceState.value = price

                if (isDualSearchActive) {
                    Log.d(TAG, "Tada done, launching Zig")
                    zigStartTime = System.currentTimeMillis()
                    openZig(context!!, lastSearchedDestination.value)
                }
            }
        }
    }

    private val zigPriceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val price = intent?.getStringExtra("price_value")
            Log.d(TAG, "MainActivity received Zig price broadcast: $price")
            if (price != null) {
                zigDuration.value = calculateDuration(zigStartTime)
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                lastUpdatedZigState.value = "$currentTime"
                zigPriceState.value = price

                isDualSearchActive = false
                Log.d(TAG, "Dual search done")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate called")

        val grabFilter = IntentFilter("COM_EXAMPLE_GRAB_PRICE_UPDATE")
        registerReceiver(grabPriceReceiver, grabFilter, RECEIVER_EXPORTED)
        val gojekFilter = IntentFilter("COM_EXAMPLE_GOJEK_PRICE_UPDATE")
        registerReceiver(gojekPriceReceiver, gojekFilter, RECEIVER_EXPORTED)
        val tadaFilter = IntentFilter("COM_EXAMPLE_TADA_PRICE_UPDATE")
        registerReceiver(tadaPriceReceiver, tadaFilter, RECEIVER_EXPORTED)
        val zigFilter = IntentFilter("COM_EXAMPLE_ZIG_PRICE_UPDATE")
        registerReceiver(zigPriceReceiver, zigFilter, RECEIVER_EXPORTED)
        val resetIntent = Intent("ACTION_GLOBAL_RESET_PROCESSORS")
        sendBroadcast(resetIntent)
        Log.d(TAG, "onCreate: Resetting processors")

        setContent {
            RideHailingSearchAutomationTheme {
                RHSAApp(
                    lastSearchedDestination = lastSearchedDestination,
                    grabPriceState = grabPriceState,
                    gojekPriceState = gojekPriceState,
                    tadaPriceState = tadaPriceState,
                    zigPriceState = zigPriceState,
                    lastUpdatedGrabState = lastUpdatedGrabState,
                    lastUpdatedGojekState = lastUpdatedGojekState,
                    lastUpdatedTadaState = lastUpdatedTadaState,
                    lastUpdatedZigState = lastUpdatedZigState,
                    grabDuration = grabDuration,
                    gojekDuration = gojekDuration,
                    tadaDuration = tadaDuration,
                    zigDuration = zigDuration,
                    onSearchTriggered = { startTimerSequence() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(grabPriceReceiver)
        unregisterReceiver(gojekPriceReceiver)
        unregisterReceiver(tadaPriceReceiver)
        unregisterReceiver(zigPriceReceiver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RHSAApp(
    lastSearchedDestination: MutableState<String>,
    grabPriceState: MutableState<String>,
    gojekPriceState: MutableState<String>,
    tadaPriceState: MutableState<String>,
    zigPriceState: MutableState<String>,
    lastUpdatedGrabState: MutableState<String>,
    lastUpdatedGojekState: MutableState<String>,
    lastUpdatedTadaState: MutableState<String>,
    lastUpdatedZigState: MutableState<String>,
    grabDuration: MutableState<String>,
    gojekDuration: MutableState<String>,
    tadaDuration: MutableState<String>,
    zigDuration: MutableState<String>,
    onSearchTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf("510190") }
    val context = LocalContext.current
    val fetchingPriceText = stringResource(R.string.fetching_price)
    var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            val currStatus = isAccessibilityServiceEnabled(context)
            if (isServiceEnabled != currStatus) {
                isServiceEnabled = currStatus
            }
            kotlinx.coroutines.delay(500)
        }
    }
//    val isServiceEnabled = true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = colorResource(R.color.off_white)
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = colorResource(R.color.floral_green)
                )
            )
        },
        containerColor = colorResource(R.color.light_grey)
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (lastSearchedDestination.value.isBlank()) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Destination search field
                SearchField(
                    destination = destination,
                    onDestinationChange = { destination = it },
                    onSearchButtonClicked = {
                        if (destination.isNotBlank()) {
                            isDualSearchActive = true
                            grabPriceState.value = fetchingPriceText
                            gojekPriceState.value = fetchingPriceText
                            tadaPriceState.value = fetchingPriceText
                            zigPriceState.value = fetchingPriceText

                            lastSearchedDestination.value = destination
                            onSearchTriggered()
                            openGrab(context, destination)
                        }
                    }
                )

                if (lastSearchedDestination.value.isBlank()) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Last searched destination
                    Text(
                        text = "Results for: ${lastSearchedDestination.value}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Grab price display
                    RideFareRow(
                        logoId = R.drawable.grab_logo,
                        appName = "Grab",
                        price = grabPriceState.value,
                        lastUpdated = lastUpdatedGrabState.value,
                        duration = grabDuration.value,
                        onSwitchToApp = { switchToGrab(context) },
                        lastSearchedDestination = lastSearchedDestination,
                    )

                    // Gojek price display
                    RideFareRow(
                        logoId = R.drawable.gojek_logo,
                        appName = "Gojek",
                        price = gojekPriceState.value,
                        lastUpdated = lastUpdatedGojekState.value,
                        duration = gojekDuration.value,
                        onSwitchToApp = { switchToGojek(context) },
                        lastSearchedDestination = lastSearchedDestination,
                    )

//             Tada price display
                    RideFareRow(
                        logoId = R.drawable.tada_logo,
                        appName = "Tada",
                        price = tadaPriceState.value,
                        lastUpdated = lastUpdatedTadaState.value,
                        duration = tadaDuration.value,
                        onSwitchToApp = { switchToTada(context) },
                        lastSearchedDestination = lastSearchedDestination,
                    )

                    // Zig price display
                    RideFareRow(
                        logoId = R.drawable.zig_logo,
                        appName = "Zig",
                        price = zigPriceState.value,
                        lastUpdated = lastUpdatedZigState.value,
                        duration = zigDuration.value,
                        onSwitchToApp = { switchToZig(context) },
                        lastSearchedDestination = lastSearchedDestination,
                    )
                }

                // App version/date
                Text(
                    text = "App updated on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                    fontSize = 10.sp,
                    color = colorResource(R.color.black)
                )
            }
            ServiceStatusBadge(
                isEnabled = isServiceEnabled,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp) // Offset from the edge
            )
        }
    }
}

@Composable
fun ServiceStatusBadge(
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .clickable {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
        color = colorResource(id = R.color.off_white),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isEnabled) "✅" else "❌",
                fontSize = 12.sp
            )
            Text(
                text = if (isEnabled) "Service Active" else "Service Disabled",
                color = if (isEnabled) colorResource(R.color.floral_green) else androidx.compose.ui.graphics.Color.Red,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SearchField(
    destination: String,
    onDestinationChange: (String) -> Unit,
    onSearchButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = destination,
        onValueChange = onDestinationChange,
        label = { Text(stringResource(R.string.enter_destination_field)) }
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onSearchButtonClicked,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 6.dp
        ),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.floral_green)
        )
    ) {
        Text("Check Prices")
    }
}

@Composable // Composable functions returning Unit should start with uppercase letter
fun RideFareRow(
    logoId: Int,
    appName: String,
    price: String,
    lastUpdated: String,
    duration: String,
    onSwitchToApp: () -> Unit,
    lastSearchedDestination: MutableState<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = colorResource(id = R.color.off_white),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // internal padding within card
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App logo
            Image(
                painter = painterResource(logoId),
                contentDescription = "$appName Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Current fare
                Text(
                    text = price,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 14.sp,
                    color = if (price == "Timeout") colorResource(R.color.red) else colorResource(R.color.black),
                )

                // Last updated at
                if (lastUpdated.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.last_updated_at)}: $lastUpdated",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 12.sp,
                    )
                }

                // Duration
                if (duration.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.duration)}: $duration",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 12.sp,
                    )
                }
            }
            // Open app button
            if (lastSearchedDestination.value.isNotBlank()) {
                Button(
                    onClick = onSwitchToApp,
                    modifier = Modifier,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.floral_green)),
                ) {
                    Text("Open")
                }
            }
        }
    }
}


fun openAppByDeeplink(context: Context, deeplink: String, appName: String) {
    val uri = deeplink.toUri()
    val intent = Intent(Intent.ACTION_VIEW,  uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "$appName app not found", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "$appName app not found, $e")
    }
}

fun openAppByDirectLaunch(context: Context, packageName: String, appName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        launchIntent.replaceExtras(Bundle())

        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        try {
            context.startActivity(launchIntent)
            Log.d(TAG, "Opening $appName")
        } catch (e: Exception) {
            Toast.makeText(context, "$appName app not found", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "$appName app not found, $e")
        }
    }
}

fun switchToApp(context: Context, packageName: String, appName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not switch to $appName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Could not switch to $appName, $e")
        }
    } else {
        Log.d(TAG, "$appName app not found")
    }
}

fun openGrab(context: Context, destination: String) {
    GrabProcessor.destinationToType = destination
    openAppByDeeplink(context, "grab://open?screenType=BOOKING", "Grab")
}

fun switchToGrab(context: Context) {
    switchToApp(context, grabPackageName, "Grab")
}

fun openGojek(context: Context, destination: String) {
    GojekProcessor.destinationToType = destination
    openAppByDirectLaunch(context, gojekPackageName, "Gojek")
}

fun switchToGojek(context: Context) {
    switchToApp(context, gojekPackageName, "Gojek")
}

fun openTada(context: Context, destination: String) {
    TadaProcessor.destinationToType = destination
    openAppByDirectLaunch(context, tadaPackageName, "Tada")
}

fun switchToTada(context: Context) {
    switchToApp(context, tadaPackageName, "Tada")
}

fun openZig(context: Context, destination: String) {
    ZigProcessor.destinationToType = destination
    openAppByDirectLaunch(context, zigPackageName, "Zig")
}

fun switchToZig(context: Context) {
    switchToApp(context, zigPackageName, "Zig")
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, UniversalRideScraperService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.contains(expectedComponentName.flattenToString())
}

@Preview(showBackground = true)
@Composable
fun RHSAPreview() {
    val lastSearchedDestination = remember { mutableStateOf("")}

    val grabPriceState = remember { mutableStateOf("S$10.10")}
    val gojekPriceState = remember { mutableStateOf("S$11.11")}
    val tadaPriceState = remember { mutableStateOf("S$12.12") }
    val zigPriceState = remember { mutableStateOf("S13.13") }

    val lastUpdatedGrabState = remember { mutableStateOf("10:10:10")}
    val lastUpdatedGojekState = remember { mutableStateOf("11:11:11")}
    val lastUpdatedTadaState = remember { mutableStateOf("12:12:12") }
    val lastUpdatedZigState = remember { mutableStateOf("13:13:13") }

    val grabDuration = remember { mutableStateOf("10.10s") }
    val gojekDuration = remember { mutableStateOf("11.11s") }
    val tadaDuration = remember { mutableStateOf("12.12s") }
    val zigDuration = remember { mutableStateOf("13.13s") }

    val onSearchTriggered = {}

    RideHailingSearchAutomationTheme {
        RHSAApp(
            lastSearchedDestination,
            grabPriceState,
            gojekPriceState,
            tadaPriceState,
            zigPriceState,
            lastUpdatedGrabState,
            lastUpdatedGojekState,
            lastUpdatedTadaState,
            lastUpdatedZigState,
            grabDuration,
            gojekDuration,
            tadaDuration,
            zigDuration,
            onSearchTriggered
        )
    }
}