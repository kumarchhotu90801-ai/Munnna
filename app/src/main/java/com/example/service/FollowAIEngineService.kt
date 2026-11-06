package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.controller.AutomationEngine
import com.example.model.ActionNeededReason
import com.example.model.AspectRatioPreset
import com.example.model.AutomationConfig
import com.example.model.CalibrationData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class FollowAIEngineService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AutomationEngine.bindService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events can be observed if needed
    }

    override fun onInterrupt() {
        AutomationEngine.log("Accessibility Service interrupted.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AutomationEngine.unbindService()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AutomationEngine.unbindService()
        super.onDestroy()
    }

    // --- Module 7: Ratio Lock & Image-to-Video (I2V) Sequences ---

    /**
     * Executes the Aspect Ratio selection sequence (16:9 or 9:16).
     */
    suspend fun executeAspectRatioSequence(preset: AspectRatioPreset, calibration: CalibrationData): Boolean {
        if (preset == AspectRatioPreset.RATIO_DEFAULT) return true
        val root = rootInActiveWindow

        if (root != null) {
            val ratioNode = findNodeMatchingKeywords(root, preset.searchKeywords)
            if (ratioNode != null) {
                try {
                    val clicked = performClickOnNodeOrParent(ratioNode)
                    if (clicked) {
                        AutomationEngine.log("Locked aspect ratio to ${preset.displayName} via DOM element.")
                        return true
                    }
                } finally {
                    ratioNode.recycle()
                }
            }
        }

        // Coordinate fallback for ratio selector
        val pt = calibration.modelRatio
        if (pt.isCalibrated) {
            AutomationEngine.log("Tapping calibrated Model/Ratio selector at (${pt.x.toInt()}, ${pt.y.toInt()})...")
            return simulateRealisticTap(pt.x, pt.y)
        }
        return false
    }

    /**
     * Executes the Image-to-Video sequence (taps image upload container).
     */
    suspend fun executeImageUploadSequence(calibration: CalibrationData): Boolean {
        val root = rootInActiveWindow
        val uploadKeywords = listOf("upload", "add image", "drop image", "image-to-video", "attach image", "reference image")

        if (root != null) {
            val uploadNode = findNodeMatchingKeywords(root, uploadKeywords)
            if (uploadNode != null) {
                try {
                    val clicked = performClickOnNodeOrParent(uploadNode)
                    if (clicked) {
                        AutomationEngine.log("Tapped image upload container via DOM.")
                        return true
                    }
                } finally {
                    uploadNode.recycle()
                }
            }
        }

        val pt = calibration.modelRatio
        if (pt.isCalibrated) {
            AutomationEngine.log("Tapping calibrated Image upload spot at (${pt.x.toInt()}, ${pt.y.toInt()})...")
            return simulateRealisticTap(pt.x, pt.y)
        }
        return false
    }

    // --- Module 8: Auto-Download Verification ---

    /**
     * Locates and clicks the Download/Save button for completed video outputs.
     */
    suspend fun executeDownloadClick(config: AutomationConfig, calibration: CalibrationData): Boolean {
        val root = rootInActiveWindow
        val dlKeywords = config.parseDownloadKeywords()

        if (root != null && !config.useCoordinates) {
            val downloadNode = findNodeMatchingKeywords(root, dlKeywords)
            if (downloadNode != null) {
                try {
                    val clicked = performClickOnNodeOrParent(downloadNode)
                    if (clicked) {
                        AutomationEngine.log("Clicked video Download button via accessibility node.")
                        return true
                    }

                    val rect = Rect()
                    downloadNode.getBoundsInScreen(rect)
                    if (!rect.isEmpty) {
                        return simulateRealisticTap(rect.centerX().toFloat(), rect.centerY().toFloat())
                    }
                } finally {
                    downloadNode.recycle()
                }
            }
        }

        // Calibrated download coordinate fallback
        val pt = calibration.downloadSave
        val tapX = if (pt.isCalibrated) pt.x else 920f
        val tapY = if (pt.isCalibrated) pt.y else 1750f

        AutomationEngine.log("Simulating tap on Download icon at (${tapX.toInt()}, ${tapY.toInt()})...")
        return simulateRealisticTap(tapX, tapY)
    }

    // --- Modal, Captcha & Error Detection ---

    fun detectModalOrCaptcha(): Pair<ActionNeededReason, String>? {
        val root = rootInActiveWindow ?: return null
        return scanNodeForModals(root)
    }

    private fun scanNodeForModals(node: AccessibilityNodeInfo): Pair<ActionNeededReason, String>? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val combined = "$text $desc"

        val captchaKeywords = listOf(
            "captcha", "verify you are human", "turnstile", "hcaptcha", "recaptcha",
            "cloudflare", "puzzle", "security verification", "bot check", "press & hold"
        )
        if (captchaKeywords.any { combined.contains(it) }) {
            return Pair(ActionNeededReason.CAPTCHA_DETECTED, "Captcha or Human Verification detected")
        }

        val loginKeywords = listOf(
            "session expired", "please log in to continue", "login to continue",
            "sign in to follow", "authentication required", "sign in with google"
        )
        if (loginKeywords.any { combined.contains(it) }) {
            return Pair(ActionNeededReason.LOGIN_REQUIRED, "Login or authentication modal detected")
        }

        val creditKeywords = listOf(
            "insufficient credits", "quota exceeded", "out of credits",
            "daily limit reached", "upgrade plan to generate", "rate limit reached"
        )
        if (creditKeywords.any { combined.contains(it) }) {
            return Pair(ActionNeededReason.CREDITS_EXHAUSTED, "Credits depleted or rate limit reached")
        }

        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            val found = scanNodeForModals(child)
            child.recycle()
            if (found != null) {
                return found
            }
        }
        return null
    }

    // --- Smart Polling: Generate Button State ---

    suspend fun pollUntilGenerateReady(config: AutomationConfig, maxWaitSeconds: Int = 15): Boolean {
        val keywords = config.parseGenerateKeywords()
        var waited = 0

        while (waited < maxWaitSeconds) {
            val root = rootInActiveWindow
            if (root == null) {
                delay(800)
                waited++
                continue
            }

            val button = findButtonNode(root, keywords)
            if (button == null) {
                return true
            }

            val text = button.text?.toString()?.lowercase() ?: ""
            val desc = button.contentDescription?.toString()?.lowercase() ?: ""
            val isBusyText = listOf("generating", "rendering", "creating", "wait", "in progress", "loading")
                .any { text.contains(it) || desc.contains(it) }

            val isDisabled = !button.isEnabled || isBusyText
            button.recycle()

            if (!isDisabled) {
                return true
            }

            AutomationEngine.log("Generate button is busy/disabled. Waiting for ready state... (${waited + 1}s)")
            delay(1000)
            waited++
        }

        return false
    }

    // --- Prompt Input & Generate Click ---

    suspend fun executePromptInput(
        promptText: String,
        config: AutomationConfig,
        calibration: CalibrationData
    ): Boolean {
        val root = rootInActiveWindow
        val keywords = config.parseInputKeywords()

        var inputNode: AccessibilityNodeInfo? = null
        if (root != null && !config.useCoordinates) {
            inputNode = findInputNode(root, keywords)
        }

        if (inputNode != null) {
            try {
                AutomationEngine.log("Found prompt input element via node tree: [${inputNode.className}]")
                inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                delay(250)

                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, promptText)
                }
                val setSuccess = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

                if (setSuccess) {
                    AutomationEngine.log("Prompt text injected successfully into input field.")
                    return true
                }

                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("follow_prompt", promptText)
                clipboard.setPrimaryClip(clip)

                val pasteSuccess = inputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                if (pasteSuccess) {
                    AutomationEngine.log("Prompt pasted successfully via clipboard.")
                    return true
                }
            } finally {
                inputNode.recycle()
            }
        }

        val targetPoint = calibration.promptInput
        val tapX = if (targetPoint.isCalibrated) targetPoint.x else 540f
        val tapY = if (targetPoint.isCalibrated) targetPoint.y else 1350f

        AutomationEngine.log("Simulating realistic tap at Input coordinates (${tapX.toInt()}, ${tapY.toInt()})...")
        val tapped = simulateRealisticTap(tapX, tapY)
        if (tapped) {
            delay(500)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("follow_prompt", promptText)
            clipboard.setPrimaryClip(clip)

            val currentRoot = rootInActiveWindow
            val focused = currentRoot?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null) {
                focused.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                focused.recycle()
                AutomationEngine.log("Pasted prompt into focused input element after coordinate tap.")
                return true
            }
            AutomationEngine.log("Tapped input coordinate. Prompt copied to clipboard ready for paste.")
            return true
        }

        return false
    }

    suspend fun executeGenerateClick(
        config: AutomationConfig,
        calibration: CalibrationData
    ): Boolean {
        val root = rootInActiveWindow
        val keywords = config.parseGenerateKeywords()

        var buttonNode: AccessibilityNodeInfo? = null
        if (root != null && !config.useCoordinates) {
            buttonNode = findButtonNode(root, keywords)
        }

        if (buttonNode != null) {
            try {
                AutomationEngine.log("Found Generate button in DOM: '${buttonNode.text ?: buttonNode.contentDescription}'")
                val clicked = performClickOnNodeOrParent(buttonNode)
                if (clicked) {
                    AutomationEngine.log("Clicked Generate button via accessibility action.")
                    return true
                }

                val rect = Rect()
                buttonNode.getBoundsInScreen(rect)
                if (!rect.isEmpty) {
                    val cx = rect.centerX().toFloat()
                    val cy = rect.centerY().toFloat()
                    AutomationEngine.log("Simulating realistic tap at detected button center ($cx, $cy)")
                    return simulateRealisticTap(cx, cy)
                }
            } finally {
                buttonNode.recycle()
            }
        }

        val targetPoint = calibration.generateButton
        val tapX = if (targetPoint.isCalibrated) targetPoint.x else 900f
        val tapY = if (targetPoint.isCalibrated) targetPoint.y else 1550f

        AutomationEngine.log("Simulating realistic tap at Generate coordinates (${tapX.toInt()}, ${tapY.toInt()})...")
        return simulateRealisticTap(tapX, tapY)
    }

    // --- Node Helpers ---

    private fun findInputNode(root: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        if (isMatchingInputNode(root, keywords)) {
            return AccessibilityNodeInfo.obtain(root)
        }

        val count = root.childCount
        for (i in 0 until count) {
            val child = root.getChild(i) ?: continue
            val found = findInputNode(child, keywords)
            child.recycle()
            if (found != null) {
                return found
            }
        }

        if (root.isEditable) {
            return AccessibilityNodeInfo.obtain(root)
        }

        return null
    }

    private fun isMatchingInputNode(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.lowercase() ?: ""
        } else {
            ""
        }
        val className = node.className?.toString()?.lowercase() ?: ""

        val isEditable = node.isEditable || className.contains("edittext")
        val matchesKeyword = keywords.any { kw ->
            text.contains(kw) || desc.contains(kw) || viewId.contains(kw) || hint.contains(kw)
        }

        return isEditable || (matchesKeyword && (node.isFocusable || node.isClickable))
    }

    private fun findButtonNode(root: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val text = root.text?.toString()?.lowercase() ?: ""
        val desc = root.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = root.viewIdResourceName?.lowercase() ?: ""

        val matchesKeyword = keywords.any { kw ->
            text.contains(kw) || desc.contains(kw) || viewId.contains(kw)
        }

        if (matchesKeyword) {
            return AccessibilityNodeInfo.obtain(root)
        }

        val count = root.childCount
        for (i in 0 until count) {
            val child = root.getChild(i) ?: continue
            val found = findButtonNode(child, keywords)
            child.recycle()
            if (found != null) {
                return found
            }
        }

        return null
    }

    private fun findNodeMatchingKeywords(root: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val text = root.text?.toString()?.lowercase() ?: ""
        val desc = root.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = root.viewIdResourceName?.lowercase() ?: ""

        val matches = keywords.any { kw ->
            text.contains(kw) || desc.contains(kw) || viewId.contains(kw)
        }

        if (matches && (root.isClickable || root.isFocusable)) {
            return AccessibilityNodeInfo.obtain(root)
        }

        val count = root.childCount
        for (i in 0 until count) {
            val child = root.getChild(i) ?: continue
            val found = findNodeMatchingKeywords(child, keywords)
            child.recycle()
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                parent.recycle()
                return true
            }
            val nextParent = parent.parent
            parent.recycle()
            parent = nextParent
        }
        return false
    }

    suspend fun simulateRealisticTap(x: Float, y: Float, durationMs: Long = 100): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    deferred.complete(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    deferred.complete(false)
                }
            },
            null
        )

        if (!dispatched) {
            AutomationEngine.log("Gesture dispatch failed to initiate at ($x, $y).")
            return false
        }

        return withTimeoutOrNull(2500) { deferred.await() } ?: false
    }

    suspend fun simulateSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 350
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    deferred.complete(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    deferred.complete(false)
                }
            },
            null
        )

        if (!dispatched) {
            return false
        }

        return withTimeoutOrNull(2500) { deferred.await() } ?: false
    }
}
