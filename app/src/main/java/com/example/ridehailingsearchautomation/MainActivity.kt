package com.example.ridehailingsearchautomation

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ridehailingsearchautomation.ui.theme.RideHailingSearchAutomationTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private var grabPriceState = mutableStateOf("No Grab price fetched yet")
    private val grabPriceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val price = intent?.getStringExtra("price_value")
            Log.d("Ride", "MainActivity received broadcast: $price")
            if (price != null) {
                grabPriceState.value = price
            }

        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("Ride", "onCreate called")

        val filter = IntentFilter("COM_EXAMPLE_GRAB_PRICE_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(grabPriceReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(grabPriceReceiver, filter)
        }

        setContent {
            RideHailingSearchAutomationTheme {
                RHSAApp(
                    grabPriceState = grabPriceState
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // UNREGISTER HERE: Clean up when the app is actually killed
        try {
            unregisterReceiver(grabPriceReceiver)
        } catch (e: Exception) {
            Log.e("Ride", "Receiver already unregistered")
        }
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onResume() {
//        super.onResume()
//        registerReceiver(
//            grabPriceReceiver,
//            IntentFilter("COM_EXAMPLE_GRAB_PRICE_UPDATE"),
//            RECEIVER_EXPORTED
//        )
//    }
//
//    override fun onPause () {
//        super.onPause()
//        unregisterReceiver(grabPriceReceiver)
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RHSAApp(
    grabPriceState: MutableState<String>,
//    gojekPriceState: MutableState<String>,
//    tadaPriceState: MutableState<String>,
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
            Text(
                text = "Current Fare: ${grabPriceState.value}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
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


@Preview(showBackground = true)
@Composable
fun RHSAPreview() {
    val grabPriceState = remember { mutableStateOf("No Grab price fetched yet")}
    RideHailingSearchAutomationTheme {
        RHSAApp(
            grabPriceState
        )
    }
}