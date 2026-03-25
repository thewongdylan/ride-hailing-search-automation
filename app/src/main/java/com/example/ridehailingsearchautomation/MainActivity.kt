package com.example.ridehailingsearchautomation

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridehailingsearchautomation.ui.theme.RideHailingSearchAutomationTheme
import androidx.core.net.toUri

private var TAG = "MainActivity"
class MainActivity : ComponentActivity() {

    private var grabPriceState = mutableStateOf("No Grab price fetched yet")
    private var lastUpdatedGrabState = mutableStateOf("")
    private val grabPriceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val price = intent?.getStringExtra("price_value")
            Log.d(TAG, "MainActivity received broadcast: $price")
            if (price != null) {
                grabPriceState.value = price
                val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                lastUpdatedGrabState.value = "$currentTime"
            }

        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate called")

        val filter = IntentFilter("COM_EXAMPLE_GRAB_PRICE_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(grabPriceReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(grabPriceReceiver, filter)
        }

        setContent {
            RideHailingSearchAutomationTheme {
                RHSAApp(
                    grabPriceState = grabPriceState,
                    lastUpdatedGrabState = lastUpdatedGrabState
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(grabPriceReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Receiver already unregistered")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RHSAApp(
    grabPriceState: MutableState<String>,
    lastUpdatedGrabState: MutableState<String>,
//    gojekPriceState: MutableState<String>,
//    lastUpdatedGojekState: MutableState<String>,
//    tadaPriceState: MutableState<String>,
//    lastUpdatedTadaState: MutableState<String>,
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf("") }
    val context = LocalContext.current

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
            modifier = Modifier
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
                label = { Text("Enter destination here") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search button
            Button(
                onClick = {
                    if (destination.isNotBlank()) {
                        grabPriceState.value = "Fetching Grab price..."
                        openGrab(context, destination)
                    }
                }
            ) {
                Text("Search")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grab price display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.grab_logo),
                    contentDescription = "Grab Logo",
                    modifier = Modifier.size(45.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Current Grab Fare: ${grabPriceState.value}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Text(
                        text = "Last updated at: ${lastUpdatedGrabState.value}",
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth().padding(top = 0.dp)
                    )
                }
                Button(
                    onClick = { switchToGrab(context) },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Open Grab")
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
    }
}

fun switchToGrab(context: Context) {
    val grabPackage = "com.grabtaxi.passenger"
    val intent = context.packageManager.getLaunchIntentForPackage(grabPackage)

    if (intent != null) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not switch to Grab", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Could not switch to Grab")
        }
    } else {
        Log.d(TAG, "Grab app not found")
    }
}


@Preview(showBackground = true)
@Composable
fun RHSAPreview() {
    val grabPriceState = remember { mutableStateOf("No Grab price fetched yet")}
    val lastUpdatedGrabState = remember { mutableStateOf("")}

    RideHailingSearchAutomationTheme {
        RHSAApp(
            grabPriceState,
            lastUpdatedGrabState
        )
    }
}