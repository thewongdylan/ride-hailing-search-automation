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

private var TAG = "MainActivity"
private val grabPackageName = "com.grabtaxi.passenger"
private val gojekPackageName = "com.gojek.app"
private var isDualSearchActive = false

class MainActivity : ComponentActivity() {
    private var lastSearchedDestination = mutableStateOf("")
    private var grabPriceState = mutableStateOf("-")
    private var lastUpdatedGrabState = mutableStateOf("")
    private var gojekPriceState = mutableStateOf("-")
    private var lastUpdatedGojekState = mutableStateOf("")

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

        setContent {
            RideHailingSearchAutomationTheme {
                RHSAApp(
                    lastSearchedDestination = lastSearchedDestination,
                    grabPriceState = grabPriceState,
                    lastUpdatedGrabState = lastUpdatedGrabState,
                    gojekPriceState = gojekPriceState,
                    lastUpdatedGojekState = lastUpdatedGojekState
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(grabPriceReceiver)
        unregisterReceiver(gojekPriceReceiver)
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
//    tadaPriceState: MutableState<String>,
//    lastUpdatedTadaState: MutableState<String>,
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf("") }
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
                        lastSearchedDestination.value = destination
                        openGrab(context, destination)
//                        openGojek(context,destination)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grab logo
                Image(
                    painter = painterResource(R.drawable.grab_logo),
                    contentDescription = "Grab Logo",
                    modifier = Modifier.size(45.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Current Grab fare
                    Text(
                        text = "${stringResource(R.string.current_fare)}: ${grabPriceState.value}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 14.sp
                    )

                    // Last updated at
                    Text(
                        text = "${stringResource(R.string.last_updated_at)}: ${lastUpdatedGrabState.value}",
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 14.sp
                    )
                }
                // Open Grab button
                if (lastSearchedDestination.value.isNotBlank()) {
                    Button(
                        onClick = { switchToGrab(context) },
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text("Open Grab")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gojek price display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gojek logo
                Image(
                    painter = painterResource(R.drawable.gojek_logo),
                    contentDescription = "Gojek Logo",
                    modifier = Modifier.size(45.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Current Gojek fare
                    Text(
                        text = "${stringResource(R.string.current_fare)}: ${gojekPriceState.value}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 14.sp
                    )

                    // Last updated at
                    Text(
                        text = "${stringResource(R.string.last_updated_at)}: ${lastUpdatedGojekState.value}",
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 14.sp
                    )
                }
                // Open Gojek button
                if (lastSearchedDestination.value.isNotBlank()) {
                    Button(
                        onClick = { switchToGojek(context) },
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text("Open Gojek")
                    }
                }
            }
        }
    }
}

fun openGrab(context: Context, destination: String) {
    GrabRideScraperService.destinationToType = destination

    val uri = "grab://open?screenType=BOOKING".toUri()
    val intent = Intent(Intent.ACTION_VIEW,  uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Grab app not found", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Grab app not found, $e")
    }
}

fun switchToGrab(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(grabPackageName)

    if (intent != null) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not switch to Grab", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Could not switch to Grab, $e")
        }
    } else {
        Log.d(TAG, "Grab app not found")
    }
}

fun openGojek(context: Context, destination: String) {
    GojekRideScraperService.destinationToType = destination
    val launchIntent = context.packageManager.getLaunchIntentForPackage(gojekPackageName)
    if (launchIntent != null) {
        launchIntent.replaceExtras(Bundle())

        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
        try {
            context.startActivity(launchIntent)
            Log.d(TAG, "opening Gojek")
        } catch (e: Exception) {
            Toast.makeText(context, "Gojek app not found", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Gojek app not found, $e")
        }
    }
}

fun switchToGojek(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(gojekPackageName)

    if (intent != null) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not switch to Gojek", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Could not switch to Gojek, $e")
        }
    } else {
        Log.d(TAG, "Gojek app not found")
    }
}


@Preview(showBackground = true)
@Composable
fun RHSAPreview() {
    val lastSearchedDestination = remember { mutableStateOf("")}
    val grabPriceState = remember { mutableStateOf("-")}
    val lastUpdatedGrabState = remember { mutableStateOf("")}
    val gojekPriceState = remember { mutableStateOf("-")}
    val lastUpdatedGojekState = remember { mutableStateOf("")}

    RideHailingSearchAutomationTheme {
        RHSAApp(
            lastSearchedDestination,
            grabPriceState,
            lastUpdatedGrabState,
            gojekPriceState,
            lastUpdatedGojekState
        )
    }
}