package com.example.model

import org.json.JSONArray
import org.json.JSONObject

enum class AutomationStatus {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}

enum class PromptStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}

enum class GenerationMode(val displayName: String, val shortName: String, val description: String) {
    TEXT_TO_VIDEO("Text-to-Video (T2V)", "T2V", "Standard direct prompt injection into video generator"),
    IMAGE_TO_VIDEO("Image-to-Video (I2V)", "I2V", "Tap upload container sequence, wait for image attach, then inject prompt")
}

enum class AspectRatioPreset(val displayName: String, val label: String, val searchKeywords: List<String>) {
    RATIO_16_9("16:9 Landscape", "16:9", listOf("16:9", "landscape", "horizontal", "widescreen")),
    RATIO_9_16("9:16 Portrait", "9:16", listOf("9:16", "portrait", "vertical", "shorts", "reels")),
    RATIO_1_1("1:1 Square", "1:1", listOf("1:1", "square")),
    RATIO_DEFAULT("Keep Default", "Default", emptyList())
}

enum class CalibrationTargetType(val displayName: String, val description: String) {
    PROMPT_INPUT("Prompt Input Box", "Tap where prompts are typed or pasted"),
    MODEL_RATIO("Model / Ratio Selector", "Tap where Text-to-Video / Ratio toggle is"),
    GENERATE_BUTTON("Generate Button", "Tap where the Generate / Create button is"),
    DOWNLOAD_SAVE("Download / Save Icon", "Tap where generated videos are saved")
}

data class ScreenPoint(
    val x: Float = 0f,
    val y: Float = 0f,
    val isCalibrated: Boolean = false
)

data class CalibrationData(
    val promptInput: ScreenPoint = ScreenPoint(540f, 1350f, false),
    val modelRatio: ScreenPoint = ScreenPoint(300f, 1150f, false),
    val generateButton: ScreenPoint = ScreenPoint(900f, 1550f, false),
    val downloadSave: ScreenPoint = ScreenPoint(920f, 1750f, false)
) {
    fun getPoint(target: CalibrationTargetType): ScreenPoint = when (target) {
        CalibrationTargetType.PROMPT_INPUT -> promptInput
        CalibrationTargetType.MODEL_RATIO -> modelRatio
        CalibrationTargetType.GENERATE_BUTTON -> generateButton
        CalibrationTargetType.DOWNLOAD_SAVE -> downloadSave
    }

    fun updatePoint(target: CalibrationTargetType, x: Float, y: Float): CalibrationData = when (target) {
        CalibrationTargetType.PROMPT_INPUT -> copy(promptInput = ScreenPoint(x, y, true))
        CalibrationTargetType.MODEL_RATIO -> copy(modelRatio = ScreenPoint(x, y, true))
        CalibrationTargetType.GENERATE_BUTTON -> copy(generateButton = ScreenPoint(x, y, true))
        CalibrationTargetType.DOWNLOAD_SAVE -> copy(downloadSave = ScreenPoint(x, y, true))
    }
}

enum class ActionNeededReason(val label: String) {
    NONE("Normal"),
    CAPTCHA_DETECTED("Captcha Verification Detected"),
    LOGIN_REQUIRED("Login / Auth Modal Detected"),
    CREDITS_EXHAUSTED("Credits or Rate Limit Exceeded"),
    GENERATION_TIMEOUT("Generation Timeout")
}

data class PromptItem(
    val id: Int,
    val text: String,
    val status: PromptStatus = PromptStatus.PENDING,
    val errorMessage: String? = null
)

data class AutomationConfig(
    val delaySeconds: Int = 30,
    val maxTimeoutSeconds: Int = 180,
    val generationMode: GenerationMode = GenerationMode.TEXT_TO_VIDEO,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_DEFAULT,
    val autoDownloadEnabled: Boolean = true,
    val soundAlertEnabled: Boolean = true,
    val hapticAlertEnabled: Boolean = true,
    val downloadKeywords: String = "download, save, export, video",
    val inputKeywords: String = "prompt, describe, enter, write, story, idea",
    val generateKeywords: String = "generate, create, submit, render, start, make",
    val useCoordinates: Boolean = false,
    val smartPollingEnabled: Boolean = true,
    val modalDetectionEnabled: Boolean = true,
    val swipeBeforeInput: Boolean = false
) {
    fun parseInputKeywords(): List<String> =
        inputKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    fun parseGenerateKeywords(): List<String> =
        generateKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    fun parseDownloadKeywords(): List<String> =
        downloadKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
}

data class AutomationState(
    val status: AutomationStatus = AutomationStatus.IDLE,
    val prompts: List<PromptItem> = emptyList(),
    val currentIndex: Int = 0,
    val countdownSeconds: Int = 0,
    val currentStepMessage: String = "Ready to start",
    val logs: List<String> = emptyList(),
    val config: AutomationConfig = AutomationConfig(),
    val calibrationData: CalibrationData = CalibrationData(),
    val activeCalibrationTarget: CalibrationTargetType? = null,
    val isCalibrating: Boolean = false,
    val actionNeeded: ActionNeededReason = ActionNeededReason.NONE,
    val actionNeededMessage: String? = null,
    val isGenerateButtonBusy: Boolean = false,
    val isAccessibilityConnected: Boolean = false,
    val isOverlayRunning: Boolean = false,
    val hasSavedHistory: Boolean = false,
    val savedHistoryIndex: Int = 0,
    val savedHistoryTotal: Int = 0
) {
    val totalCount: Int get() = prompts.size
    val completedCount: Int get() = prompts.count { it.status == PromptStatus.DONE }
    val progress: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    val progressPercent: Int
        get() = (progress * 100).toInt()
    val isActionNeeded: Boolean
        get() = actionNeeded != ActionNeededReason.NONE
}

object PromptSanitizer {
    /**
     * Sanitizes raw text input or JSON payload:
     * - Parses JSON array if present (e.g., ["prompt 1", "prompt 2"] or [{"prompt": "..."}])
     * - Strips empty lines and removes leading/trailing whitespaces
     * - Replaces multiple inline whitespaces/tabs with single space
     * - Strips unprintable control characters
     */
    fun sanitize(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Check if JSON array
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                val jsonArray = JSONArray(trimmed)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.opt(i)
                    when (item) {
                        is String -> {
                            val clean = cleanLine(item)
                            if (clean.isNotBlank()) list.add(clean)
                        }
                        is JSONObject -> {
                            val promptText = item.optString("prompt", item.optString("text", ""))
                            val clean = cleanLine(promptText)
                            if (clean.isNotBlank()) list.add(clean)
                        }
                    }
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {
                // Fall back to line-by-line parsing
            }
        }

        // Line-by-line text processing
        return raw.lines()
            .map { cleanLine(it) }
            .filter { it.isNotBlank() }
    }

    private fun cleanLine(line: String): String {
        return line
            .replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "") // Remove control characters
            .replace(Regex("\\s+"), " ") // Normalize internal whitespaces
            .trim()
    }
}
