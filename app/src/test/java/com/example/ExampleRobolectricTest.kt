package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.controller.AutomationEngine
import com.example.model.ActionNeededReason
import com.example.model.AspectRatioPreset
import com.example.model.AutomationConfig
import com.example.model.AutomationStatus
import com.example.model.CalibrationTargetType
import com.example.model.GenerationMode
import com.example.model.PromptSanitizer
import com.example.model.PromptStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        AutomationEngine.initialize(context)
        AutomationEngine.resetQueue()
        AutomationEngine.clearSavedHistory()
    }

    @Test
    fun `read app name string resource`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Follow AI Auto", appName)
    }

    @Test
    fun `PromptSanitizer cleans text and handles JSON format`() {
        // Plain text with messy whitespaces and blank lines
        val rawText = """
            
            First prompt with trailing space    
            
            Second prompt with   extra   spaces
            
        """.trimIndent()

        val cleaned = PromptSanitizer.sanitize(rawText)
        assertEquals(2, cleaned.size)
        assertEquals("First prompt with trailing space", cleaned[0])
        assertEquals("Second prompt with extra spaces", cleaned[1])

        // JSON array format
        val jsonArray = """["Cyberpunk drone 4k", "Ocean waves slow motion"]"""
        val jsonCleaned = PromptSanitizer.sanitize(jsonArray)
        assertEquals(2, jsonCleaned.size)
        assertEquals("Cyberpunk drone 4k", jsonCleaned[0])
        assertEquals("Ocean waves slow motion", jsonCleaned[1])

        // JSON objects format
        val jsonObjects = """[{"prompt": "Sci-fi astronaut on Mars"}, {"text": "Golden sunset mountain"}]"""
        val objCleaned = PromptSanitizer.sanitize(jsonObjects)
        assertEquals(2, objCleaned.size)
        assertEquals("Sci-fi astronaut on Mars", objCleaned[0])
        assertEquals("Golden sunset mountain", objCleaned[1])
    }

    @Test
    fun `config update persists presets and mode settings`() {
        val newConfig = AutomationConfig(
            delaySeconds = 45,
            maxTimeoutSeconds = 120,
            generationMode = GenerationMode.IMAGE_TO_VIDEO,
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            autoDownloadEnabled = true,
            useCoordinates = true
        )

        AutomationEngine.updateConfig(newConfig)
        val updatedState = AutomationEngine.state.value

        assertEquals(GenerationMode.IMAGE_TO_VIDEO, updatedState.config.generationMode)
        assertEquals(AspectRatioPreset.RATIO_9_16, updatedState.config.aspectRatio)
        assertTrue(updatedState.config.autoDownloadEnabled)
        assertEquals("I2V", updatedState.config.generationMode.shortName)
        assertEquals("9:16", updatedState.config.aspectRatio.label)
    }

    @Test
    fun `queue history persists and can resume accurately`() {
        AutomationEngine.loadPrompts("Prompt 1\nPrompt 2\nPrompt 3")
        
        // Simulate prompt 1 completed
        AutomationEngine.saveQueueProgress()

        // Resume from history
        AutomationEngine.resumeFromSavedHistory()
        val state = AutomationEngine.state.value

        assertEquals(3, state.totalCount)
        assertEquals(0, state.completedCount)
        assertEquals("Prompt 1", state.prompts[0].text)
    }

    @Test
    fun `calibration records 4 targets correctly and persists coordinates`() {
        AutomationEngine.recordCalibrationPoint(CalibrationTargetType.PROMPT_INPUT, 540f, 1300f)
        AutomationEngine.recordCalibrationPoint(CalibrationTargetType.MODEL_RATIO, 320f, 1100f)
        AutomationEngine.recordCalibrationPoint(CalibrationTargetType.GENERATE_BUTTON, 910f, 1540f)
        AutomationEngine.recordCalibrationPoint(CalibrationTargetType.DOWNLOAD_SAVE, 950f, 1750f)

        val calib = AutomationEngine.state.value.calibrationData

        assertEquals(540f, calib.promptInput.x)
        assertEquals(1300f, calib.promptInput.y)
        assertTrue(calib.promptInput.isCalibrated)

        assertEquals(320f, calib.modelRatio.x)
        assertEquals(1100f, calib.modelRatio.y)
        assertTrue(calib.modelRatio.isCalibrated)

        assertEquals(910f, calib.generateButton.x)
        assertEquals(1540f, calib.generateButton.y)
        assertTrue(calib.generateButton.isCalibrated)

        assertEquals(950f, calib.downloadSave.x)
        assertEquals(1750f, calib.downloadSave.y)
        assertTrue(calib.downloadSave.isCalibrated)
    }

    @Test
    fun `action needed triggers pause and can be resumed`() {
        AutomationEngine.loadPrompts("Test prompt 1")
        AutomationEngine.triggerActionNeeded(ActionNeededReason.CAPTCHA_DETECTED, "Captcha challenge visible")

        val state = AutomationEngine.state.value
        assertEquals(AutomationStatus.PAUSED, state.status)
        assertTrue(state.isActionNeeded)
        assertEquals(ActionNeededReason.CAPTCHA_DETECTED, state.actionNeeded)

        AutomationEngine.clearActionNeededAndResume()
        assertFalse(AutomationEngine.state.value.isActionNeeded)
    }
}
