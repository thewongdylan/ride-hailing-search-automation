package com.example.ridehailingsearchautomation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GrabRideScraperService : AccessibilityService() {
    private val TAG = "GrabRideScraperService"
    private enum class AutomationState { IDLE, TYPING, WAITING_FOR_RESULTS, CONFIRMING_PICKUP, SCRAPING_PRICE}
    private var currState = AutomationState.IDLE
    private var lastPrice: String? = null
    private var resultsVisibleStartTime = 0L
    private var lastClickConfirmTime = 0L
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

        // Reset if user leaves Grab
        if (currPackage != "com.grabtaxi.passenger" && currState != AutomationState.IDLE) {
            Log.d(TAG, "User left Grab, resetting state")
            currState = AutomationState.IDLE
            return
        }

        // State machine: handle states within Grab app
        if (currPackage == "com.grabtaxi.passenger") {
            when (currState) {
                AutomationState.IDLE -> {
                    if (destinationToType != null) navigateToSearch(rootNode)
                }
                AutomationState.TYPING -> {
                    enterDestination(rootNode, destinationToType ?: return)
                }
                AutomationState.WAITING_FOR_RESULTS -> {
                    selectFirstSearchResult()
                }
                AutomationState.CONFIRMING_PICKUP -> {
                    confirmPickup(rootNode)
                }
                AutomationState.SCRAPING_PRICE -> {
                    scrapePrice(rootNode)
                }
            }
        }
    }

    private fun navigateToSearch(rootNode: AccessibilityNodeInfo) {
        if (currState != AutomationState.IDLE) return
        val grabTextView = findGrabTextView(rootNode)
        if (grabTextView != null) {
            val clickableParent = findClickableParent(grabTextView)
            clickableParent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            currState = AutomationState.TYPING
            Log.d(TAG, "Clicked Grab TextView")
        }
    }
    private fun enterDestination(rootNode: AccessibilityNodeInfo, destination: String) {
        if (currState != AutomationState.TYPING) return
        val editTexts = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClass(rootNode, "android.widget.EditText", editTexts)

        Log.d(TAG, "Found editTexts: $editTexts")
        if (editTexts.isNotEmpty()) {
            val targetField = editTexts.find { it.isFocused } ?: if (editTexts.size > 1) editTexts[1] else editTexts[0]
            Log.d(TAG, "Found EditText: $targetField")

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
        val poiListNodes = rootNode.findAccessibilityNodeInfosByViewId("com.grabtaxi.passenger:id/poi_list")
        if (poiListNodes.isNotEmpty()) {
            Log.d(TAG, "Found POI list: $poiListNodes")
            val recyclerView = poiListNodes[0]
            if (recyclerView.childCount > 1) {
                if (resultsVisibleStartTime == 0L) {
                    resultsVisibleStartTime = System.currentTimeMillis()
                    Log.d(TAG, "Results detected, starting 1.5s timer")
                    return
                }
                if (System.currentTimeMillis() - resultsVisibleStartTime > 1500) {
                    Log.d(TAG, "1.5s timer expired, selecting first result")
                    val firstResult = recyclerView.getChild(1)
                    if (firstResult != null) {
                        val clickableNode = if (firstResult.isClickable) firstResult else findClickableParent(firstResult)
                        val success = clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        if (success == true) {
                            Log.d(TAG, "Clicked first result after 1.5s delay")
                            currState = AutomationState.CONFIRMING_PICKUP
                            resultsVisibleStartTime = 0L
                        }
                    }
                }
            }
        } else {
            if (System.currentTimeMillis() % 1000 < 100) {
                Log.d(TAG, "Waiting for POI list")
            }
        }
    }

    private fun confirmPickup(rootNode: AccessibilityNodeInfo) {
        if (currState != AutomationState.CONFIRMING_PICKUP) return
        var confirmNodes = rootNode.findAccessibilityNodeInfosByViewId("com.grabtaxi.passenger:id/bottom_control_card_click_button")
        if (confirmNodes.isNotEmpty()) Log.d(TAG, "Found confirm button by UI")

        if (confirmNodes.isNotEmpty()) {
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
//        Log.d(TAG, "Scraping price")
        val currPrice = findJustGrabPrice(rootNode)
        if (currPrice != null && currPrice != lastPrice) {
            val intent = Intent("COM_EXAMPLE_GRAB_PRICE_UPDATE").apply {
                putExtra("price_value", currPrice)
                setPackage(packageName)
            }
            sendBroadcast(intent)
            Log.d(TAG, "Broadcasted price: $currPrice")

            lastPrice = currPrice
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

    private fun findGrabTextView(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 1. Check current node
        val className = node.className?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        // Heuristic: Is it a TextView (or subclass) and does it mention "Where to"?
        if (className.contains("TextView") &&
            (text.contains("Where to", ignoreCase = true) ||
                    contentDesc.contains("Where to", ignoreCase = true))) {
            return node
        }

        // 2. Recursive Step: Check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findGrabTextView(child)
            if (result != null) return result
        }

        return null
    }

    private fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        Log.d(TAG, "Looking for clickable parent, current: $current")
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun findJustGrabPrice(rootNode: AccessibilityNodeInfo): String? {
        val labelNodes = rootNode.findAccessibilityNodeInfosByViewId(
            "com.grabtaxi.passenger:id/xsell_confirmation_taxi_type_name_autoscroll")

        for (node in labelNodes) {
            if (node.text?.toString()?.contains("JustGrab", ignoreCase = true) == true) {
                var parentNode = node.parent
                while (parentNode != null &&
                    parentNode.viewIdResourceName != "com.grabtaxi.passenger:id/xsell_confirmation_item_container") {
                    parentNode = parentNode.parent
                }

                if (parentNode != null) {
                    return extractPriceFromRow(parentNode)
                }
            }
        }

        Log.d(TAG, "JustGrab row could not be found")
        return null
    }

    private fun extractPriceFromRow(rowNode: AccessibilityNodeInfo) : String? {
        val fareNodes = rowNode.findAccessibilityNodeInfosByViewId("com.grabtaxi.passenger:id/fareTextView")
        val currencyNodes = rowNode.findAccessibilityNodeInfosByViewId("com.grabtaxi.passenger:id/currencyLeft")

        if (fareNodes.isNotEmpty()) {
            val amount = fareNodes[0].text?.toString() ?: ""
            val currency = if (currencyNodes.isNotEmpty()) currencyNodes[0].text?.toString() ?: "S$" else "S$"

            val fullPrice = "$currency$amount"
            Log.d(TAG, "Found price: $fullPrice")
            return fullPrice
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