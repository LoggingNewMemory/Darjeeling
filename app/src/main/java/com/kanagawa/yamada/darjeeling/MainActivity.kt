package com.kanagawa.yamada.darjeeling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kanagawa.yamada.darjeeling.ui.theme.DarjeelingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DarjeelingTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Darjeeling", fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    CaffeineSettingsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CaffeineSettingsScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
    
    var useRoot by remember { mutableStateOf(prefs.getBoolean("use_root", false)) }
    var isRootAvailable by remember { mutableStateOf(prefs.getBoolean("root_available", false)) }
    var isCheckingRoot by remember { mutableStateOf(false) }
    var hasWriteSettingsPermission by remember { mutableStateOf(Settings.System.canWrite(context)) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasWriteSettingsPermission = Settings.System.canWrite(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    var currentState by remember { mutableStateOf(CaffeineService.currentState) }
    var currentEndTime by remember { mutableStateOf(CaffeineService.endTime) }
    var displayTime by remember { mutableStateOf("") }
    
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentState = CaffeineService.currentState
                currentEndTime = CaffeineService.endTime
            }
        }
        val filter = IntentFilter(CaffeineTileService.ACTION_UPDATE_TILE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }
    
    LaunchedEffect(currentState, currentEndTime) {
        while (true) {
            if (currentState in 1..4) {
                val remainingMillis = currentEndTime - System.currentTimeMillis()
                if (remainingMillis > 0) {
                    val totalSeconds = remainingMillis / 1000
                    val m = totalSeconds / 60
                    val s = totalSeconds % 60
                    displayTime = String.format("%02d:%02d", m, s)
                } else {
                    displayTime = ""
                }
            } else {
                displayTime = ""
            }
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Control Darjeeling", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val statusText = when (currentState) {
                    0 -> "Off"
                    1 -> "5 minutes ${if (displayTime.isNotEmpty()) "($displayTime left)" else ""}"
                    2 -> "10 minutes ${if (displayTime.isNotEmpty()) "($displayTime left)" else ""}"
                    3 -> "15 minutes ${if (displayTime.isNotEmpty()) "($displayTime left)" else ""}"
                    4 -> "30 minutes ${if (displayTime.isNotEmpty()) "($displayTime left)" else ""}"
                    5 -> "Unlimited"
                    else -> "Off"
                }
                
                Text(
                    text = "Current Status: $statusText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        CaffeineService.currentState = when (CaffeineService.currentState) {
                            0 -> 1
                            1 -> 2
                            2 -> 3
                            3 -> 4
                            4 -> 5
                            5 -> 0
                            else -> 0
                        }
                        
                        val intent = Intent(context, CaffeineService::class.java)
                        intent.action = CaffeineService.ACTION_APPLY_CURRENT_STATE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentState == 0) "Start Darjeeling" else "Cycle Timer")
                }
            }
        }
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Operating Mode", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Non-Root Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = !useRoot,
                        onClick = { 
                            useRoot = false
                            prefs.edit().putBoolean("use_root", false).apply()
                        }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Non-Root Method", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Requires WRITE_SETTINGS permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!useRoot && !hasWriteSettingsPermission) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .padding(start = 48.dp, top = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Grant Permission")
                    }
                } else if (!useRoot && hasWriteSettingsPermission) {
                    Text(
                        text = "Permission Granted \u2713",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 48.dp, top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Root Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = useRoot,
                        enabled = isRootAvailable,
                        onClick = { 
                            if (isRootAvailable) {
                                useRoot = true
                                prefs.edit().putBoolean("use_root", true).apply()
                            }
                        }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Root Method", 
                            fontWeight = FontWeight.Medium,
                            color = if (isRootAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        Text(
                            text = if (isRootAvailable) "Root access granted" else "Requires Magisk/SU",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRootAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!isRootAvailable) {
                    FilledTonalButton(
                        onClick = {
                            isCheckingRoot = true
                            coroutineScope.launch {
                                val hasRoot = withContext(Dispatchers.IO) {
                                    try {
                                        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                                        process.waitFor() == 0
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                isRootAvailable = hasRoot
                                prefs.edit().putBoolean("root_available", hasRoot).apply()
                                if (hasRoot) {
                                    useRoot = true
                                    prefs.edit().putBoolean("use_root", true).apply()
                                }
                                isCheckingRoot = false
                            }
                        },
                        enabled = !isCheckingRoot,
                        modifier = Modifier
                            .padding(start = 48.dp, top = 8.dp)
                            .fillMaxWidth()
                    ) {
                        if (isCheckingRoot) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Request Root Access")
                        }
                    }
                }
            }
        }
        

        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How to use", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                InstructionItem("1", "Add the Darjeeling tile to your Quick Settings panel.")
                InstructionItem("2", "Tap repeatedly to cycle timers: 5m, 10m, 15m, 30m, Unlimited, Off.")
                InstructionItem("3", "If the screen turns off naturally or via the power button, Darjeeling is automatically disabled.")
            }
        }
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Credits", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Developed by: Kanagawa Yamada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun InstructionItem(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}