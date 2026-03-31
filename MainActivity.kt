package com.mindgate.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindgate.app.data.datastore.MindGateDataStore
import com.mindgate.app.data.model.AppConfig
import com.mindgate.app.service.MonitorService
import com.mindgate.app.ui.theme.MindGateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ds = (application as MindGateApp).dataStore

        setContent {
            MindGateTheme {
                MainScreen(ds)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(ds: MindGateDataStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val configs by ds.appConfigs.collectAsStateWithLifecycle(emptyList())
    val serviceEnabled by ds.serviceEnabled.collectAsStateWithLifecycle(false)
    val unlockGateEnabled by ds.unlockGateEnabled.collectAsStateWithLifecycle(false)
    val unlockGateMessage by ds.unlockGateMessage.collectAsStateWithLifecycle("শুভেচ্ছা! আজকের দিনটা সুন্দর করো।")
    val unlockGateDelay by ds.unlockGateDelay.collectAsStateWithLifecycle(3)

    var showAppPicker by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<AppConfig?>(null) }
    var currentTab by remember { mutableStateOf(0) }
    var showUnlockSettings by remember { mutableStateOf(false) }

    // Check permissions
    val hasAccessibility = remember { mutableStateOf(isAccessibilityEnabled(context)) }
    val hasOverlay = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    LaunchedEffect(Unit) {
        hasAccessibility.value = isAccessibilityEnabled(context)
        hasOverlay.value = Settings.canDrawOverlays(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050D1A), Color(0xFF0A1628))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D2040).copy(alpha = 0.8f))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("MindGate", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64FFDA))
                            Text("তোমার মনের দরজা", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Spacer(Modifier.weight(1f))
                        // Master toggle
                        Switch(
                            checked = serviceEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { ds.setServiceEnabled(enabled) }
                                if (enabled) {
                                    context.startForegroundService(Intent(context, MonitorService::class.java))
                                } else {
                                    context.stopService(Intent(context, MonitorService::class.java))
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF64FFDA),
                                checkedTrackColor = Color(0xFF1A3A5C)
                            )
                        )
                    }

                    // Permission warnings
                    if (!hasAccessibility.value || !hasOverlay.value) {
                        Spacer(Modifier.height(12.dp))
                        PermissionBanner(
                            hasAccessibility = hasAccessibility.value,
                            hasOverlay = hasOverlay.value,
                            context = context
                        )
                    }
                }
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A1628))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("অ্যাপ সেটিংস", "আনলক গেট").forEachIndexed { idx, title ->
                    val selected = currentTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Color(0xFF64FFDA) else Color(0xFF1A3A5C))
                            .clickable { currentTab = idx }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color(0xFF050D1A) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Content
            when (currentTab) {
                0 -> AppListTab(
                    configs = configs,
                    onAddApp = { showAppPicker = true },
                    onEditApp = { editingConfig = it },
                    onRemoveApp = { pkg -> scope.launch { ds.removeAppConfig(pkg) } }
                )
                1 -> UnlockGateTab(
                    enabled = unlockGateEnabled,
                    message = unlockGateMessage,
                    delay = unlockGateDelay,
                    onSave = { en, msg, del ->
                        scope.launch { ds.setUnlockGate(en, msg, del) }
                    }
                )
            }
        }

        // FAB for adding apps (tab 0)
        if (currentTab == 0) {
            FloatingActionButton(
                onClick = { showAppPicker = true },
                containerColor = Color(0xFF64FFDA),
                contentColor = Color(0xFF050D1A),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add App")
            }
        }
    }

    // App picker dialog
    if (showAppPicker) {
        AppPickerDialog(
            existingPackages = configs.map { it.packageName },
            onSelect = { pkg, name ->
                editingConfig = AppConfig(packageName = pkg, appName = name)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    // App config editor
    editingConfig?.let { config ->
        AppConfigDialog(
            config = config,
            onSave = { updated ->
                scope.launch { ds.saveAppConfig(updated) }
                editingConfig = null
            },
            onDismiss = { editingConfig = null }
        )
    }
}

@Composable
fun PermissionBanner(hasAccessibility: Boolean, hasOverlay: Boolean, context: android.content.Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!hasAccessibility) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7C2D12).copy(alpha = 0.4f))
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text("Accessibility Permission দাও →", fontSize = 13.sp, color = Color(0xFFFCA5A5))
            }
        }
        if (!hasOverlay) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7C2D12).copy(alpha = 0.4f))
                    .clickable {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text("Overlay Permission দাও →", fontSize = 13.sp, color = Color(0xFFFCA5A5))
            }
        }
    }
}

@Composable
fun AppListTab(
    configs: List<AppConfig>,
    onAddApp: () -> Unit,
    onEditApp: (AppConfig) -> Unit,
    onRemoveApp: (String) -> Unit
) {
    if (configs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("📱", fontSize = 64.sp)
                Text("কোনো অ্যাপ যোগ করা হয়নি", fontSize = 18.sp, color = Color(0xFF64748B))
                Text("নিচের + বোতামে চাপো", fontSize = 14.sp, color = Color(0xFF475569))
                Button(
                    onClick = onAddApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA), contentColor = Color(0xFF050D1A)),
                    shape = RoundedCornerShape(50)
                ) { Text("অ্যাপ যোগ করো") }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(configs, key = { it.packageName }) { config ->
                AppConfigCard(
                    config = config,
                    onEdit = { onEditApp(config) },
                    onRemove = { onRemoveApp(config.packageName) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AppConfigCard(config: AppConfig, onEdit: () -> Unit, onRemove: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D2040))
            .clickable { onEdit() }
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A3A5C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📱", fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(config.appName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E8F0))
                    Text(config.packageName, fontSize = 11.sp, color = Color(0xFF475569), maxLines = 1)
                }
                IconButton(onClick = { showDelete = !showDelete }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF64748B))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (config.sessionTimerEnabled) {
                    FeaturePill("⏱️ ${config.defaultSessionMinutes}m সেশন", Color(0xFF00B4D8))
                }
                if (config.gateMessageEnabled) {
                    FeaturePill("💭 গেট বার্তা", Color(0xFF64FFDA))
                }
                if (!config.sessionTimerEnabled && !config.gateMessageEnabled) {
                    FeaturePill("কোনো ফিচার নেই", Color(0xFF475569))
                }
            }

            AnimatedVisibility(showDelete) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDelete = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B))
                    ) { Text("বাদ দাও") }
                    Button(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
                    ) { Text("মুছে ফেলো") }
                }
            }
        }
    }
}

@Composable
fun FeaturePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 12.sp, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigDialog(config: AppConfig, onSave: (AppConfig) -> Unit, onDismiss: () -> Unit) {
    var sessionEnabled by remember { mutableStateOf(config.sessionTimerEnabled) }
    var defaultMins by remember { mutableStateOf(config.defaultSessionMinutes) }
    var lockMins by remember { mutableStateOf(config.lockAfterSessionMinutes) }
    var gateEnabled by remember { mutableStateOf(config.gateMessageEnabled) }
    var gateMsg by remember { mutableStateOf(config.gateMessage) }
    var gateDelay by remember { mutableStateOf(config.gateDelaySeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D2040),
        title = {
            Text(config.appName, color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Feature 1: Session Timer
                item {
                    SectionHeader("⏱️ সেশন টাইমার (Feature 1)")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("সক্রিয় করো", color = Color(0xFFE2E8F0))
                        Switch(
                            checked = sessionEnabled,
                            onCheckedChange = { sessionEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF64FFDA))
                        )
                    }
                    if (sessionEnabled) {
                        Spacer(Modifier.height(8.dp))
                        SettingSlider("ডিফল্ট সেশন সময়", defaultMins, 5, 120) { defaultMins = it }
                        Spacer(Modifier.height(8.dp))
                        SettingSlider("সেশন পরে লক (মিনিট)", lockMins, 1, 30) { lockMins = it }
                    }
                }

                // Feature 2: Gate Message
                item {
                    Divider(color = Color(0xFF1A3A5C))
                    Spacer(Modifier.height(4.dp))
                    SectionHeader("💭 গেট বার্তা (Feature 2)")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("সক্রিয় করো", color = Color(0xFFE2E8F0))
                        Switch(
                            checked = gateEnabled,
                            onCheckedChange = { gateEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF64FFDA))
                        )
                    }
                    if (gateEnabled) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = gateMsg,
                            onValueChange = { gateMsg = it },
                            label = { Text("বার্তা", color = Color(0xFF64748B)) },
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF64FFDA),
                                unfocusedBorderColor = Color(0xFF1A3A5C)
                            ),
                            minLines = 2
                        )
                        Spacer(Modifier.height(8.dp))
                        SettingSlider("Skip করার আগে অপেক্ষা (সেকেন্ড)", gateDelay, 0, 10) { gateDelay = it }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(config.copy(
                        sessionTimerEnabled = sessionEnabled,
                        defaultSessionMinutes = defaultMins,
                        lockAfterSessionMinutes = lockMins,
                        gateMessageEnabled = gateEnabled,
                        gateMessage = gateMsg,
                        gateDelaySeconds = gateDelay
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA), contentColor = Color(0xFF050D1A))
            ) { Text("সংরক্ষণ করো") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাদ দাও", color = Color(0xFF64748B)) }
        }
    )
}

@Composable
fun SectionHeader(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64FFDA), modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun SettingSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = Color(0xFF94A3B8))
            Text("$value মিনিট", fontSize = 13.sp, color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF64FFDA), activeTrackColor = Color(0xFF64FFDA))
        )
    }
}

@Composable
fun UnlockGateTab(enabled: Boolean, message: String, delay: Int, onSave: (Boolean, String, Int) -> Unit) {
    var localEnabled by remember(enabled) { mutableStateOf(enabled) }
    var localMessage by remember(message) { mutableStateOf(message) }
    var localDelay by remember(delay) { mutableStateOf(delay) }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0D2040))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("🔓 ফোন আনলক গেট", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64FFDA))
                    Text(
                        "ফোন আনলক করলেই একটি বার্তা দেখাবে।\nকিছু সময় পরে Skip করা যাবে।",
                        fontSize = 14.sp, color = Color(0xFF94A3B8), lineHeight = 22.sp
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("সক্রিয় করো", color = Color(0xFFE2E8F0), fontWeight = FontWeight.Medium)
                        Switch(
                            checked = localEnabled,
                            onCheckedChange = { localEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF64FFDA))
                        )
                    }

                    if (localEnabled) {
                        OutlinedTextField(
                            value = localMessage,
                            onValueChange = { localMessage = it },
                            label = { Text("আনলকের বার্তা", color = Color(0xFF64748B)) },
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF64FFDA),
                                unfocusedBorderColor = Color(0xFF1A3A5C)
                            ),
                            minLines = 3,
                            placeholder = { Text("যেমন: আজকে কী করবে?", color = Color(0xFF475569)) }
                        )

                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Skip বিলম্ব", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                Text("${localDelay} সেকেন্ড", fontSize = 13.sp, color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = localDelay.toFloat(),
                                onValueChange = { localDelay = it.toInt() },
                                valueRange = 0f..10f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF64FFDA), activeTrackColor = Color(0xFF64FFDA))
                            )
                        }
                    }

                    Button(
                        onClick = { onSave(localEnabled, localMessage, localDelay) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA), contentColor = Color(0xFF050D1A)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("সংরক্ষণ করো", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    existingPackages: List<String>,
    onSelect: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val installedApps = remember {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { it.packageName != context.packageName }
            .map { it.packageName to (pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.second }
    }

    val filtered = installedApps.filter {
        it.second.contains(searchQuery, ignoreCase = true) ||
        it.first.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D2040),
        title = { Text("অ্যাপ বেছে নাও", color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("খোঁজো...", color = Color(0xFF475569)) },
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF64FFDA),
                        unfocusedBorderColor = Color(0xFF1A3A5C)
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(filtered) { (pkg, name) ->
                        val alreadyAdded = pkg in existingPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (alreadyAdded) Color(0xFF1A3A5C).copy(0.5f) else Color.Transparent)
                                .clickable(enabled = !alreadyAdded) { onSelect(pkg, name) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📱", fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, fontSize = 15.sp, color = if (alreadyAdded) Color(0xFF475569) else Color(0xFFE2E8F0))
                                Text(pkg, fontSize = 11.sp, color = Color(0xFF334155), maxLines = 1)
                            }
                            if (alreadyAdded) Text("✓", color = Color(0xFF64FFDA))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করো", color = Color(0xFF64748B)) }
        }
    )
}

fun isAccessibilityEnabled(context: android.content.Context): Boolean {
    return try {
        val enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (enabled != 1) return false
        val services = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        services.contains(context.packageName)
    } catch (e: Exception) { false }
}
