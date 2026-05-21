package com.nexusblock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.nexusblock.router.StrategyRouter
import com.nexusblock.vision.AdSkipOrchestrator
import com.nexusblock.vision.LiteRTClassifier
import com.nexusblock.vision.MediaProjectionCapture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Full AccessibilityService implementation for SSAI ad detection and auto-skip.
 *
 * Sprint 2 Feature Set:
 *   - Monitors foreground app changes via window events
 *   - Activates/deactivates AdSkipOrchestrator per StrategyRouter profile
 *   - Requests MediaProjection permission for visual ad classification
 *   - Manages AudioRecord for loudness spike detection
 *   - Auto-clicks "Skip Ad" buttons found in node tree
 *   - Mutes/unmutes audio around detected ad segments
 *   - Provides minimal anti-detect overlay feedback
 *
 * This service runs persistently when enabled by the user in TV Settings
 * → Accessibility → Argus Ad Blocker. It works alongside the VPN service.
 */
@AndroidEntryPoint
class ArgusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "Argus/Accessibility"
        private const val REQUEST_MEDIA_PROJECTION = 3001
        const val ACTION_REQUEST_MEDIA_PROJECTION = "com.nexusblock.REQUEST_MEDIA_PROJECTION"
    }

    @Inject
    lateinit var strategyRouter: StrategyRouter

    @Inject
    lateinit var liteRTClassifier: LiteRTClassifier

    private var adSkipOrchestrator: AdSkipOrchestrator? = null
    private var mediaProjectionCapture: MediaProjectionCapture? = null
    private var mediaProjection: MediaProjection? = null

    @Volatile
    private var isServiceReady = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AccessibilityService connected")
        isServiceReady = true

        // Initialize orchestrator (MediaProjection will be added later)
        adSkipOrchestrator = AdSkipOrchestrator(
            context = this,
            strategyRouter = strategyRouter,
            classifier = liteRTClassifier,
            accessibilityService = this
        )

        // Start strategy router if not already running
        strategyRouter.start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                handleAppChanged(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Content changed — the orchestrator may scan for skip buttons
                // This is handled internally by AdSkipOrchestrator on its own schedule
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        Log.i(TAG, "AccessibilityService destroyed")
        stopAllDetection()
        strategyRouter.stop()
        isServiceReady = false
        super.onDestroy()
    }

    /**
     * Called when the foreground app changes. Determine if we should
     * activate ad detection for this app.
     */
    private fun handleAppChanged(packageName: String) {
        val profile = strategyRouter.currentProfile.value
        val shouldUse = profile?.accessibility == true
        Log.d(TAG, "App changed: $packageName, accessibility=$shouldUse")

        if (shouldUse) {
            startDetection()
        } else {
            stopDetection()
        }
    }

    /**
     * Start all SSAI detection layers: audio, visual, orchestration.
     */
    private fun startDetection() {
        if (!isServiceReady) return

        // Ensure MediaProjection is available
        if (mediaProjection == null) {
            requestMediaProjection()
            // Start audio-only detection while waiting for projection
            adSkipOrchestrator?.start()
            return
        }

        // Start media projection capture if not already running
        if (mediaProjectionCapture == null) {
            mediaProjectionCapture = MediaProjectionCapture(this).apply {
                mediaProjection?.let { start(it) }
            }
            adSkipOrchestrator?.setMediaProjection(mediaProjectionCapture!!)
        }

        adSkipOrchestrator?.start()
        Log.i(TAG, "SSAI detection layers activated")
    }

    /**
     * Stop all detection but keep the service running.
     */
    private fun stopDetection() {
        adSkipOrchestrator?.stop()
        Log.i(TAG, "SSAI detection layers deactivated")
    }

    /**
     * Full stop: called on destroy or when user disables via settings.
     */
    private fun stopAllDetection() {
        adSkipOrchestrator?.stop()
        mediaProjectionCapture?.stop()
        mediaProjectionCapture = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    /**
     * Request MediaProjection permission from the user.
     * On Android TV, this shows a confirmation dialog.
     */
    private fun requestMediaProjection() {
        try {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = mgr.createScreenCaptureIntent()
            val broadcast = Intent(ACTION_REQUEST_MEDIA_PROJECTION)
            broadcast.setPackage(applicationContext.packageName)
            sendBroadcast(broadcast)
            Log.d(TAG, "Requested MediaProjection permission via broadcast")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request MediaProjection", e)
        }
    }

    /**
     * Called by MainActivity when MediaProjection permission is granted.
     */
    fun onMediaProjectionGranted(projection: MediaProjection) {
        Log.i(TAG, "MediaProjection granted")
        mediaProjection = projection
        startDetection()
    }

    /**
     * Get the current ad detection confidence (0.0 = content, 1.0 = ad).
     * Used by the dashboard UI to show real-time status.
     */
    fun getCurrentAdConfidence(): Float {
        return adSkipOrchestrator?.adConfidence?.value ?: 0.0f
    }


}
