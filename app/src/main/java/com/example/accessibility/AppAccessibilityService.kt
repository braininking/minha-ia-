package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.ScreenContext

class AppAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppAccessibility"
        @Volatile
        private var instance: AppAccessibilityService? = null

        fun getInstance(): AppAccessibilityService? = instance

        fun isServiceRunning(): Boolean = instance != null

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${AppAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(expectedServiceName) || enabledServices.contains(AppAccessibilityService::class.java.simpleName)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AppAccessibilityService conectado com sucesso!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events can be logged or monitored for screen state transitions
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppAccessibilityService interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun getCurrentScreenSummary(): ScreenContext {
        val root = rootInActiveWindow ?: return ScreenContext()
        val texts = mutableListOf<String>()
        val clickable = mutableListOf<String>()
        val pkg = root.packageName?.toString()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()

            val label = when {
                !text.isNullOrEmpty() -> text
                !desc.isNullOrEmpty() -> desc
                else -> null
            }

            if (label != null) {
                texts.add(label)
                if (node.isClickable) {
                    clickable.add(label)
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        try {
            traverse(root)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inspecionar nós: ${e.message}")
        }

        return ScreenContext(
            packageName = pkg,
            visibleTexts = texts.distinct().take(50),
            clickableElements = clickable.distinct().take(30)
        )
    }

    fun clickElementByText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanTarget = targetText.trim().lowercase()

        // 1. Direct search
        val matchingNodes = root.findAccessibilityNodeInfosByText(targetText)
        if (!matchingNodes.isNullOrEmpty()) {
            for (node in matchingNodes) {
                if (performClickOnNodeOrParent(node)) return true
            }
        }

        // 2. Deep recursive search for partial/contentDescription match
        var found = false
        fun searchAndClick(node: AccessibilityNodeInfo?): Boolean {
            if (node == null || found) return false
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            if (text.contains(cleanTarget) || desc.contains(cleanTarget)) {
                if (performClickOnNodeOrParent(node)) {
                    found = true
                    return true
                }
            }

            for (i in 0 until node.childCount) {
                if (searchAndClick(node.getChild(i))) return true
            }
            return false
        }

        return searchAndClick(root)
    }

    fun setTextOnInputField(textToEnter: String, fieldLabel: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        // 1. If focused editable node already exists
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && (focused.isEditable || focused.className?.contains("EditText") == true)) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToEnter)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // 2. Search for any editable node
        var targetNode: AccessibilityNodeInfo? = null
        fun findEditable(node: AccessibilityNodeInfo?) {
            if (node == null || targetNode != null) return
            if (node.isEditable || node.className?.contains("EditText") == true) {
                if (fieldLabel.isNullOrBlank()) {
                    targetNode = node
                    return
                } else {
                    val hint = node.hintText?.toString()?.lowercase() ?: ""
                    val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
                    val text = node.text?.toString()?.lowercase() ?: ""
                    val target = fieldLabel.lowercase()
                    if (hint.contains(target) || contentDesc.contains(target) || text.contains(target)) {
                        targetNode = node
                        return
                    }
                }
            }
            for (i in 0 until node.childCount) {
                findEditable(node.getChild(i))
            }
        }

        findEditable(root)

        val nodeToUse = targetNode ?: return false
        nodeToUse.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToEnter)
        }
        return nodeToUse.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    fun scrollWindow(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD

        var scrolled = false
        fun findAndScroll(node: AccessibilityNodeInfo?): Boolean {
            if (node == null || scrolled) return false
            if (node.isScrollable) {
                if (node.performAction(action)) {
                    scrolled = true
                    return true
                }
            }
            for (i in 0 until node.childCount) {
                if (findAndScroll(node.getChild(i))) return true
            }
            return false
        }

        return findAndScroll(root)
    }

    fun navigateBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun navigateHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        // Fallback: try click anyway
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}
