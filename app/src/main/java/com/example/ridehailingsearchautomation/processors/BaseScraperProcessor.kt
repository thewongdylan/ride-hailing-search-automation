package com.example.ridehailingsearchautomation.processors

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ridehailingsearchautomation.MainActivity

abstract class BaseScraperProcessor(open val service: AccessibilityService) {
    abstract val packageName: String
    abstract val TAG: String
    abstract fun onAccessibilityEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo)
    val context: Context get() = service
    fun findNodesByClass(node: AccessibilityNodeInfo, className: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node.className?.toString() == className) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNodesByClass(it, className, result) }
        }
    }

    fun findNodeByText(node: AccessibilityNodeInfo, textToSearch: String): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: ""
        if (text.contains(textToSearch, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, textToSearch)
            if (result != null) return result
        }
        return null
    }

    fun findNodeByTextFragment(node: AccessibilityNodeInfo, fragment: String): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: ""
        if (text.contains(fragment)) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByTextFragment(child, fragment)
            if (found != null) return found
        }
        return null
    }

    fun findNodeByTokens(node: AccessibilityNodeInfo?, tokens: List<String>): AccessibilityNodeInfo? {
        if (node == null) return null

        val nodeText = node.text?.toString()?.lowercase() ?: ""
        if (tokens.isNotEmpty() && tokens.all { nodeText.contains(it) }) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByTokens(child, tokens)
            if (found != null) return found
        }
        return null
    }

    fun findNodeByContentDescription(node: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByContentDescription(child, desc)
            if (found != null) return found
        }
        return null
    }

    fun findNodeByViewId(node: AccessibilityNodeInfo?, id: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.viewIdResourceName == id) return node
        for (i in 0 until node.childCount) {
            val found = findNodeByViewId(node.getChild(i), id)
            if (found != null) return found
        }
        return null
    }

    fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    fun broadcastPriceAndReturn(price: String, intentAction: String) {
        val intent = Intent(intentAction).apply {
            putExtra("price_value", price)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)

        val returnIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        context.startActivity(returnIntent)
        Log.d(TAG, "Returned to MainActivity")
    }
}