package com.example.controller

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.example.model.ActionNeededReason
import com.example.model.AspectRatioPreset
import com.example.model.AutomationConfig
import com.example.model.AutomationState
import com.example.model.AutomationStatus
import com.example.model.CalibrationData
import com.example.model.CalibrationTargetType
import com.example.model.GenerationMode
import com.example.model.PromptItem
import com.example.model.PromptSanitizer
import com.example.model.PromptStatus
import com.example.model.ScreenPoint
import com.example.service.FollowAIEngineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutomationEngine {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var executionJob: Job? = null

    private val _state = MutableStateFlow(AutomationState())
    val state: StateFlow<AutomationState> = _state.asStateFlow()

    private var activeService: FollowAIEngineService? = null
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = context.getSharedPreferences("follow_ai_auto_prefs", Context.MODE_PRIVATE)
            loadConfigAndCalibration()
            checkSavedHistory()
        }
    }

    private fun loadConfigAndCalibration() {
        val p = prefs ?: return
        val delaySec = p.getInt("delay_seconds", 30)
        val maxTimeout = p.getInt("max_timeout_seconds", 180)
        val genModeStr = p.getString("generation_mode", GenerationMode.TEXT_TO_VIDEO.name) ?: GenerationMode.TEXT_TO_VIDEO.name
        val aspectStr = p.getString("aspect_ratio", AspectRatioPreset.RATIO_DEFAULT.name) ?: AspectRatioPreset.RATIO_DEFAULT.name
        val autoDownload = p.getBoolean("auto_download_enabled", true)
        val soundAlert = p.getBoolean("sound_alert_enabled", true)
        val hapticAlert = p.getBoolean("haptic_alert_enabled", true)
        val dlKeywords = p.getString("download_keywords", "download, save, export, video") ?: "download"
        val inKeywords = p.getString("input_keywords", "prompt, describe, enter, write, story, idea") ?: "prompt"
        val genKeywords = p.getString("generate_keywords", "generate, create, submit, render, start, make") ?: "generate"
        val useCoord = p.getBoolean("use_coordinates", false)
        val smartPolling = p.getBoolean("smart_polling_enabled", true)
        val modalDetect = p.getBoolean("modal_detection_enabled", true)

        val genMode = try { GenerationMode.valueOf(genModeStr) } catch (_: Exception) { GenerationMode.TEXT_TO_VIDEO }
        val aspect = try { AspectRatioPreset.valueOf(aspectStr) } catch (_: Exception) { AspectRatioPreset.RATIO_DEFAULT }

        // Calibration data
        val inX = p.getFloat("calib_prompt_input_x", 540f)
        val inY = p.getFloat("calib_prompt_input_y", 1350f)
        val inSet = p.getBoolean("calib_prompt_input_set", false)

        val modX = p.getFloat("calib_model_ratio_x", 300f)
        val modY = p.getFloat("calib_model_ratio_y", 1150f)
        val modSet = p.getBoolean("calib_model_ratio_set", false)

        val genX = p.getFloat("calib_generate_btn_x", 900f)
        val genY = p.getFloat("calib_generate_btn_y", 1550f)
        val genSet = p.getBoolean("calib_generate_btn_set", false)

        val dlX = p.getFloat("calib_download_x", 920f)
        val dlY = p.getFloat("calib_download_y", 1750f)
        val dlSet = p.getBoolean("calib_download_set", false)

        val calibration = CalibrationData(
            promptInput = ScreenPoint(inX, inY, inSet),
            modelRatio = ScreenPoint(modX, modY, modSet),
            generateButton = ScreenPoint(genX, genY, genSet),
            downloadSave = ScreenPoint(dlX, dlY, dlSet)
        )

        _state.update {
            it.copy(
                config = AutomationConfig(
                    delaySeconds = delaySec,
                    maxTimeoutSeconds = maxTimeout,
                    generationMode = genMode,
                    aspectRatio = aspect,
                    autoDownloadEnabled = autoDownload,
                    soundAlertEnabled = soundAlert,
                    hapticAlertEnabled = hapticAlert,
                    downloadKeywords = dlKeywords,
                    inputKeywords = inKeywords,
                    generateKeywords = genKeywords,
                    useCoordinates = useCoord,
                    smartPollingEnabled = smartPolling,
                    modalDetectionEnabled = modalDetect
                ),
                calibrationData = calibration
            )
        }
    }

    fun updateConfig(config: AutomationConfig) {
        _state.update { it.copy(config = config) }
        prefs?.edit()?.apply {
            putInt("delay_seconds", config.delaySeconds)
            putInt("max_timeout_seconds", config.maxTimeoutSeconds)
            putString("generation_mode", config.generationMode.name)
            putString("aspect_ratio", config.aspectRatio.name)
            putBoolean("auto_download_enabled", config.autoDownloadEnabled)
            putBoolean("sound_alert_enabled", config.soundAlertEnabled)
            putBoolean("haptic_alert_enabled", config.hapticAlertEnabled)
            putString("download_keywords", config.downloadKeywords)
            putString("input_keywords", config.inputKeywords)
            putString("generate_keywords", config.generateKeywords)
            putBoolean("use_coordinates", config.useCoordinates)
            putBoolean("smart_polling_enabled", config.smartPollingEnabled)
            putBoolean("modal_detection_enabled", config.modalDetectionEnabled)
            apply()
        }
        log("Saved config: mode=${config.generationMode.shortName}, ratio=${config.aspectRatio.label}, autoDownload=${config.autoDownloadEnabled}")
    }

    // --- History & Queue Persistence ---

    private fun checkSavedHistory() {
        val p = prefs ?: return
        val savedJson = p.getString("history_prompts_json", null) ?: return
        try {
            val arr = JSONArray(savedJson)
            if (arr.length() > 0) {
                val lastIdx = p.getInt("history_current_index", 0)
                _state.update {
                    it.copy(
                        hasSavedHistory = true,
                        savedHistoryIndex = lastIdx + 1,
                        savedHistoryTotal = arr.length()
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun saveQueueProgress() {
        val s = _state.value
        if (s.prompts.isEmpty()) return
        val p = prefs ?: return

        try {
            val arr = JSONArray()
            s.prompts.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("status", item.status.name)
                }
                arr.put(obj)
            }
            p.edit().apply {
                putString("history_prompts_json", arr.toString())
                putInt("history_current_index", s.currentIndex)
                apply()
            }
        } catch (_: Exception) {}
    }

    fun resumeFromSavedHistory() {
        val p = prefs ?: return
        val savedJson = p.getString("history_prompts_json", null) ?: return
        try {
            val arr = JSONArray(savedJson)
            val items = mutableListOf<PromptItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("id")
                val text = obj.getString("text")
                val statusStr = obj.getString("status")
                val status = try { PromptStatus.valueOf(statusStr) } catch (_: Exception) { PromptStatus.PENDING }
                items.add(PromptItem(id = id, text = text, status = status))
            }

            val pendingIdx = items.indexOfFirst { it.status == PromptStatus.PENDING }.coerceAtLeast(0)

            _state.update {
                it.copy(
                    prompts = items,
                    currentIndex = pendingIdx,
                    hasSavedHistory = false,
                    currentStepMessage = "Resumed queue at prompt #${pendingIdx + 1} of ${items.size}"
                )
            }
            log("Resumed saved queue from browser/session restart at item #${pendingIdx + 1}/${items.size}.")
            showToast("Queue Resumed at #${pendingIdx + 1} of ${items.size}")
        } catch (e: Exception) {
            log("Failed to restore saved history: ${e.message}")
        }
    }

    fun clearSavedHistory() {
        prefs?.edit()?.apply {
            remove("history_prompts_json")
            remove("history_current_index")
            apply()
        }
        _state.update { it.copy(hasSavedHistory = false) }
        log("Saved queue history cleared.")
    }

    // --- Bulk File Import & Sanitation ---

    fun loadPrompts(rawText: String) {
        val cleanList = PromptSanitizer.sanitize(rawText)

        val items = cleanList.mapIndexed { index, line ->
            PromptItem(id = index + 1, text = line, status = PromptStatus.PENDING)
        }

        _state.update {
            it.copy(
                prompts = items,
                currentIndex = 0,
                countdownSeconds = 0,
                currentStepMessage = if (items.isNotEmpty()) "${items.size} sanitized prompts ready" else "No prompts loaded"
            )
        }
        saveQueueProgress()
        log("Imported and sanitized ${items.size} prompts into the queue.")
    }

    // --- Calibration Control ---

    fun startCalibration(target: CalibrationTargetType? = null) {
        val nextTarget = target ?: CalibrationTargetType.PROMPT_INPUT
        _state.update {
            it.copy(
                isCalibrating = true,
                activeCalibrationTarget = nextTarget
            )
        }
        log("Touch Calibration started. Tap on screen for: [${nextTarget.displayName}]")
    }

    fun recordCalibrationPoint(target: CalibrationTargetType, x: Float, y: Float) {
        val updated = _state.value.calibrationData.updatePoint(target, x, y)
        _state.update { it.copy(calibrationData = updated) }

        prefs?.edit()?.apply {
            when (target) {
                CalibrationTargetType.PROMPT_INPUT -> {
                    putFloat("calib_prompt_input_x", x)
                    putFloat("calib_prompt_input_y", y)
                    putBoolean("calib_prompt_input_set", true)
                }
                CalibrationTargetType.MODEL_RATIO -> {
                    putFloat("calib_model_ratio_x", x)
                    putFloat("calib_model_ratio_y", y)
                    putBoolean("calib_model_ratio_set", true)
                }
                CalibrationTargetType.GENERATE_BUTTON -> {
                    putFloat("calib_generate_btn_x", x)
                    putFloat("calib_generate_btn_y", y)
                    putBoolean("calib_generate_btn_set", true)
                }
                CalibrationTargetType.DOWNLOAD_SAVE -> {
                    putFloat("calib_download_x", x)
                    putFloat("calib_download_y", y)
                    putBoolean("calib_download_set", true)
                }
            }
            apply()
        }

        log("Calibrated [${target.displayName}] at (${x.toInt()}, ${y.toInt()})")

        val allTargets = CalibrationTargetType.values()
        val currentIndex = allTargets.indexOf(target)
        if (currentIndex < allTargets.size - 1) {
            val next = allTargets[currentIndex + 1]
            _state.update { it.copy(activeCalibrationTarget = next) }
            log("Next target: Tap on screen for [${next.displayName}]")
        } else {
            _state.update {
                it.copy(
                    isCalibrating = false,
                    activeCalibrationTarget = null
                )
            }
            log("Touch Calibration complete! All 4 targets saved successfully.")
            showToast("Calibration Complete! 4 targets saved.")
        }
    }

    fun cancelCalibration() {
        _state.update {
            it.copy(
                isCalibrating = false,
                activeCalibrationTarget = null
            )
        }
        log("Touch Calibration cancelled.")
    }

    fun testTapTarget(target: CalibrationTargetType) {
        val point = _state.value.calibrationData.getPoint(target)
        val service = activeService
        if (service == null) {
            log("Cannot test tap: Accessibility Service not connected.")
            return
        }

        scope.launch {
            log("Testing realistic tap on [${target.displayName}] at (${point.x.toInt()}, ${point.y.toInt()})...")
            val success = service.simulateRealisticTap(point.x, point.y)
            if (success) {
                log("Tap on [${target.displayName}] dispatched successfully.")
            } else {
                log("Failed to dispatch tap on [${target.displayName}].")
            }
        }
    }

    // --- Action Needed / Modal Detection ---

    fun triggerActionNeeded(reason: ActionNeededReason, message: String) {
        _state.update {
            it.copy(
                status = AutomationStatus.PAUSED,
                actionNeeded = reason,
                actionNeededMessage = message,
                currentStepMessage = "ACTION NEEDED: $message"
            )
        }
        log("[ACTION NEEDED] $message - Queue automatically paused.")
        showToast("⚠️ Action Needed: $message")
        triggerCompletionAlert(isQueueFinished = false)
    }

    fun clearActionNeededAndResume() {
        _state.update {
            it.copy(
                actionNeeded = ActionNeededReason.NONE,
                actionNeededMessage = null
            )
        }
        log("Action cleared by user. Resuming queue...")
        resumeQueue()
    }

    private fun showToast(text: String) {
        Handler(Looper.getMainLooper()).post {
            appContext?.let {
                Toast.makeText(it, text, Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Sound & Haptic Alerts ---

    private fun triggerCompletionAlert(isQueueFinished: Boolean) {
        val ctx = appContext ?: return
        val config = _state.value.config

        // Haptic feedback
        if (config.hapticAlertEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    val vibrator = vibratorManager?.defaultVibrator
                    if (isQueueFinished) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150, 100, 350), -1))
                    } else {
                        vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (isQueueFinished) {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(longArrayOf(0, 150, 100, 150, 100, 350), -1)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(300)
                    }
                }
            } catch (_: Exception) {}
        }

        // Sound alert
        if (config.soundAlertEnabled) {
            try {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(ctx, soundUri)
                ringtone?.play()
            } catch (_: Exception) {}
        }
    }

    // --- Queue Execution Loop ---

    fun bindService(service: FollowAIEngineService) {
        activeService = service
        _state.update { it.copy(isAccessibilityConnected = true) }
        log("Accessibility Service connected successfully.")
    }

    fun unbindService() {
        activeService = null
        _state.update { it.copy(isAccessibilityConnected = false) }
        log("Accessibility Service disconnected.")
    }

    fun setOverlayRunning(running: Boolean) {
        _state.update { it.copy(isOverlayRunning = running) }
    }

    fun startQueue() {
        val currentState = _state.value
        if (currentState.prompts.isEmpty()) {
            log("Cannot start: prompt queue is empty!")
            _state.update { it.copy(currentStepMessage = "Please add prompts first") }
            return
        }

        if (activeService == null) {
            log("Warning: Accessibility Service is not active. Please enable in Settings.")
            _state.update { it.copy(status = AutomationStatus.ERROR, currentStepMessage = "Accessibility Service inactive") }
            return
        }

        _state.update {
            it.copy(
                status = AutomationStatus.RUNNING,
                actionNeeded = ActionNeededReason.NONE,
                actionNeededMessage = null
            )
        }
        log("Started queue processing (${currentState.prompts.size} items) in ${currentState.config.generationMode.shortName} mode.")

        executionJob?.cancel()
        executionJob = scope.launch {
            runExecutionLoop()
        }
    }

    fun pauseQueue() {
        if (_state.value.status == AutomationStatus.RUNNING) {
            _state.update {
                it.copy(
                    status = AutomationStatus.PAUSED,
                    currentStepMessage = "Queue paused"
                )
            }
            saveQueueProgress()
            log("Queue paused by user.")
        }
    }

    fun resumeQueue() {
        if (_state.value.status == AutomationStatus.PAUSED) {
            _state.update {
                it.copy(
                    status = AutomationStatus.RUNNING,
                    actionNeeded = ActionNeededReason.NONE,
                    actionNeededMessage = null
                )
            }
            log("Resuming queue execution.")
            executionJob?.cancel()
            executionJob = scope.launch {
                runExecutionLoop()
            }
        }
    }

    fun stopQueue() {
        executionJob?.cancel()
        executionJob = null
        _state.update {
            it.copy(
                status = AutomationStatus.STOPPED,
                countdownSeconds = 0,
                currentStepMessage = "Queue stopped"
            )
        }
        saveQueueProgress()
        log("Queue stopped by user.")
    }

    fun resetQueue() {
        executionJob?.cancel()
        executionJob = null
        _state.update { current ->
            current.copy(
                status = AutomationStatus.IDLE,
                currentIndex = 0,
                countdownSeconds = 0,
                actionNeeded = ActionNeededReason.NONE,
                actionNeededMessage = null,
                currentStepMessage = "Queue reset",
                prompts = current.prompts.map { it.copy(status = PromptStatus.PENDING, errorMessage = null) }
            )
        }
        saveQueueProgress()
        log("Queue reset to initial state.")
    }

    private suspend fun runExecutionLoop() {
        while (_state.value.status == AutomationStatus.RUNNING) {
            val s = _state.value
            val pendingIndex = s.prompts.indexOfFirst { it.status == PromptStatus.PENDING }

            if (pendingIndex == -1) {
                _state.update {
                    it.copy(
                        status = AutomationStatus.COMPLETED,
                        countdownSeconds = 0,
                        currentStepMessage = "All ${it.prompts.size} prompts completed! 🎉"
                    )
                }
                saveQueueProgress()
                log("All ${s.prompts.size} prompts finished processing!")
                triggerCompletionAlert(isQueueFinished = true)
                showToast("🎉 Follow AI: All ${s.prompts.size} videos finished!")
                break
            }

            val currentItem = s.prompts[pendingIndex]
            _state.update {
                it.copy(
                    currentIndex = pendingIndex,
                    currentStepMessage = "Processing ${pendingIndex + 1}/${it.prompts.size}...",
                    prompts = it.prompts.mapIndexed { idx, p ->
                        if (idx == pendingIndex) p.copy(status = PromptStatus.PROCESSING) else p
                    }
                )
            }

            log("Processing item #${currentItem.id}: \"${currentItem.text.take(45)}...\"")

            val service = activeService
            if (service == null) {
                log("Error: Accessibility service disconnected during execution.")
                _state.update {
                    it.copy(
                        status = AutomationStatus.ERROR,
                        currentStepMessage = "Accessibility service lost"
                    )
                }
                break
            }

            // --- Smart Pre-Check: Captcha / Login Modals ---
            if (s.config.modalDetectionEnabled) {
                val modalDetection = service.detectModalOrCaptcha()
                if (modalDetection != null) {
                    triggerActionNeeded(modalDetection.first, modalDetection.second)
                    break
                }
            }

            // --- Module 7: Ratio Lock Preset Sequence ---
            if (s.config.aspectRatio != AspectRatioPreset.RATIO_DEFAULT) {
                _state.update { it.copy(currentStepMessage = "Locking aspect ratio: ${s.config.aspectRatio.label}...") }
                service.executeAspectRatioSequence(s.config.aspectRatio, s.calibrationData)
                delay(600)
            }

            // --- Module 7: Image-to-Video (I2V) Sequence ---
            if (s.config.generationMode == GenerationMode.IMAGE_TO_VIDEO) {
                _state.update { it.copy(currentStepMessage = "I2V Mode: Triggering image attach container...") }
                service.executeImageUploadSequence(s.calibrationData)
                delay(1800) // Allow container attach / file picker trigger
            }

            // Step 1: Locate and input the prompt
            _state.update { it.copy(currentStepMessage = "Finding input box (#${currentItem.id})...") }
            val inputSuccess = service.executePromptInput(currentItem.text, s.config, s.calibrationData)

            if (!inputSuccess) {
                log("Failed to locate or fill prompt box for item #${currentItem.id}. Retrying once...")
                delay(1500)
                val retrySuccess = service.executePromptInput(currentItem.text, s.config, s.calibrationData)
                if (!retrySuccess) {
                    log("Skipping item #${currentItem.id} after retry failure.")
                    _state.update {
                        it.copy(
                            prompts = it.prompts.mapIndexed { idx, p ->
                                if (idx == pendingIndex) p.copy(status = PromptStatus.FAILED, errorMessage = "Input failed") else p
                            }
                        )
                    }
                    saveQueueProgress()
                    continue
                }
            }

            // Small delay to allow web app reactivity / button state change
            delay(1200)

            // --- Smart Polling: Wait for Generate Button to become ready/idle ---
            if (s.config.smartPollingEnabled) {
                _state.update { it.copy(currentStepMessage = "Checking Generate button state...", isGenerateButtonBusy = true) }
                val ready = service.pollUntilGenerateReady(s.config, maxWaitSeconds = 15)
                _state.update { it.copy(isGenerateButtonBusy = false) }

                if (!ready) {
                    log("Warning: Generate button appears disabled or rendering. Attempting click with caution...")
                }
            }

            // Step 2: Locate and click Generate button
            _state.update { it.copy(currentStepMessage = "Clicking Generate (#${currentItem.id})...") }
            val clickSuccess = service.executeGenerateClick(s.config, s.calibrationData)

            if (!clickSuccess) {
                log("Failed to click Generate button for item #${currentItem.id}. Retrying once...")
                delay(1500)
                service.executeGenerateClick(s.config, s.calibrationData)
            }

            // Step 3: Wait for render delay with countdown and timeout protection
            log("Prompt #${currentItem.id} submitted! Waiting ${s.config.delaySeconds}s for generation...")
            var remaining = s.config.delaySeconds
            val maxTimeout = s.config.maxTimeoutSeconds
            var totalWaited = 0

            while (remaining > 0 && _state.value.status == AutomationStatus.RUNNING) {
                _state.update {
                    it.copy(
                        countdownSeconds = remaining,
                        currentStepMessage = "Rendering video (#${currentItem.id}): ${remaining}s left"
                    )
                }
                delay(1000)
                remaining--
                totalWaited++

                if (totalWaited % 10 == 0 && s.config.modalDetectionEnabled) {
                    val popup = service.detectModalOrCaptcha()
                    if (popup != null) {
                        triggerActionNeeded(popup.first, popup.second)
                        break
                    }
                }

                if (totalWaited >= maxTimeout) {
                    log("Warning: Generation wait exceeded timeout (${maxTimeout}s). Proceeding...")
                    break
                }
            }

            if (_state.value.status != AutomationStatus.RUNNING) {
                break
            }

            // --- Module 8: Auto-Download Verification ---
            if (s.config.autoDownloadEnabled) {
                _state.update { it.copy(currentStepMessage = "Auto-Downloading video (#${currentItem.id})...") }
                val dlSuccess = service.executeDownloadClick(s.config, s.calibrationData)
                if (dlSuccess) {
                    log("Triggered Auto-Download for prompt #${currentItem.id}.")
                } else {
                    log("Auto-Download button not found or tap dispatched to fallback.")
                }
            }

            // Mark prompt DONE
            _state.update {
                it.copy(
                    prompts = it.prompts.mapIndexed { idx, p ->
                        if (idx == pendingIndex) p.copy(status = PromptStatus.DONE) else p
                    }
                )
            }
            saveQueueProgress()
        }
    }

    fun log(message: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $message"
        _state.update {
            val updated = (it.logs + entry).takeLast(100)
            it.copy(logs = updated)
        }
    }

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
    }
}
