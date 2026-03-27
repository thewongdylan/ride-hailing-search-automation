package com.example.ridehailingsearchautomation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ZigRideScraperService : AccessibilityService() {
    private val TAG = "ZigRideScraperService"
    private val packageName = "com.codigo.comfort"
    private enum class AutomationState { IDLE, TYPING, WAITING_FOR_RESULTS, CONFIRMING_PICKUP, SCRAPING_PRICE}
    private var currState = AutomationState.IDLE
    private var resultsVisibleStartTime = 0L
    private var lastClickConfirmTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var isClickPending = false
    companion object {
        var destinationToType: String? = null
        private var lastProcessedDestination : String? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        val currPackage = event?.packageName?.toString() ?: ""

        // Reset if new destination sent from app
        if (destinationToType != null && destinationToType != lastProcessedDestination) {
            Log.d(TAG, "New destination input, resetting state")
            currState = AutomationState.IDLE
            lastProcessedDestination = destinationToType
            resultsVisibleStartTime = 0L
        }

        // Reset if user leaves Gojek
        if (currPackage != packageName && currState != AutomationState.IDLE) {
            Log.d(TAG, "User left Gojek, resetting state")
            currState = AutomationState.IDLE
            return
        }

        // State machine: handle states within Gojek app
        if (currPackage == packageName) {
            when (currState) {
                AutomationState.IDLE -> {
//                    if (destinationToType != null) navigateToSearch(rootNode)
                }
                AutomationState.TYPING -> {
//                    enterDestination(rootNode, destinationToType ?: return)
                }
                AutomationState.WAITING_FOR_RESULTS -> {
//                    selectFirstSearchResult()
                }
                AutomationState.CONFIRMING_PICKUP -> {
//                    confirmPickup(rootNode)
                }
                AutomationState.SCRAPING_PRICE -> {
//                    scrapePrice(rootNode)
                }
            }
        }
    }

    private fun navigateToSearch(rootNode: AccessibilityNodeInfo) {
        val searchBars = rootNode.findAccessibilityNodeInfosByViewId("com.gojek.app:id/2131368260")
        if (searchBars.isNotEmpty()) {
            Log.d(TAG, "Found search bar")
            searchBars[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Clicked on search bar")
            currState = AutomationState.TYPING
        } else {
            Log.d(TAG, "Could not find search bar")
        }
    }
    private fun enterDestination(rootNode: AccessibilityNodeInfo, destination: String) {
        if (currState != AutomationState.TYPING) return
        val editTexts = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClass(rootNode, "android.widget.EditText", editTexts)

        Log.d(TAG, "Found editTexts")
        if (editTexts.isNotEmpty()) {
            val targetField = editTexts.find { it.isFocused } ?: if (editTexts.size > 1) editTexts[1] else editTexts[0]
            Log.d(TAG, "Found EditText")

            if (targetField.isEditable) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, destination)
                }

                val success = targetField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                if (success) {
                    Log.d(TAG, "Successfully injected '$destination' into EditText")
                    currState = AutomationState.WAITING_FOR_RESULTS
                } else {
                    Log.d(TAG, "Failed to inject '$destination' into EditText")
                }
            }
        } else {
            Log.d(TAG, "No EditText found")
        }
    }

    private fun selectFirstSearchResult() {
        if (currState != AutomationState.WAITING_FOR_RESULTS) return
        val rootNode = rootInActiveWindow ?: return
        val poiTitles = rootNode.findAccessibilityNodeInfosByViewId("com.gojek.app:id/2131382467")
        val tokens = destinationToType?.split(" ") ?: emptyList()
        Log.d(TAG, "Checking ${poiTitles.size} results for '$destinationToType' using $tokens")

        val targetPoiNode = poiTitles.find { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            tokens.isNotEmpty() && tokens.all { text.contains(it) }
        }

        Log.d(TAG, "Found targetPoiNode: $targetPoiNode")

        if (targetPoiNode != null) {
            isClickPending = true

            handler.postDelayed({
                val latestRoot = rootInActiveWindow
                if (latestRoot != null) {
                    val clickableRow = findClickableParent(targetPoiNode)
                    if (clickableRow != null) {
                        val success = clickableRow.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        if (success) {
                            // Clear keyboard and previous UI
//                            performGlobalAction(GLOBAL_ACTION_BACK)
                            val xButtons =
                                rootNode.findAccessibilityNodeInfosByViewId("com.gojek.app:id/2131371946")
                            if (xButtons.isNotEmpty()) {
                                val clickableX = findClickableParent(xButtons[0])
                                clickableX?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                Log.d(TAG, "Clicked 'X' button to clear search query")
                            }

                            currState = AutomationState.CONFIRMING_PICKUP
                            resultsVisibleStartTime = 0L
                            Log.d(TAG, "Clicked first result after 1.5s delay")
                        }
                    } else {
                        Log.d(TAG, "Could not find clickable element")
                    }
                }
                isClickPending = false
            }, 1000)
        }  else {
            if (poiTitles.isEmpty()) {
                resultsVisibleStartTime = 0L
                Log.d(TAG, "No results on screen. Timer reset.")
            } else {
                if (System.currentTimeMillis() % 2000 < 100) {
                    Log.d(TAG, "Ignoring stale results, waiting for network...")
                }
            }
        }

    }

    private fun confirmPickup(rootNode: AccessibilityNodeInfo) {
        if (currState != AutomationState.CONFIRMING_PICKUP) return
        val priceNode = rootNode.findAccessibilityNodeInfosByViewId("com.gojek.app:id/text_service_pricing_with_voucher")
        if (priceNode.isNotEmpty()) {
            Log.d(TAG, "Price detected! Bypassing confirmation.")
            currState = AutomationState.SCRAPING_PRICE
            return
        }

        val confirmNodes = rootNode.findAccessibilityNodeInfosByText("Next")
        Log.d(TAG, "Found ${confirmNodes.size} next buttons")
        if (confirmNodes.isNotEmpty()) {
            Log.d(TAG, "Found next button by text")
            val textNode = confirmNodes[0]

            if (System.currentTimeMillis() - lastClickConfirmTime < 1000) return
            lastClickConfirmTime = System.currentTimeMillis()

            if (!textNode.isVisibleToUser) {
                Log.d(TAG, "Confirm button not yet visible")
                return
            }

            if (textNode != null) {
                val clickableButton = if (textNode.isClickable) textNode else findClickableParent(textNode)
                val success = clickableButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success == true) {
                    currState = AutomationState.SCRAPING_PRICE
                    Log.d(TAG, "Clicked confirm button")
                }
            }
        }
    }

    private fun scrapePrice(rootNode: AccessibilityNodeInfo) {
        if (currState != AutomationState.SCRAPING_PRICE) return
        val currPriceNode = rootNode.findAccessibilityNodeInfosByViewId("com.gojek.app:id/text_service_pricing_with_voucher")
        val currPrice = currPriceNode.firstOrNull()?.text?.toString()
        Log.d(TAG, "Found currPrice: $currPrice")

        if (currPrice != null) {
            val intent = Intent("COM_EXAMPLE_GOJEK_PRICE_UPDATE").apply {
                putExtra("price_value", currPrice)
                setPackage("com.example.ridehailingsearchautomation")
            }
            sendBroadcast(intent)
            Log.d(TAG, "Broadcasted price: $currPrice")

//            lastPrice = currPrice
            currState = AutomationState.IDLE
            destinationToType = null
            Log.d(TAG, "Workflow complete, reset state to IDLE")

            val returnIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(returnIntent)
            Log.d(TAG, "Returned to MainActivity")
        }
    }

    // Helper Functions
    private fun findNodesByClass(node: AccessibilityNodeInfo, className: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node.className?.toString() == className) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNodesByClass(it, className, result) }
        }
    }

    private fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    override fun onInterrupt() {
        Log.e(TAG, "Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
    }

}