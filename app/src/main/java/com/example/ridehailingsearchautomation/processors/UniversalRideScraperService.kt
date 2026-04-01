package com.example.ridehailingsearchautomation.processors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent

@SuppressLint("AccessibilityPolicy")
class UniversalRideScraperService: AccessibilityService() {
    private val TAG = "UniversalScraper"
    private lateinit var processors: Map<String, BaseScraperProcessor>

    override fun onServiceConnected() {
        super.onServiceConnected()
        processors = mapOf(
            "com.grabtaxi.passenger" to GrabProcessor(this),
            "com.gojek.app" to GojekProcessor(this),
            "io.mvlchain.tada" to TadaProcessor(this),
            "com.codigo.comfort" to ZigProcessor(this)
        )

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
}