package com.mindgate.app.data.model

data class AppConfig(
    val packageName: String,
    val appName: String,
    val iconBase64: String = "",
    // Feature 1: Session Timer
    val sessionTimerEnabled: Boolean = false,
    val defaultSessionMinutes: Int = 30,
    val lockAfterSessionMinutes: Int = 5,  // lock duration after session ends
    // Feature 2: Gate Message
    val gateMessageEnabled: Boolean = false,
    val gateMessage: String = "একটু ভাবো — এই মুহূর্তে কি সত্যিই এটা দরকার?",
    val gateDelaySeconds: Int = 3
)

data class AppSession(
    val packageName: String,
    val sessionStartTime: Long = 0L,
    val sessionDurationMinutes: Int = 0,
    val isLocked: Boolean = false,
    val lockUntil: Long = 0L
)

enum class OverlayType {
    GATE_MESSAGE,   // Feature 2: mindfulness message before app
    SESSION_SET,    // Feature 1: set session timer
    SESSION_LOCKED  // Feature 1: app locked screen
}
