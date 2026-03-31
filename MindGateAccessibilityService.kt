package com.mindgate.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.mindgate.app.MindGateApp
import com.mindgate.app.data.model.AppConfig
import com.mindgate.app.data.model.AppSession
import com.mindgate.app.ui.screens.GateOverlayActivity
import com.mindgate.app.ui.screens.SessionSetActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MindGateAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastPackage = ""
    private var configs: List<AppConfig> = emptyList()
    private var sessions: Map<String, AppSession> = emptyMap()

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch {
            val ds = (application as MindGateApp).dataStore
            ds.appConfigs.collect { configs = it }
        }
        scope.launch {
            val ds = (application as MindGateApp).dataStore
            ds.appSessions.collect { sessions = it }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return
        if (pkg == lastPackage) return
        lastPackage = pkg

        scope.launch {
            val ds = (application as MindGateApp).dataStore
            val serviceEnabled = ds.serviceEnabled.first()
            if (!serviceEnabled) return@launch

            val config = configs.find { it.packageName == pkg } ?: return@launch

            // Check if app is locked (Feature 1)
            val session = sessions[pkg]
            if (session != null && session.isLocked) {
                val now = System.currentTimeMillis()
                if (now < session.lockUntil) {
                    // Still locked — show locked screen
                    showOverlay(GateOverlayActivity.MODE_LOCKED, pkg, session.lockUntil, config)
                    return@launch
                } else {
                    // Lock expired, clear session
                    ds.clearSession(pkg)
                }
            }

            // Feature 1: Session timer — if enabled and no active session
            if (config.sessionTimerEnabled) {
                val hasActiveSession = session != null && !session.isLocked && session.sessionStartTime > 0
                if (!hasActiveSession) {
                    showSessionSet(pkg, config)
                    return@launch
                }
                // Check if session expired
                val elapsed = (System.currentTimeMillis() - (session?.sessionStartTime ?: 0)) / 60000
                if (session != null && elapsed >= session.sessionDurationMinutes) {
                    // Session over — lock it
                    val lockUntil = System.currentTimeMillis() + (config.lockAfterSessionMinutes * 60 * 1000L)
                    ds.saveSession(session.copy(isLocked = true, lockUntil = lockUntil))
                    showOverlay(GateOverlayActivity.MODE_LOCKED, pkg, lockUntil, config)
                    return@launch
                }
            }

            // Feature 2: Gate message
            if (config.gateMessageEnabled) {
                showOverlay(GateOverlayActivity.MODE_GATE, pkg, 0L, config)
            }
        }
    }

    private fun showOverlay(mode: String, pkg: String, lockUntil: Long, config: AppConfig) {
        val intent = Intent(this, GateOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(GateOverlayActivity.EXTRA_MODE, mode)
            putExtra(GateOverlayActivity.EXTRA_PACKAGE, pkg)
            putExtra(GateOverlayActivity.EXTRA_LOCK_UNTIL, lockUntil)
            putExtra(GateOverlayActivity.EXTRA_MESSAGE, config.gateMessage)
            putExtra(GateOverlayActivity.EXTRA_DELAY, config.gateDelaySeconds)
        }
        startActivity(intent)
    }

    private fun showSessionSet(pkg: String, config: AppConfig) {
        val intent = Intent(this, SessionSetActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(SessionSetActivity.EXTRA_PACKAGE, pkg)
            putExtra(SessionSetActivity.EXTRA_DEFAULT_MINUTES, config.defaultSessionMinutes)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
