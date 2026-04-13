package com.example.ridehailingsearchautomation.processors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

@SuppressLint("AccessibilityPolicy")
class UniversalRideScraperService: AccessibilityService() {
    private val TAG = "UniversalScraper"
    private lateinit var processors: Map<String, BaseScraperProcessor>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        val resetFilter = IntentFilter("ACTION_GLOBAL_RESET_PROCESSORS")
        registerReceiver(resetReceiver, resetFilter, RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(resetReceiver)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        processors = mapOf(
            "com.grabtaxi.passenger" to GrabProcessor(this),
            "com.gojek.app" to GojekProcessor(this),
            "io.mvlchain.tada" to TadaProcessor(this),
            "com.codigo.comfort" to ZigProcessor(this)
        )
        Log.d(TAG, "Service Connected. Initializing processors to IDLE.")
        processors.values.forEach { it.resetToIdle() }

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            packageNames = processors.keys.toTypedArray()
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return
        processors[packageName]?.onAccessibilityEvent(event, rootNode)
    }

    override fun onInterrupt() {
        Log.e(TAG, "Service interrupted")
    }

    private val resetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Manual reset triggered from UI. Clearing all states.")
            processors.values.forEach { it.resetToIdle() }
        }
    }
}