package com.mindgate.app.ui.screens

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindgate.app.MindGateApp
import com.mindgate.app.data.model.AppSession
import com.mindgate.app.ui.theme.MindGateTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class GateOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_LOCK_UNTIL = "lock_until"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_DELAY = "delay_seconds"
        const val MODE_GATE = "gate"
        const val MODE_UNLOCK_GATE = "unlock_gate"
        const val MODE_LOCKED = "locked"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_GATE
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val lockUntil = intent.getLongExtra(EXTRA_LOCK_UNTIL, 0L)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "একটু ভাবো..."
        val delaySeconds = intent.getIntExtra(EXTRA_DELAY, 3)

        setContent {
            MindGateTheme {
                when (mode) {
                    MODE_GATE, MODE_UNLOCK_GATE -> GateScreen(
                        message = message,
                        delaySeconds = delaySeconds,
                        onSkip = { finish() }
                    )
                    MODE_LOCKED -> LockedScreen(
                        lockUntil = lockUntil,
                        packageName = pkg,
                        onUnlock = {
                            CoroutineScope(Dispatchers.IO).launch {
                                (application as MindGateApp).dataStore.clearSession(pkg)
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        // Block back during gate — user must wait
    }
}

@Composable
fun GateScreen(message: String, delaySeconds: Int, onSkip: () -> Unit) {
    var remaining by remember { mutableStateOf(delaySeconds) }
    var canSkip by remember { mutableStateOf(delaySeconds == 0) }

    val breathScale by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOut), RepeatMode.Reverse
        ), label = "scale"
    )

    LaunchedEffect(Unit) {
        if (delaySeconds > 0) {
            repeat(delaySeconds) {
                delay(1000)
                remaining--
                if (remaining <= 0) canSkip = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0E1A), Color(0xFF0D1B2A), Color(0xFF112240))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background circles for atmosphere
        Box(
            modifier = Modifier
                .size(350.dp)
                .scale(breathScale)
                .clip(CircleShape)
                .background(Color(0xFF1A3A5C).copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .scale(breathScale * 0.9f)
                .clip(CircleShape)
                .background(Color(0xFF2A5F8F).copy(alpha = 0.2f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            // Lotus / mindful icon
            Text("🪷", fontSize = 56.sp, modifier = Modifier.scale(breathScale))

            Text(
                text = message,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(Modifier.height(8.dp))

            // Breathing hint
            Text(
                text = "শ্বাস নাও... শান্ত থাকো",
                fontSize = 14.sp,
                color = Color(0xFF64FFDA).copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = !canSkip) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1E3A5F))
                        .padding(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "${remaining} সেকেন্ড...",
                        color = Color(0xFF94A3B8),
                        fontSize = 16.sp
                    )
                }
            }

            AnimatedVisibility(visible = canSkip) {
                Button(
                    onClick = onSkip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64FFDA),
                        contentColor = Color(0xFF0A0E1A)
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(52.dp).widthIn(min = 160.dp)
                ) {
                    Text("এগিয়ে যাও →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LockedScreen(lockUntil: Long, packageName: String, onUnlock: () -> Unit) {
    var timeLeft by remember { mutableStateOf("") }

    LaunchedEffect(lockUntil) {
        while (true) {
            val remaining = lockUntil - System.currentTimeMillis()
            if (remaining <= 0) {
                timeLeft = "সময় শেষ"
                break
            }
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
            timeLeft = String.format("%02d:%02d", minutes, seconds)
            delay(1000)
        }
    }

    val lockPulse by rememberInfiniteTransition(label = "lock").animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0208), Color(0xFF1A0511), Color(0xFF2D0A1E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(lockPulse)
                .clip(CircleShape)
                .background(Color(0xFFFF4081).copy(alpha = 0.1f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text("🔒", fontSize = 72.sp, modifier = Modifier.scale(lockPulse))

            Text(
                text = "বিরতির সময়",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC1D4)
            )

            Text(
                text = "তোমার সেশন শেষ হয়েছে।\nএখন একটু বিশ্রাম নাও।",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2D0A1E))
                    .padding(horizontal = 36.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeLeft,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4081)
                )
            }

            Text(
                text = "পরে আবার আসতে পারবে ✨",
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )

            Spacer(Modifier.height(8.dp))

            if (timeLeft == "সময় শেষ") {
                Button(
                    onClick = onUnlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4081),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("অ্যাপে ফিরে যাও", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
