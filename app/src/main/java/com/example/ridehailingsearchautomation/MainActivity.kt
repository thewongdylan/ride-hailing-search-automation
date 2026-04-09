package com.example.ridehailingsearchautomation

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridehailingsearchautomation.ui.theme.RideHailingSearchAutomationTheme
import androidx.core.net.toUri
import com.example.ridehailingsearchautomation.processors.GojekProcessor
import com.example.ridehailingsearchautomation.processors.GrabProcessor
import com.example.ridehailingsearchautomation.processors.TadaProcessor
import com.example.ridehailingsearchautomation.processors.ZigProcessor

private var TAG = "MainActivityLogs"
private const val grabPackageName = "com.grabtaxi.passenger"
private const val gojekPackageName = "com.gojek.app"
private const val tadaPackageName = "io.mvlchain.tada"
private const val zigPackageName = "com.codigo.comfort"
private var isDualSearchActive = false

class MainActivity : ComponentActivity() {
    private var lastSearchedDestination = mutableStateOf("")
    private var grabPriceState = mutableStateOf("-")
    private var lastUpdatedGrabState = mutableStateOf("")
    private var gojekPriceState = mutableStateOf("-")
    private var lastUpdatedGojekState = mutableStateOf("")
    private var tadaPriceState = mutableStateOf("-")
    private var lastUpdatedTadaState = mutableStateOf("")
    private var zigPriceState = mutableStateOf("-")
    private var lastUpdatedZigState = mutableStateOf("")

    private val grabPriceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val price = intent?.getStringExtra("price_value")
            Log.d(TAG, "MainActivity received Grab price broadcast: $price")
            if (price != null) {
                grabPriceState.value = price
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                lastUpdatedGrabState.value = "$currentTime"

                if (isDualSearchActive) {
                    Log.d(TAG, "Grab done, launching Gojek")
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
                gojekPriceState.value = price
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                lastUpdatedGojekState.value = "$currentTime"

                if (isDualSearchActive) {
                    Log.d(TAG, "Gojek done, launching Tada")
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
                tadaPriceState.value = price
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                lastUpdatedTadaState.value = "$currentTime"

                if (isDualSearchActive) {
                    Log.d(TAG, "Tada done, launching Zig")
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
                zigPriceState.value = price
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                lastUpdatedZigState.value = "$currentTime"

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

        setContent {
            RideHailingSearchAutomationTheme {
                RHSAApp(
                    lastSearchedDestination = lastSearchedDestination,
                    grabPriceState = grabPriceState,
                    lastUpdatedGrabState = lastUpdatedGrabState,
                    gojekPriceState = gojekPriceState,
                    lastUpdatedGojekState = lastUpdatedGojekState,
                    tadaPriceState = tadaPriceState,
                    lastUpdatedTadaState = lastUpdatedTadaState,
                    zigPriceState = zigPriceState,
                    lastUpdatedZigState = lastUpdatedZigState
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
    lastUpdatedGrabState: MutableState<String>,
    gojekPriceState: MutableState<String>,
    lastUpdatedGojekState: MutableState<String>,
    tadaPriceState: MutableState<String>,
    lastUpdatedTadaState: MutableState<String>,
    zigPriceState: MutableState<String>,
    lastUpdatedZigState: MutableState<String>,
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf("bedok mall") }
    val context = LocalContext.current
    val fetchingPriceText = stringResource(R.string.fetching_price)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Destination search bar
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it},
                label = { Text(stringResource(R.string.enter_destination_field)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search button
            Button(
                onClick = {
                    if (destination.isNotBlank()) {
                        isDualSearchActive = true
                        grabPriceState.value = fetchingPriceText
                        gojekPriceState.value = fetchingPriceText
                        tadaPriceState.value = fetchingPriceText
                        zigPriceState.value = fetchingPriceText
                        lastSearchedDestination.value = destination
                        openGrab(context, destination)
                    }
                }
            ) {
                Text("Search")
            }

            // Last searched destination
            if (lastSearchedDestination.value.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grab price display
            RideFareRow(
                R.drawable.grab_logo,
                "Grab",
                grabPriceState.value,
                lastUpdatedGrabState.value,
                { switchToGrab(context) },
                lastSearchedDestination,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gojek price display
            RideFareRow(
                R.drawable.gojek_logo,
                "Gojek",
                gojekPriceState.value,
                lastUpdatedGojekState.value,
                { switchToGojek(context) },
                lastSearchedDestination,
            )

            Spacer(modifier = Modifier.height(16.dp))

//             Tada price display
            RideFareRow(
                R.drawable.tada_logo,
                "Tada",
                tadaPriceState.value,
                lastUpdatedTadaState.value,
                { switchToTada(context) },
                lastSearchedDestination,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Zig price display
            RideFareRow(
                R.drawable.zig_logo,
                "Zig",
                zigPriceState.value,
                lastUpdatedZigState.value,
                { switchToZig(context) },
                lastSearchedDestination,
            )
        }
    }
}

@Composable // Composable functions returning Unit should start with uppercase letter
fun RideFareRow(
    logoId: Int,
    appName: String,
    price: String,
    lastUpdated: String,
    onSwitchToApp: () -> Unit,
    lastSearchedDestination: MutableState<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App logo
        Image(
            painter = painterResource(logoId),
            contentDescription = "$appName Logo",
            modifier = Modifier
                .size(45.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Current fare
            Text(
                text = "${stringResource(R.string.current_fare)}: $price",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 14.sp
            )

            // Last updated at
            Text(
                text = "${stringResource(R.string.last_updated_at)}: $lastUpdated",
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 14.sp
            )
        }
        // Open Tada button
        if (lastSearchedDestination.value.isNotBlank()) {
            Button(
                onClick = onSwitchToApp,
                modifier = Modifier.padding(vertical = 8.dp),
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 6.dp
                )
            ) {
                Text("Open $appName")
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


@Preview(showBackground = true)
@Composable
fun RHSAPreview() {
    val lastSearchedDestination = remember { mutableStateOf("")}
    val grabPriceState = remember { mutableStateOf("-")}
    val lastUpdatedGrabState = remember { mutableStateOf("")}
    val gojekPriceState = remember { mutableStateOf("-")}
    val lastUpdatedGojekState = remember { mutableStateOf("")}
    val tadaPriceState = remember { mutableStateOf("-") }
    val lastUpdatedTadaState = remember { mutableStateOf("") }
    val zigPriceState = remember { mutableStateOf("-") }
    val lastUpdatedZigState = remember { mutableStateOf("") }


    RideHailingSearchAutomationTheme {
        RHSAApp(
            lastSearchedDestination,
            grabPriceState,
            lastUpdatedGrabState,
            gojekPriceState,
            lastUpdatedGojekState,
            tadaPriceState,
            lastUpdatedTadaState,
            zigPriceState,
            lastUpdatedZigState
        )
    }
}