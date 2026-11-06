package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.controller.AutomationEngine
import com.example.ui.overlay.FloatingBubble
import com.example.ui.overlay.FloatingControlPanel
import com.example.ui.overlay.InteractiveCalibrationOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "follow_ai_floating_overlay_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var isExpanded by mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AutomationEngine.initialize(this)
        createNotificationChannel()
        startForegroundServiceNotification()
        setupOverlayWindow()
        observeCalibrationState()
        AutomationEngine.setOverlayRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Follow AI Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status while floating overlay is active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Follow AI Automator Overlay Active")
            .setContentText("Tap to open dashboard or use floating bubble over browser")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        overlayLifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
        }

        overlayComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)

            setContent {
                MyApplicationTheme(darkTheme = true) {
                    val state by AutomationEngine.state.collectAsState()

                    if (state.isCalibrating) {
                        InteractiveCalibrationOverlay()
                    } else if (isExpanded) {
                        FloatingControlPanel(
                            onMinimize = {
                                isExpanded = false
                            },
                            onCloseService = {
                                stopSelf()
                            }
                        )
                    } else {
                        FloatingBubble(
                            onExpand = {
                                isExpanded = true
                            }
                        )
                    }
                }
            }

            // Draggable Touch Listener
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var hasMoved = false

            setOnTouchListener { _, event ->
                // If calibrating, do not intercept drag, let Compose capture click coordinates
                if (AutomationEngine.state.value.isCalibrating) {
                    return@setOnTouchListener false
                }

                val params = windowParams ?: return@setOnTouchListener false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        hasMoved = false
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()

                        if (abs(dx) > 10 || abs(dy) > 10) {
                            hasMoved = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager?.updateViewLayout(this, params)
                            true
                        } else {
                            false
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        hasMoved
                    }

                    else -> false
                }
            }
        }

        try {
            windowManager?.addView(overlayComposeView, windowParams)
        } catch (e: Exception) {
            AutomationEngine.log("Error adding overlay to WindowManager: ${e.message}")
        }
    }

    private fun observeCalibrationState() {
        serviceScope.launch {
            var lastCalibrating = false
            AutomationEngine.state.collect { state ->
                if (state.isCalibrating != lastCalibrating) {
                    lastCalibrating = state.isCalibrating
                    val params = windowParams ?: return@collect
                    val view = overlayComposeView ?: return@collect

                    if (state.isCalibrating) {
                        // Expand to full-screen to capture touch anywhere
                        params.width = WindowManager.LayoutParams.MATCH_PARENT
                        params.height = WindowManager.LayoutParams.MATCH_PARENT
                        params.x = 0
                        params.y = 0
                    } else {
                        // Restore compact window
                        params.width = WindowManager.LayoutParams.WRAP_CONTENT
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT
                        params.x = 40
                        params.y = 200
                    }
                    try {
                        windowManager?.updateViewLayout(view, params)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        AutomationEngine.setOverlayRunning(false)
        overlayLifecycleOwner?.onDestroy()
        overlayComposeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayComposeView = null
        windowManager = null
        super.onDestroy()
    }
}
