package com.mindgate.app.ui.screens

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionSetActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_DEFAULT_MINUTES = "default_minutes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val defaultMinutes = intent.getIntExtra(EXTRA_DEFAULT_MINUTES, 30)

        setContent {
            MindGateTheme {
                SessionSetScreen(
                    packageName = pkg,
                    defaultMinutes = defaultMinutes,
                    onConfirm = { minutes ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val session = AppSession(
                                packageName = pkg,
                                sessionStartTime = System.currentTimeMillis(),
                                sessionDurationMinutes = minutes,
                                isLocked = false
                            )
                            (application as MindGateApp).dataStore.saveSession(session)
                        }
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    override fun onBackPressed() { finish() }
}

@Composable
fun SessionSetScreen(
    packageName: String,
    defaultMinutes: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(defaultMinutes) }
    val presets = listOf(5, 10, 15, 20, 30, 45, 60, 90)

    val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050D1A), Color(0xFF0A1628), Color(0xFF0D2040))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(glowPulse)
                .clip(CircleShape)
                .background(Color(0xFF00B4D8).copy(alpha = 0.07f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Text("⏱️", fontSize = 52.sp)

            Spacer(Modifier.height(16.dp))

            Text(
                text = "কতক্ষণ ব্যবহার করবে?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )

            Text(
                text = "সময় শেষে অ্যাপটি লক হয়ে যাবে",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(36.dp))

            // Big time display
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0D2040))
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = { if (selectedMinutes > 5) selectedMinutes -= 5 },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1A3A5C))
                            .size(44.dp)
                    ) {
                        Text("−", fontSize = 24.sp, color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$selectedMinutes",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64FFDA)
                        )
                        Text("মিনিট", fontSize = 14.sp, color = Color(0xFF94A3B8))
                    }

                    IconButton(
                        onClick = { if (selectedMinutes < 180) selectedMinutes += 5 },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1A3A5C))
                            .size(44.dp)
                    ) {
                        Text("+", fontSize = 24.sp, color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick presets
            Text("দ্রুত বেছে নাও:", fontSize = 13.sp, color = Color(0xFF64748B))
            Spacer(Modifier.height(12.dp))

            // Grid of preset chips
            val rows = presets.chunked(4)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { mins ->
                        val isSelected = selectedMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) Color(0xFF64FFDA)
                                    else Color(0xFF1A3A5C)
                                )
                                .clickable { selectedMinutes = mins }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0A0E1A) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B))
                ) {
                    Text("বাদ দাও", fontSize = 15.sp)
                }

                Button(
                    onClick = { onConfirm(selectedMinutes) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64FFDA),
                        contentColor = Color(0xFF050D1A)
                    )
                ) {
                    Text("শুরু করো ✓", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
