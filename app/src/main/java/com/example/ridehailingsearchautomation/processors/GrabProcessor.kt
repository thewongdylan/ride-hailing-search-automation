package com.example.ridehailingsearchautomation.processors

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GrabProcessor(override val service: AccessibilityService) : BaseScraperProcessor(service) {
    override val TAG = "GrabProcessor"
    override val packageName = "com.grabtaxi.passenger"

    private enum class AutomationState { IDLE, TYPING, WAITING_FOR_RESULTS, CONFIRMING_PICKUP, SCRAPING_PRICE}
    private var currState = AutomationState.IDLE
    private var resultsVisibleStartTime = 0L
    private var lastClickConfirmTime = 0L
    companion object {
        var destinationToType: String? = null
        private var lastProcessedDestination : String? = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo) {
        val currPackage = event.packageName?.toString() ?: ""

        // Reset if new destination sent from app
        if (destinationToType != null && destinationToType != lastProcessedDestination) {
            Log.d(TAG, "New destination input, resetting state")
            currState = AutomationState.IDLE
            lastProcessedDestination = destinationToType
            resultsVisibleStartTime = 0L
        }

        // Reset if user leaves Grab
        if (currPackage != packageName && currState != AutomationState.IDLE) {
            Log.d(TAG, "User left Grab, resetting state")
            currState = AutomationState.IDLE
            return
        }

        // State machine: handle states within Grab app
        if (currPackage == packageName) {
            when (currState) {
                AutomationState.IDLE -> if (destinationToType != null) navigateToSearch(rootNode)
                AutomationState.TYPING -> enterDestination(rootNode, destinationToType ?: return)
                AutomationState.WAITING_FOR_RESULTS -> selectFirstSearchResult(rootNode)
                AutomationState.CONFIRMING_PICKUP -> confirmPickup(rootNode)
                AutomationState.SCRAPING_PRICE -> scrapePrice(rootNode)
            }
        }
    }

    override fun resetToIdle() {
        currState = AutomationState.IDLE
        resultsVisibleStartTime = 0L
        lastClickConfirmTime = 0L
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

    private fun selectFirstSearchResult(rootNode: AccessibilityNodeInfo) {
        if (currState != AutomationState.WAITING_FOR_RESULTS) return
        val poiListNodes = rootNode.findAccessibilityNodeInfosByViewId("com.grabtaxi.passenger:id/poi_list")
        if (poiListNodes.isNotEmpty()) {
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
        val confirmNodes = rootNode.findAccessibilityNodeInfosByViewId("com.grabtaxi.passenger:id/bottom_control_card_click_button")
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

        val standardLabelNode = findNodeByText(rootNode, "Standard")
        if (standardLabelNode != null) {
            Log.d(TAG, "Found Standard label node")
            val rowContainer = findClickableParent(standardLabelNode)
            if (rowContainer != null) {
                val priceNode = findNodeByTextFragment(rowContainer, "S$")
                if (priceNode != null) {
                    val price = priceNode.text?.toString()
                    Log.d(TAG, "Found price: $price")
                    broadcastPriceAndReturn(price ?: "", "COM_EXAMPLE_GRAB_PRICE_UPDATE")
                    Log.d(TAG, "Broadcasted Grab price: $price")
                    currState = AutomationState.IDLE
                    destinationToType = null
                    Log.d(TAG, "Workflow complete, reset state to IDLE")
                } else {
                    Log.d(TAG, "Could not find price")
                }
            }
        } else {
            Log.d(TAG, "Cannot find Standard label node")
        }
    }

    // Helper Functions
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
}