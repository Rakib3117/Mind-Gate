package com.mindgate.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindgate.app.MindGateApp
import com.mindgate.app.service.MonitorService
import com.mindgate.app.ui.screens.GateOverlayActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                val ds = (context.applicationContext as MindGateApp).dataStore
                if (ds.serviceEnabled.first()) {
                    val svc = Intent(context, MonitorService::class.java)
                    context.startForegroundService(svc)
                }
            }
        }
    }
}

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            // Phone unlocked — check if unlock gate is enabled
            CoroutineScope(Dispatchers.IO).launch {
                val ds = (context.applicationContext as MindGateApp).dataStore
                val enabled = ds.unlockGateEnabled.first()
                val serviceEnabled = ds.serviceEnabled.first()
                if (enabled && serviceEnabled) {
                    val message = ds.unlockGateMessage.first()
                    val delay = ds.unlockGateDelay.first()
                    val overlayIntent = Intent(context, GateOverlayActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(GateOverlayActivity.EXTRA_MODE, GateOverlayActivity.MODE_UNLOCK_GATE)
                        putExtra(GateOverlayActivity.EXTRA_MESSAGE, message)
                        putExtra(GateOverlayActivity.EXTRA_DELAY, delay)
                    }
                    context.startActivity(overlayIntent)
                }
            }
        }
    }
}
